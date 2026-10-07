package com.hospital.scheduling.common.websocket;

import com.hospital.scheduling.common.security.CustomUserDetailsService;
import com.hospital.scheduling.common.security.JwtTokenProvider;
import com.hospital.scheduling.common.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            String token = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
            if (token == null) {
                token = accessor.getFirstNativeHeader("access_token");
            }
            if (token != null && tokenProvider.validateToken(token)) {
                try {
                    String userId = tokenProvider.getUserIdFromToken(token);
                    UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserById(userId);
                    accessor.setUser(new StompPrincipal(principal.getUsername()));
                } catch (Exception e) {
                    log.warn("STOMP CONNECT authentication failed: {}", e.getMessage());
                }
            }
        }
        return message;
    }
}
