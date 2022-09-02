package com.nowcoder.community;


import com.nowcoder.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * 这是一个关于事务功能的测试类
 * 这里再解释一下相关注解的意思，我们要进行一个功能的单元测试，在没有出现测试框架之前
 *     我们怎么做测试呢？单独写main方法，有了测试框架后，我们的测试依赖于
 *     JUnit
 *  Spring Test & Spring Boot Test
 *  AssertJ
 *  Hamcrest
 *  Mockito
 *  这些类库再没有springboot框架之前需要一个一个的导入，但是有了spring框架之后只需要
 *  再pom.xml中配置一个依赖即可spring-boot-starter-test
 */

//@SpringBootTest该注解启动测试服务
//    @RunWith指定运行器可以是JUnit4.class等等

@SpringBootTest
@RunWith(SpringRunner.class)
public class TranssactionTests {
    @Autowired
    private AlphaService alphaService;

    @Test
    public void testSave1()
    {
        Object obj=alphaService.save1();
        System.out.println(obj);
    }

    @Test
    public void testSave2()
    {
        Object obj=alphaService.save2();
        System.out.println(obj);
    }
}
