package com.alikeyou.itmoduleai.dto.front.request;

import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseMemberCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FrontKnowledgeBaseMemberCreateRequest {

    private Long userId;
    private KnowledgeBaseMember.RoleCode roleCode;

    public KnowledgeBaseMemberCreateRequest toServiceRequest() {
        KnowledgeBaseMemberCreateRequest target = new KnowledgeBaseMemberCreateRequest();
        target.setUserId(userId);
        target.setRoleCode(roleCode);
        return target;
    }
}
