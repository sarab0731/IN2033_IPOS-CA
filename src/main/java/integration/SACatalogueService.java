package integration;

import domain.SACatalogueItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates the InfoPharma SA catalogue service.
 * In a full integration this would make HTTP calls to the InfoPharma API.
 * For the prototype, it provides a realistic mock catalogue of supplier products
 * that are distinct from the CA's local stock.
 */
public class SACatalogueService {

    // Supplier account statuses
    public enum AccountStatus {
        NORMAL, SUSPENDED, IN_DEFAULT
    }

    // Simulated supplier account state (would be fetched from SA in real integration)
    private static AccountStatus supplierAccountStatus = AccountStatus.NORMAL;
    private static double supplierAccountBalance = 0.00;
    private static double supplierCreditLimit    = 50000.00;

    // -------------------------------------------------------------------------
    // Account status management
    // -------------------------------------------------------------------------

    public static AccountStatus getAccountStatus() {
        return supplierAccountStatus;
    }

    public static boolean isAccountNormal() {
        return supplierAccountStatus == AccountStatus.NORMAL;
    }

    public static double getAccountBalance() {
        return supplierAccountBalance;
    }

    public static double getCreditLimit() {
        return supplierCreditLimit;
    }

    public static void addToBalance(double amount) {
        supplierAccountBalance += amount;
        // Auto-suspend if credit limit exceeded
        if (supplierAccountBalance > supplierCreditLimit) {
            supplierAccountStatus = AccountStatus.SUSPENDED;
        }
    }

    public static void recordPayment(double amount) {
        supplierAccountBalance = Math.max(0, supplierAccountBalance - amount);
        if (supplierAccountBalance == 0 && supplierAccountStatus != AccountStatus.NORMAL) {
            supplierAccountStatus = AccountStatus.NORMAL;
        }
    }

    public static String getAccountStatusSummary() {
        return String.format(
                "InfoPharma SA Account Summary\n\n" +
                        "Account Status  : %s\n" +
                        "Current Balance : £%.2f\n" +
                        "Credit Limit    : £%.2f\n" +
                        "Available Credit: £%.2f\n\n" +
                        "%s",
                supplierAccountStatus.name(),
                supplierAccountBalance,
                supplierCreditLimit,
                Math.max(0, supplierCreditLimit - supplierAccountBalance),
                supplierAccountStatus == AccountStatus.NORMAL
                        ? "Account is in good standing. Orders can be placed."
                        : "Account is " + supplierAccountStatus.name() + ". Orders cannot be placed until account is restored."
        );
    }

    // -------------------------------------------------------------------------
    // SA Catalogue — mock InfoPharma product list
    // These are supplier-side products, distinct from the CA's local stock
    // -------------------------------------------------------------------------

    public static List<SACatalogueItem> getCatalogue() {
        List<SACatalogueItem> catalogue = new ArrayList<>();

        // Analgesics
        catalogue.add(new SACatalogueItem("IP-ANA-001", "Paracetamol 500mg Tablets x100",    "Analgesics",     3.50,  100, "PharmaCo Ltd"));
        catalogue.add(new SACatalogueItem("IP-ANA-002", "Ibuprofen 400mg Tablets x84",       "Analgesics",     4.20,   84, "MediGen UK"));
        catalogue.add(new SACatalogueItem("IP-ANA-003", "Aspirin 75mg Tablets x56",          "Analgesics",     2.80,   56, "PharmaCo Ltd"));
        catalogue.add(new SACatalogueItem("IP-ANA-004", "Codeine Phosphate 30mg x28",        "Analgesics",    12.60,   28, "BritPharma"));

        // Antibiotics
        catalogue.add(new SACatalogueItem("IP-ANT-001", "Amoxicillin 500mg Capsules x21",    "Antibiotics",    8.40,   21, "EuroPharma"));
        catalogue.add(new SACatalogueItem("IP-ANT-002", "Penicillin V 250mg Tablets x28",    "Antibiotics",    6.30,   28, "MediGen UK"));
        catalogue.add(new SACatalogueItem("IP-ANT-003", "Erythromycin 250mg Tablets x28",    "Antibiotics",    9.10,   28, "BritPharma"));

        // Cardiovascular
        catalogue.add(new SACatalogueItem("IP-CAR-001", "Atorvastatin 20mg Tablets x28",     "Cardiovascular", 7.80,   28, "CardioMed"));
        catalogue.add(new SACatalogueItem("IP-CAR-002", "Amlodipine 5mg Tablets x28",        "Cardiovascular", 5.60,   28, "CardioMed"));
        catalogue.add(new SACatalogueItem("IP-CAR-003", "Ramipril 5mg Capsules x28",         "Cardiovascular", 6.90,   28, "EuroPharma"));
        catalogue.add(new SACatalogueItem("IP-CAR-004", "Bisoprolol 5mg Tablets x28",        "Cardiovascular", 5.20,   28, "PharmaCo Ltd"));

        // Respiratory
        catalogue.add(new SACatalogueItem("IP-RES-001", "Salbutamol 100mcg Inhaler",         "Respiratory",    6.50,    1, "RespiraMed"));
        catalogue.add(new SACatalogueItem("IP-RES-002", "Beclometasone 200mcg Inhaler",      "Respiratory",   14.20,    1, "RespiraMed"));
        catalogue.add(new SACatalogueItem("IP-RES-003", "Cetirizine 10mg Tablets x30",       "Respiratory",    3.20,   30, "AllergyPharma"));

        // Gastrointestinal
        catalogue.add(new SACatalogueItem("IP-GAS-001", "Omeprazole 20mg Capsules x28",      "Gastrointestinal", 4.80, 28, "GastroMed"));
        catalogue.add(new SACatalogueItem("IP-GAS-002", "Metoclopramide 10mg Tablets x28",   "Gastrointestinal", 3.90, 28, "MediGen UK"));
        catalogue.add(new SACatalogueItem("IP-GAS-003", "Lansoprazole 30mg Capsules x28",    "Gastrointestinal", 6.10, 28, "GastroMed"));

        // Diabetes
        catalogue.add(new SACatalogueItem("IP-DIA-001", "Metformin 500mg Tablets x84",       "Diabetes",       4.20,   84, "DiabeCare"));
        catalogue.add(new SACatalogueItem("IP-DIA-002", "Gliclazide 80mg Tablets x28",       "Diabetes",       5.80,   28, "DiabeCare"));

        // Vitamins & Supplements
        catalogue.add(new SACatalogueItem("IP-VIT-001", "Vitamin D3 1000IU Tablets x90",     "Vitamins",       4.50,   90, "NutriPharma"));
        catalogue.add(new SACatalogueItem("IP-VIT-002", "Vitamin B12 1000mcg Tablets x50",   "Vitamins",       5.20,   50, "NutriPharma"));
        catalogue.add(new SACatalogueItem("IP-VIT-003", "Folic Acid 400mcg Tablets x90",     "Vitamins",       2.90,   90, "PharmaCo Ltd"));

        return catalogue;
    }

    public static List<SACatalogueItem> getCatalogueByCategory(String category) {
        if (category == null || "All".equals(category)) return getCatalogue();
        List<SACatalogueItem> filtered = new ArrayList<>();
        for (SACatalogueItem item : getCatalogue()) {
            if (item.getCategory().equals(category)) filtered.add(item);
        }
        return filtered;
    }

    public static List<String> getCategories() {
        List<String> cats = new ArrayList<>();
        cats.add("All");
        cats.add("Analgesics");
        cats.add("Antibiotics");
        cats.add("Cardiovascular");
        cats.add("Respiratory");
        cats.add("Gastrointestinal");
        cats.add("Diabetes");
        cats.add("Vitamins");
        return cats;
    }
}