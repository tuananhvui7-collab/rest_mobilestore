package com.ecommerce.mobile.controller;

import com.ecommerce.mobile.dto.manager.ManagerProductForm;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.service.ManagerService;
import com.ecommerce.mobile.response.ApiResponse;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/products")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public ApiResponse<List<Product>> list() {
        return ApiResponse.success("Lấy danh sách sản phẩm thành công", managerService.getAllProducts());
    }

    @GetMapping("/create-info")
    public ApiResponse<Map<String, Object>> createInfo() {
        Map<String, Object> data = new HashMap<>();
        data.put("form", managerService.newProductForm());
        data.put("categories", managerService.getAllCategories());
        return ApiResponse.success("Lấy thông tin form tạo sản phẩm thành công", data);
    }

    @PostMapping("/save")
    public ApiResponse<Product> save(@RequestBody ManagerProductForm form) {
        Product saved = managerService.saveProduct(form);
        return ApiResponse.success("Đã lưu sản phẩm", saved);
    }

    @GetMapping("/{id}")
    public ApiResponse<Product> detail(@PathVariable Long id) {
        return ApiResponse.success("Lấy chi tiết sản phẩm thành công", managerService.getProductDetail(id));
    }

    @GetMapping("/{id}/edit")
    public ApiResponse<Map<String, Object>> editInfo(@PathVariable Long id) {
        Map<String, Object> data = new HashMap<>();
        data.put("form", managerService.loadProductForm(id));
        data.put("categories", managerService.getAllCategories());
        return ApiResponse.success("Lấy thông tin form sửa sản phẩm thành công", data);
    }

    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        managerService.softDeleteProduct(id);
        return ApiResponse.success("Đã chuyển sản phẩm sang INACTIVE", null);
    }

    @PostMapping("/{id}/restore")
    public ApiResponse<Void> restore(@PathVariable Long id) {
        managerService.restoreProduct(id);
        return ApiResponse.success("Đã kích hoạt lại sản phẩm", null);
    }
}
