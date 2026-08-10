package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.event.OrderCompletedEvent;

/**
 * The seam a real channel drops into later. CLAUDE.md §5.2 permits a
 * logging implementation for now ("no real email/SMS integration — optional
 * later enhancement"); {@code architecture-baseline}'s accepted mitigation
 * for that deferral is exactly this: "a real sender interface with a
 * logging implementation, so the seam exists and a provider could be
 * dropped in." {@link LoggingNotificationSender} is that logging
 * implementation. Throwing from {@link #send} is how a real implementation
 * reports a failed delivery — {@link NotificationServiceImpl} catches it and
 * records {@code FAILED} instead of {@code SENT}.
 */
public interface NotificationSender {
    void send(OrderCompletedEvent event);
}
