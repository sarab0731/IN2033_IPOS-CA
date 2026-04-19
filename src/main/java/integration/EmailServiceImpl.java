package integration;

// AI-assisted: used Claude to learn how to implement IEmailService by delegating
// to the SMTP client and handling delivery failures gracefully.

import integration.interfaces.IEmailService;

/**
 * Implements {@link IEmailService} by routing emails through PU's email gateway
 * via {@link PUApiClient}.
 *
 * The {@code reference} and {@code subsystem} fields are folded into the subject
 * line so the existing PU endpoint (which only accepts to/subject/body) can carry
 * the full context.
 */
public class EmailServiceImpl implements IEmailService {

    /**
     * Sends an email via PU's email service.
     *
     * @param email      recipient email address
     * @param content    body of the email
     * @param reference  category or reference label (e.g. "INVOICE", "ADVERT")
     * @param sender     the sender's email address (included in subject for traceability)
     * @param subsystem  the IPOS subsystem that generated the email (e.g. "CA", "SA")
     * @return true if PU accepted the email for delivery
     */
    @Override
    public boolean produceEmail(String email, String content, String reference,
                                String sender, String subsystem) {
        if (email == null || email.isEmpty()) return false;
        String subject = "[" + subsystem + "] " + reference + " from " + sender;
        return PUApiClient.sendEmail(email, subject, content);
    }
}
