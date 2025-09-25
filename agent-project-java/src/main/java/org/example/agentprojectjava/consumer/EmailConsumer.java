package org.example.agentprojectjava.consumer;

import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.agentprojectjava.dao.LoginAndRegisterRedis;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Random;

@Component
@RabbitListener(queues = "email.direct.queue")
public class EmailConsumer {

    @Autowired
    private JavaMailSender mailSender;

    @Resource
    private LoginAndRegisterRedis loginAndRegisterRedis;

    // 关键：方法参数中需要添加 org.springframework.amqp.core.Message 和 Channel
    @RabbitHandler
    public void receiveMessage(String idWithEmail,
                               Channel channel,
                               org.springframework.amqp.core.Message amqpMessage) throws IOException, InterruptedException { // 添加 throws IOException

        String userEmail = idWithEmail.split(":")[1];

        long deliveryTag = amqpMessage.getMessageProperties().getDeliveryTag(); // 获取消息的唯一标签
        boolean success = false;

        try {
            MimeMessagePreparator preparation = getMimeMessagePreparator(idWithEmail);

            mailSender.send(preparation);
            System.out.println("邮件发送成功至: " + userEmail);
            success = true; // 标记处理成功

        } catch (MailException e) {
            System.err.println("邮件发送失败: " + userEmail + ", 错误: " + e.getMessage());
            // 处理失败，可以选择重试或拒绝消息
        } finally {
            // 无论如何，最终都要进行确认操作
            if (success) {
                // 成功处理：手动确认消息，single = false 表示不批量确认
                channel.basicAck(deliveryTag, false);
                System.out.println("消息已确认 (ACK)");
            } else {
                // 处理失败：拒绝消息。
                // requeue = false 表示不重新放回队列，如果配置了死信队列，消息会进入死信队列
                // requeue = true 表示重新放回队列，可能会再次被消费（慎用，容易造成无限循环）
                channel.basicNack(deliveryTag, false, false);
                System.out.println("消息已拒绝 (NACK)，不重新入队");
            }
        }
    }

    private MimeMessagePreparator getMimeMessagePreparator(String idWithEmail) throws InterruptedException {
        String userEmail = idWithEmail.split(":")[1];
        long id = Long.parseLong(idWithEmail.split(":")[0]);
        StringBuilder res = new StringBuilder();
        Random random = new Random();
        for(int i = 0; i < 6; i++) {
            res.append(random.nextInt(10));
        }
        String theRes = res.toString();
        if(!(id == 0)) loginAndRegisterRedis.storeVerificationCode(id,theRes);
        else loginAndRegisterRedis.storeVerificationCodeForRegister(userEmail,theRes);
        return new MimeMessagePreparator() {
            @Override
            public void prepare(MimeMessage mimeMessage) throws Exception {
                mimeMessage.setRecipient(Message.RecipientType.TO,
                        new InternetAddress(userEmail));
                mimeMessage.setFrom(new InternetAddress("3887768494@qq.com"));
                mimeMessage.setText("您的验证码是" + theRes + "(有效期为5分钟)");
                mimeMessage.setSubject("验证码通知");
            }
        };
    }
}