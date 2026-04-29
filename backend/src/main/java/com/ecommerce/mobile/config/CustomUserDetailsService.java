package com.ecommerce.mobile.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ecommerce.mobile.entity.User;
import com.ecommerce.mobile.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService { //debug 1: lỗi chính tả.
    @Autowired
    private UserRepository userRepository; // dùng userRepository để tìm email cho tất cả user.

    // chỉ cần dùng đúng hàm này trong userdetailsservice.
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException{
       // Bước 1 tìm user theo email. Nếu k có trả về lỗi
        User user = userRepository.findByEmail(email). // dependency injection ở đây. (Thay vì tạo thằng user mới. ta gọi thằng repo này và lấy hàm của nó chạy SQL)
        orElseThrow(
            () -> new UsernameNotFoundException("Không tìm thấy tài khoản" + email) 
        );
         // Bước 2:kiểm tra xem tài khoản có bị khóa không . Có thì trả về lỗi
         if (user.getIsActive() == null || !user.getIsActive()){
            throw new UsernameNotFoundException("Tài khoản đã bị khóa !");
         }

         // SPRING SECURITY BẮT CÓ TIỀN TỐ ROLE_ TROGN TÊN QUYỀN
         // security viết hasRole("CUSTOMER") -> SPRING KIỂM TRA "ROLE_CUSTOMER"
         // Bước 3: lấy quyền theo role nếu trải qua 2 bước kiểm duyệt

         // Chỗ này thực ra chưa hiểu lắm.
         // Phân quyền?
         GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().getNameRole());



        // Trả về danh sách Đóng gói lại mang cho spring security config xem. 
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getHashPassword(), 
            List.of(authority) // Danh sách quyền
        );
}
    
    
}
