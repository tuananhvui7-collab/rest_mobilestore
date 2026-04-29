package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.service.ProductService;
import com.ecommerce.mobile.service.ReviewService;
import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.dto.response.ProductDto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.HashMap;

@RestController
@CrossOrigin("*")
public class ProductController {

    private static final int DEFAULT_PAGE_SIZE = 8;

    @Autowired
    private ProductService productService;

    @Autowired
    private ReviewService reviewService;

    @GetMapping("/api/products")
    public ApiResponse<Page<ProductDto>> listProducts(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", defaultValue = "0") int page) {

        Page<Product> products = productService.getActiveProducts(keyword, page, DEFAULT_PAGE_SIZE);
        Page<ProductDto> dtos = products.map(com.ecommerce.mobile.mapper.ProductMapper::toDto);
        return ApiResponse.success("Lấy danh sách sản phẩm thành công", dtos);
    }

    @GetMapping("/api/products/{id}")
    public ApiResponse<Map<String, Object>> productDetail(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails principal) {

        Product product = productService.getActiveProductDetailById(id);

        if (product == null) {
            return ApiResponse.error(404, "Không tìm thấy sản phẩm");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("product", com.ecommerce.mobile.mapper.ProductMapper.toDto(product));
        data.put("reviews", reviewService.getReviewsForProduct(id));
        data.put("averageRating", reviewService.getAverageRating(id));
        data.put("reviewCount", reviewService.getReviewCount(id));
        if (principal != null) {
            data.put("myReview", reviewService.getMyReview(principal.getUsername(), id));
            data.put("canReview", reviewService.canReviewProduct(principal.getUsername(), id));
        } else {
            data.put("myReview", null);
            data.put("canReview", false);
        }
        
        return ApiResponse.success("Lấy chi tiết sản phẩm thành công", data);
    }
}
