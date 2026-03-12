package domain;

public class DiscountPlan {

    private String planName;
    private double discountPercentage;

    public DiscountPlan(String planName, double discountPercentage) {
        this.planName = planName;
        this.discountPercentage = discountPercentage;
    }

    public String getPlanName() {
        return planName;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }
}