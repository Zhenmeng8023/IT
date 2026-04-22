package com.alikeyou.itmoduleai.dto.front.response;

import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FrontKnowledgeBaseMemberResponse {

    private Long id;
    private Long knowledgeBaseId;
    private Long userId;
    private KnowledgeBaseMember.RoleCode roleCode;
    private Long grantedBy;
    private Instant createdAt;

    public static FrontKnowledgeBaseMemberResponse from(KnowledgeBaseMember entity) {
        if (entity == null) {
            return null;
        }
        return FrontKnowledgeBaseMemberResponse.builder()
                .id(entity.getId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .userId(entity.getUserId())
                .roleCode(entity.getRoleCode())
                .grantedBy(entity.getGrantedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
