package com.ecommerce.mobile.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.entity.Address;
import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.service.CustomerService;
import com.ecommerce.mobile.response.ApiResponse;
import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/profile/addresses")
@Tag(name = "Sổ Địa Chỉ (Address)", description = "Các API quản lý địa chỉ nhận hàng của khách hàng")
public class AddressController {

    private final CustomerService customerService;

    public AddressController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Lấy danh sách địa chỉ", description = "Trả về danh sách tất cả địa chỉ của khách hàng đang đăng nhập")
    public ApiResponse<List<Address>> list(@Parameter(hidden = true) @AuthenticationPrincipal UserDetails principal) {
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        List<Address> addresses = customerService.getAddresses(customer.getUserID());
        return ApiResponse.success("Lấy danh sách địa chỉ thành công", addresses);
    }

    @PostMapping("/add")
    public ApiResponse<Void> add(@AuthenticationPrincipal UserDetails principal,
                      @RequestParam String street,
                      @RequestParam(required = false) String ward,
                      @RequestParam(required = false) String district,
                      @RequestParam String city,
                      @RequestParam(required = false) String phone,
                      @RequestParam(defaultValue = "false") boolean setDefault) {
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        customerService.addAddress(customer.getUserID(), street, ward, district, city, phone, setDefault);
        return ApiResponse.success("Đã thêm địa chỉ", null);
    }

    @PutMapping("/{addressId}/edit")
    public ApiResponse<Void> edit(@AuthenticationPrincipal UserDetails principal,
                       @PathVariable Long addressId,
                       @RequestParam String street,
                       @RequestParam(required = false) String ward,
                       @RequestParam(required = false) String district,
                       @RequestParam String city,
                       @RequestParam(required = false) String phone) {
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        customerService.updateAddress(customer.getUserID(), addressId, street, ward, district, city, phone);
        return ApiResponse.success("Đã cập nhật địa chỉ", null);
    }

    @DeleteMapping("/{addressId}/delete")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserDetails principal,
                         @PathVariable Long addressId) {
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        customerService.deleteAddress(customer.getUserID(), addressId);
        return ApiResponse.success("Đã xóa địa chỉ", null);
    }

    @PostMapping("/{addressId}/default")
    public ApiResponse<Void> setDefault(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long addressId) {
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        customerService.setDefaultAddress(customer.getUserID(), addressId);
        return ApiResponse.success("Đã đặt địa chỉ mặc định", null);
    }
}
