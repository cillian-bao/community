package com.nowcoder.community;


import com.nowcoder.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

//用于测试MailClient
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {

//    利用springboot自动注入MailClient这个类的对象

    @Autowired
    private MailClient mailClient;

//    thymeleaf中有一个重要的模板引擎类也被springboot自动管理起来了
//    我们直接注入进行使用
    @Autowired
    private TemplateEngine templateEngine;
    @Test
    public void testTextMail(){
        mailClient.sendMail("bzacillian@163.com","TEXT","welcome");
    }

    @Test
    public void testHtmlMail(){
        Context context=new Context();
        context.setVariable("username","sunday");

        //通过模板引擎处理获得邮件内容
        String content=templateEngine.process("/mail/demo",context);
        System.out.println(content);

        mailClient.sendMail("bzacillian@163.com","HTML",content);
    }

}
