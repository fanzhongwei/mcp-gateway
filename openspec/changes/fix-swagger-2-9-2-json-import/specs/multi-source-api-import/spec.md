## MODIFIED Requirements

### Requirement: 导入流程与 Swagger 保持一致
对于非手动导入来源，系统 SHALL 复用 Swagger 导入的核心流程，包括解析结果预览、用户确认、统一入库与结果反馈，不得引入额外强制步骤。系统 MUST 支持 Swagger 2.x（含 2.9.2 JSON）在该流程中成功完成解析与导入，同时 MUST 保持既有 OpenAPI 导入路径与行为不变。

#### Scenario: 新增来源进入统一确认流程
- **WHEN** 任一新增来源完成解析
- **THEN** 系统展示与 Swagger 相同语义的确认与导入反馈流程

#### Scenario: 导入失败时给出可操作反馈
- **WHEN** 来源文件或命令解析失败
- **THEN** 系统返回明确错误信息并提示用户修正输入后重试

#### Scenario: Swagger 2.9.2 JSON 导入成功
- **WHEN** 用户上传符合 Swagger 2.9.2 规范的 API JSON
- **THEN** 系统完成解析并进入与现有 Swagger/OpenAPI 一致的预览确认流程

#### Scenario: OpenAPI 导入行为保持不变
- **WHEN** 用户上传原本可成功导入的 OpenAPI 文档
- **THEN** 系统沿用既有 OpenAPI 解析路径并输出与修复前一致的导入结果
