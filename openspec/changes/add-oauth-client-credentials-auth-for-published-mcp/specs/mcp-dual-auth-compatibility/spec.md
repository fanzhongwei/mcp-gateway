## ADDED Requirements

### Requirement: MCP 服务必须声明 OAuth Client Credentials 扩展能力
系统在 MCP 初始化能力协商响应中 MUST 声明 `io.modelcontextprotocol/oauth-client-credentials` 扩展，以便支持 OAuth 客户端凭证模式的客户端发现并启用对应认证流程。

#### Scenario: 初始化能力声明包含 OAuth 扩展
- **WHEN** 客户端请求 MCP 初始化能力信息
- **THEN** 响应中包含 `capabilities.extensions.io.modelcontextprotocol/oauth-client-credentials`

### Requirement: MCP 发布接口必须支持双认证并行
系统 SHALL 在同一 MCP 发布入口并行支持旧 Bearer Token 认证与 OAuth Bearer Token 认证，且通过配置控制启用模式与认证优先级。

#### Scenario: 双认证并行时旧调用方可继续访问
- **WHEN** 系统启用双认证并行且请求携带符合旧 Bearer 规则的令牌
- **THEN** 请求通过旧 Bearer 分支鉴权并进入后续工具执行逻辑

### Requirement: 系统必须提供内置 OAuth Token 签发端点
系统 MUST 在 `mcp-gateway-server` 内提供 OAuth client credentials 标准签发端点，用于校验 `client_id/client_secret` 并返回 Bearer access token。

#### Scenario: 客户端凭据合法时成功签发 token
- **WHEN** 客户端以 `grant_type=client_credentials` 携带合法 `client_id/client_secret` 请求 token 端点
- **THEN** 系统返回可用于 MCP 接口访问的 Bearer access token

### Requirement: 系统必须提供内置 JWKS 端点
系统 MUST 暴露内置 JWKS 端点用于发布当前签名公钥，以支持 OAuth 令牌验签与密钥轮换。

#### Scenario: 客户端可获取当前公钥集合
- **WHEN** 客户端请求 JWKS 端点
- **THEN** 系统返回包含当前有效公钥的 JWKS 文档

### Requirement: 系统必须支持 OAuth Bearer Token 校验
系统 MUST 基于内置密钥体系校验 OAuth 访问令牌签名，并校验至少 `exp`、`iss`、`aud` 等关键声明；任一校验失败都必须拒绝该 OAuth 分支请求。

#### Scenario: OAuth 令牌声明不满足约束时拒绝访问
- **WHEN** 请求走 OAuth 分支且访问令牌签名合法但 `iss` 或 `aud` 与网关配置不匹配
- **THEN** 系统返回鉴权失败响应并记录可审计日志

### Requirement: 认证路由失败时必须执行回退策略
系统 MUST 在双认证并行模式下定义明确认证路由与回退策略；当首选认证分支无法通过时，按配置决定是否尝试次选分支，且行为可预测。

#### Scenario: OAuth 优先且允许回退时走旧 Bearer
- **WHEN** 系统配置为 OAuth 优先并允许回退，且请求令牌不满足 OAuth 校验但满足旧 Bearer 规则
- **THEN** 系统通过旧 Bearer 分支完成鉴权并继续处理请求

### Requirement: 认证失败响应必须可区分且可观测
系统 MUST 对认证失败（token 缺失、无效、过期）返回可区分错误语义，并记录统一结构化日志（含命中分支）用于排障与审计。

#### Scenario: 认证失败输出包含认证分支信息
- **WHEN** 请求在双认证并行模式下鉴权失败
- **THEN** 系统返回明确失败语义并输出包含认证分支与失败原因的结构化日志
