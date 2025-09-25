package org.example.agentprojectjava;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.client.RestTemplate;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
class AgentProjectJavaApplicationTests {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Test
    void contextLoads() {
        String text = "如果仍无法确定路线，可参考校园内的指示牌或联系保卫处（电话：85286985）获取实时帮助。同时，部分场馆的缴费二维码[./processed_images/体育场馆预约的攻略指南.docx_image_2.jpg]可能标注在入口处，方便用户快速完成支付后进入场地。";

        String result = text.replaceAll("\\[.*?\\]", "");
        // 定义正则表达式模式：匹配中括号内的内容（不包括中括号本身）
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(text);

        // 查找所有匹配的内容
        while (matcher.find()) {
            String content = matcher.group(1); // group(1) 获取第一个捕获组的内容
            String string = "/src/images" + content.substring(1);
            System.out.println(string);
        }

        System.out.println(result);


    }

}
