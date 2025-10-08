package com.corsairops.corsairopsuserservice.client;

import com.corsairops.corsairopsuserservice.dto.authservice.AuthenticateResponse;
import com.corsairops.shared.dto.User;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;

public interface AuthServiceClient {

    @PostExchange(value = "/realms/{realm}/protocol/openid-connect/token", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    AuthenticateResponse authenticate(@PathVariable String realm,
                                      @RequestParam("grant_type") String grantType,
                                      @RequestParam("client_id") String clientId,
                                      @RequestParam("client_secret") String clientSecret);

    @GetExchange("/admin/realms/{realm}/users?max=5000")
    List<User> getAllUsers(@RequestHeader("Authorization") String bearerToken, @PathVariable String realm);

    @GetExchange("/admin/realms/{realm}/users/{userId}")
    User getUserById(@RequestHeader("Authorization") String bearerToken, @PathVariable String realm, @PathVariable String userId);
}