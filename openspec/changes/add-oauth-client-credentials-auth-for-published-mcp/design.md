## Context

当前 `mcp-gateway-server` 已具备 MCP 协议接入与现有认证机制，但对外发布场景缺少标准 OAuth Client Credentials 认证路径。  
目标接入方（如 Dify）通常以后端服务身份调用 MCP，不具备交互式登录能力，因此需要网关支持无用户参与的机器到机器鉴权模式。  
本次约束是不引入外部授权服务，因此需在 `mcp-gateway-server` 内置授权服务器最小能力，同时保持现有 Bearer Token 调用方可继续访问，避免存量集成受影响。

## Goals / Non-Goals

**Goals:**
- 支持在 MCP 发布接口上并行启用旧 Bearer Token 与 OAuth Client Credentials Bearer Token 鉴权。
- 在 `mcp-gateway-server` 内提供 OAuth Client Credentials 最小闭环（token 签发与 JWKS 发布）。
- 完成访问令牌签名和声明校验。
- 在协议能力声明中体现 `io.modelcontextprotocol/oauth-client-credentials` 扩展支持。
- 保持 controller 简洁，将复杂鉴权逻辑下沉到 service/认证组件。

**Non-Goals:**
- 不在本次变更中引入交互式用户授权流程（如 Authorization Code）。
- 不改造现有 MCP 工具执行、会话语义和业务路由逻辑。
- 不扩展与 OAuth 无关的新功能。

## Decisions

### 决策 1：采用“统一入口 + 双认证路由”的拦截链
- 方案：在 MCP 发布入口添加统一鉴权拦截逻辑，先识别令牌特征并按策略路由到 OAuth 校验或旧 Bearer 校验；同时支持配置优先级。
- 原因：可在不改变调用协议的前提下兼容存量与增量客户端，且对现有代码侵入最小。
- 备选方案：
  - 直接切换为仅 OAuth：会破坏现有调用方，不采用。
  - 在每个 controller 方法内重复校验：实现分散、维护成本高，不采用。

### 决策 2：OAuth 令牌校验采用“本地 JWT 校验 + 可配置声明约束”
- 方案：在网关内置私钥签发 JWT access token，并通过内置 JWKS 公开公钥，发布接口基于同一密钥体系完成本地验签；同时校验 `exp`/`nbf`/`iss`/`aud` 等关键声明。
- 原因：不依赖外部授权服务且可保持高可用，避免每次请求远程 introspection。
- 备选方案：
  - 对接外部授权服务器：不满足“无外部服务依赖”的约束，不采用。
  - 仅做 introspection：增加运行时依赖，不采用为主路径。
  - 仅验签不验声明：安全性不足，不采用。

### 决策 3：不在单服务内实现 scope 校验
- 方案：OAuth 分支仅校验令牌真伪与关键声明，不实现 scope 检查。
- 原因：当前发布接口默认均可访问，更细粒度授权通过发布不同 MCP 服务实现，避免在单服务内引入额外授权复杂度。
- 备选方案：
  - 默认强制校验 scope：与现网“接口可访问”目标冲突，不采用。
  - 配置化可选 scope：增加配置与测试复杂度，当前阶段不采用。
  - 硬编码 scope：灵活性不足，不采用。

### 决策 4：MCP 扩展能力通过初始化能力声明暴露
- 方案：在初始化能力响应中声明 `io.modelcontextprotocol/oauth-client-credentials`。
- 原因：符合 MCP 扩展发现机制，便于客户端自动识别能力。
- 备选方案：
  - 仅文档声明不在协议中体现：可发现性差，不采用。

### 决策 5：内置授权端点最小化实现
- 方案：在网关内新增 `POST /oauth/token`（client_credentials）和 `GET /oauth/jwks`，客户端凭据采用配置化管理（支持密文存储）。
- 原因：满足 OAuth client credentials 基本互操作能力，并控制变更范围最小化。
- 备选方案：
  - 直接扩展更多授权流（如 authorization_code）：超出当前目标，不采用。
  - 自定义非标准 token 接口：客户端兼容性差，不采用。

### 决策 6：认证结果统一归一化
- 方案：无论走旧 Bearer 还是 OAuth 校验，最终输出统一的认证上下文（主体、来源类型、权限信息）供后续业务使用。
- 原因：减少业务层分支判断，降低后续演进复杂度。
- 备选方案：
  - 业务层感知两套认证结果：耦合上升，不采用。

## Risks / Trade-offs

- [Risk] 内置私钥泄露会导致令牌伪造风险 → Mitigation：密钥密文存储、最小权限访问、轮换机制与审计日志。
- [Risk] 内置授权端点被暴力请求可能影响可用性 → Mitigation：对 `/oauth/token` 增加限流、失败计数与告警。
- [Risk] 不在单服务内做 scope 校验可能导致单服务权限边界过粗 → Mitigation：通过发布不同 MCP 服务进行能力隔离，按服务划分访问边界。
- [Risk] 双认证路由增加分支复杂度 → Mitigation：抽象统一认证接口并增加覆盖两分支的测试用例。
- [Risk] 认证链增加请求开销 → Mitigation：使用本地验签、缓存公钥、避免高频远程调用。
- [Risk] 误配置导致合法请求被拒绝 → Mitigation：启动时配置校验、提供清晰错误码和排障日志。

## Migration Plan

1. 增加双认证配置项（模式开关、优先级）与内置 OAuth 配置（issuer、audience、签名密钥、客户端凭据）。
2. 实现内置授权端点（`/oauth/token`、`/oauth/jwks`）与令牌签发逻辑。
3. 实现认证 service 与网关拦截接入，完成认证路由、token 解析与校验。
4. 在 MCP 能力声明中增加扩展标识并补充配置说明。
5. 在测试环境联调 Dify（或等效客户端）完成通路验证。
6. 灰度开启双认证并行模式；若异常可仅保留旧 Bearer 分支快速回退。

## 联调检查清单（5.1）

1. 在测试环境开启 `mcp.published-auth.oauth-server.enabled=true`，并配置至少一个 `client_id/client_secret` 与 `service-ids`。
2. 调用 `POST /oauth/token`（`grant_type=client_credentials`）获取 `access_token`，确认返回 `token_type=Bearer` 与 `expires_in`。
3. 调用 `GET /oauth/jwks` 确认存在当前 `kid` 公钥。
4. 使用获取到的 Bearer token 调用 `POST /mcp/service/{serviceId}/stateless` 完成 initialize 与 tools/list。
5. 验证旧静态 token 在 `mode=BOTH` 下仍可访问，确保存量调用不受影响。
6. 验证非法 client_secret、过期 token、错误 audience 均被拒绝并输出可定位日志。

## 灰度与回滚策略（5.2）

1. 灰度阶段一：`mode=LEGACY_ONLY` + `oauth-server.enabled=true`，只开放 token/JWKS 端点，不切流量。
2. 灰度阶段二：切换 `mode=BOTH` 且 `order=OAUTH_FIRST`，观察 OAuth 分支成功率与回退命中率。
3. 稳定后按租户/服务逐步收紧至 `mode=OAUTH_ONLY`（可选）。
4. 快速回滚：出现异常时立刻切回 `mode=LEGACY_ONLY`，并保留 OAuth 端点用于排障。
5. 故障处置：如签名密钥异常，先更换 `private-key-pem` 与 `key-id`，再重启实例并验证 JWKS 与新 token。

## Open Questions

- 客户端凭据（`client_id/client_secret`）是否支持在线热更新与动态吊销？
- 密钥轮换窗口如何设计，才能兼顾令牌存活期与无损切换？
- 旧 Bearer 分支的长期淘汰窗口是否需要在本次方案中预置配置化退场策略？
