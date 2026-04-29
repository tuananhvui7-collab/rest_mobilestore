package com.ecommerce.mobile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.service.CustomerService;
import com.ecommerce.mobile.response.ApiResponse;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/profile")
public class ProfileController {

    private final CustomerService customerService;

    public ProfileController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    public ApiResponse<Customer> getProfile(@AuthenticationPrincipal UserDetails principal) {
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        return ApiResponse.success("Lấy thông tin hồ sơ thành công", customer);
    }

    @PostMapping
    public ApiResponse<Void> updateProfile(@AuthenticationPrincipal UserDetails principal,
                                @RequestParam String fullName,
                                @RequestParam(required = false) String phone) {
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        customerService.updateCustomerInfo(customer.getUserID(), fullName, phone);
        return ApiResponse.success("Đã cập nhật thông tin.", null);
    }

    @PostMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal UserDetails principal,
                                 @RequestParam String oldPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            return ApiResponse.error(400, "Mật khẩu mới không được để trống");
        }
        if (!newPassword.equals(confirmPassword)) {
            return ApiResponse.error(400, "Mật khẩu xác nhận không khớp");
        }
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        customerService.changePassword(customer.getUserID(), oldPassword, newPassword);
        return ApiResponse.success("Đã đổi mật khẩu.", null);
    }
}
