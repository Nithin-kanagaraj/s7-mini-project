package com.hospital.scheduling.common.websocket;

import com.hospital.scheduling.common.security.CustomUserDetailsService;
import com.hospital.scheduling.common.security.JwtTokenProvider;
import com.hospital.scheduling.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token != null && tokenProvider.validateToken(token)) {
            try {
                String userId = tokenProvider.getUserIdFromToken(token);
                UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserById(userId);
                attributes.put("user", new StompPrincipal(principal.getUsername()));
            } catch (Exception e) {
                log.warn("WebSocket handshake JWT resolved but user load failed: {}", e.getMessage());
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            String queryToken = servletRequest.getServletRequest().getParameter("access_token");
            if (queryToken != null && !queryToken.isBlank()) {
                return queryToken;
            }
            String auth = servletRequest.getServletRequest().getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                return auth.substring(7);
            }
        }
        return null;
    }
}
