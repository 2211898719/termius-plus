package com.codeages.termiusplus.common.cache;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.TypeReference;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class RedisCacheManager {

    private final RedisTemplate<String, Object> redis;

    private final String globalNamespace;

    private final Duration globalTTl;

    public RedisCacheManager(Environment environment, RedisTemplate<String, Object> redis) {
        this.redis = redis;

        globalNamespace = environment.getProperty("cache.namespace", "cache");
        var ttl = Convert.toLong(environment.getProperty("cache.ttl"), 36000L);
        globalTTl = Duration.ofSeconds(ttl);
    }

    public <T> Optional<T> get(String namespace, String key, CacheGetCallable<T> callable) {
        return get(namespace, key, callable, globalTTl);
    }

    public <T> Optional<T> get(String namespace, String key, CacheGetCallable<T> callable, Duration ttl) {
        var obj = redis.opsForValue().get(makeKey(namespace, key));
        if ("NULL".equals(obj)) {
            return Optional.empty();
        }

        if (obj != null) {
            var convertedObj = Convert.convert(new TypeReference<T>() {}, obj);
            if (convertedObj != null) {
                return Optional.of(convertedObj);
            }
        }

        var objOp = callable.get();

        redis.opsForValue().set(makeKey(namespace, key), objOp.isEmpty() ? "NULL" : objOp.get(), ttl);

        return objOp;
    }

    public <T> Optional<T> getById(String namespace, Long id, CacheGetCallable<T> callable) {
        return getById(namespace, id, callable, globalTTl);
    }

    public <T> Optional<T> getById(String namespace, Long id, CacheGetCallable<T> callable, Duration ttl) {
        return get(namespace, "id/" + id, callable, ttl);
    }

    public <T extends IdObject> Optional<T> getByRel(String namespace, String relKey, CacheGetCallable<T> callable) {
        return getByRel(namespace, relKey, callable, globalTTl);
    }

    public <T extends IdObject> Optional<T> getByRel(String namespace, String relKey, CacheGetCallable<T> callable, Duration ttl) {
        var idObj = redis.opsForValue().get(makeKey(namespace, relKey));
        if ("NULL".equals(idObj)) {
            return Optional.empty();
        }

        var id = Convert.toLong(idObj);

        Optional<T> objOp;
        var fromCache = false;
        if (id == null) {
            objOp = callable.get();
        } else {
            var obj = redis.opsForValue().get(makeKey(namespace, "id/"+id));
            if (obj == null || "NULL".equals(obj)) {
                objOp = callable.get();
            } else {
                var convertedObj = Convert.convert(new TypeReference<T>() {}, obj);
                if (convertedObj == null) {
                    objOp = callable.get();
                } else {
                    objOp = Optional.of(convertedObj);
                    fromCache = true;
                }
            }
        }

        if (!fromCache) {
            if (objOp.isPresent()) {
                var obj = objOp.get();
                redis.opsForValue().set(makeKey(namespace, "id/"+obj.getId()), obj, ttl);
                redis.opsForValue().set(makeKey(namespace, relKey), obj.getId(), ttl);
            } else {
                redis.opsForValue().set(makeKey(namespace, relKey), "NULL", ttl);
            }
        }

        return objOp;
    }

    public void set(String namespace, String key, Object val) {
        set(namespace, key, val, globalTTl);
    }

    public void set(String namespace, String key, Object val, Duration ttl) {
        redis.opsForValue().set(makeKey(namespace, key), val, ttl);
    }

    public void delete(String namespace, String key) {
        redis.delete(makeKey(namespace, key));
    }

    public void delete(String namespace, List<String> keys) {
        var fullKeys = keys.stream().map(key -> globalNamespace + "::" + namespace + "::" + key).collect(Collectors.toList());
        redis.delete(fullKeys);
    }

    private String makeKey(String namespace, String key) {
        return globalNamespace + "::" + namespace + "::" + key;
    }
}
