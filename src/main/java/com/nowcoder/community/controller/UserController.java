package com.nowcoder.community.controller;


import com.nowcoder.community.annotation.LoginRequired;
import com.nowcoder.community.entity.User;
import com.nowcoder.community.service.FollowService;
import com.nowcoder.community.service.LikeService;
import com.nowcoder.community.service.UserService;
import com.nowcoder.community.util.CommunityConstant;
import com.nowcoder.community.util.CommunityUtil;
import com.nowcoder.community.util.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.Multipart;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    //我们需要获取当前用户是谁，怎么获取，通过HostHolder
    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;
    
    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @LoginRequired
    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    public String getSettingPage(Model model)
    {
        //生成七牛云服务器的上传凭证
        String fileName=CommunityUtil.generateUUID();
        StringMap policy=new StringMap();
        policy.put("returnBody",CommunityUtil.getJSONString(0));
        //生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);

        //此处的凭证上传，七牛云为我们提供了几种方式
        /*
        第一种：简单上传凭证 String upToken = auth.uploadToken(bucket);
         */

        /*
        第二种：覆盖上传除了需要简单上传所需要的信息之外，还需要想进行覆盖的文件名称，
      这个文件名称同时可是客户端上传代码中指定的文件名，两者必须一致。
      String upToken = auth.uploadToken(bucket, key);
         */

        /*
        第三种：自定义上传回复的凭证
        默认情况下，文件上传到七牛之后，在没有设置returnBody或者回调相关的参数情况下，七牛返回给上传端的回复格式为hash和key
        我们希望能自定义这个返回的JSON格式的内容，可以通过设置returnBody参数来实现
         */
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600, policy);

        //将上传的凭证直接保留给网页前端
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    // 更新头像路径
    @RequestMapping(path = "/header/url", method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空!");
        }
        String url = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), url);
        return CommunityUtil.getJSONString(0);
    }
    @RequestMapping(path = "/modify", method = RequestMethod.GET)
    public String getmodifyPage() {
        return "/site/setting";
    }
    @LoginRequired
    @RequestMapping(path = "/modify", method = RequestMethod.POST)
    public String modifyPassword(@RequestParam(value = "old-password") String oldPassword,
                                 @RequestParam(value = "new-password") String newPassword, Model model,
                                 @CookieValue("ticket") String ticket) {
        if (StringUtils.isBlank(oldPassword)) {
            model.addAttribute("oldPassMsg", "原始密码不能为空");
            return "/site/setting";
        }
        if (StringUtils.isBlank(newPassword)) {
            model.addAttribute("newPassMsg", "新密码不能为空");
            return "/site/setting";
        }
        User user = hostHolder.getUser();
        String password = CommunityUtil.md5(oldPassword + user.getSalt());
        if (password.equals(user.getPassword())) {
            userService.modify(newPassword, user);
            userService.logout(ticket);
        } else {
            model.addAttribute("oldPassMsg", "请输入正确的密码");
            return "/site/setting";
        }
        //返回重新登录页面,一定要注意这里要用重定向，而不能使用 return "/site/login";这样会出现
        return "redirect:/login";
    }

    //废弃
    @LoginRequired
    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片");
            //回到要上传的页面
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确");
            return "/site/setting";
        }
        //生成随机文件名
        fileName = CommunityUtil.generateUUID() + suffix;

        //文件存放路径
        File dest = new File(uploadPath + "/" + fileName);
        //存储文件
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败:" + e.getMessage());
            throw new RuntimeException("上传文件失败，服务器异常!", e);
        }
        //更新当前用户的头像的外部访问路径
        // 更新当前用户的头像的路径(web访问路径)
        // http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headerUrl);
        //重定向到首页
        return "redirect:/index";
    }

    //废弃
    //这里的路径要和上传时设置的路径一致，不能乱写
    @RequestMapping(path = "/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 服务器存放路径
        fileName = uploadPath + "/" + fileName;
        // 文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        // 响应图片
        response.setContentType("image/" + suffix);
        try (
                //读入流
                FileInputStream fis = new FileInputStream(fileName);
                //输出流，springmvc会自动将其关闭
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败: " + e.getMessage());
        }
    }

    //个人主页,可以显示任意用户的主页，包括当前登录人的主页，以及通过头像点击获得别人的主页
    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId,Model model)
    {
        User user=userService.findUserById(userId);
        if(user==null)
        {
            throw new RuntimeException("该用户不存在！");
        }
        //用户
        model.addAttribute("user",user);
        //点赞
        int likeCount=likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        //注意当前用户可不是userId,当前用户是在hostholder中的
        //关注数量（注意当前的运作背景是在某个人的个人主页下，故entityType我们是已知的）
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);
        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
         model.addAttribute("followerCount",followerCount);
        //当前用户是否已关注该实体
        boolean hasFollowed=false;
        if(hostHolder.getUser()!=null)
        {
            hasFollowed = followService.hasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }
}
