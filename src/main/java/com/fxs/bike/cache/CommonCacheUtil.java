package com.fxs.bike.cache;

import com.fxs.bike.common.exception.BikeException;
import com.fxs.bike.user.entity.UserElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import java.util.Map;

@Component
@Slf4j
public class CommonCacheUtil {
    @Autowired
    private JedisPoolWrapper jedisPoolWrapper;

    private static final String TOKEN_PREFIX = "token.";

    private static final String USER_PREFIX = "user.";

    public void cache(String key, String value) {
        try {
            JedisPool pool = jedisPoolWrapper.getJedisPool();
            if(pool != null) {
                try(Jedis jedis = pool.getResource()) {
                    jedis.select(0);
                    jedis.set(key, value);
                }
            }
        } catch (Exception e) {
            log.error("Fail to cache value", e);
        }
    }

    public String getCacheValue(String key) {
        String value = null;
        try {
            JedisPool pool = jedisPoolWrapper.getJedisPool();
            if(pool != null) {
                try(Jedis jedis = pool.getResource()) {
                    jedis.select(0);
                    value = jedis.get(key);
                }
            }
        } catch (Exception e) {
            log.error("Fail to get cache value", e);
        }
        return value;
    }

    public long cacheNxExpire(String key, String value, int expire) {
        long result = 0;
        try {
            JedisPool pool = jedisPoolWrapper.getJedisPool();
            if(pool != null) {
                try(Jedis jedis = pool.getResource()) {
                    jedis.select(0);
                    result = jedis.setnx(key, value);
                    jedis.expire(key, expire);
                }
            }
        } catch (Exception e) {
            log.error("Fail to cacheNx value", e);
        }
        return result;
    }

    public void delKey(String key) {
            JedisPool pool = jedisPoolWrapper.getJedisPool();
            if(pool != null) {
                try(Jedis jedis = pool.getResource()) {
                    jedis.select(0);
                    try {
                        jedis.del(key);
                    } catch (Exception e) {
                        log.error("Fail to remove key from redis", e);
                    }
                }
            }
    }

    public void putTokenWhenLogin(UserElement ue) {
            JedisPool pool = jedisPoolWrapper.getJedisPool();
            if(pool != null) {
                try(Jedis jedis = pool.getResource()) {
                    jedis.select(0);
                    Transaction trans = jedis.multi();
                    try {
                        trans.del(TOKEN_PREFIX + ue.getToken());
                        //??????token??????????????????????????????session
                        trans.hmset(TOKEN_PREFIX + ue.getToken(), ue.toMap());
                        trans.expire(TOKEN_PREFIX + ue.getToken(), 2592000);
                        //??????id?????????token
                        trans.sadd(USER_PREFIX + ue.getUserId(), ue.getToken());
                        trans.expire(USER_PREFIX + ue.getUserId(), 2592000);
                        trans.exec();
                    } catch (Exception e) {
                        log.error("Fail to cache token to redis", e);
                    }
                }
            }
    }

    public UserElement getUserByToken(String token) {
        UserElement ue = null;
        JedisPool pool = jedisPoolWrapper.getJedisPool();
        if(pool != null) {
            try(Jedis jedis = pool.getResource()) {
                jedis.select(0);
                try {
                    Map<String, String> map = jedis.hgetAll(TOKEN_PREFIX + token);
                    if(!CollectionUtils.isEmpty(map)) {
                        ue = UserElement.fromMap(map);

                    }
                    else {
                        log.warn("Fail to find cached userElement");
                    }
                } catch (Exception e) {
                    log.error("Fail to get user by token", e);
                }
            }
        }
        return ue;
    }

    public int cacheForVercode(String key, String verCode, String type, int second, String ip) throws BikeException {
        //??????????????? 3 ???ip???????????? 2 ???????????????????????? 1??????????????????????????????????????? 0:??????
        JedisPool pool = jedisPoolWrapper.getJedisPool();
        if(pool != null) {
            try(Jedis jedis = pool.getResource()) {
                jedis.select(0);
                try {
                    String ipKey = "ip." + ip;
                    if(ip == null) {
                        return 3;
                    }
                    else {
                        //???????????????????????????ip???????????????
                        String ipSendConut = jedis.get(ipKey);
                        try {
                            if(ipSendConut != null && Integer.parseInt(ipSendConut) >= 10) return 3;

                        } catch (NumberFormatException e) {
                            log.error("Fail to process ip send count", e);
                            return 3;
                        }
                        //??????key???verCode???????????????0????????????verCode????????????
                        long succ = jedis.setnx(key, verCode);
                        if(succ == 0) return 1;

                        //???????????????????????????????????????????????????
                        String sendCount = jedis.get(key + "." + type);
                        try {
                            if(sendCount != null && Integer.parseInt(sendCount) >= 10) {
                                jedis.del(key);
                                return 2;
                            }
                        } catch (NumberFormatException e) {
                            log.error("Fail to process send count", e);
                            jedis.del(key);
                            return 2;
                        }
                        try {
                            jedis.expire(key, second);
                            long val = jedis.incr(key + "." + type);
                            if(val == 1) {
                                jedis.expire(key + "." + type, 3600);
                            }
                            jedis.incr(ipKey);
                            if(val == 1) {
                                jedis.expire(ipKey, 3600);
                            }
                        } catch (Exception e) {
                            log.error("Fail to cache data into redis", e);
                        }
                    }
                } catch (Exception e) {
                    log.error("Fail to set vercode to redis", e);
                }
            } catch (Exception e) {
                log.error("Fail to cache for expiry", e);
                throw new BikeException("Fail to cache for expiry");
            }
        }
        return 0;
    }
}
