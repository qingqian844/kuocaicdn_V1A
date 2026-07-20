package com.kuocai.cdn.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Redis 工具类
 */
@Slf4j
@Component
public class JedisUtil {

    private static JedisPool jedisPool;

    private static final String RETURN_OK = "OK";

    @Autowired
    public void setJedisPool(JedisPool jedisPool) {
        JedisUtil.jedisPool = jedisPool;
    }

    /**
     * 获取Jedis对象
     */
    private static synchronized Jedis getJedis() {
        return jedisPool.getResource();
    }

    private synchronized static void close(Jedis jedis) {
        if (Assert.notEmpty(jedis)) {
            jedis.close();
        }
    }

    /**
     * 获取Json
     */
    public static JSONObject getJson(String key) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                String value = jedis.get(key);
                if (Assert.notEmpty(value)) {
                    return JSONObject.parseObject(value);
                }
                return null;
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置Json
     */
    public static String setJson(String key, Object value) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.set(key.getBytes(), JSONObject.toJSONString(value).getBytes(StandardCharsets.UTF_8));
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置Json
     */
    public synchronized static String setJson(String key, Object value, long expire) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                String result = setJson(key, value);
                if (RETURN_OK.equals(result)) {
                    jedis.pexpire(key.getBytes(), expire * 1000);
                }
                return result;
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 获取Json
     */
    public static <T> List<T> getJsonArray(String key, Class<T> clz) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                String value = jedis.get(key);
                if (Assert.notEmpty(value)) {
                    return JSONArray.parseArray(value, clz);
                }
                return null;
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置Json
     */
    public static String setJsonArray(String key, Object value) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.set(key.getBytes(), JSONArray.toJSONString(value).getBytes(StandardCharsets.UTF_8));
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置Json
     */
    public synchronized static String setJsonArray(String key, Object value, long expire) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                String result = setJsonArray(key, value);
                if (RETURN_OK.equals(result)) {
                    jedis.pexpire(key.getBytes(), expire * 1000);
                }
                return result;
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 获取字符串
     */
    public static String getStr(String key) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.get(key);
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * Atomically reads and removes a one-time value.
     */
    public static String getAndDeleteStr(String key) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                Object value = jedis.eval(
                        "local value = redis.call('GET', KEYS[1]); " +
                                "if value then redis.call('DEL', KEYS[1]); end; return value",
                        1,
                        key);
                return value == null ? null : value.toString();
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置字符串
     */
    public static String setStr(String key, String value) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.set(key, value);
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置字符串
     */
    public synchronized static String setStr(String key, String value, int expire) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                String result = setStr(key, value);
                if (RETURN_OK.equals(result)) {
                    jedis.expire(key, expire);
                }
                return result;
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置自增键
     *
     * @param key    键
     * @param expire 过期时间
     */
    public synchronized static void setIncr(String key, int expire) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                jedis.incr(key);
                jedis.expire(key, expire);
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置自增键
     *
     * @param key 键
     */
    public static Long incr(String key) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.incr(key);
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 键减值
     */
    public static Long decrBy(String key, int num) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.decrBy(key, num);
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 删除
     */
    public static Long delKey(String key) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.del(key.getBytes());
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 批量删除
     */
    public static void delKeys(String[] keys) {
        if (Assert.isEmpty(keys)) {
            return;
        }
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                jedis.del(keys);
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 批量删除
     */
    public static void delKeys(Collection<String> keys) {
        if (Assert.isEmpty(keys)) {
            return;
        }
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                jedis.del(keys.toArray(new String[]{}));
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 判断key是否存在
     */
    public static Boolean exists(String key) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.exists(key.getBytes());
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 模糊查询获取key集合
     */
    public static Set<String> keys(String key) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.keys(key);
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 获取key的剩余时间
     */
    public static Long ttl(String key) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                return jedis.ttl(key);
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 设置字符串列表
     */
    public static void setList(String key, List<String> list) {
        setList(key, list, null);
    }

    public synchronized static void setList(@NonNull String key, List<String> list, Integer seconds) {
        synchronized (JedisUtil.class) {
            Jedis jedis = getJedis();
            try {
                assert jedis != null;
                if (seconds == null) {
                    jedis.set(key, JSONObject.toJSONString(list));
                } else {
                    jedis.setex(key, seconds, JSONObject.toJSONString(list));
                }
            } finally {
                close(jedis);
            }
        }
    }

    /**
     * 获取字符串列表
     */
    public static List<String> getListString(String key) {
        return getJsonArray(key, String.class);
    }
}
