package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.dto.response.NotificationResponse;
import com.cakedelight.notificationservice.entity.Notification;
import com.cakedelight.notificationservice.entity.NotificationChannel;
import com.cakedelight.notificationservice.entity.NotificationStatus;
import com.cakedelight.notificationservice.event.OrderCompletedEvent;
import com.cakedelight.notificationservice.exception.UnauthenticatedException;
import com.cakedelight.notificationservice.mapper.NotificationMapper;
import com.cakedelight.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationSender notificationSender;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void recordOrderCompleted(OrderCompletedEvent event) {
        // Idempotency (CLAUDE.md §5.3): Kafka's at-least-once delivery means
        // this can run twice for the same event (consumer restart before
        // offset commit, broker retry, etc.) — a redelivery must not create
        // a second notification. Checked here rather than relying solely on
        // the DB's unique constraint so a redelivery logs a clean skip
        // instead of surfacing as a constraint-violation error.
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.info("Skipping already-processed order.completed event {} (order {})",
                    event.eventId(), event.orderId());
            return;
        }

        Notification notification = new Notification();
        notification.setUserId(event.userId());
        notification.setOrderId(event.orderId());
        notification.setEventId(event.eventId());
        notification.setChannel(notificationSender.channel());
        notification.setPayload(toPayload(event));

        // Routed through the NotificationSender seam rather than logging
        // inline — a real channel implementation can throw here for a
        // genuine delivery failure, which is what makes FAILED a real
        // possible outcome (SD-N3) instead of a column that's structurally
        // stuck on SENT.
        try {
            notificationSender.send(event);
            notification.setStatus(NotificationStatus.SENT);
        } catch (Exception ex) {
            log.error("Failed to send notification for order {}", event.orderId(), ex);
            notification.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications(String rawUserId) {
        Long userId = parseUserId(rawUserId);
        return notificationRepository.findByUserId(userId).stream().map(notificationMapper::toResponse).toList();
    }

    private String toPayload(OrderCompletedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            // Should never happen for a plain record of primitives/strings —
            // fall back to toString() rather than letting a serialization
            // quirk block recording that the notification happened at all.
            log.warn("Could not serialize order.completed event {} to JSON; falling back to toString()", event.eventId(), ex);
            return event.toString();
        }
    }

    private Long parseUserId(String rawUserId) {
        if (rawUserId == null || rawUserId.isBlank()) {
            throw new UnauthenticatedException();
        }
        try {
            return Long.parseLong(rawUserId);
        } catch (NumberFormatException ex) {
            throw new UnauthenticatedException();
        }
    }
}
