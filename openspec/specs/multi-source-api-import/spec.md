# multi-source-api-import Specification

## Purpose
TBD - created by archiving change extend-api-import-source-types. Update Purpose after archive.
## Requirements
### Requirement: 支持多来源接口定义导入
系统 MUST 支持从 `Postman`、`cURL`、`Apifox`、`HAR` 导入接口定义，并将解析结果统一转换为系统既有接口规范模型后再进入保存流程。

#### Scenario: 从 Postman 集合导入成功
- **WHEN** 用户选择 Postman 导入并提交合法集合文件
- **THEN** 系统完成解析并展示与 Swagger 导入一致的接口预览结果

#### Scenario: 从 cURL 命令导入成功
- **WHEN** 用户输入合法的 cURL 命令并执行导入
- **THEN** 系统将请求方法、URL、Header、Body 解析为标准接口模型并进入预览确认步骤

#### Scenario: 从 Apifox 项目导出文件导入成功
- **WHEN** 用户上传受支持版本的 Apifox 导出文件
- **THEN** 系统完成结构解析并输出可确认的标准接口列表

#### Scenario: 从 HAR 文件导入成功
- **WHEN** 用户上传合法 HAR 文件
- **THEN** 系统提取可识别请求并转换为标准接口模型进入预览确认步骤

### Requirement: 导入流程与 Swagger 保持一致
对于非手动导入来源，系统 SHALL 复用 Swagger 导入的核心流程，包括解析结果预览、用户确认、统一入库与结果反馈，不得引入额外强制步骤。

#### Scenario: 新增来源进入统一确认流程
- **WHEN** 任一新增来源完成解析
- **THEN** 系统展示与 Swagger 相同语义的确认与导入反馈流程

#### Scenario: 导入失败时给出可操作反馈
- **WHEN** 来源文件或命令解析失败
- **THEN** 系统返回明确错误信息并提示用户修正输入后重试

### Requirement: 标准化映射可追踪
系统 MUST 在来源适配阶段维护字段映射规则，并对无法完整映射的内容执行可追踪降级处理，确保导入结果可解释。

#### Scenario: 遇到不支持字段时降级
- **WHEN** 来源包含当前规范不支持的字段
- **THEN** 系统忽略该字段并在预览或日志中标记降级信息

