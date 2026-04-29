package com.ecommerce.mobile.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration: day la class cau hinh, Spring doc khi khoi dong
/// QUY TRÌNH
/// INJECTION THẰNG UserDetailsService
/// KHAI BÁO BEAN CHO HÀM MÃ HÓA MK
/// KHAI BÁO BEAN CHO HÀM TẠO CHUỖI CẤU HÌNH.
    /// CẤU HÌNH PHÂN QUYỀN (AUTHORIZE, AUTHENTICATED,REQUESTMATCHER, HAS ROLE, PERMITALL(),  ANYREQUEST,...)
    /// CẤU HÌNH LOGIN (THÀNH CÔNG, THẤT BẠI,XỬ LÝ FORM,...)
    /// CẤU HÌNH LOGOUT.(THÀNH CÔNG, THẤT BẠI, HỦY SESSION, XÓA COOKIES)
/// 
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import org.springframework.security.config.Customizer;

@Configuration
public class SecurityConfig {
    @Autowired
    private CustomUserDetailsService customUserDetailsService; 

    @Autowired
    private RoleBasedSuccessHandler roleBasedSuccessHandler;

    @Autowired
    private JsonAuthFailureHandler jsonAuthFailureHandler;

    @Autowired
    private JsonLogoutSuccessHandler jsonLogoutSuccessHandler;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .userDetailsService(customUserDetailsService)// cái này phải gọi cái object này mới đúng
            // ===== PHAN QUYEN URL =====
            .authorizeHttpRequests(auth -> auth

                // REVIEW: endpoint đánh giá sản phẩm — yêu cầu CUSTOMER
                .requestMatchers(
                    "/api/products/*/reviews/**",
                    "/api/products/*/reviews"
                ).hasRole("CUSTOMER")

                // CONG KHAI -- ai cung vao duoc, khong can dang nhap
                .requestMatchers(
                    "/",               // Trang chu API
                    "/api/products/**",// Xem san pham
                    "/login",          // Trang dang nhap
                    "/register",       // Trang dang ky
                    "/webhook/ghn/**", // GHN webhook
                    "/webhooks/ghn/**",
                    "/api/payments/vnpay/return", // VNPAY return URL (callback công khai)
                    "/api/payments/vnpay/ipn",    // VNPAY IPN (server-to-server)
                    "/assets/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/*.html",             // Frontend HTML pages
                    "/admin/*.html",       // Admin HTML pages
                    "/api/public/**",
                    "/swagger-ui/**",
                    "/v3/api-docs/**"
                ).permitAll()

                // ADMIN (Manager) — quản trị sản phẩm, nhân viên, báo cáo
                .requestMatchers(
                    "/api/admin/**"
                ).hasRole("MANAGER")

                // STAFF — quản lý đơn hàng (cả EMPLOYEE và MANAGER đều truy cập được)
                .requestMatchers(
                    "/api/staff/**"
                ).hasAnyRole("EMPLOYEE", "MANAGER")

                // EMPLOYEE — xử lý phản hồi
                .requestMatchers(
                    "/api/employee/**"
                ).hasRole("EMPLOYEE")

                // CUSTOMER — giỏ hàng, đơn hàng, thanh toán, hồ sơ
                .requestMatchers(
                    "/api/payments/**",
                    "/api/profile/**",
                    "/api/cart/**",
                    "/api/orders/**"
                ).hasRole("CUSTOMER")

                // TAT CA CON LAI: phai dang nhap
                .anyRequest().authenticated()
            )

            // ===== CAU HINH FORM DANG NHAP (REST API) =====
            .formLogin(form -> form
                .loginProcessingUrl("/login") // URL xu ly POST form
                .successHandler(roleBasedSuccessHandler) // Tra JSON khi login thanh cong
                .failureHandler(jsonAuthFailureHandler)  // Tra JSON khi login that bai
                .permitAll()
            )

            // ===== CAU HINH DANG XUAT (REST API) =====
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler(jsonLogoutSuccessHandler) // Tra JSON khi logout
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build(); //xây dựng web cấu hình từ chuỗi (securityFilterChain )
    }
}
