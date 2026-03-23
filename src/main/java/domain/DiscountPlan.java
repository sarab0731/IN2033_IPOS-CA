package domain;

public class DiscountPlan {

    private int discountPlanId;
    private String planName;
    private String planType;       // FIXED or FLEXIBLE
    private double discountPercent;
    private String notes;

    public DiscountPlan(int discountPlanId, String planName, String planType,
                        double discountPercent, String notes) {
        this.discountPlanId  = discountPlanId;
        this.planName        = planName;
        this.planType        = planType;
        this.discountPercent = discountPercent;
        this.notes           = notes;
    }

    // constructor for creating new plans (no ID yet)
    public DiscountPlan(String planName, String planType, double discountPercent, String notes) {
        this(0, planName, planType, discountPercent, notes);
    }

    public int    getDiscountPlanId()  { return discountPlanId; }
    public String getPlanName()        { return planName; }
    public String getPlanType()        { return planType; }
    public double getDiscountPercent() { return discountPercent; }
    public String getNotes()           { return notes; }

    public boolean isFixed()    { return "FIXED".equals(planType); }
    public boolean isFlexible() { return "FLEXIBLE".equals(planType); }

    @Override
    public String toString() {
        return planName + " (" + planType + " - " + discountPercent + "%)";
    }
}