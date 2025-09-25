package org.example.agentprojectjava.interceptor;

import jakarta.annotation.Resource;
import lombok.Data;
import org.example.agentprojectjava.dao.LoginAndRegisterRedis;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.logging.Logger;

@Component
public class StompHeaderInterceptor implements ChannelInterceptor {

    @Resource
    private LoginAndRegisterRedis loginAndRegisterRedis;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从STOMP头信息中获取用户数据
            String userId = accessor.getFirstNativeHeader("id");
            String uuid = accessor.getFirstNativeHeader("uuid");

            if(userId != null && uuid != null) {

                boolean b = loginAndRegisterRedis.existsIdAndUuid(userId, uuid);

                if(b) {
                    System.out.println("拦截到CONNECT请求 - id: " + userId + ", uuid: " + uuid);

                    if (userId != null && !userId.trim().isEmpty()) {
                        // 创建并设置Principal
                        accessor.setUser(new SimplePrincipal(userId, uuid));
                        System.out.println("已设置Principal: " + userId);
                    }
                }
            }
        }

        return message;
    }

    @Data
    public static class SimplePrincipal implements Principal {
        private final String userId;
        private final String uuid;

        public SimplePrincipal(String userId, String uuid) {
            this.userId = userId;
            this.uuid = uuid;
        }

        @Override
        public String getName() {
            return userId;
        }

        @Override
        public String toString() {
            return "SimplePrincipal{userId='" + userId + "', uuid='" + uuid + "'}";
        }
    }
}