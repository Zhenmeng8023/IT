package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeIndexTaskCreateRequest {

    private Long documentId;
    private KnowledgeIndexTask.TaskType taskType;
}
