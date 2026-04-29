package com.ecommerce.mobile.dto.report;

import java.util.List;

import lombok.Data;

@Data
public class ManagerReportView {
    private String periodType;
    private String periodLabel;
    private ManagerReportSummary summary;
    private List<ReportPeriodPoint> periodPoints;
    private List<TopProductRow> topProducts;
    private List<StockAlertRow> stockAlerts;
}
