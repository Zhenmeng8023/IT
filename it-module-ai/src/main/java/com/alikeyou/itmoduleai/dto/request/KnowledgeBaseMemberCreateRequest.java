package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeBaseMemberCreateRequest {

    private Long userId;
    private KnowledgeBaseMember.RoleCode roleCode;
}
