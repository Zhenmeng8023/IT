# it-module-ai

本模块提供 AI 会话、知识库检索、代码/文档 RAG、知识库导入任务、embedding 回填与调试接口。本文档记录当前实现状态，作为测试回归和后续交接依据。

## 1. 核心接口

### AI 问答

- `POST /api/ai/chat/turn`：非流式单轮问答，返回 `ApiResponse<AiChatTurnResponse>`。
- `POST /api/ai/chat/stream`：SSE 流式问答，返回 `AiChatStreamChunkResponse` 流。
- 请求必须包含非空 `content`，当前登录用户由 `AiCurrentUserProvider` 解析，前端传入的 `userId` 不参与身份判定。
- 未传 `sessionId` 时由 `AiSessionService.createSession` 创建会话；已传 `sessionId` 且带 `knowledgeBaseIds` 或 `defaultKnowledgeBaseId` 时会先更新会话知识库绑定。
- `actionCode`、`clientScene`、`sceneCode` 会经 `AiSceneActionCatalog` 归一化，例如代码定位动作会归一为 `code.locate`。

### 知识库管理

- `POST /api/ai/knowledge-bases`：创建知识库；个人知识库会强制使用当前登录用户作为 `ownerId`。
- `PUT /api/ai/knowledge-bases/{id}`：更新知识库，需要编辑权限。
- `GET /api/ai/knowledge-bases/{id}`：读取知识库，需要读取权限。
- `POST /api/ai/knowledge-bases/{knowledgeBaseId}/documents`：新增文档，需要编辑权限。
- `POST /api/ai/knowledge-bases/{knowledgeBaseId}/documents/upload`：多文件上传，需要编辑权限。
- `POST /api/ai/knowledge-bases/{knowledgeBaseId}/documents/upload-zip`：创建 ZIP 导入任务，需要编辑权限。
- `GET /api/ai/knowledge-bases/{knowledgeBaseId}/documents`：分页读取文档，需要读取权限。
- `GET /api/ai/knowledge-bases/documents/{documentId}/chunks`：读取文档切块，需要文档读取权限。
- `GET /api/ai/knowledge-bases/documents/{documentId}/download`：下载单文档，需要文档读取权限。
- `POST /api/ai/knowledge-bases/{knowledgeBaseId}/documents/download-zip`：打包下载文档，需要知识库读取权限。
- `POST /api/ai/knowledge-bases/{knowledgeBaseId}/members`：添加成员，需要 owner 权限。
- `GET /api/ai/knowledge-bases/{knowledgeBaseId}/members`：读取成员，需要读取权限。
- `DELETE /api/ai/knowledge-bases/{knowledgeBaseId}/members/{memberId}`：移除成员，需要 owner 权限。
- `POST /api/ai/knowledge-bases/{knowledgeBaseId}/index-tasks`：创建索引任务，需要编辑权限。
- `GET /api/ai/knowledge-bases/{knowledgeBaseId}/index-tasks`：查询知识库索引任务，需要读取权限。
- `GET /api/ai/knowledge-bases/documents/{documentId}/index-tasks`：查询文档索引任务，需要文档读取权限。

### 检索与调试

- `POST /api/ai/knowledge-bases/documents/{documentId}/chunk-preview`：预览文档切块，需要文档读取权限。
- `POST /api/ai/knowledge-bases/{knowledgeBaseId}/embedding-backfill`：回填知识库 embedding，需要编辑权限。
- `POST /api/ai/knowledge-bases/documents/{documentId}/embedding-backfill`：回填单文档 embedding，需要文档编辑权限。
- `GET /api/ai/knowledge-bases/{knowledgeBaseId}/embedding-status`：查看知识库 embedding 状态，需要读取权限。
- `GET /api/ai/knowledge-bases/documents/{documentId}/embedding-status`：查看文档 embedding 状态，需要文档读取权限。
- `POST /api/ai/knowledge-bases/{knowledgeBaseId}/search-debug`：执行检索调试，需要读取权限，`query` 不允许为空。

`search-debug` 返回内容包括：

- `mode`、`rerankProfile`、`strictGrounding`、`groundingStatus`、`refused`、`refusalReason`。
- 向量、关键词、可用 embedding、provider/model/status 过滤计数。
- `degradeReason`、`finalContextSource`、`embeddingProfile`。
- 每条命中的 chunk、文档、路径、分数、召回阶段、候选来源、命中原因和代码符号信息。

### ZIP 导入任务

- `GET /api/ai/knowledge-import-tasks/{taskId}`：读取导入任务，需要任务读取权限。
- `GET /api/ai/knowledge-import-tasks/knowledge-base/{knowledgeBaseId}`：按知识库读取导入任务，需要知识库读取权限。
- `POST /api/ai/knowledge-import-tasks/{taskId}/cancel`：取消导入任务，需要任务编辑权限。

## 2. 检索、重排与 grounding

当前检索主链路由 `AiKnowledgeResolver` 执行：

- 知识库范围优先使用请求指定的 `knowledgeBaseIds`，并结合会话绑定知识库和默认知识库。
- `topK=0` 会直接返回空命中；未传时使用知识库 `defaultTopK`，再回退到配置 `ai.rag.default-top-k`，最终受 `ai.rag.max-top-k` 限制。
- `AiSceneActionCatalog` 和 `CodeQueryIntentClassifier` 会将动作和问题归类到 `DOC_QA`、`CODE_LOCATE`、`CODE_LOGIC` 等模式。
- `CODE_LOCATE` 优先声明命中和路径定位；`CODE_LOGIC` 在严格模式下优先声明、图扩展和相邻 chunk 扩展。
- 文档问答走向量召回和关键词召回的混合链路。
- 向量召回按 embedding profile 选择 provider/model/dimension，并只读取最新可用 `ACTIVE` embedding。
- provider 和 model 会做规范化与别名兼容，例如 `ollama-local` 归一到 `ollama`，模型展示后缀会被折叠到规范名。
- query embedding 失败时保留关键词召回，并在 `degradeReason` 中记录降级原因。
- 重排由 `CodeReranker` 处理，保留 `keywordScore`、`vectorScore`、`graphScore`、`rerankScore` 和最终 `score`。
- `strictGrounding=true` 时，如果证据不足会返回拒答状态，不把不完整证据交给模型自由发挥。
- `buildKnowledgeAugmentedQuestion` 会生成结构化 evidence blocks，包含知识库、文档、路径、chunk、语言、符号、行号和裁剪后的 snippet。

## 3. 切块、索引与 embedding

切块由 `KnowledgeChunkingServiceImpl` 统一生成索引草稿：

- `MARKDOWN` 按标题和段落切分。
- `PARAGRAPH` 按段落切分。
- `FIXED` 使用固定窗口切分。
- `CUSTOM` 与代码文件优先使用语义切块。
- Java、Vue、JS/TS、SQL 等代码文件会尽量识别类、方法、函数、imports、bindings、Vue sections 和 SQL 语句边界。
- 切块元数据写入 `metadata_json`，保留语言、路径、符号、行号、section、索引版本和 embedding rebuild 标记。

索引任务由 `KnowledgeBaseServiceImpl.createIndexTask` 创建：

- 初始状态为 `PENDING`。
- 后台 executor 负责将任务从 `PENDING` 原子切换到 `RUNNING`。
- runner 结束时只允许从 `RUNNING` 进入 `SUCCESS`、`FAILED` 或 `CANCELLED`。
- 重复 runner 执行会被状态机拦截，避免同一任务二次入库。

embedding 回填由 `KnowledgeEmbeddingServiceImpl` 执行：

- `EmbeddingProfileResolver` 统一解析请求覆盖值、知识库配置和系统默认值。
- `EmbeddingProviderManager` 负责 provider 注册与查找，目前已有 `OllamaEmbeddingProvider`。
- 同一 `chunkId + providerCode + modelName` 优先复用现有 embedding 行。
- backfill 成功写入 `ACTIVE` 和真实向量；失败写入 `FAILED` 及错误信息。
- `embedding-status` 会暴露当前 profile、总 chunk 数、已 embedding 数、可用 active embedding 数和是否需要 rebuild。

## 4. 权限模型

权限校验入口是 `KnowledgeAccessGuard` 和 `AiCurrentUserProvider`。

- 未登录用户访问 chat 会返回 401。
- 知识库、文档、导入任务均按读取、编辑、owner 三类 guard 校验。
- 私有知识库以 owner 或 `knowledge_base_member` 成员关系为准。
- `owner` 和 `admin` 可管理知识库。
- `editor` 可编辑知识库文档和任务，但不能绕过 owner 边界。
- `viewer` 只能读取授权范围内的数据。
- `pageByOwner` 对非管理员会强制收敛到当前用户。
- `pageByProject` 对普通用户只返回其可访问的数据，管理员可看全量。
- 文档级接口先按 `documentId` 找到所属知识库，再执行知识库权限判断。
- 导入任务读取、取消、列表查询分别走任务级或知识库级 guard。

## 5. 导入任务状态机

`KnowledgeImportTask.Status` 当前状态：

- `PENDING`：任务已创建，等待后台执行。
- `RUNNING`：后台 runner 已拿到任务。
- `SUCCESS`：导入流程正常结束。
- `FAILED`：ZIP 损坏、读取失败或执行异常导致任务失败。
- `CANCELLED`：收到取消请求并由 runner 消费。

`KnowledgeImportTask.Stage` 当前阶段：

- `UPLOADED`：ZIP 已落盘。
- `SCANNING`：扫描 ZIP 条目。
- `IMPORTING`：抽取并导入文件。
- `FINISHED`：成功或失败终态。
- `CANCELLED`：取消终态。

关键约束：

- 创建 ZIP 导入任务后接口立即返回 `PENDING`，后台 executor 异步处理。
- 只有 `PENDING` 可以切到 `RUNNING`。
- 只有 `RUNNING` 可以切到终态。
- `cancelTask` 只对 `PENDING` 和 `RUNNING` 设置 `cancelRequested=true`。
- 对 `SUCCESS`、`FAILED`、`CANCELLED` 再次取消会直接返回当前任务，不会重开状态。
- runner 每个阶段会检查 `cancelRequested`。
- runner 的成功、失败和取消路径都会在 `finally` 中清理临时导入目录。
- 重复执行同一个 runner 不会把终态任务重新切回运行态。

## 6. 当前测试覆盖

本轮新增或同步的 HTTP 级 controller 测试：

- `AiChatControllerHttpTest`
- `KnowledgeBaseControllerHttpTest`
- `KnowledgeDebugControllerHttpTest`
- `KnowledgeImportTaskControllerHttpTest`
- `KnowledgeDebugControllerSecurityTest` 同步了当前 controller 构造器。

覆盖路径：

- 正常问答：会话自动创建、动作归一化、`traceDepth` 截断、响应 JSON。
- chat 参数异常：空 `content` 返回 400。
- chat 未登录：当前用户缺失返回 401。
- 知识库读取：guard 通过后返回知识库数据。
- 文档新增：编辑权限通过后返回文档状态。
- 文档 chunks：正常读取、越权 403、不存在 404。
- 索引任务创建：返回 `PENDING` 任务。
- 检索调试：正常返回命中、分数、mode、grounding 和候选计数。
- 检索调试参数异常：空 `query` 返回 400。
- 检索调试越权：guard 拒绝后不调用 resolver。
- 切块预览：返回 preview items。
- embedding 状态：正常返回 profile/counter，不存在资源返回 404。
- 导入任务读取：guard 通过后返回任务。
- 导入任务列表：按知识库返回任务列表。
- 导入任务取消：正常返回 `CANCELLED`，越权不调用 service。
- 导入任务不存在：guard 返回 404。

本轮扩展的导入任务异常路径测试：

- 创建 ZIP 导入任务只入队，不内联执行。
- 损坏 ZIP 会进入 `FAILED`，写入错误信息并清理临时目录。
- 空 ZIP 正常结束为 `SUCCESS` 并清理临时目录。
- 取消可重复请求，重复取消保持 `cancelRequested=true`。
- 已终态任务再次取消不改变状态。
- 取消请求被 runner 消费后进入 `CANCELLED` 并清理临时目录。
- runner 重复执行终态失败任务时，不会重新跑成其他状态。
- 已有异步测试继续覆盖重复 runner 对成功任务的状态机拦截。

已有服务级安全和能力测试仍保留：

- `KnowledgeAccessGuardTest`
- `KnowledgeBaseServiceImplSecurityTest`
- `KnowledgeImportTaskServiceImplSecurityTest`
- `AiSessionServiceImplSecurityTest`
- `AiLogServiceImplSecurityTest`
- `DefaultAiChatOrchestratorSecurityTest`
- `AiKnowledgeResolverTest`
- `KnowledgeChunkingServiceImplTest`
- `CodeIndexServiceImplTest`

## 7. 本地验证命令

运行 AI 模块全量测试：

```powershell
mvn -pl it-module-ai test
```

运行本轮新增和直接相关测试：

```powershell
mvn -pl it-module-ai "-Dtest=AiChatControllerHttpTest,KnowledgeBaseControllerHttpTest,KnowledgeDebugControllerHttpTest,KnowledgeImportTaskControllerHttpTest,KnowledgeDebugControllerSecurityTest,KnowledgeImportTaskServiceImplAsyncTaskTest" test
```

如需连同依赖模块一起构建：

```powershell
mvn -pl it-module-ai -am test
```

## 8. 剩余风险与后续待办

- 当前 HTTP 测试使用 standalone MockMvc，重点覆盖 controller 路由、参数、guard 和 JSON 响应；未启动完整 Spring Security filter chain。
- `POST /api/ai/chat/stream` 的 SSE HTTP 行为已有 orchestrator 流式测试保护，但 controller 层本轮未新增完整 SSE wire-level 断言。
- ZIP 导入测试覆盖损坏 ZIP、空 ZIP、取消和清理；真实 doc/pdf/docx 解析链路仍建议在后续增加样例文件级集成测试。
- embedding provider 的真实 Ollama HTTP 调用未在单元测试中访问外部服务，当前通过 provider/profile 解析和服务逻辑测试保护。
- README 只记录当前实现；若后续修改权限、状态机或 debug 字段，需要同步更新本文档和对应测试。
