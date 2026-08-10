package com.cakedelight.notificationservice.service;

import com.cakedelight.notificationservice.entity.NotificationChannel;
import com.cakedelight.notificationservice.event.OrderCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.stream.Collectors;

/**
 * Real outbound channel (task #11) — sends a plain-text order confirmation
 * over SMTP (Gmail, App Password auth; see README's "Real email
 * notifications" section for account setup). Active by default
 * ({@code app.notification.channel=email}); {@link LoggingNotificationSender}
 * is the opt-in fallback.
 * <p>
 * Deliberately plain text, not HTML/templated — CLAUDE.md's "simple beats
 * clever" applies here same as anywhere else in this project; a templating
 * engine for one email shape would be over-engineering.
 */
@Component
@ConditionalOnProperty(prefix = "app.notification", name = "channel", havingValue = "email", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${app.notification.email.from}")
    private String fromAddress;

    @Override
    public void send(OrderCompletedEvent event) {
        if (event.userEmail() == null || event.userEmail().isBlank()) {
            // No email on the event (the checkout JWT had no "email"
            // claim) — nothing to send to. Thrown rather than silently
            // skipped, so NotificationServiceImpl records this as FAILED
            // instead of a false SENT.
            throw new IllegalStateException(
                    "Cannot send email notification for order " + event.orderId() + ": event has no userEmail");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(event.userEmail());
        message.setSubject("Cake Delight — order #" + event.orderId() + " confirmed");
        message.setText(buildBody(event));

        mailSender.send(message);
        log.info("Email notification sent for order {} to {}", event.orderId(), event.userEmail());
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    private String buildBody(OrderCompletedEvent event) {
        String itemLines = event.items().stream()
                .map(item -> "  - %s x%d (%s each)".formatted(item.cakeName(), item.quantity(), formatAmount(item.unitPrice())))
                .collect(Collectors.joining("\n"));

        return """
                Thanks for your order!

                Order #%d
                Total: %s

                Items:
                %s

                — Cake Delight
                """.formatted(event.orderId(), formatAmount(event.totalAmount()), itemLines);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toPlainString();
    }
}
