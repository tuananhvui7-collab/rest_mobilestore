package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.dto.report.ManagerReportView;
import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.service.ReportService;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public ApiResponse<ManagerReportView> report(@RequestParam(name = "period", defaultValue = "month") String period) {
        ManagerReportView view = reportService.getManagerReport(period);
        return ApiResponse.success("Lấy báo cáo thành công", view);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(name = "period", defaultValue = "month") String period) {
        byte[] bytes = reportService.exportManagerReportExcel(period);
        String fileName = "phoneshop-report-" + period + ".xlsx";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }
}
