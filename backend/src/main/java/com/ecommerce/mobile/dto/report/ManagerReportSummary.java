package com.ecommerce.mobile.dto.report;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ManagerReportSummary {
    private long totalOrders;
    private long deliveredOrders;
    private long cancelledOrders;
    private long activeProducts;
    private long totalVariants;
    private long lowStockVariants;
    private long totalStockQty;

    private BigDecimal grossRevenue = BigDecimal.ZERO;
    private BigDecimal realizedRevenue = BigDecimal.ZERO;
    private BigDecimal estimatedCost = BigDecimal.ZERO;
    private BigDecimal estimatedProfit = BigDecimal.ZERO;
    private BigDecimal inventoryValue = BigDecimal.ZERO;
}
