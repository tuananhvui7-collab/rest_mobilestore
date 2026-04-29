package com.ecommerce.mobile.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ecommerce.mobile.entity.Employee;
import com.ecommerce.mobile.service.ManagerEmployeeService;
import com.ecommerce.mobile.response.ApiResponse;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/admin/employees")
public class ManagerEmployeeController {

    private final ManagerEmployeeService managerEmployeeService;

    public ManagerEmployeeController(ManagerEmployeeService managerEmployeeService) {
        this.managerEmployeeService = managerEmployeeService;
    }

    @GetMapping
    public ApiResponse<List<Employee>> list() {
        return ApiResponse.success("Lấy danh sách nhân viên thành công", managerEmployeeService.getAllEmployees());
    }

    @GetMapping("/{id}")
    public ApiResponse<Employee> detail(@PathVariable Long id) {
        return ApiResponse.success("Lấy thông tin nhân viên thành công", managerEmployeeService.getEmployee(id));
    }

    @PostMapping("/save")
    public ApiResponse<Void> save(@RequestParam(required = false) Long employeeId,
                       @RequestParam String email,
                       @RequestParam(required = false) String password,
                       @RequestParam String fullName,
                       @RequestParam(required = false) String phone,
                       @RequestParam(required = false) BigDecimal salary,
                       @RequestParam(required = false, defaultValue = "true") Boolean active) {
        if (employeeId == null) {
            managerEmployeeService.createEmployee(email, password, fullName, phone, salary, active);
            return ApiResponse.success("Đã tạo nhân viên mới", null);
        } else {
            managerEmployeeService.updateEmployee(employeeId, fullName, phone, salary, active, password);
            return ApiResponse.success("Đã cập nhật nhân viên", null);
        }
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<Void> activate(@PathVariable Long id) {
        managerEmployeeService.setActive(id, true);
        return ApiResponse.success("Đã kích hoạt nhân viên", null);
    }

    @PostMapping("/{id}/deactivate")
    public ApiResponse<Void> deactivate(@PathVariable Long id) {
        managerEmployeeService.setActive(id, false);
        return ApiResponse.success("Đã khóa nhân viên", null);
    }

    @PostMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                @RequestParam String newPassword) {
        managerEmployeeService.resetPassword(id, newPassword);
        return ApiResponse.success("Đã đặt lại mật khẩu cho nhân viên", null);
    }
}
