package com.DiagramParcialBackend.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private Integer statusCode;
    private String message;
    private T data;


    public ApiResponse(Integer statusCode, String message, T data){
        this.statusCode = statusCode;
        this.message = message;
        this.data = data;
    }
}
