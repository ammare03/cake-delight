package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.entity.NotificationChannel;
import com.cakedelight.notificationservice.event.OrderCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * "Sending" a notification is logging to console — the original CLAUDE.md
 * §5.2 behavior, kept as an opt-in fallback now that {@link EmailNotificationSender}
 * is the default. Select it with {@code app.notification.channel=log}, e.g.
 * for local development without SMTP credentials configured.
 */
@Component
@ConditionalOnProperty(prefix = "app.notification", name = "channel", havingValue = "log")
@Slf4j
public class LoggingNotificationSender implements NotificationSender {

    @Override
    public void send(OrderCompletedEvent event) {
        log.info("Notification sent: order {} completed for user {} ({}), total {}",
                event.orderId(), event.userId(), event.userEmail(), event.totalAmount());
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.IN_APP;
    }
}
