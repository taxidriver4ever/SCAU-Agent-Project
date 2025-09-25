package org.example.agentprojectjava.dao.imp;

import jakarta.annotation.Resource;
import org.example.agentprojectjava.dao.ChatRedis;
import org.example.agentprojectjava.pojo.HistorySession;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class ChatRedisImp implements ChatRedis {

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private RedissonClient redisson;

    @Override
    public boolean SelectFriendRequest(String receiverId, String userId) throws InterruptedException {
        RLock lock = redisson.getLock("FriendRequestLock");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                long epochMilli = LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long nowHour = epochMilli / (1000 * 60 * 60);
                Double score = redisTemplate.opsForZSet().score("friend:request:" + receiverId, "0:" + userId);
                if(score != null) {
                    long l = nowHour - score.longValue() / (1000 * 60 * 60);
                    if (l > 3 * 24) {
                        redisTemplate.opsForZSet().remove("friend:request:" + receiverId, "0:" + userId);
                        redisTemplate.opsForZSet().remove("friend:request:" + receiverId, "2:" + userId);
                        return false;
                    }
                    else return true;
                }
                else if(redisTemplate.opsForZSet().score("friend:request:" + receiverId, "1:" + userId) != null)
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
    public boolean AddFriendRequest(String userId, String receiverId) throws InterruptedException {
        RLock lock = redisson.getLock("FriendRequestLock");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                redisTemplate.opsForZSet().add(
                        "friend:request:" + receiverId,
                        "0:" + userId,
                        LocalDateTime
                                .now()
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                );
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
    public List<Map.Entry<String, Double>> GetFriendRequestByUserId(String userId) throws InterruptedException {
        Set<ZSetOperations.TypedTuple<String>> typedTuples = redisTemplate.opsForZSet()
                .reverseRangeWithScores("friend:request:" + userId, 0, -1);

        if (typedTuples != null && !typedTuples.isEmpty()) {
            return typedTuples.stream()
                    .map(tuple ->
                            new AbstractMap.SimpleEntry<>(tuple.getValue(), tuple.getScore()))
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public String getUserInformation(String id) {
        return (String) redisTemplate.opsForValue().get("login:UserInformation:" + id);
    }

    @Override
    public void setUserOutOfDateRequest(String id, String receiverValue) throws InterruptedException {
        RLock lock = redisson.getLock("OutdatedRequestLock");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                String string = receiverValue.split(":")[1];
                redisTemplate.opsForZSet().remove("friend:request:" + id, "0:" + string);
                redisTemplate.opsForZSet().add(
                        "friend:request:" + id,
                        receiverValue,
                        LocalDateTime
                                .now()
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                );
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
    }

    @Override
    public boolean setUserReceiveRequest(String id, Long index) throws InterruptedException {
        RLock lock = redisson.getLock("OutdatedRequestLock");
        boolean b = lock.tryLock(5,  TimeUnit.SECONDS);
        if (b) {
            try {
                Set<ZSetOperations.TypedTuple<String>> myZSet = redisTemplate.opsForZSet().rangeWithScores("friend:request:"+id, -1 - index, -1 - index);
                if (myZSet != null && !myZSet.isEmpty()) {
                    ConcurrentMap<String, Double> collect = myZSet.stream().collect(Collectors.toConcurrentMap(
                            tuple -> tuple.getValue(),
                            tuple -> tuple.getScore(),
                            (existing, replacement) -> existing
                    ));
                    Double nextScore = collect.values().iterator().next();
                    String nextId = collect.keySet().iterator().next();
                    String string = nextId.split(":")[1];

                    redisTemplate.opsForZSet().remove("friend:request:" + id,"0:" + string);
                    redisTemplate.opsForZSet().add("friend:request:" + id, "1:" + string, nextScore);
                    Double score = redisTemplate.opsForZSet().score("friend:request:" + string, "0:" + id);
                    if (score != null) {
                        redisTemplate.opsForZSet().remove("friend:request:" + string,"0:" + id);
                        redisTemplate.opsForZSet().add("friend:request:" + string, "1:" + id, score);
                    }
                }
                return true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(lock.isHeldByCurrentThread()) lock.unlock();
            }
        }
        return true;
    }

    @Override
    public HistorySession GetHistorySession(String id, Long index) {
        Set<String> range = redisTemplate.opsForZSet().range("friend:request:" + id, -1 - index, -1 - index);
        System.out.println(range);
        HistorySession historySession = new HistorySession();
        if(range != null && !range.isEmpty()) {
            String receiverId = range.iterator().next().substring(2);
            String receiverInformation = getUserInformation(receiverId);
            String stringEmail = receiverInformation.split(":")[0];
            String avatar = receiverInformation.split(":")[1];
            String receiverName = receiverInformation.substring(stringEmail.length() + avatar.length() + 2);
            historySession.setUserId(Long.parseLong(id));
            historySession.setAvatar(avatar);
            historySession.setReceiverName(receiverName);
            historySession.setReceiverId(Long.parseLong(receiverId));
        }
        return historySession;
    }

    @Override
    public HistorySession GetReceiverHistorySession(String id) throws InterruptedException {
        HistorySession historySession = new HistorySession();
        String string = redisTemplate.opsForValue().get("login:UserInformation:" + id);
        if(string != null && !string.isEmpty()) {
            String[] split = string.split(":");
            historySession.setAvatar(split[1]);
            historySession.setReceiverName(string.substring(split[0].length() + split[1].length() + 2));
            historySession.setReceiverId(Long.parseLong(id));
        }
        return historySession;
    }

    @Override
    public String GetSenderName(String id) {
        String string = redisTemplate.opsForValue().get("login:UserInformation:" + id);
        if(string != null && !string.isEmpty()) {
            String[] split = string.split(":");
            return string.substring(split[0].length() + split[1].length() + 2);
        }
        return null;
    }

    @Override
    public boolean storeUserChatHistory(String senderId, String receiverId, String message) throws InterruptedException {
        RLock lock = redisson.getLock("ChatHistoryLock");
        boolean b = lock.tryLock(5, TimeUnit.SECONDS);
        if (b) {
            try {
                redisTemplate.opsForZSet()
                        .add("private:history:" + receiverId + ":" + senderId,
                                senderId + ":" + System.currentTimeMillis() + ":" + message,
                                System.currentTimeMillis());
                redisTemplate.opsForZSet()
                        .add("private:history:" + senderId + ":" + receiverId,
                                senderId + ":" + System.currentTimeMillis() + ":" + message,
                                System.currentTimeMillis());
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
    public Set<String> GetUserChatHistory(String senderId, String receiverId) throws InterruptedException {
        return redisTemplate.opsForZSet().range("private:history:" + receiverId +":" + senderId, 0, -1);
    }

}
