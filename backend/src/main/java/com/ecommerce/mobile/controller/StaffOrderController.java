package com.ecommerce.mobile.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.dto.response.OrderDto;
import com.ecommerce.mobile.entity.Order;
import com.ecommerce.mobile.enums.OrderStatus;
import com.ecommerce.mobile.mapper.OrderMapper;
import com.ecommerce.mobile.response.ApiResponse;
import com.ecommerce.mobile.service.OrderService;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/staff/orders")
public class StaffOrderController {

    private final OrderService orderService;

    public StaffOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<List<OrderDto>> list() {
        List<Order> orders = orderService.getAllOrdersForStaff();
        List<OrderDto> dtos = orders.stream().map(OrderMapper::toDto).collect(Collectors.toList());
        return ApiResponse.success("Lấy danh sách đơn hàng thành công", dtos);
    }

    @GetMapping("/{orderId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long orderId) {
        Order order = orderService.getOrderForStaff(orderId);
        if (order == null) {
            return ApiResponse.error(404, "Không tìm thấy đơn hàng");
        }

        Map<String, Object> data = new HashMap<>();
        data.put("order", OrderMapper.toDto(order));
        data.put("nextStatuses", orderService.getAllowedNextStatusesForStaff(orderId));
        data.put("canCancel", orderService.canStaffCancel(order));
        data.put("statusGuide", orderService.getStaffStatusGuide(order));
        return ApiResponse.success("Lấy chi tiết đơn hàng thành công", data);
    }

    @PostMapping("/{orderId}/status")
    public ApiResponse<OrderDto> updateStatus(@PathVariable Long orderId,
                                              @RequestParam OrderStatus status) {
        Order order = orderService.advanceOrderStatusForStaff(orderId, status);
        return ApiResponse.success("Đã cập nhật trạng thái đơn hàng", OrderMapper.toDto(order));
    }

    @PostMapping("/{orderId}/receive")
    public ApiResponse<OrderDto> receiveOrder(@PathVariable Long orderId) {
        Order order = orderService.receiveOrderForStaff(orderId);
        return ApiResponse.success("Đã tiếp nhận đơn hàng", OrderMapper.toDto(order));
    }
}
