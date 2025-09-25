package org.example.agentprojectjava.dao.imp;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.dao.ChatWithAiRedis;
import org.example.agentprojectjava.pojo.ChatContent;
import org.example.agentprojectjava.pojo.HistoryMessage;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;

@Repository
public class ChatWithAiRedisImp implements ChatWithAiRedis {

    @Resource
    private RedisTemplate<String,String> redisTemplate;

    @Resource
    private RedissonClient redisson;

    @Override
    public boolean SaveHistoryMessage(HistoryMessage message) throws InterruptedException {
        RLock lock = redisson.getLock("saveHistoryMessage");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                Long size = redisTemplate.opsForZSet().size("history:message:" + message.getSessionId());
                if (size != null && size > 50) {
                    redisTemplate.opsForZSet().removeRange("history:message:" + message.getSessionId()
                            , 0, 0);
                    redisTemplate.opsForZSet().removeRange("history:information:" + message.getSessionId()
                            , 0, 0);
                }
                long epochMilli = message.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                Boolean add = redisTemplate.opsForZSet().add(
                        "history:message:" + message.getSessionId(),
                        message.getHistoryMessage() + ":" + epochMilli,
                        epochMilli
                );
                Boolean add1 = redisTemplate.opsForZSet().add(
                        "history:information:" + message.getSessionId(),
                        message.getStatus() + ":" + message.getAvatar() + ":" + epochMilli,
                        epochMilli
                );
                return (add == Boolean.TRUE) && (add1 == Boolean.TRUE);
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
        return false;
    }

    @Override
    public CopyOnWriteArrayList<ChatContent> GetHistoryMessage(String uuid) throws InterruptedException {
        Set<String> rangeInformation = redisTemplate.opsForZSet().range("history:information:" + uuid, 0, -1);
        Set<String> rangeMessage = redisTemplate.opsForZSet().range("history:message:" + uuid, 0, -1);
        if (rangeMessage != null && rangeInformation != null) {
            CopyOnWriteArrayList<ChatContent> chatContents = new CopyOnWriteArrayList<>();
            int i = 0;
            for (String stringInformation : rangeInformation) {
                ChatContent chatContent = getChatContent(stringInformation);
                if(i == 0) {
                    long score = Objects
                            .requireNonNull(redisTemplate.opsForZSet()
                            .score("history:information:" + uuid, stringInformation))
                            .longValue();
                    String format = Instant.ofEpochMilli(score)
                            .atZone(ZoneId.of("Asia/Shanghai"))
                            .plusHours(8)
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    chatContent.setCreateTime(format);
                    i++;
                }
                chatContents.add(chatContent);
            }
            i = 0;
            for(String stringMessage : rangeMessage) {
                chatContents.get(i).setContent(stringMessage.substring(0, stringMessage.length() - 14));
                i++;
            }
            return chatContents;
        }
        return null;
    }

    @Override
    public boolean AddSession(HistoryMessage message) throws InterruptedException {
        RLock lock = redisson.getLock("addSession");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                redisTemplate.opsForZSet().add("history:id:" + message.getUserId(),
                        message.getSessionId() + ":" + message.getHistoryMessage(),
                        message.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
        return false;
    }

    @Override
    public CopyOnWriteArrayList<String> GetSessions(String userId) throws InterruptedException {
        Set<String> range = redisTemplate.opsForZSet().reverseRange("history:id:" + userId, 0, -1);
        if (range != null) {
            CopyOnWriteArrayList<String> stringsInformation = new CopyOnWriteArrayList<>();
            for (String titleWithUUID : range)
                stringsInformation.add(titleWithUUID.substring(37));
            return stringsInformation;
        }
        return null;
    }

    @Override
    public String GetUUid(String userId, String index) throws InterruptedException {
        Set<String> range = redisTemplate.opsForZSet().reverseRange("history:id:" + userId, Long.parseLong(index), Long.parseLong(index));
        if (range != null) return range.iterator().next();
        return null;
    }

    @Override
    public boolean RemoveSession(String sessionId, String userId, String index) throws InterruptedException {
        RLock lock = redisson.getLock("removeSession");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                long l = Long.parseLong(index);
                Long size = redisTemplate.opsForZSet().size("history:id:" + userId);
                if (size != null) {
                    size--;
                    redisTemplate.opsForZSet().removeRange("history:id:" + userId, size - l, size - l);
                    redisTemplate.delete("history:information:" + sessionId);
                    redisTemplate.delete("history:message:" + sessionId);
                    return true;
                }
            } catch (NumberFormatException e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
        return false;
    }

    @Override
    public boolean existsIdAndUuid(String id, String uuid) {
        return Objects.equals(redisTemplate.opsForValue().get("login:uuid:" + id), uuid);
    }

    @Override
    public boolean existsSession(String sessionId) {
        return redisTemplate.hasKey("history:message:" + sessionId);
    }

    private static ChatContent getChatContent(String stringInformation) {
        String substringInformation = stringInformation.substring(0, stringInformation.length() - 14);
        ChatContent chatContent = new ChatContent();
        chatContent.setAvatar(substringInformation.split(":")[1]);
        chatContent.setStatus(substringInformation.split(":")[0]);
        return chatContent;
    }
}
