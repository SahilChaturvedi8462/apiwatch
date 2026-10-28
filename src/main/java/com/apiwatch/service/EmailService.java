package com.apiwatch.service;

import com.apiwatch.entity.MonitoredEndpoint;
import com.apiwatch.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Service for dispatching email notifications upon endpoint health transitions.
 * Methods are @Async — email delivery never blocks the health-check polling cycle.
 * Falls back to console simulation when MAIL_ENABLED=false or credentials are absent.
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromAddress;
    private final String mailUsername;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.mail.enabled:false}") boolean mailEnabled,
            @Value("${app.mail.from:no-reply@apiwatch.com}") String fromAddress,
            @Value("${spring.mail.username:}") String mailUsername) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailEnabled = mailEnabled;
        this.fromAddress = fromAddress;
        this.mailUsername = mailUsername;
    }

    /**
     * Sends a DOWN alert asynchronously — does not block the scheduler thread.
     */
    @Async
    public void sendEndpointDownAlert(
            User user,
            MonitoredEndpoint endpoint,
            Integer statusCode,
            String errorMessage,
            Instant transitionedAt) {

        String subject = String.format("[APIWatch ALERT] '%s' is DOWN!", endpoint.getName());
        String body = String.format("""
                Hello %s,

                APIWatch detected that your monitored endpoint has gone DOWN:

                - Endpoint Name : %s
                - Target URL    : %s
                - HTTP Status   : %s
                - Error Details : %s
                - Timestamp     : %s

                We will continue monitoring and alert you when it recovers.

                --
                APIWatch Automated Monitoring System
                """,
                user.getName(),
                endpoint.getName(),
                endpoint.getUrl(),
                statusCode != null ? statusCode : "N/A (Connection Timeout / Refused)",
                errorMessage != null ? errorMessage : "None",
                transitionedAt
        );

        dispatchEmail(user.getEmail(), subject, body);
    }

    /**
     * Sends a RECOVERY notification asynchronously.
     */
    @Async
    public void sendEndpointRecoveredAlert(
            User user,
            MonitoredEndpoint endpoint,
            Integer statusCode,
            Instant transitionedAt) {

        String subject = String.format("[APIWatch RECOVERED] '%s' is back UP!", endpoint.getName());
        String body = String.format("""
                Hello %s,

                Good news! Your monitored endpoint has recovered:

                - Endpoint Name : %s
                - Target URL    : %s
                - HTTP Status   : %s
                - Recovered At  : %s

                --
                APIWatch Automated Monitoring System
                """,
                user.getName(),
                endpoint.getName(),
                endpoint.getUrl(),
                statusCode != null ? statusCode : 200,
                transitionedAt
        );

        dispatchEmail(user.getEmail(), subject, body);
    }

    private void dispatchEmail(String recipientEmail, String subject, String body) {
        if (mailEnabled && mailSender != null && !mailUsername.isBlank()) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromAddress);
                message.setTo(recipientEmail);
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                log.info("Email sent to '{}': {}", recipientEmail, subject);
            } catch (MailException e) {
                log.error("SMTP delivery failed to '{}': {}. Falling back to console log.",
                        recipientEmail, e.getMessage());
                logSimulatedEmail(recipientEmail, subject, body);
            }
        } else {
            logSimulatedEmail(recipientEmail, subject, body);
        }
    }

    private void logSimulatedEmail(String recipientEmail, String subject, String body) {
        log.info("""
                
                ========================= [SIMULATED EMAIL ALERT] =========================
                TO:      {}
                FROM:    {}
                SUBJECT: {}
                BODY:
                {}
                ===========================================================================
                """, recipientEmail, fromAddress, subject, body.trim());
    }
}
