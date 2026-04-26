package com.delma.doctorservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        JacksonJsonRedisSerializer<Object> serializer =
                new JacksonJsonRedisSerializer<>(Object.class);

        RedisCacheConfiguration config = RedisCacheConfiguration
                .defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                /*
                 LESSON — TTL (Time To Live):
                 Cached data expires after 10 minutes automatically.
                 Why not cache forever? Because doctor data CAN change —
                 admin approves a new doctor. Without TTL, the cache would
                 serve stale data forever even after DB is updated.
                 10 minutes is a balance: fresh enough, fast enough.
                */
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(serializer)
                )
                /*
                LESSON — Serialization:
                Redis stores everything as bytes. We need to tell Spring
                HOW to convert your Java objects to bytes and back.
                - Keys: StringRedisSerializer → stored as plain strings
                - Values: GenericJackson2JsonRedisSerializer → stored as JSON
                So in Redis, your doctor list looks like:
                Key: "doctors::all"
                Value: "[{\"id\":1,\"firstName\":\"John\",...}]"
               */
                .disableCachingNullValues();
        // Never cache null — if DB returns empty, don't cache it

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}