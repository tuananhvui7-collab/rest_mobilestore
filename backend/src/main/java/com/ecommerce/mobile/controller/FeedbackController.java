package com.ecommerce.mobile.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.entity.Feedback;
import com.ecommerce.mobile.service.FeedbackService;
import com.ecommerce.mobile.response.ApiResponse;

@RestController
@CrossOrigin("*")
@RequestMapping("/api")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping("/profile/feedbacks")
    public ApiResponse<List<Feedback>> customerList(@AuthenticationPrincipal UserDetails principal) {
        List<Feedback> feedbacks = feedbackService.getFeedbacksForCustomer(principal.getUsername());
        return ApiResponse.success("Lấy danh sách phản hồi thành công", feedbacks);
    }

    @PostMapping("/profile/feedbacks")
    public ApiResponse<Void> customerCreate(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String content) {
        feedbackService.createFeedback(principal.getUsername(), content);
        return ApiResponse.success("Đã gửi phản hồi", null);
    }

    @GetMapping("/employee/feedbacks")
    public ApiResponse<Map<String, List<Feedback>>> employeeList(@AuthenticationPrincipal UserDetails principal) {
        Map<String, List<Feedback>> data = new HashMap<>();
        data.put("feedbacks", feedbackService.getPendingFeedbacks());
        data.put("myFeedbacks", feedbackService.getFeedbacksForEmployee(principal.getUsername()));
        return ApiResponse.success("Lấy danh sách phản hồi cho nhân viên thành công", data);
    }

    @PostMapping("/employee/feedbacks/{feedbackId}/assign")
    public ApiResponse<Void> assignToMe(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long feedbackId) {
        feedbackService.assignToMe(principal.getUsername(), feedbackId);
        return ApiResponse.success("Đã nhận xử lý phản hồi", null);
    }

    @GetMapping("/employee/feedbacks/{feedbackId}")
    public ApiResponse<Feedback> employeeDetail(@AuthenticationPrincipal UserDetails principal,
                                 @PathVariable Long feedbackId) {
        Feedback feedback = feedbackService.viewFeedbackForEmployee(principal.getUsername(), feedbackId);
        return ApiResponse.success("Lấy chi tiết phản hồi thành công", feedback);
    }

    @PutMapping("/employee/feedbacks/{feedbackId}/resolve")
    public ApiResponse<Void> resolve(@AuthenticationPrincipal UserDetails principal,
                          @PathVariable Long feedbackId,
                          @RequestParam String resolution) {
        feedbackService.resolveFeedback(principal.getUsername(), feedbackId, resolution);
        return ApiResponse.success("Đã xử lý phản hồi", null);
    }
}
