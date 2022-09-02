package com.nowcoder.community.service;

import com.nowcoder.community.dao.LoginTicketMapper;
import com.nowcoder.community.dao.UserMapper;
import com.nowcoder.community.entity.LoginTicket;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.MailClient;
import com.nowcoder.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisAccessor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    @Autowired
    private UserMapper userMapper;

    //发邮件需要注入客户端以及模板引擎
    @Autowired
    private MailClient mailClient;
    @Autowired
    private TemplateEngine templateEngine;

//    @Autowired
//    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    //发邮件要生成激活码，激活码要包含域名以及项目名,从application.properties中查找
    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id) {

//        return userMapper.selectById(id);
        User user = getCache(id);
        if(user==null)
        {
            user=initCache(id);
        }
        return user;
    }

    public Map<String, Object> register(User user)
    {
        Map<String,Object> map=new HashMap<>();

        //空值处理
        if(user==null)
        {
            throw new IllegalArgumentException("参数不能为空");
        }
        if(StringUtils.isBlank(user.getUsername()))
        {
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword()))
        {
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail()))
        {
            map.put("emailMsg","账号不能为空");
            return map;
        }

        //验证账号在数据中是否存在
        User u = userMapper.selectByName(user.getUsername());
        if (u != null) {
            map.put("usernameMsg", "该账号已存在!");
            return map;
        }

        // 验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u != null) {
            map.put("emailMsg", "该邮箱已被注册!");
            return map;
        }
        //在上述信息都不为空的情况下，我们就可以进行注册

        //对用户密码进行加密，Salt随机，MD5加密
        // 注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));
        user.setType(0);
//        刚注册的时候设置为0，表示还未激活，在下面的激活业务执行之后将会改成1
        user.setStatus(0);

//        将随机生成的字符串作为激活码来使用
        user.setActivationCode(CommunityUtil.generateUUID());

//        牛客网中有1001个随机的头像
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());

//        将其添加到数据库中
        userMapper.insertUser(user);

        // 激活邮件
        Context context = new Context();
        context.setVariable("email", user.getEmail());
        // http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);
//        通过TemplateEngine 和Context 的配合，我们可以使用thymeleaf模版来生产html文件。
        //其中/mail/activation是一个模板页面，Context是给thymeleaf模版提供变量的。
        String content = templateEngine.process("/mail/activation", context);
//        subject是邮件的标题
        mailClient.sendMail(user.getEmail(), "激活账号", content);
        return map;
    }

    public int activation(int userId,String code)
    {
        User user=userMapper.selectById(userId);
        if(user.getStatus()==1)
        {
            return ACTIVATION_REPEAT;
        }
        else if(user.getActivationCode().equals(code))
        {
            userMapper.updateStatus(userId,1);
            clearCache(userId);
            return ACTIVATION_SUCCESS;
        }
        else{
            return ACTIVATION_FAILURE;
        }
    }

    /**
     * 由于我们在登录的时候会有多种情况发生，可能成功也可能不成功
     * 可能失败的原因可能有多种情况，用户不存在、用户密码输入不对等等
     * 所以我们采用返回Map这个数据结构处理这样多结果的情况
     */
//    md5加密算法只要你给的字符串是一定的，那么其生成的加密字符串也是一致的
    public Map<String,Object> login(String username,String password,int expiredSeconds)
    {
        HashMap<String,Object> map=new HashMap<>();
//   分别需要处理的业务
        /**
         * 1. 判断是用户名是否为空
         */
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "账号不能为空!");
            return map;
        }
        /**
         * 2.判断密码是否为空
         */
        if (StringUtils.isBlank(password)) {
            map.put("passwordMsg", "密码不能为空!");
            return map;
        }
        /**
         * 3.判断用户名是否存在，不存在的话返回map
         */
        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "该账号不存在!");
            return map;
        }
        /**
         * 4.判断用户的状态是否激活，未激活返回map
         */
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "该账号未激活!");
            return map;
        }
        /**
         * 判断密码（先加密，和数据库里的比较）
         */
        password = CommunityUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "密码不正确!");
            return map;
        }
        /**
         * 生成登录凭证
         */
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
//        loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);
        map.put("ticket", loginTicket.getTicket());
        return map;
    }
    public void logout(String ticket)
    {
//        loginTicketMapper.updateStatus(ticket,1);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket= (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        //注意设置状态后重新返回。
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }


    public LoginTicket findLoginTicket(String ticket)
    {
//       return loginTicketMapper.selectByTicket(ticket);
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket= (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        return loginTicket;
    }

    public void modify(String newPassword,User user)
    {
        String password=CommunityUtil.md5(newPassword + user.getSalt());
         userMapper.updatePassword(user.getId(),password);
         clearCache(user.getId());
    }

    public int updateHeader(int userId,String headerUrl)
    {
        int rows=userMapper.updateHeader(userId,headerUrl);
        clearCache(userId);
        return rows;
    }

    public User findUserByName(String toName)
    {
        return userMapper.selectByName(toName);
    }
    //做用户缓存，
    // 1.优先从缓存中取值
    private User getCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    // 2.取不到时初始化缓存数据
    private User initCache(int userId) {
        User user = userMapper.selectById(userId);
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据
    private void clearCache(int userId) {
        String redisKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();
        list.add(new GrantedAuthority() {

            @Override
            public String getAuthority() {
                switch (user.getType()) {
                    case 1:
                        return AUTHORITY_ADMIN;
                    case 2:
                        return AUTHORITY_MODERATOR;
                    default:
                        return AUTHORITY_USER;
                }
            }
        });
        return list;
    }
}
