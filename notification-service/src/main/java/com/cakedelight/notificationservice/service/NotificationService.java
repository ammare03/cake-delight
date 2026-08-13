package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.dto.NotificationResponse;
import com.cakedelight.notificationservice.entity.Notification;
import com.cakedelight.notificationservice.entity.NotificationStatus;
import com.cakedelight.notificationservice.exception.UnauthenticatedException;
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
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSender notificationSender;
    private final ObjectMapper objectMapper;

    @Transactional
    public void recordOrderCompleted(OrderCompletedEvent event) {
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

        try {
            notificationSender.send(event);
            notification.setStatus(NotificationStatus.SENT);
        } catch (Exception ex) {
            log.error("Failed to send notification for order {}", event.orderId(), ex);
            notification.setStatus(NotificationStatus.FAILED);
        }

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listNotifications(String rawUserId) {
        Long userId = parseUserId(rawUserId);
        return notificationRepository.findByUserId(userId).stream().map(NotificationResponse::from).toList();
    }

    private String toPayload(OrderCompletedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
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
