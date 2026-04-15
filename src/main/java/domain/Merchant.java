package domain;

/**
 * Represents this CA installation's InfoPharma SA merchant account.
 * There is exactly one row in the merchants table — this pharmacy's SA account.
 */
public class Merchant {

    private final String merchantId;
    private final boolean saLinked;
    private final boolean saStatusVerified;
    private final String saAccountStatus;  // NORMAL | SUSPENDED | IN_DEFAULT
    private final double saDiscountRate;
    private final double saCreditLimit;
    private final double saBalance;

    public Merchant(String merchantId, boolean saLinked,
                    boolean saStatusVerified, String saAccountStatus,
                    double saDiscountRate, double saCreditLimit, double saBalance) {
        this.merchantId       = merchantId;
        this.saLinked         = saLinked;
        this.saStatusVerified = saStatusVerified;
        this.saAccountStatus  = saAccountStatus != null ? saAccountStatus : "NORMAL";
        this.saDiscountRate   = saDiscountRate;
        this.saCreditLimit    = saCreditLimit;
        this.saBalance        = saBalance;
    }

    public String getMerchantId()       { return merchantId; }
    public boolean isSaLinked()         { return saLinked; }
    public boolean isSaStatusVerified() { return saStatusVerified; }
    public String getSaAccountStatus()  { return saAccountStatus; }
    public double getSaDiscountRate()   { return saDiscountRate; }
    public double getSaCreditLimit()    { return saCreditLimit; }
    public double getSaBalance()        { return saBalance; }

    public double getAvailableCredit()  { return Math.max(0, saCreditLimit - saBalance); }
    public boolean isAccountNormal()    { return "NORMAL".equalsIgnoreCase(saAccountStatus); }
}