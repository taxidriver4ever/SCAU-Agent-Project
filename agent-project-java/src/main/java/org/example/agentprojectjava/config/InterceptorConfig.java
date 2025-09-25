package org.example.agentprojectjava.config;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.interceptor.LoggedInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {

    @Resource
    private LoggedInterceptor loggedInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggedInterceptor)
                .excludePathPatterns("/login/**")
                .excludePathPatterns("/register/**")
                .excludePathPatterns("/ai/stream")
                .excludePathPatterns("/ws-chat")
                .excludePathPatterns("/app/chat.private");
    }
}
