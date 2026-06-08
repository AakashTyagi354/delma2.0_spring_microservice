package com.delma.gateway.rateLimit;


import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SlidingWindowLuaScript {
    // This script runs atomically inside Redis
    // KEYS[1] = the Redis key for this user+route combo
    // ARGV[1] = limit (max requests allowed)
    // ARGV[2] = window in milliseconds (60000 for 60 seconds)
    // ARGV[3] = current timestamp in milliseconds
    // ARGV[4] = unique request ID (to use as sorted set member)
    //
    // Returns: [allowed, current_count]
    //   allowed = 1 (allow) or 0 (reject)
    //   current_count = how many requests in current window

    private static final String SCRIPT =
            // Step 1: Remove all entries older than the window
            // ZREMRANGEBYSCORE removes members with score between -inf and (now - window)
            "local key = KEYS[1] " +
                    "local limit = tonumber(ARGV[1]) " +
                    "local window = tonumber(ARGV[2]) " +
                    "local now = tonumber(ARGV[3]) " +
                    "local requestId = ARGV[4] " +
                    "local windowStart = now - window " +

                    // Remove expired entries (older than window)
                    "redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart) " +

                    // Step 2: Count how many requests are in current window
                    "local count = redis.call('ZCARD', key) " +

                    // Step 3: Check if under limit
                    "if count < limit then " +
                    // Add this request with current timestamp as score
                    // requestId as member ensures uniqueness even if 2 requests arrive same millisecond
                    "    redis.call('ZADD', key, now, requestId) " +
                    // Set key expiry = window duration so Redis auto-cleans
                    "    redis.call('PEXPIRE', key, window) " +
                    // Return: allowed=1, remaining = limit - count - 1
                    "    return {1, limit - count - 1} " +
                    "else " +
                    // Return: allowed=0, remaining = 0
                    "    return {0, 0} " +
                    "end";

    public RedisScript<List<Long>> getScript(){
        return RedisScript.of(SCRIPT,(Class<List<Long>>)(Class<?>) List.class);
    }
    public String getScriptText(){
        return SCRIPT;
    }

}
