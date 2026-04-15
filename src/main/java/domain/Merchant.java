package domain;

/**
 * Represents this CA installation's InfoPharma SA merchant account.
 * There is exactly one row in the merchants table — this pharmacy's SA account.
 * Identity fields (companyName, email, etc.) mirror SA's merchant table.
 */
public class Merchant {

    // Identity — mirrors SA's merchant table
    private final String merchantId;
    private final String companyName;
    private final String registrationNumber;
    private final String email;
    private final String phone;
    private final String address;

    // SA account state — refreshed by SASync
    private final boolean saLinked;
    private final boolean saStatusVerified;
    private final String  saAccountStatus;  // NORMAL | SUSPENDED | IN_DEFAULT
    private final double  saDiscountRate;   // percentage, e.g. 5.00 = 5%
    private final double  saCreditLimit;
    private final double  saBalance;        // outstanding_balance on SA

    public Merchant(String merchantId, String companyName, String registrationNumber,
                    String email, String phone, String address,
                    boolean saLinked, boolean saStatusVerified, String saAccountStatus,
                    double saDiscountRate, double saCreditLimit, double saBalance) {
        this.merchantId          = merchantId;
        this.companyName         = companyName         != null ? companyName         : "";
        this.registrationNumber  = registrationNumber  != null ? registrationNumber  : "";
        this.email               = email               != null ? email               : "";
        this.phone               = phone               != null ? phone               : "";
        this.address             = address             != null ? address             : "";
        this.saLinked            = saLinked;
        this.saStatusVerified    = saStatusVerified;
        this.saAccountStatus     = saAccountStatus     != null ? saAccountStatus.toUpperCase() : "NORMAL";
        this.saDiscountRate      = saDiscountRate;
        this.saCreditLimit       = saCreditLimit;
        this.saBalance           = saBalance;
    }

    public String  getMerchantId()          { return merchantId; }
    public String  getCompanyName()         { return companyName; }
    public String  getRegistrationNumber()  { return registrationNumber; }
    public String  getEmail()               { return email; }
    public String  getPhone()               { return phone; }
    public String  getAddress()             { return address; }
    public boolean isSaLinked()             { return saLinked; }
    public boolean isSaStatusVerified()     { return saStatusVerified; }
    public String  getSaAccountStatus()     { return saAccountStatus; }
    public double  getSaDiscountRate()      { return saDiscountRate; }
    public double  getSaCreditLimit()       { return saCreditLimit; }
    public double  getSaBalance()           { return saBalance; }

    public double  getAvailableCredit()     { return Math.max(0, saCreditLimit - saBalance); }
    public boolean isAccountNormal()        { return "NORMAL".equalsIgnoreCase(saAccountStatus); }

    /** Display label used in UI: "Cosymed Ltd  (M001)" */
    public String getDisplayName() {
        return companyName.isEmpty() ? merchantId : companyName + "  (" + merchantId + ")";
    }
}