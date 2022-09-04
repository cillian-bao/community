package com.nowcoder.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.swing.*;
import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class Wktests {
    @Value("${wk.image.command}")
    private String commandlocate;
    @Value("${wk.image.storage}")
    private String imageStorage;

    @Test
    public void TestWk() {
        String command=commandlocate+" --quality 75 https://www.zhihu.com d:/work/data/wk-images/3.png";
        try {
            Runtime.getRuntime().exec(command);
            System.out.println("ok");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
