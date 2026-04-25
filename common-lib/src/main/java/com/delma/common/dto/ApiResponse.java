package com.delma.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * Standard API response wrapper used by ALL Delma microservices.
 *
 * Every endpoint returns one consistent shape:
 *   Success → { "success": true,  "message": "...", "data": {...} }
 *   Failure → { "success": false, "message": "...", "errorCode": "..." }
 *
 * @JsonInclude(NON_NULL) means null fields are excluded from JSON output.
 * So a success response won't include "errorCode: null" in the response body.
 */

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final Boolean success;
    private final String message;
    private final T data;
    private final String errorCode;

    private ApiResponse(Boolean success,String message,T data,String errorCode){
        this.success = success;
        this.errorCode = errorCode;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> success(T data,String message){
        return new ApiResponse<>(true,message,data,null);
    }

    public static <T> ApiResponse<T> success(String message){
        return new ApiResponse<>(true,message,null,null);
    }

    public static <T> ApiResponse<T> failure(String message,String errorCode){
        return new ApiResponse<>(false,message,null,errorCode);
    }


}
