package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.response.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
public class HomeController {
    @GetMapping("/")
    public ApiResponse<String> home() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            String roles = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.joining(","));
            return ApiResponse.success("Welcome to PhoneShop API. You are authenticated.", roles);
        }

        return ApiResponse.success("Welcome to PhoneShop API", "Guest");
    }
}
