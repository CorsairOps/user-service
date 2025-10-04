package com.corsairops.corsairopsuserservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash(value = "users", timeToLive = 3600) // 1 hour TTL
@Data
@AllArgsConstructor @NoArgsConstructor @Builder
public class CachedUser {
    @Id
    private String id;
    private String email;
    private String givenName;
    private String familyName;
    private String gender;
}