package com.alikeyou.itmodulesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminLogResponseDTO<T> {
    private boolean success;
    private String message;
    private T data;
    private PageDTO page;

    public static <T> AdminLogResponseDTO<T> success(T data, PageDTO page) {
        return new AdminLogResponseDTO<>(true, "ok", data, page);
    }

    public static <T> AdminLogResponseDTO<T> success(T data) {
        return new AdminLogResponseDTO<>(true, "ok", data, null);
    }

    public static <T> AdminLogResponseDTO<T> failure(String message) {
        return new AdminLogResponseDTO<>(false, message, null, null);
    }
}
