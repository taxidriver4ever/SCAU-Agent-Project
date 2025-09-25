package org.example.agentprojectjava.service.imp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import jakarta.annotation.Resource;
import org.example.agentprojectjava.dao.ChatWithAiRedis;
import org.example.agentprojectjava.mapper.ChatWithAiMapper;
import org.example.agentprojectjava.pojo.ChatContent;
import org.example.agentprojectjava.pojo.HistoryMessage;
import org.example.agentprojectjava.service.ChatWithAiService;
import org.mybatis.logging.Logger;
import org.mybatis.logging.LoggerFactory;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class ChatWithAiServiceImp implements ChatWithAiService {

    @Resource
    private ChatWithAiMapper chatWithAiMapper;

    @Resource
    private RedissonClient redisson;

    @Resource
    private ChatWithAiRedis chatWithAiRedis;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String SaveHistoryMessage(HistoryMessage historyMessage) throws InterruptedException {
        RLock lock = redisson.getLock("chatWithAiService");
        boolean b = lock.tryLock(5, 30, TimeUnit.SECONDS);
        if (b) {
            try {
                String uuid;
                if(historyMessage.getSessionId() == null) {
                    uuid = UUID.randomUUID().toString();
                    historyMessage.setSessionId(uuid);
                } else {
                    uuid = historyMessage.getSessionId();
                    if(!chatWithAiRedis.existsSession(uuid)
                            && chatWithAiMapper.SearchHistoryId(uuid).isEmpty())
                        return "Deleted";
                }
                chatWithAiMapper.SaveHistoryMessage(historyMessage);
                if(chatWithAiRedis.SaveHistoryMessage(historyMessage)) return uuid;
                else return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
        return null;
    }

    @Override
    public CopyOnWriteArrayList<ChatContent> GetHistoryMessages(String uuid) throws InterruptedException {
        CopyOnWriteArrayList<ChatContent>chatContents = chatWithAiRedis.GetHistoryMessage(uuid);
        if(chatContents.isEmpty()) {
            int a = 0;
            CopyOnWriteArrayList<HistoryMessage> historyMessages = chatWithAiMapper.getHistoryMessages(uuid);
            for(HistoryMessage historyMessage : historyMessages) {
                ChatContent chatContent = new ChatContent();
                chatContent.setContent(historyMessage.getHistoryMessage());
                chatContent.setAvatar(historyMessage.getAvatar());
                chatContent.setStatus(historyMessage.getStatus());
                if(a == 0) {
                    chatContent.setCreateTime(historyMessage
                            .getCreateTime()
                            .plusHours(8)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    );
                    a++;
                }
                chatContents.add(chatContent);
            }
        }
        return chatContents;
    }

    @Override
    public boolean AddNewSession(HistoryMessage historyMessage) throws InterruptedException {
        if(historyMessage.getHistoryMessage().length() > 40)
            historyMessage.setHistoryMessage(historyMessage.getHistoryMessage().substring(0, 40));
        return chatWithAiRedis.AddSession(historyMessage);
    }

    @Override
    public CopyOnWriteArrayList<String> GetSessions(String userId) throws InterruptedException {
        return chatWithAiRedis.GetSessions(userId);
    }

    @Override
    public String GetUUid(String userId, String index) throws InterruptedException {
            String string = chatWithAiRedis.GetUUid(userId, index);
        return string.substring(0,36);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean RemoveSession(String sessionId, String userId, String index) throws InterruptedException {
        if(chatWithAiRedis.RemoveSession(sessionId, userId, index)) {
            chatWithAiMapper.DeleteHistoryMessage(sessionId);
            return true;
        }
        return false;
    }

    @Override
    public boolean validateUser(String id, String uuid) {
        return id != null && uuid != null && chatWithAiRedis.existsIdAndUuid(id, uuid);
    }

    @Override
    public Flux<String> streamAIResponse(String message, String sessionId) throws NoSuchAlgorithmException {
        WebClient client = WebClient.builder()
                .baseUrl("http://localhost:8000")
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .responseTimeout(Duration.ofSeconds(30))
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                ))
                .build();

        // 原有的MD5逻辑
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(sessionId.getBytes(StandardCharsets.UTF_8));
        ByteBuffer buffer = ByteBuffer.wrap(hash, 0, 8);

        String requestBody = String.format("{\"query\":\"%s\",\"stream\":%b,\"session_id\":%d}",
                message, true, buffer.getLong());

        return client.post()
                .uri("/query_with_intent")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(5 * 60))
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))) // 重试3次，每次间隔2秒
                .onErrorResume(e -> Flux.just("Error: " + e.getMessage()));
    }

}
