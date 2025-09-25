package org.example.agentprojectjava.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("direct_email_exchange",true,false);
    }

    @Bean
    public Queue emailQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置死信交换机
        args.put("x-dead-letter-exchange", "dlx.email.exchange");
        // (可选) 设置死信路由键，不设置则使用原消息的路由键
        args.put("x-dead-letter-routing-key", "dead.letter");
        // (可选) 设置消息TTL(毫秒)，超过此时间未消费的消息会变成死信
        args.put("x-message-ttl", 60000); // 例如 1分钟

        return new Queue("email.direct.queue", true, false, false, args);
    }

    // 定义死信交换机 (DLX)
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.email.exchange", true, false);
    }

    // 定义死信队列 (DLQ)
    @Bean
    public Queue dlxQueue() {
        return new Queue("dlx.email.queue", true, false, false);
    }

    // 绑定死信交换机和死信队列
    @Bean
    public Binding dlxBinding(DirectExchange dlxExchange, Queue dlxQueue) {
        return BindingBuilder.bind(dlxQueue)
                .to(dlxExchange)
                .with("dead.letter"); // 使用一个路由键
    }

    @Bean
    public Binding binding(DirectExchange directExchange, Queue emailQueue) {
        return BindingBuilder.bind(emailQueue).to(directExchange).with("verificationCode");
    }
}
