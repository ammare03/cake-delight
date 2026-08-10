package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.event.OrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * "Sending" a notification is logging to console — no real email/SMS
 * integration yet (CLAUDE.md §5.2). This is the only {@link NotificationSender}
 * implementation today; a real one (email via SMTP) is a tracked follow-up
 * that drops in here without touching {@link NotificationServiceImpl}.
 */
@Component
@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(OrderCompletedEvent event) {
        log.info("Notification sent: order {} completed for user {} ({}), total {}",
                event.orderId(), event.userId(), event.userEmail(), event.totalAmount());
    }
}
