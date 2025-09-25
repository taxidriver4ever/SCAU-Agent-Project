package org.example.agentprojectjava.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import jakarta.annotation.Resource;
import org.example.agentprojectjava.pojo.ChatMessage;
import org.example.agentprojectjava.pojo.NormalResult;
import org.example.agentprojectjava.pojo.VoiceMessage;
import org.example.agentprojectjava.service.VoiceToAiService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@CrossOrigin
@RestController
public class VoiceToAiController {

    @Resource
    private VoiceToAiService voiceToAiService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final ConcurrentHashMap<String, Disposable> userSessions = new ConcurrentHashMap<>();

    @Resource
    private RedissonClient redisson;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @MessageMapping("/VoiceChat.StopService")
    public NormalResult StopService(VoiceMessage voiceMessage) throws InterruptedException {
        String userId = voiceMessage.getUserId();
        RLock lock = redisson.getLock("lock" + userId);
        boolean b = lock.tryLock(5, TimeUnit.SECONDS);
        if(b) {
            try {
                Disposable streamDisposable = userSessions.get(userId);
                if (streamDisposable != null && !streamDisposable.isDisposed()) {
                    streamDisposable.dispose();
                    System.out.println("Previous stream disposed");
                }
                // 发送结束消息
                System.out.println("Stream disposed");
                VoiceMessage voiceMessageEnd = new VoiceMessage();
                voiceMessageEnd.setContent("");
                voiceMessageEnd.setAvatar("");
                messagingTemplate.convertAndSendToUser(
                        userId,
                        "/queue/VoiceChat.private",
                        voiceMessageEnd,
                        createMessageHeaders());
                return NormalResult.success();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
        return NormalResult.error("Fail to stop service");
    }



    @MessageMapping("/VoiceChat.private")
    public void privateMessage(@Payload VoiceMessage voiceMessage,
                               Principal principal) throws InterruptedException, NoSuchAlgorithmException {
        RLock lock = redisson.getLock("VoiceChat" + principal.getName());
        boolean b = lock.tryLock(5, TimeUnit.SECONDS);

        String name = principal.getName();
        Disposable streamDisposable = userSessions.get(name);

        if(b) {

            try {

                if (streamDisposable != null && !streamDisposable.isDisposed()) {
                    streamDisposable.dispose();
                    System.out.println("Previous stream disposed");
                }

                if(voiceMessage.getContent().equals("暂停。") || voiceMessage.getContent().equals("Stop。")) {
                    // 发送结束消息
                    VoiceMessage voiceMessageEnd = new VoiceMessage();
                    voiceMessageEnd.setContent("");
                    voiceMessageEnd.setAvatar("");
                    messagingTemplate.convertAndSendToUser(
                            principal.getName(),
                            "/queue/VoiceChat.private",
                            voiceMessageEnd,
                            createMessageHeaders());
                    return;
                }

                voiceToAiService.sendToAi(voiceMessage);

                // 发送原始消息
                messagingTemplate.convertAndSendToUser(
                        name,
                        "/queue/VoiceChat.private",
                        voiceMessage,
                        createMessageHeaders());

                WebClient client = WebClient.builder()
                        .baseUrl("http://localhost:7999")
                        .clientConnector(new ReactorClientHttpConnector(
                                HttpClient.create()
                                        .responseTimeout(Duration.ofSeconds(30))
                                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        ))
                        .build();

                String sessionId = UUID.randomUUID().toString();
                // 原有的MD5逻辑
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(sessionId.getBytes(StandardCharsets.UTF_8));
                ByteBuffer buffer = ByteBuffer.wrap(hash, 0, 8);

                String requestBody = String.format("{\"query\":\"%s\",\"stream\":%b,\"session_id\":%d}",
                        voiceMessage.getContent(), true, buffer.getLong());

                Flux<String> stringFlux = client.post()
                        .uri("/query_with_intent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .timeout(Duration.ofSeconds(5 * 60))
                        .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))) // 重试3次，每次间隔2秒
                        .onErrorResume(e -> Flux.just("Error: " + e.getMessage()));

                String initialAvatar = voiceMessage.getAvatar();
                AtomicReference<String> avatarRef = new AtomicReference<>(initialAvatar);
                streamDisposable = stringFlux.subscribe(
                        (data) -> {
                            if (data.startsWith("Error:")) {
                                handleError(data);
                            } else {
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(data);
                                    System.out.println(jsonNode.toString());
                                    if ("\"content\"".equals(jsonNode.get("type").toString())) {
                                        String newAvatar = jsonNode.get("avatar").asText();
                                        System.out.println(newAvatar);
                                        avatarRef.set(newAvatar);
                                        System.out.println("Avatar updated to: " + newAvatar);
                                        processResponseData(jsonNode, principal, avatarRef.get());
                                    }
                                } catch (InterruptedException | JsonProcessingException e) {
                                    Thread.currentThread().interrupt();
                                    throw new RuntimeException("Error processing stream data", e);
                                }
                            }
                        },
                        error -> {
                            System.err.println("Fatal error: " + error.getMessage());
                        },
                        () -> {
                            System.out.println("Stream processing finished successfully");
                            // 发送结束消息
                            VoiceMessage voiceMessageEnd = new VoiceMessage();
                            voiceMessageEnd.setContent("");
                            voiceMessageEnd.setAvatar("");
                            messagingTemplate.convertAndSendToUser(
                                    principal.getName(),
                                    "/queue/VoiceChat.private",
                                    voiceMessageEnd,
                                    createMessageHeaders());
                        }
                );

                userSessions.put(name, streamDisposable);
            } catch (MessagingException | NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
    }

    private void processResponseData(JsonNode jsonNode,Principal principal,String avatar) throws InterruptedException, JsonProcessingException {
        // 使用局部变量避免竞争
        JsonNode delta = jsonNode.get("delta");

        if(delta == null) return;

        VoiceMessage voiceMessageAi = new VoiceMessage();  // 每次创建新对象
        voiceMessageAi.setContent(delta.asText().replaceAll("\\[.*?\\]", ""));
        voiceMessageAi.setAvatar(avatar.replaceAll("\\s+", ""));
        // 定义正则表达式模式：匹配中括号内的内容（不包括中括号本身）
        Pattern pattern = Pattern.compile("\\[(.*?)\\]");
        Matcher matcher = pattern.matcher(delta.asText());

        String string = "";
        // 查找所有匹配的内容
        while (matcher.find()) {
            string = matcher.group(1); // group(1) 获取第一个捕获组的内容
        }

        if(!string.isEmpty()) voiceMessageAi.setImage("/src/images" + string.substring(1));

        messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/VoiceChat.private",
                voiceMessageAi,
                createMessageHeaders());

        TimeUnit.MILLISECONDS.sleep(voiceMessageAi.getContent().length() * 270L);
        // 可以解析JSON、更新UI、存储数据等
    }

    private void handleError(String errorMessage) {
        System.err.println("Stream error: " + errorMessage);
    }

    // 创建包含跟踪信息的消息头
    private MessageHeaders createMessageHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("message-id", UUID.randomUUID().toString());
        headers.put("timestamp", System.currentTimeMillis());
        headers.put("tracking", true);
        return new MessageHeaders(headers);
    }

}
