
package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/register")
    public ApiResponse<String> processRegister(@RequestParam String email, @RequestParam String password, @RequestParam String fullName, @RequestParam String phone) {
        customerService.register(email, password, fullName, phone);
        return ApiResponse.success("Registration successful", null);
    }
}
