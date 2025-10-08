package com.corsairops.corsairopsuserservice.dto.authservice;

public record AuthenticateResponse(
        String access_token,
        String expires_in
) {

}