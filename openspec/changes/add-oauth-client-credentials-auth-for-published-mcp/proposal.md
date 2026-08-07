## Why

当前已发布的 MCP 服务缺少标准 OAuth Client Credentials 认证路径，无法满足 Dify 等 machine-to-machine 集成场景；同时现网已存在 Bearer Token 调用方，不能因升级而中断。  
在不引入外部授权服务的约束下，需要在 `mcp-gateway-server` 内置授权服务器能力，并提供“旧 Bearer 与 OAuth 并行”的双认证能力以保障平滑迁移。

## What Changes

- 为已发布 MCP 服务新增 OAuth Client Credentials 认证能力，支持通过 `client_id` + `client_secret` 获取并校验 Bearer Token 后访问 MCP 接口。
- 在网关侧实现双认证并行：同一入口同时支持旧 Bearer Token 校验与 OAuth Token 校验，并支持可配置优先级与开关策略。
- 在 `mcp-gateway-server` 内新增授权服务器最小闭环能力：客户端凭据校验、`/oauth/token` 签发 access token、`/oauth/jwks` 发布公钥。
- 新增 OAuth 配置项与认证校验流程，覆盖 token 解析、签名/声明校验与鉴权失败响应，不引入 scope 校验逻辑。
- 在 MCP 服务能力声明中补充 `io.modelcontextprotocol/oauth-client-credentials` 扩展支持信息，用于客户端发现。
- 保持原有调用链路兼容，不改变已有业务协议与工具执行语义。

## Capabilities

### New Capabilities

- `mcp-dual-auth-compatibility`: 定义 MCP 服务在旧 Bearer 与 OAuth Client Credentials 并行模式下的认证路由、令牌校验与错误处理行为。

### Modified Capabilities

- 无

## Impact

- 影响系统：`mcp-gateway-server`（认证中间层、MCP 协议入口、配置读取）。
- 影响接口：新增内置授权端点（`/oauth/token`、`/oauth/jwks`）；MCP HTTP 接口维持 Bearer Token 形态并支持旧 Bearer 与 OAuth Bearer 并行认证。
- 影响配置：新增双认证策略配置（启用模式、优先级）及内置 OAuth 配置（issuer、签名密钥、audience、客户端凭据）。
- 授权策略：不在单服务内引入 scope 细粒度授权，更细粒度授权通过发布不同 MCP 服务实现能力边界隔离。
- 外部集成：提升 Dify 等支持 OAuth Client Credentials 的 MCP 客户端对接能力，且无需依赖外部授权服务器。
