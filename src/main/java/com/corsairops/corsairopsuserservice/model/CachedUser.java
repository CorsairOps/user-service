package com.corsairops.corsairopsuserservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("users")
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