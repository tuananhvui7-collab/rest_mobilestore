package com.ecommerce.mobile.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.mobile.dto.report.ManagerReportSummary;
import com.ecommerce.mobile.dto.report.ManagerReportView;
import com.ecommerce.mobile.dto.report.ReportPeriodPoint;
import com.ecommerce.mobile.dto.report.StockAlertRow;
import com.ecommerce.mobile.dto.report.TopProductRow;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.OrderItem;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.entity.ProductVariant;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.enums.ProductStatus;
import com.ecommerce.mobile.repository.OrderRepository;
import com.ecommerce.mobile.repository.ProductRepository;
import com.ecommerce.mobile.repository.ProductVariantRepository;

@Service
public class ReportService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    public ReportService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         ProductVariantRepository productVariantRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional(readOnly = true)
    public ManagerReportView getManagerReport() {
        return getManagerReport("month");
    }

    @Transactional(readOnly = true)
    public ManagerReportView getManagerReport(String periodTypeRaw) {
        ReportPeriodType periodType = ReportPeriodType.from(periodTypeRaw);

        List<Order> allOrders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<Order> reportableOrders = allOrders.stream()
                .filter(order -> order != null && order.getStatus() != OrderStatus.CANCELLED)
                .toList();
        List<Order> deliveredOrders = allOrders.stream()
                .filter(order -> order != null && order.getStatus() == OrderStatus.DELIVERED)
                .toList();

        List<Product> allProducts = productRepository.findAll();
        List<ProductVariant> allVariants = productVariantRepository.findAll();

        ManagerReportSummary summary = buildSummary(reportableOrders, deliveredOrders, allProducts, allVariants);
        List<ReportPeriodPoint> periodPoints = buildPeriodPoints(reportableOrders, deliveredOrders, periodType);
        List<TopProductRow> topProducts = buildTopProducts(deliveredOrders);
        List<StockAlertRow> stockAlerts = buildStockAlerts(allVariants);

        ManagerReportView view = new ManagerReportView();
        view.setPeriodType(periodType.code);
        view.setPeriodLabel(periodType.displayLabel);
        view.setSummary(summary);
        view.setPeriodPoints(periodPoints);
        view.setTopProducts(topProducts);
        view.setStockAlerts(stockAlerts);
        return view;
    }

    @Transactional(readOnly = true)
    public byte[] exportManagerReportExcel(String periodTypeRaw) {
        ManagerReportView view = getManagerReport(periodTypeRaw);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSummarySheet(workbook, view);
            writePeriodSheet(workbook, view);
            writeTopProductsSheet(workbook, view);
            writeStockAlertsSheet(workbook, view);
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể xuất báo cáo Excel", ex);
        }
    }

    private ManagerReportSummary buildSummary(List<Order> reportableOrders,
                                              List<Order> deliveredOrders,
                                              List<Product> allProducts,
                                              List<ProductVariant> allVariants) {
        ManagerReportSummary summary = new ManagerReportSummary();
        summary.setTotalOrders(reportableOrders.size());
        summary.setDeliveredOrders(deliveredOrders.size());
        summary.setCancelledOrders((long) orderRepository.findAll().stream()
                .filter(order -> order != null && order.getStatus() == OrderStatus.CANCELLED)
                .count());
        summary.setActiveProducts((long) allProducts.stream()
                .filter(product -> product != null && product.getStatus() == ProductStatus.ACTIVE)
                .count());
        summary.setTotalVariants(allVariants.size());
        summary.setLowStockVariants(allVariants.stream().filter(this::isLowStock).count());
        summary.setTotalStockQty(allVariants.stream()
                .mapToLong(variant -> variant == null || variant.getStockQty() == null ? 0L : variant.getStockQty())
                .sum());

        BigDecimal grossRevenue = sumOrders(reportableOrders);
        BigDecimal realizedRevenue = sumOrders(deliveredOrders);
        BigDecimal realizedCost = sumDeliveryCost(deliveredOrders);
        BigDecimal inventoryValue = sumInventoryValue(allVariants);

        summary.setGrossRevenue(grossRevenue);
        summary.setRealizedRevenue(realizedRevenue);
        summary.setEstimatedCost(realizedCost);
        summary.setEstimatedProfit(realizedRevenue.subtract(realizedCost));
        summary.setInventoryValue(inventoryValue);
        return summary;
    }

    private List<ReportPeriodPoint> buildPeriodPoints(List<Order> reportableOrders,
                                                      List<Order> deliveredOrders,
                                                      ReportPeriodType periodType) {
        Map<String, ReportPeriodPoint> points = new LinkedHashMap<>();
        for (PeriodSlot slot : buildPeriodSlots(periodType)) {
            ReportPeriodPoint point = new ReportPeriodPoint();
            point.setLabel(slot.label());
            points.put(slot.key(), point);
        }

        for (Order order : reportableOrders) {
            if (order == null || order.getCreatedAt() == null) {
                continue;
            }
            String key = periodKey(order.getCreatedAt(), periodType);
            ReportPeriodPoint point = points.get(key);
            if (point != null) {
                point.setOrders(point.getOrders() + 1);
                point.setGrossRevenue(point.getGrossRevenue().add(safeBigDecimal(order.getTotalAmount())));
            }
        }

        for (Order order : deliveredOrders) {
            if (order == null || order.getCreatedAt() == null) {
                continue;
            }
            String key = periodKey(order.getCreatedAt(), periodType);
            ReportPeriodPoint point = points.get(key);
            if (point != null) {
                point.setRealizedRevenue(point.getRealizedRevenue().add(safeBigDecimal(order.getTotalAmount())));
                point.setEstimatedProfit(point.getEstimatedProfit().add(calculateOrderProfit(order)));
            }
        }

        return new ArrayList<>(points.values());
    }

    private List<PeriodSlot> buildPeriodSlots(ReportPeriodType periodType) {
        List<PeriodSlot> slots = new ArrayList<>();
        switch (periodType) {
            case MONTH -> {
                YearMonth current = YearMonth.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy", Locale.getDefault());
                for (int i = periodType.window - 1; i >= 0; i--) {
                    YearMonth month = current.minusMonths(i);
                    slots.add(new PeriodSlot(month.toString(), month.format(formatter)));
                }
            }
            case QUARTER -> {
                int currentIndex = Year.now().getValue() * 4 + currentQuarterIndex(LocalDateTime.now()) - 1;
                for (int i = periodType.window - 1; i >= 0; i--) {
                    int index = currentIndex - i;
                    int year = index / 4;
                    int quarter = index % 4 + 1;
                    slots.add(new PeriodSlot(year + "-Q" + quarter, "Q" + quarter + "/" + year));
                }
            }
            case YEAR -> {
                int currentYear = Year.now().getValue();
                for (int i = periodType.window - 1; i >= 0; i--) {
                    int year = currentYear - i;
                    slots.add(new PeriodSlot(String.valueOf(year), String.valueOf(year)));
                }
            }
        }
        return slots;
    }

    private String periodKey(LocalDateTime dateTime, ReportPeriodType periodType) {
        switch (periodType) {
            case MONTH -> {
                return YearMonth.from(dateTime).toString();
            }
            case QUARTER -> {
                int quarter = currentQuarterIndex(dateTime);
                return dateTime.getYear() + "-Q" + quarter;
            }
            case YEAR -> {
                return String.valueOf(dateTime.getYear());
            }
            default -> throw new IllegalStateException("Unsupported period type");
        }
    }

    private int currentQuarterIndex(LocalDateTime dateTime) {
        return ((dateTime.getMonthValue() - 1) / 3) + 1;
    }

    private List<TopProductRow> buildTopProducts(List<Order> deliveredOrders) {
        Map<Long, TopProductRow> byProductId = new LinkedHashMap<>();
        for (Order order : deliveredOrders) {
            if (order == null || order.getItems() == null) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (item == null || item.getVariant() == null) {
                    continue;
                }
                Product product = item.getVariant().getProduct();
                Long productId = product == null ? null : product.getProductId();
                if (productId == null) {
                    continue;
                }

                TopProductRow row = byProductId.computeIfAbsent(productId, id -> {
                    TopProductRow created = new TopProductRow();
                    created.setProductId(id);
                    created.setProductName(item.getProductName() != null ? item.getProductName()
                            : (product.getName() != null ? product.getName() : "Sản phẩm"));
                    created.setBrand(product.getBrand());
                    return created;
                });

                long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
                row.setQuantitySold(row.getQuantitySold() + quantity);
                row.setGrossRevenue(row.getGrossRevenue().add(safeBigDecimal(item.getSubtotal())));

                BigDecimal importPrice = safeBigDecimal(item.getVariant().getImportPrice());
                BigDecimal cost = importPrice.multiply(BigDecimal.valueOf(quantity));
                row.setEstimatedCost(row.getEstimatedCost().add(cost));
                row.setEstimatedProfit(row.getGrossRevenue().subtract(row.getEstimatedCost()));
            }
        }

        return byProductId.values().stream()
                .sorted(Comparator.comparing(TopProductRow::getQuantitySold).reversed()
                        .thenComparing(TopProductRow::getGrossRevenue, Comparator.reverseOrder()))
                .limit(10)
                .toList();
    }

    private List<StockAlertRow> buildStockAlerts(List<ProductVariant> allVariants) {
        return allVariants.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((ProductVariant v) -> v.getStockQty() == null ? Integer.MAX_VALUE : v.getStockQty())
                        .thenComparing(v -> v.getSku() == null ? "" : v.getSku()))
                .filter(this::isLowStock)
                .map(variant -> {
                    StockAlertRow row = new StockAlertRow();
                    row.setVariantId(variant.getVariant_id());
                    row.setSku(variant.getSku());
                    row.setStorageGb(variant.getStorage_gb());
                    row.setStockQty(variant.getStockQty());
                    row.setPrice(safeBigDecimal(variant.getPrice()));
                    row.setImportPrice(safeBigDecimal(variant.getImportPrice()));
                    row.setInventoryValue(safeBigDecimal(variant.getImportPrice())
                            .multiply(BigDecimal.valueOf(variant.getStockQty() == null ? 0 : variant.getStockQty())));
                    row.setProductName(variant.getProduct() != null ? variant.getProduct().getName() : "—");
                    return row;
                })
                .limit(12)
                .toList();
    }

    private boolean isLowStock(ProductVariant variant) {
        return variant != null
                && variant.getStockQty() != null
                && variant.getStockQty() <= LOW_STOCK_THRESHOLD;
    }

    private BigDecimal sumOrders(List<Order> orders) {
        return orders.stream()
                .map(order -> safeBigDecimal(order.getTotalAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumDeliveryCost(List<Order> orders) {
        BigDecimal total = BigDecimal.ZERO;
        for (Order order : orders) {
            if (order == null || order.getItems() == null) {
                continue;
            }
            for (OrderItem item : order.getItems()) {
                if (item == null || item.getVariant() == null) {
                    continue;
                }
                BigDecimal importPrice = safeBigDecimal(item.getVariant().getImportPrice());
                long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
                total = total.add(importPrice.multiply(BigDecimal.valueOf(quantity)));
            }
        }
        return total;
    }

    private BigDecimal sumInventoryValue(List<ProductVariant> variants) {
        BigDecimal total = BigDecimal.ZERO;
        for (ProductVariant variant : variants) {
            if (variant == null) {
                continue;
            }
            BigDecimal importPrice = safeBigDecimal(variant.getImportPrice());
            long stock = variant.getStockQty() == null ? 0L : variant.getStockQty();
            total = total.add(importPrice.multiply(BigDecimal.valueOf(stock)));
        }
        return total;
    }

    private BigDecimal calculateOrderProfit(Order order) {
        if (order == null || order.getItems() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal revenue = safeBigDecimal(order.getTotalAmount());
        BigDecimal cost = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            if (item == null || item.getVariant() == null) {
                continue;
            }
            BigDecimal importPrice = safeBigDecimal(item.getVariant().getImportPrice());
            long quantity = item.getQuantity() == null ? 0L : item.getQuantity();
            cost = cost.add(importPrice.multiply(BigDecimal.valueOf(quantity)));
        }
        return revenue.subtract(cost);
    }

    private BigDecimal safeBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private void writeSummarySheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Summary");
        CellStyle headerStyle = createHeaderStyle(workbook);
        int rowNum = 0;

        Row title = sheet.createRow(rowNum++);
        title.createCell(0).setCellValue("PhoneShop Business Report");
        title.createCell(1).setCellValue(view.getPeriodLabel() == null ? "month" : view.getPeriodLabel());

        rowNum++;
        rowNum = writeKeyValue(sheet, rowNum, "Tổng đơn hàng", String.valueOf(view.getSummary().getTotalOrders()));
        rowNum = writeKeyValue(sheet, rowNum, "Đơn đã giao", String.valueOf(view.getSummary().getDeliveredOrders()));
        rowNum = writeKeyValue(sheet, rowNum, "Đơn đã hủy", String.valueOf(view.getSummary().getCancelledOrders()));
        rowNum = writeKeyValue(sheet, rowNum, "Sản phẩm active", String.valueOf(view.getSummary().getActiveProducts()));
        rowNum = writeKeyValue(sheet, rowNum, "Biến thể", String.valueOf(view.getSummary().getTotalVariants()));
        rowNum = writeKeyValue(sheet, rowNum, "Biến thể tồn thấp", String.valueOf(view.getSummary().getLowStockVariants()));
        rowNum = writeKeyValue(sheet, rowNum, "Tổng tồn kho", String.valueOf(view.getSummary().getTotalStockQty()));
        rowNum = writeKeyValue(sheet, rowNum, "Doanh số", formatMoney(view.getSummary().getGrossRevenue()));
        rowNum = writeKeyValue(sheet, rowNum, "Doanh thu thực nhận", formatMoney(view.getSummary().getRealizedRevenue()));
        rowNum = writeKeyValue(sheet, rowNum, "Chi phí ước tính", formatMoney(view.getSummary().getEstimatedCost()));
        rowNum = writeKeyValue(sheet, rowNum, "Lợi nhuận ước tính", formatMoney(view.getSummary().getEstimatedProfit()));
        writeKeyValue(sheet, rowNum, "Giá trị tồn kho", formatMoney(view.getSummary().getInventoryValue()));

        autoSize(sheet, 2);
        sheet.getRow(0).getCell(0).setCellStyle(headerStyle);
        sheet.getRow(0).getCell(1).setCellStyle(headerStyle);
    }

    private void writePeriodSheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Period");
        createTableHeader(sheet, "Kỳ", "Đơn hàng", "Doanh số", "Doanh thu thực nhận", "Lợi nhuận ước tính");
        int rowNum = 1;
        for (ReportPeriodPoint point : view.getPeriodPoints()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(point.getLabel());
            row.createCell(1).setCellValue(point.getOrders());
            row.createCell(2).setCellValue(point.getGrossRevenue().doubleValue());
            row.createCell(3).setCellValue(point.getRealizedRevenue().doubleValue());
            row.createCell(4).setCellValue(point.getEstimatedProfit().doubleValue());
        }
        autoSize(sheet, 5);
    }

    private void writeTopProductsSheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Top Products");
        createTableHeader(sheet, "Sản phẩm", "Hãng", "SL bán", "Doanh số", "Chi phí", "Lợi nhuận");
        int rowNum = 1;
        for (TopProductRow rowData : view.getTopProducts()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowData.getProductName());
            row.createCell(1).setCellValue(rowData.getBrand() == null ? "" : rowData.getBrand());
            row.createCell(2).setCellValue(rowData.getQuantitySold());
            row.createCell(3).setCellValue(rowData.getGrossRevenue().doubleValue());
            row.createCell(4).setCellValue(rowData.getEstimatedCost().doubleValue());
            row.createCell(5).setCellValue(rowData.getEstimatedProfit().doubleValue());
        }
        autoSize(sheet, 6);
    }

    private void writeStockAlertsSheet(Workbook workbook, ManagerReportView view) {
        Sheet sheet = workbook.createSheet("Stock Alerts");
        createTableHeader(sheet, "Sản phẩm", "SKU", "Dung lượng", "Tồn kho", "Giá", "Giá nhập", "Giá trị tồn");
        int rowNum = 1;
        for (StockAlertRow rowData : view.getStockAlerts()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowData.getProductName());
            row.createCell(1).setCellValue(rowData.getSku() == null ? "" : rowData.getSku());
            row.createCell(2).setCellValue(rowData.getStorageGb() == null ? "" : rowData.getStorageGb() + " GB");
            row.createCell(3).setCellValue(rowData.getStockQty() == null ? 0 : rowData.getStockQty());
            row.createCell(4).setCellValue(rowData.getPrice().doubleValue());
            row.createCell(5).setCellValue(rowData.getImportPrice().doubleValue());
            row.createCell(6).setCellValue(rowData.getInventoryValue().doubleValue());
        }
        autoSize(sheet, 7);
    }

    private void createTableHeader(Sheet sheet, String... headers) {
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            header.createCell(i).setCellValue(headers[i]);
        }
    }

    private int writeKeyValue(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return rowNum;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void autoSize(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String formatMoney(BigDecimal value) {
        return safeBigDecimal(value).toPlainString();
    }

    private enum ReportPeriodType {
        MONTH("month", "Báo cáo theo tháng", 6),
        QUARTER("quarter", "Báo cáo theo quý", 6),
        YEAR("year", "Báo cáo theo năm", 5);

        private final String code;
        private final String displayLabel;
        private final int window;

        ReportPeriodType(String code, String displayLabel, int window) {
            this.code = code;
            this.displayLabel = displayLabel;
            this.window = window;
        }

        private static ReportPeriodType from(String raw) {
            if (raw == null || raw.isBlank()) {
                return MONTH;
            }
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "quarter", "quý", "qui" -> QUARTER;
                case "year", "năm", "nam" -> YEAR;
                default -> MONTH;
            };
        }
    }

    private record PeriodSlot(String key, String label) {}
}
