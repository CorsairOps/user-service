package com.corsairops.corsairopsuserservice.service;

import com.corsairops.corsairopsuserservice.client.AuthServiceClient;
import com.corsairops.corsairopsuserservice.config.AuthServiceProperties;
import com.corsairops.corsairopsuserservice.dto.authservice.AuthenticateResponse;
import com.corsairops.shared.dto.User;
import com.corsairops.shared.exception.HttpResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthServiceClient authServiceClient;
    private final AuthServiceProperties authServiceProperties;

    private LocalDateTime tokenExpiryTime;
    private String bearerToken;

    private void authenticate() {
        AuthenticateResponse response = authServiceClient.authenticate(authServiceProperties.getRealm(),
                "client_credentials",
                authServiceProperties.getClientId(),
                authServiceProperties.getClientSecret());
        this.tokenExpiryTime = LocalDateTime.now().plusSeconds(Long.parseLong(response.expires_in()));
        this.bearerToken = "Bearer " + response.access_token();
    }

    private void verifyAuthenticated() {
        if (bearerToken == null || bearerToken.isBlank() || LocalDateTime.now().isAfter(tokenExpiryTime)) {
            log.info("Access token is missing or expired. Authenticating...");
            authenticate();
        }
    }

    public List<User> getAllUsers() {
        try {
            verifyAuthenticated();
            return authServiceClient.getAllUsers(bearerToken, authServiceProperties.getRealm());
        } catch (WebClientResponseException e) {
            log.error("Error fetching all users: {}", e.getMessage());
            throw new HttpResponseException("User Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public User getUserById(String userId) {
        try {
            verifyAuthenticated();
            return authServiceClient.getUserById(bearerToken, authServiceProperties.getRealm(), userId);
        } catch (WebClientResponseException e) {
            log.error("Error fetching user by ID: {}", e.getMessage());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new HttpResponseException("User not found", HttpStatus.NOT_FOUND);
            }
            throw new HttpResponseException("User Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    public List<User> getUsersByIds(Set<String> userIds) {
        try {
            verifyAuthenticated();
            return userIds.parallelStream()
                    .map(id -> {
                        try {
                            return authServiceClient.getUserById(bearerToken, authServiceProperties.getRealm(), id);
                        } catch (WebClientResponseException e) {
                            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                                log.warn("User with ID {} not found", id);
                                throw new HttpResponseException("User with ID " + id + " not found", HttpStatus.NOT_FOUND);
                            } else {
                                log.error("Error fetching user by ID {}: {}", id, e.getMessage());
                            }
                            throw e;
                        }
                    })
                    .toList();
        } catch (WebClientResponseException e) {
            log.error("Error fetching users by IDs: {}", e.getMessage());
            log.info(e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new HttpResponseException("One or more users not found", HttpStatus.NOT_FOUND);
            }
            throw new HttpResponseException("User Service is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

}