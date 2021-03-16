package com.fxs.bike.cache;

import com.fxs.bike.common.constants.Parameters;
import com.fxs.bike.common.exception.BikeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class JedisPoolWrapper {

    private JedisPool jedisPool = null;
    @Autowired
    private Parameters parameters;

    @PostConstruct
    public void init() throws BikeException {
        try {
            JedisPoolConfig config = new JedisPoolConfig();
            config.setMaxWaitMillis(parameters.getRedisMaxWaitMillis());
            config.setMaxIdle(parameters.getRedisMaxIdle());
            config.setMaxTotal(parameters.getRedisMaxTotal());
            jedisPool = new JedisPool(config, parameters.getRedisHost(), parameters.getRedisPort(), 2000);
        } catch (Exception e) {
            log.error("Fail to initialize redis", e);
            throw new BikeException("初始化redis失败");
        }
    }

    public JedisPool getJedisPool() { return jedisPool; }

}
