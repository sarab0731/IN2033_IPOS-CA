package integration;

import database.DatabaseManager;
import database.RestockOrderDB;
import domain.CatalogueItem;
import domain.DiscountPlan;
import integration.interfaces.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class Class {

    // IOrderService — handles restock orders via SA
    public static class OrderServiceImpl implements IOrderService {

        @Override
        public String placeRestockOrder(String merchantID, String orderDetails) {
            return "OK: Order received for merchant " + merchantID;
        }

        @Override
        public String trackDelivery(String orderID) {
            String status = new OrderStatusImpl().getOrderStatus(orderID);
            return status;
        }

        @Override
        public double queryOutstandingBalance(String merchantID) {
            String sql = """
                SELECT COALESCE(SUM(total_value), 0) AS balance
                FROM restock_orders
                WHERE merchant_id = ? AND status != 'DELIVERED'
                """;

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, merchantID);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) return rs.getDouble("balance");

            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }

        @Override
        public String getInvoice(String orderID) {
            return "INV-" + orderID;
        }

        @Override
        public boolean getAccStatus(String merchantID, String status) {
            return "ACTIVE".equals(status);
        }

        @Override
        public boolean viewDiscountPlan(String merchantID, DiscountPlan plan) {
            return plan != null;
        }
    }

    // IPaymentProcessing — processes card payments via SA
    public static class PaymentProcessingImpl implements IPaymentProcessing {

        @Override
        public String requestPayment(String merchantID, String orderID, String fullName,
                                     String address, String[] cardDetails, double amount) {
            if (cardDetails == null || cardDetails.length < 4) {
                return "ERROR: Invalid card details";
            }
            if (amount <= 0) {
                return "ERROR: Invalid amount";
            }

            return "OK: Payment of " + String.format("%.2f", amount)
                    + " processed for order " + orderID;
        }
    }

    // IEmailService — sends emails via PU
    public static class EmailServiceImpl implements IEmailService {

        @Override
        public boolean produceEmail(String email, String content, String reference,
                                    String sender, String subsystem) {
            return email != null && !email.isEmpty();
        }
    }

    // ISMTPConnection — SMTP relay via PU
    public static class SMTPConnectionImpl implements ISMTPConnection {

        @Override
        public boolean sendEmail(String email, String content, String reference, String sender) {
            return email != null && !email.isEmpty();
        }
    }

    // ICommercialMembershipService — forwards membership applications via PU to SA
    public static class CommercialMembershipServiceImpl implements ICommercialMembershipService {

        @Override
        public boolean requestMembership(String[] candidate) {
            if (candidate == null || candidate.length == 0) {
                return false;
            }
            return true;
        }
    }
}
