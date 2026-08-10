package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.entity.NotificationChannel;
import com.cakedelight.notificationservice.event.OrderCompletedEvent;

/**
 * The seam a real channel drops into later. CLAUDE.md §5.2 originally
 * permitted a logging-only implementation ("no real email/SMS integration —
 * optional later enhancement"); {@link LoggingNotificationSender} is that
 * implementation and still exists as an opt-in fallback. {@link EmailNotificationSender}
 * is the real channel (task #11) — active by default via
 * {@code app.notification.channel=email}; flip it to {@code log} to fall
 * back to console-only sending. Throwing from {@link #send} is how an
 * implementation reports a failed delivery — {@link NotificationService}
 * catches it and records {@code FAILED} instead of {@code SENT}.
 */
public interface NotificationSender {
    void send(OrderCompletedEvent event);

    /**
     * Which channel this sender represents, so {@link NotificationService}
     * records an accurate {@code Notification.channel} instead of a
     * hardcoded literal that would misdescribe what was actually attempted.
     */
    NotificationChannel channel();
}
