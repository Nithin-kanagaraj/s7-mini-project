package com.hospital.scheduling.auth;

import com.hospital.scheduling.auth.dto.AuthResponse;
import com.hospital.scheduling.auth.dto.LoginRequest;
import com.hospital.scheduling.auth.dto.UserSummaryDto;
import com.hospital.scheduling.common.security.JwtTokenProvider;
import com.hospital.scheduling.common.security.UserPrincipal;
import com.hospital.scheduling.user.User;
import com.hospital.scheduling.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // Update last login timestamp
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate Access Token
        String accessToken = tokenProvider.generateAccessToken(authentication);

        // Generate and save Refresh Token (rotation)
        RefreshToken refreshToken = createRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .refreshToken(refreshToken.getToken())
                .user(UserSummaryDto.fromPrincipal(userPrincipal))
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token is not in database!"));

        if (Boolean.TRUE.equals(refreshToken.getIsRevoked())) {
            throw new IllegalArgumentException("Refresh token has been revoked!");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token was expired. Please make a new signin request");
        }

        User user = refreshToken.getUser();
        UserPrincipal userPrincipal = UserPrincipal.create(user);

        // Rotate token: revoke current refresh token, issue a new one
        refreshToken.setIsRevoked(true);
        refreshTokenRepository.save(refreshToken);

        RefreshToken newRefreshToken = createRefreshToken(user);
        String newAccessToken = tokenProvider.generateAccessTokenFromPrincipal(userPrincipal);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .refreshToken(newRefreshToken.getToken())
                .user(UserSummaryDto.fromPrincipal(userPrincipal))
                .build();
    }

    @Transactional
    public void logout(String refreshTokenStr, String userId) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
                token.setIsRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
        if (userId != null && !userId.isBlank()) {
            userRepository.findById(userId).ifPresent(user -> {
                refreshTokenRepository.deleteByUser(user);
            });
        }
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .isRevoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}
