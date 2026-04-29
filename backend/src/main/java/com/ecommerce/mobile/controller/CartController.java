package com.ecommerce.mobile.controller;

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

import com.ecommerce.mobile.entity.Cart;
import com.ecommerce.mobile.service.CartService;
import com.ecommerce.mobile.response.ApiResponse;

import java.util.Map;
import java.util.HashMap;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> viewCart(@AuthenticationPrincipal UserDetails principal) {
        Cart cart = cartService.getCartByCustomerEmail(principal.getUsername());
        Map<String, Object> data = new HashMap<>();
        data.put("cart", cart);
        data.put("total", cartService.calculateTotal(cart));
        return ApiResponse.success("Lấy thông tin giỏ hàng thành công", data);
    }

    @PostMapping("/add/{variantId}")
    public ApiResponse<Void> addToCart(@AuthenticationPrincipal UserDetails principal,
                            @PathVariable Long variantId,
                            @RequestParam(defaultValue = "1") Integer quantity) {
        cartService.addToCart(principal.getUsername(), variantId, quantity);
        return ApiResponse.success("Đã thêm vào giỏ hàng", null);
    }

    @PutMapping("/update/{itemId}")
    public ApiResponse<Void> updateItem(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long itemId,
                             @RequestParam Integer quantity) {
        cartService.updateItemQuantity(principal.getUsername(), itemId, quantity);
        return ApiResponse.success("Đã cập nhật giỏ hàng", null);
    }

    @DeleteMapping("/remove/{itemId}")
    public ApiResponse<Void> removeItem(@AuthenticationPrincipal UserDetails principal,
                             @PathVariable Long itemId) {
        cartService.removeItem(principal.getUsername(), itemId);
        return ApiResponse.success("Đã xóa sản phẩm khỏi giỏ", null);
    }
}
