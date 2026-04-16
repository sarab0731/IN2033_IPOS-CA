package domain;

public class Customer {

    private int customerId;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private double creditLimit;
    private double currentBalance;
    private String accountStatus;
    private int discountPlanId;

    public Customer(int customerId, String fullName, String email,
                    String phone, String address, double creditLimit,
                    double currentBalance, String accountStatus, int discountPlanId) {
        this.customerId    = customerId;
        this.fullName      = fullName;
        this.email         = email;
        this.phone         = phone;
        this.address       = address;
        this.creditLimit   = creditLimit;
        this.currentBalance = currentBalance;
        this.accountStatus = accountStatus;
        this.discountPlanId = discountPlanId;
    }

    public int    getCustomerId()     { return customerId; }
    public String getFullName()       { return fullName; }
    public String getEmail()          { return email; }
    public String getPhone()          { return phone; }
    public String getAddress()        { return address; }
    public double getCreditLimit()    { return creditLimit; }
    public double getCurrentBalance() { return currentBalance; }
    public String getAccountStatus()  { return accountStatus; }
    public int    getDiscountPlanId() { return discountPlanId; }

    public boolean isActive()    { return "ACTIVE".equals(accountStatus); }
    public boolean isSuspended() { return "SUSPENDED".equals(accountStatus); }
    public boolean isInDefault() { return "IN_DEFAULT".equals(accountStatus); }

    public double getAvailableCredit() {
        return creditLimit - currentBalance;
    }

    @Override
    public String toString() {
        return fullName;
    }
}