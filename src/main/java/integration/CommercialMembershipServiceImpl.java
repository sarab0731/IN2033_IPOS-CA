package integration;

// AI-assisted: used Claude to learn how to implement the commercial membership
// flow, including DB persistence and email notification on submission.

import integration.interfaces.ICommercialMembershipService;

/**
 * Implements {@link ICommercialMembershipService} by forwarding membership
 * applications to SA via {@link SAApiClient}.
 *
 * Expected {@code candidate} array layout (indices 0–7):
 * <pre>
 *   0 — companyName
 *   1 — registrationNumber
 *   2 — directors
 *   3 — businessType
 *   4 — address
 *   5 — email
 *   6 — fax
 *   7 — preferPhysicalMail ("true"/"false")
 * </pre>
 */
public class CommercialMembershipServiceImpl implements ICommercialMembershipService {

    /**
     * Submits a commercial membership application to SA.
     *
     * @param candidate array of applicant details (see class Javadoc for layout)
     * @return true if SA accepted the application
     */
    @Override
    public boolean requestMembership(String[] candidate) {
        if (candidate == null || candidate.length < 8) return false;

        return SAApiClient.requestMembership(
                candidate[0],                           // companyName
                candidate[1],                           // registrationNumber
                candidate[2],                           // directors
                candidate[3],                           // businessType
                candidate[4],                           // address
                candidate[5],                           // email
                candidate[6],                           // fax
                Boolean.parseBoolean(candidate[7])      // preferPhysicalMail
        );
    }
}
