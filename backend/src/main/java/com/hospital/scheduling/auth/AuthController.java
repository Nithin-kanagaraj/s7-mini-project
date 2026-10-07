package com.hospital.scheduling.auth;

import com.hospital.scheduling.auth.dto.AuthResponse;
import com.hospital.scheduling.auth.dto.LoginRequest;
import com.hospital.scheduling.auth.dto.RefreshRequest;
import com.hospital.scheduling.auth.dto.UserSummaryDto;
import com.hospital.scheduling.common.security.UserPrincipal;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response) {
        AuthResponse authResponse = authService.login(loginRequest);
        setRefreshTokenCookie(response, authResponse.getRefreshToken(), refreshTokenExpirationMs / 1000);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest refreshRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        String token = null;
        if (refreshRequest != null && refreshRequest.getRefreshToken() != null && !refreshRequest.getRefreshToken().isBlank()) {
            token = refreshRequest.getRefreshToken();
        } else if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(c -> "refresh_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        AuthResponse authResponse = authService.refreshToken(token);
        setRefreshTokenCookie(response, authResponse.getRefreshToken(), refreshTokenExpirationMs / 1000);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal currentUser,
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = null;
        if (request.getCookies() != null) {
            refreshToken = Arrays.stream(request.getCookies())
                    .filter(c -> "refresh_token".equals(c.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        String userId = currentUser != null ? currentUser.getId() : null;
        authService.logout(refreshToken, userId);

        setRefreshTokenCookie(response, "", 0); // Clear cookie
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(UserSummaryDto.fromPrincipal(currentUser));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in HTTPS production
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
