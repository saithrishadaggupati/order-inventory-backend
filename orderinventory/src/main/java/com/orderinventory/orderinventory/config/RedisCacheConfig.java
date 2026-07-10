package com.orderinventory.orderinventory.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
public class RedisCacheConfig {

    // switches redis away from java's default serializer (which needs Serializable
    // objects) to plain JSON via jackson - this is the standard approach and lets us
    // cache normal JPA entities without implementing Serializable on every one of them
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // using the no-arg constructor here (not our app's ObjectMapper) - this one
        // embeds type info (@class) into the stored JSON, which is what lets spring
        // cache correctly reconstruct the right java type when reading back. our
        // app's normal ObjectMapper doesn't do this, which caused a ClassCastException
        // (got a LinkedHashMap back instead of a Product)
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}