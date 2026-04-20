package com.alikeyou.itmodulecommon.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdminUserPageResponseDTO {
    private List<UserResponseDTO> list;
    private long total;
    private int page;
    private int size;

    /**
     * Transitional compatibility fields for clients that still consume Spring Page shape.
     * Introduced on April 20, 2026. Planned removal date: June 30, 2026.
     */
    private List<UserResponseDTO> content;
    private Long totalElements;

    public static AdminUserPageResponseDTO from(Page<UserResponseDTO> pageData) {
        AdminUserPageResponseDTO response = new AdminUserPageResponseDTO();
        response.setList(pageData.getContent());
        response.setTotal(pageData.getTotalElements());
        response.setPage(pageData.getNumber());
        response.setSize(pageData.getSize());

        // Transitional aliases, keep until June 30, 2026.
        response.setContent(pageData.getContent());
        response.setTotalElements(pageData.getTotalElements());
        return response;
    }
}
