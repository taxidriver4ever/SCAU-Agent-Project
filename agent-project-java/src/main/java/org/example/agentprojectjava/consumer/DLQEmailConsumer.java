package org.example.agentprojectjava.consumer;

import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@RabbitListener(queues = "dlx.email.queue") // 监听死信队列
public class DLQEmailConsumer {

    private static final Logger logger = LoggerFactory.getLogger(DLQEmailConsumer.class);

    @RabbitHandler
    public void processFailedMessage(String failedMessage) {
        // 这里处理失败的消息，例如记录错误日志、发送警报、存入数据库等
        logger.error("邮件发送失败，消息进入死信队列。原始消息内容: {}", failedMessage);

        // 可以根据需要添加更多的处理逻辑，比如：
        // 1. 将失败信息存入数据库
        // 2. 发送通知给管理员
        // 3. 尝试其他备用发送方式
        System.err.println("需要人工干预的失败邮件: " + failedMessage);
    }
}