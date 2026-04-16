package domain;

/**
 * One spend band within a FLEXIBLE discount plan.
 * If a customer's total monthly spend is >= minSpend (and < the next tier's minSpend),
 * the rate applies to that month's total.
 * maxSpend is informational only (null = no upper limit / top tier).
 */
public class DiscountTier {

    private int tierId;
    private int discountPlanId;
    private double minSpend;
    private Double maxSpend;   // null means unlimited (highest tier)
    private double rate;

    public DiscountTier(int tierId, int discountPlanId,
                        double minSpend, Double maxSpend, double rate) {
        this.tierId         = tierId;
        this.discountPlanId = discountPlanId;
        this.minSpend       = minSpend;
        this.maxSpend       = maxSpend;
        this.rate           = rate;
    }

    public int    getTierId()         { return tierId; }
    public int    getDiscountPlanId() { return discountPlanId; }
    public double getMinSpend()       { return minSpend; }
    public Double getMaxSpend()       { return maxSpend; }
    public double getRate()           { return rate; }

    /** Human-readable band label, e.g. "£0 – £100" or "£200+" */
    public String bandLabel() {
        String lo = String.format("£%.0f", minSpend);
        if (maxSpend == null) return lo + "+";
        return lo + " – " + String.format("£%.0f", maxSpend);
    }
}
