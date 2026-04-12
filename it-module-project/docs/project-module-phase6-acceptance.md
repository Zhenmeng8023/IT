# Project 模块 Phase 6 手工验收清单

## 适用范围
- 只覆盖 `project` 模块主链路。
- 目标是验证 Phase 1 到 Phase 5 已落地的工作区、提交、MR、发布、回退语义。

## 验收前置条件
- 已有一个可访问的项目，并且当前用户具备该项目的管理权限。
- 项目仓库已初始化，或者准备按下面步骤从空仓库开始初始化。
- 浏览器入口使用管理页：`/projectmanage?projectId=<项目ID>&tab=overview`

## 主链路验收步骤

### 1. 初始化仓库
- 进入项目管理页，打开 `仓库工作区 / Commit 历史`。
- 如果仓库尚未初始化，执行仓库初始化。
- 预期结果：
  - 仓库详情可以正常返回。
  - 默认分支存在且可见。
  - 管理页不白屏，`projectId` 和当前 `tab` 能稳定保留。

### 2. 创建分支
- 基于默认分支创建一个新功能分支，例如 `feature/phase6-check`。
- 预期结果：
  - 分支创建成功。
  - 新分支出现在分支列表。
  - source branch 必须属于当前仓库，不能跨仓库拿错 head。

### 3. 上传进入工作区
- 切换到新功能分支。
- 上传一个文件，例如 `src/demo.txt`。
- 再次上传同一路径的文件，内容不同。
- 再删除同一路径。
- 预期结果：
  - 工作区始终按 `repo + branch + owner + active` 命中同一个 workspace。
  - 同一路径重复上传走覆盖，不会出现两条 workspace item。
  - 删除也走同一路径覆盖逻辑。
  - 如果同一批上传里归一化后路径重复，接口直接报错。

### 4. 提交 commit
- 重新上传 `src/demo.txt`，然后执行提交。
- 预期结果：
  - 提交成功后，workspace 立即刷新。
  - commit list 立即出现新 commit。
  - 风险提示区同步刷新。
  - `changeType` 以 workspace 的 `baseCommitId` 快照为准，而不是直接跟当前 branch head 对比。

### 5. 验证提交前冲突检测
- 保持当前分支已有一个旧 workspace，不要先提交。
- 用另一个账号或另一条链路把同一路径先提交到当前分支 head。
- 回到当前 workspace 再次执行提交。
- 预期结果：
  - 系统比较 `base snapshot / branch head snapshot / workspace items`。
  - 冲突项会写入 `conflictFlag=true`。
  - 前端提交失败后会立即刷新 workspace。
  - 冲突原因 `detectedMessage` 可见，且本次提交被拒绝。

### 6. 创建 MR 并 review / check
- 从功能分支发起 MR 到目标分支。
- 在审核中心完成至少一次 review。
- 如果目标分支受保护，再补齐需要的检查项。
- 预期结果：
  - MR 列表可见。
  - 受保护分支在缺少 approve 或检查失败时不能 merge。
  - `/projectaudit` 与 `/projectmanage` 之间切换时，来源 `tab` 上下文不会丢。

### 7. 执行 merge
- 在审核中心执行 merge。
- 预期结果：
  - merge 使用 merge base 做三方合并，不是简单覆盖 source snapshot。
  - 只 source 改时收 source；只 target 改时保 target；双方同结果时直接收同结果。
  - 双方同路径结果不同则直接拒绝 merge，并显示冲突路径或失败原因。
  - merge 成功后 commit 历史、文件最新状态和 snapshot 都同步更新。

### 8. 里程碑与发布
- 为当前版本绑定一个 milestone。
- 基于明确 commit 创建 release，并尝试绑定发布文件。
- 预期结果：
  - release 必须绑定明确 commit，不能只指向“当前分支状态”。
  - 发布详情能看到 milestone、branch、commit、operator、createdAt。
  - 只允许绑定该 release 基线 commit 快照内的文件。
  - 发布页可以稳定展示这次发布对应哪个里程碑、哪个 commit、哪个分支。

### 9. 回退 rollback
- 选择一个较早的 commit 执行 rollback。
- 预期结果：
  - rollback 会生成新的回退 commit，而不是改写历史 commit。
  - branch head 前移到新的回退 commit。
  - 回退后的文件状态、commit changes、latest 状态都同步更新。
  - 已发布版本仍然能追溯到发布时绑定的 commit，不会被后续提交污染。

## 结果记录建议
- 记录每一步使用的项目 ID、仓库分支、commit SHA、MR ID、release 版本号。
- 对冲突提交、merge 冲突、rollback 结果各截一张图，便于后续复测。

## 当前已知残余风险
- 仍然没有真正覆盖 `rename / move` 语义，现阶段按路径结果处理。
- 前端冲突提示已可见，但还不是结构化冲突面板。
- 历史上如果存在未绑定 `basedCommitId` 的旧 release，需要在后续治理时单独清理。
