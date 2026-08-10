package com.cakedelight.notificationservice.entity;

public enum NotificationChannel {
    EMAIL,
    SMS,
    // What this log-only Phase 4 build actually records — there's no real
    // outbound send yet (CLAUDE.md §5.2), so labeling it EMAIL/SMS would
    // claim a delivery that never happened. IN_APP is the honest description
    // of "recorded internally, nothing sent externally". order-service's
    // real email upgrade (see project task tracking) will set EMAIL once it
    // lands.
    IN_APP
}
