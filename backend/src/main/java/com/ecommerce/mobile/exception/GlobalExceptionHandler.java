package com.ecommerce.mobile.exception;

import com.ecommerce.mobile.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleGlobalException(Exception ex) {
        ex.printStackTrace(); 
        return ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "System error: " + ex.getMessage());
    }
}
