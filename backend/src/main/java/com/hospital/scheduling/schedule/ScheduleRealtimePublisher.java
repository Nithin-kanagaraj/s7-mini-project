package com.hospital.scheduling.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishScheduleChanged(String scheduleId, String event, String message) {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("event", event);
        if (scheduleId != null) {
            payload.put("scheduleId", scheduleId);
        }
        payload.put("message", message != null ? message : "");

        try {
            if (scheduleId != null) {
                messagingTemplate.convertAndSend("/topic/schedules/" + scheduleId, payload);
            }
            messagingTemplate.convertAndSend("/topic/schedules", payload);
        } catch (Exception e) {
            log.warn("Failed to publish schedule realtime event {}: {}", event, e.getMessage());
        }
    }
}
