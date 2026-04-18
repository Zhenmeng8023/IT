package com.alikeyou.itmodulecircle.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CircleManageApiResponse<T> {

    private Integer code;

    private String message;

    private T data;

    private Long timestamp;

    public static <T> CircleManageApiResponse<T> success(String message, T data) {
        return new CircleManageApiResponse<>(200, message, data, System.currentTimeMillis());
    }

    public static <T> CircleManageApiResponse<T> success(T data) {
        return success("success", data);
    }

    public static <T> CircleManageApiResponse<T> failure(int code, String message) {
        return new CircleManageApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}

