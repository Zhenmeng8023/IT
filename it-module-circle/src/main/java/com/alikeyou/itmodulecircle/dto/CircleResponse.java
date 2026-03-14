package com.alikeyou.itmodulecircle.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class CircleResponse {

    private Long id;
    private String name;
    private String description;
    private String type;
    private String visibility;
    private Integer maxMembers;
    private Instant createdAt;
    private Instant updatedAt;
    private CircleCreatorInfo creator;
}
