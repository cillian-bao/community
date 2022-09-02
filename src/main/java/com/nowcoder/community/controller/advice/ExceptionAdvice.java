package com.nowcoder.community.controller.advice;

import com.nowcoder.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

//@ControllerAdvice用于修饰类，表示该类是Controller的全局配置类
//在此类中，可以对Controller进行如下三种全局配置：
//异常处理方案、绑定数据方案、绑定参数方案

//我们给其加annotations=Controller.class的目的在于限制其配置的范围
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class})
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //异常的概括信息
        logger.error("服务器发生异常: " + e.getMessage());
        //异常的详细的栈信息
        for (StackTraceElement element : e.getStackTrace()) {
            logger.error(element.toString());
        }
        //通过请求来判断是同步请求还是异步请求
        String xRequestedWith = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(xRequestedWith)) {
            //异步请求
            //我们返回普通的字符串
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器异常!"));
        }else {
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
