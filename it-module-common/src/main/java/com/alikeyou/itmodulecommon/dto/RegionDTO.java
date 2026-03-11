package com.alikeyou.itmodulecommon.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegionDTO {
    private Long id;
    private String name;
    private String level;
    private String code;
    private Long parentId;
    
    public RegionDTO(Long id, String name, String level, String code, Long parentId) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.code = code;
        this.parentId = parentId;
    }
}
