package com.nowcoder.community.controller;

import ch.qos.logback.core.joran.spi.ElementSelector;
import com.google.code.kaptcha.Producer;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

//不加@requestMapping注解访问的直接就是这个方法
@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger= LoggerFactory.getLogger(LoginController.class);
    @Autowired
    private UserService userService;
    @RequestMapping(path="/register",method= RequestMethod.GET)
    public String getRegisterPage(){
        return "/site/register";
    }
    //浏览器向我们提交数据，所以需要使用POST请求
   // 只要页面传入的值与我们的User属性相匹配，springboot就会自动注入

    //自动注入Kaptcha生成对象
    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    //注入项目路径
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @RequestMapping(path="/register",method= RequestMethod.POST)
    public String register(Model model, User user)
    {
        Map<String,Object> map=userService.register(user);
        if(map==null || map.isEmpty())
        {
//            为什么跳转过来就能发送邮件，因为在上面的userService.register中就进行了操作
            model.addAttribute("msg","注册成功，我们已经向您的邮箱发送了激活邮件，请尽快激活");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }
        else
        {
            //注册不成功时，将错误信息返回
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/site/register";
        }
    }

    @RequestMapping(path="/activation/{userId}/{code}",method=RequestMethod.GET)
    //通过@PathVariable注解获取链接中的数据
    public String activation(Model model, @PathVariable("userId") int userId,@PathVariable("code") String code)
    {
        int result=userService.activation(userId,code);
        if(result==ACTIVATION_SUCCESS)
        {
            //成功直接跳转到登录页面
            model.addAttribute("msg","激活成功，您的账号已经可以正常使用了");
//            /login是我们设置的登录页面
            model.addAttribute("target","/login");

        } else if (result==ACTIVATION_REPEAT) {
//            失败则跳转到首页
            model.addAttribute("msg","操作无效，该账号已经激活");
//            /login是我们设置的登录页面
            model.addAttribute("target","/index");
        }
        else{
            // 失败则跳转到首页
            model.addAttribute("msg","激活失败，您提供的激活码不正确");
//            /login是我们设置的登录页面
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }

//    只是单纯的得到页面，不需要用POST
    @RequestMapping(path="/login",method= RequestMethod.GET)
    public String getLoginPage(){
        return "/site/login";
    }
    @RequestMapping(path="/kaptcha",method=RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/)
    {
        // 生成验证码
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        //验证码的归属
        String kaptchaOwner= CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        //验证码存入redis
        String redisKey= RedisKeyUtil.getKaptchaKey(kaptchaOwner);

        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);

        // 将验证码存入session
//        session.setAttribute("kaptcha", text);
        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }
//    和上面的一个方法的路径一样，但是提交方法不能也一样，否则会引起冲突
//    需要model的原因在于需要返回数据给前端、需要session的原因在于需要从中获取图片验证码
//    需要HttpReponse的原因在于我们需要将凭证通过cookie发送给客户端
    @RequestMapping(path="/login",method=RequestMethod.POST)
    public String login(String username,String password,String code,boolean rememberme,
                        Model model/*,HttpSession session*/,HttpServletResponse response,@CookieValue("kaptchaOwner") String kaptchaOwner)
    {
        //首先判断图片验证码，为空则将错误信息加入model中用于回传
//        String kaptcha= (String) session.getAttribute("kaptcha");
        String kaptcha=null;
        if(StringUtils.isNotBlank(kaptchaOwner))
        {
            String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha= (String) redisTemplate.opsForValue().get(kaptchaKey);
        }
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code)||!kaptcha.equalsIgnoreCase(code))
        {
            model.addAttribute("codeMsg","验证码不正确");
            return "/site/login";
        }
        //用户是否点击记住我，这将决定其cookie信息的保存时间
        int expiredSeconds=rememberme?REMEMBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
        //生成凭证
        Map<String,Object> map=userService.login(username,password,expiredSeconds);
        if(map.containsKey("ticket"))
        {
            Cookie cookie=new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }
        else {
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path="/logout",method=RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket)
    {
        userService.logout(ticket);
        return "redirect:/login";
    }
}
