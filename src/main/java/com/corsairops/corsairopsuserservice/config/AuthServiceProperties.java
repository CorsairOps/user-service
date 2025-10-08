package com.corsairops.corsairopsuserservice.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth-service")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthServiceProperties {
    private String url;
    private String realm;
    private String clientId;
    private String clientSecret;
}