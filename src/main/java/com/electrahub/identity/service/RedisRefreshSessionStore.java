package com.electrahub.identity.service;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class RedisRefreshSessionStore {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisRefreshSessionStore.class);


    public record RefreshSessionView(UUID userId, String deviceId, UUID sessionId, OffsetDateTime expiresAt) {}

    private final StringRedisTemplate redis;
    private final ObjectMapper om;

    private final String rtPrefix;
    private final String rtuPrefix;
    private final String rtdPrefix;

    public RedisRefreshSessionStore(
            StringRedisTemplate redis,
            ObjectMapper om,
            @Value("${app.redis.refresh-prefix}") String rtPrefix,
            @Value("${app.redis.refresh-user-prefix}") String rtuPrefix,
            @Value("${app.redis.refresh-device-prefix}") String rtdPrefix
    ) {
        this.redis = redis;
        this.om = om;
        this.rtPrefix = rtPrefix;
        this.rtuPrefix = rtuPrefix;
        this.rtdPrefix = rtdPrefix;
    }

    /**
     * Executes put for `RedisRefreshSessionStore`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param refreshHash input consumed by put.
     * @param view input consumed by put.
     * @param ttl input consumed by put.
     */
    public void put(String refreshHash, RefreshSessionView view, Duration ttl) {
        LOGGER.info("CODEx_ENTRY_LOG: Entering RedisRefreshSessionStore#put");
        LOGGER.debug("CODEx_ENTRY_LOG: Entering RedisRefreshSessionStore#put with debug context");
        try {
            redis.opsForValue().set(rtPrefix + refreshHash, om.writeValueAsString(view), ttl);

            redis.opsForSet().add(rtuPrefix + view.userId(), refreshHash);
            redis.opsForSet().add(rtdPrefix + view.userId() + ":" + view.deviceId(), refreshHash);

            redis.expire(rtuPrefix + view.userId(), ttl.plusHours(6));
            redis.expire(rtdPrefix + view.userId() + ":" + view.deviceId(), ttl.plusHours(6));
        } catch (Exception e) {
            throw new IllegalStateException("Redis refresh session write failed", e);
        }
    }

    /**
     * Retrieves get if present for `RedisRefreshSessionStore`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param refreshHash input consumed by getIfPresent.
     * @return result produced by getIfPresent.
     */
    public RefreshSessionView getIfPresent(String refreshHash) {
        try {
            String v = redis.opsForValue().get(rtPrefix + refreshHash);
            return (v == null) ? null : om.readValue(v, RefreshSessionView.class);
        } catch (Exception e) {
            throw new IllegalStateException("Redis refresh session read failed", e);
        }
    }

    /**
     * Removes delete for `RedisRefreshSessionStore`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param refreshHash input consumed by delete.
     * @param userId input consumed by delete.
     * @param deviceId input consumed by delete.
     */
    public void delete(String refreshHash, UUID userId, String deviceId) {
        redis.delete(rtPrefix + refreshHash);
        redis.opsForSet().remove(rtuPrefix + userId, refreshHash);
        redis.opsForSet().remove(rtdPrefix + userId + ":" + deviceId, refreshHash);
    }

    /**
     * Executes revoke all for user for `RedisRefreshSessionStore`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param userId input consumed by revokeAllForUser.
     */
    public void revokeAllForUser(UUID userId) {
        Set<String> hashes = redis.opsForSet().members(rtuPrefix + userId);
        if (hashes != null) for (String h : hashes) redis.delete(rtPrefix + h);
        redis.delete(rtuPrefix + userId);
    }

    /**
     * Executes revoke all for user device for `RedisRefreshSessionStore`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.service`.
     * @param userId input consumed by revokeAllForUserDevice.
     * @param deviceId input consumed by revokeAllForUserDevice.
     */
    public void revokeAllForUserDevice(UUID userId, String deviceId) {
        String setKey = rtdPrefix + userId + ":" + deviceId;
        Set<String> hashes = redis.opsForSet().members(setKey);
        if (hashes != null) for (String h : hashes) redis.delete(rtPrefix + h);
        redis.delete(setKey);
    }
}
