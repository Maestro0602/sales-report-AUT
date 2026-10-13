package edu.itc.salesreport.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SaleTransactionTest {

    @Test
    void roundingCalculatesHalfUp() {
        var tx = new SaleTransaction(
                "PNH",
                LocalDate.of(2026, 9, 1),
                "PNH-000001",
                "SKU-1001",
                "Item",
                "Grocery",
                3,
                new BigDecimal("0.335"),
                new BigDecimal("0.00"),
                PaymentMethod.CASH);
        assertEquals(new BigDecimal("1.01"), tx.revenue());
    }

    @Test
    void branchSummaryTopProductsIsUnmodifiable() {
        var product = new ProductTotal("SKU-1001", "Jasmine Rice 5kg", new BigDecimal("13.00"), 2);
        var summary = new BranchSummary(
                "PNH",
                new BigDecimal("13.00"),
                new BigDecimal("0.00"),
                1,
                Map.of("Grocery", new BigDecimal("13.00")),
                List.of(product),
                Map.of(PaymentMethod.KHQR, new BigDecimal("13.00")));

        assertThrows(UnsupportedOperationException.class, () ->
                summary.topProducts().add(new ProductTotal("SKU-1002", "Fish Sauce", new BigDecimal("1.80"), 1)));
    }

    @Test
    void compactConstructorRejectsInvalidInvariants() {
        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "PNH", LocalDate.of(2026, 9, 1), "PNH-1", "SKU-1", "Item", "Grocery",
                0, new BigDecimal("5.00"), new BigDecimal("0.00"), PaymentMethod.CASH));

        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "PNH", LocalDate.of(2026, 9, 1), "PNH-1", "SKU-1", "Item", "Grocery",
                1, new BigDecimal("-5.00"), new BigDecimal("0.00"), PaymentMethod.CASH));

        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "PNH", LocalDate.of(2026, 9, 1), "PNH-1", "SKU-1", "Item", "Grocery",
                1, new BigDecimal("5.00"), new BigDecimal("-1.00"), PaymentMethod.CASH));

        assertThrows(IllegalArgumentException.class, () -> new SaleTransaction(
                "PNH", LocalDate.of(2026, 9, 1), "PNH-1", "SKU-1", "Item", "Grocery",
                1, new BigDecimal("5.00"), new BigDecimal("6.00"), PaymentMethod.CASH));
    }
}
