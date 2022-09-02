package com.nowcoder.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

//该类的作用就相当于一个客户端，将发送邮件的事情委托给新浪去做
@Component
public class MailClient {
private static final Logger logger=LoggerFactory.getLogger(MailClient.class);

//让Springboot自动帮我们注入JavaMailSender对象
//查看JavaMailSender的源码可以发现其中有一个 MimeMessage对象
//    这个MimeMessage对象就是我们要发送的邮件内容，我们需要将其构建出来
@Autowired
private JavaMailSender mailSender;

@Value("${spring.mail.username}")
private String from;

public void sendMail(String to,String subject,String content)
{
    try {
        MimeMessage message=mailSender.createMimeMessage();
        //利用MimeMessageHelper帮助我们构建邮件内容
        MimeMessageHelper helper=new MimeMessageHelper(message);
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
//        如果不加true这个参数，默认是普通文本，加入true,表示支持html文本
        helper.setText(content,true);
        mailSender.send(helper.getMimeMessage());
    } catch (MessagingException e) {
        logger.error("发送邮件失败："+e.getMessage());
    }
}
}
