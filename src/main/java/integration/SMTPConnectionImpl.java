package integration;

// AI-assisted: used Claude to learn how to configure an SMTP connection and
// send transactional emails through the external comms subsystem.

import integration.interfaces.ISMTPConnection;

/**
 * Implements {@link ISMTPConnection} by delegating to PU's email gateway
 * via {@link PUApiClient}.
 *
 * {@code reference} is used as the email subject; {@code sender} is appended
 * for traceability since the PU endpoint does not expose a sender field directly.
 */
public class SMTPConnectionImpl implements ISMTPConnection {

    /**
     * Sends an email via the PU SMTP relay.
     *
     * @param email     recipient email address
     * @param content   body of the email
     * @param reference subject/category of the email
     * @param sender    originating email address (appended to subject)
     * @return true if PU accepted the message for delivery
     */
    @Override
    public boolean sendEmail(String email, String content, String reference, String sender) {
        if (email == null || email.isEmpty()) return false;
        String subject = reference + " (from " + sender + ")";
        return PUApiClient.sendEmail(email, subject, content);
    }
}
