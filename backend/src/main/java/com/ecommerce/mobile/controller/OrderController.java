package com.ecommerce.mobile.controller;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.entity.Address;
import com.ecommerce.mobile.entity.Cart;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.entity.Customer;
import com.ecommerce.mobile.enums.PaymentMethod;
import com.ecommerce.mobile.service.CartService;
import com.ecommerce.mobile.service.CustomerService;
import com.ecommerce.mobile.service.OrderService;
import com.ecommerce.mobile.service.VnpayService;

import jakarta.servlet.http.HttpServletRequest;

import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.dto.response.OrderDto;
import com.ecommerce.mobile.mapper.OrderMapper;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/orders")
public class OrderController {

    private final CartService cartService;
    private final CustomerService customerService;
    private final OrderService orderService;

    public OrderController(CartService cartService,
                           CustomerService customerService,
                           OrderService orderService) {
        this.cartService = cartService;
        this.customerService = customerService;
        this.orderService = orderService;
    }

    @GetMapping("/checkout-info")
    public ApiResponse<Map<String, Object>> checkoutInfo(@AuthenticationPrincipal UserDetails principal,
                           @RequestParam(required = false) Long addressId) {
        Customer customer = customerService.requireCustomerByEmail(principal.getUsername());
        Cart cart = cartService.getCartByCustomerEmail(principal.getUsername());

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            return ApiResponse.error(400, "Giỏ hàng đang trống");
        }

        List<Address> addresses = customerService.getAddresses(customer.getUserID());
        Address selectedAddress = null;
        if (addressId != null) {
            selectedAddress = customerService.getAddressForCustomer(customer.getUserID(), addressId);
        } else if (!addresses.isEmpty()) {
            selectedAddress = addresses.stream()
                    .filter(a -> Boolean.TRUE.equals(a.getIsDefault()))
                    .findFirst()
                    .orElse(addresses.get(0));
        }

        Map<String, Object> data = new HashMap<>();
        data.put("addresses", addresses);
        data.put("selectedAddressId", selectedAddress == null ? null : selectedAddress.getAddressId());
        data.put("cart", cart);
        data.put("total", cartService.calculateTotal(cart));
        data.put("shippingName", customer.getFullName());
        data.put("shippingPhone", selectedAddress != null && selectedAddress.getPhone() != null
                ? selectedAddress.getPhone()
                : customer.getPhone());
        data.put("shippingAddress", selectedAddress != null ? composeAddress(selectedAddress) : "");
        data.put("shippingWard", selectedAddress != null && selectedAddress.getWard() != null ? selectedAddress.getWard() : "");
        data.put("shippingDistrict", selectedAddress != null && selectedAddress.getDistrict() != null ? selectedAddress.getDistrict() : "");
        data.put("shippingCity", selectedAddress != null && selectedAddress.getCity() != null ? selectedAddress.getCity() : "");
        
        return ApiResponse.success("Lấy thông tin thanh toán thành công", data);
    }

@PostMapping("/place")
    public ApiResponse<Map<String, Object>> placeOrder(@AuthenticationPrincipal UserDetails principal,
                                         @RequestParam String shippingName,
                                         @RequestParam String shippingPhone,
                                         @RequestParam String shippingAddress,
                                         @RequestParam(required = false) String shippingWard,
                                         @RequestParam(required = false) String shippingDistrict,
                                         @RequestParam String shippingCity,
                                         @RequestParam(required = false) String voucherCode,
                                         @RequestParam String paymentMethod,
                                         @RequestParam Long cartId,
                                         HttpServletRequest request) { // Thêm HttpServletRequest để VNPAY lấy IP
        
        PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
        Order order = orderService.placeOrder(
                principal.getUsername(), shippingName, shippingPhone,
                shippingAddress, shippingWard, shippingDistrict,
                shippingCity, voucherCode, method, cartId
        );
        
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("order", OrderMapper.toDto(order));
        
        // NẾU LÀ VNPAY: Sinh link thanh toán ngay lập tức
        if (method == PaymentMethod.VN_PAY && !order.getPayments().isEmpty()) {
            String paymentUrl = VnpayService.createPaymentUrl(order.getPayments().get(0), request);
            responseData.put("paymentUrl", paymentUrl);
        }
        
        return ApiResponse.success("Đặt hàng thành công", responseData);
    }
    @GetMapping
    public ApiResponse<List<OrderDto>> orderList(@AuthenticationPrincipal UserDetails principal) {
        List<Order> orders = orderService.getOrdersByCustomerEmail(principal.getUsername());
        List<OrderDto> dtos = orders.stream().map(OrderMapper::toDto).collect(Collectors.toList());
        return ApiResponse.success("Lấy danh sách đơn hàng thành công", dtos);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderDto> orderDetail(@AuthenticationPrincipal UserDetails principal,
                              @PathVariable Long orderId) {
        Order order = orderService.getOrderDetailByCustomerEmail(principal.getUsername(), orderId);
        if (order == null) {
            return ApiResponse.error(404, "Không tìm thấy đơn hàng");
        }
        return ApiResponse.success("Lấy thông tin đơn hàng thành công", OrderMapper.toDto(order));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderDto> cancelOrder(@AuthenticationPrincipal UserDetails principal,
                              @PathVariable Long orderId,
                              @RequestParam(required = false) String reason) {
        Order order = orderService.cancelOrderByCustomerEmail(principal.getUsername(), orderId, reason);
        return ApiResponse.success("Đã hủy đơn hàng", OrderMapper.toDto(order));
    }

    private String composeAddress(Address address) {
        StringBuilder sb = new StringBuilder();
        if (address.getStreet() != null && !address.getStreet().isBlank()) {
            sb.append(address.getStreet().trim());
        }
        if (address.getWard() != null && !address.getWard().isBlank()) {
            appendWithComma(sb, address.getWard().trim());
        }
        if (address.getDistrict() != null && !address.getDistrict().isBlank()) {
            appendWithComma(sb, address.getDistrict().trim());
        }
        return sb.toString();
    }

    private void appendWithComma(StringBuilder sb, String value) {
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(value);
    }
}
