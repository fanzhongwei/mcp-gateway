## Why

当前接口导入仅支持手动录入与 OpenAPI/Swagger，无法覆盖团队常见的 Postman、cURL、Apifox 等来源，导致已有接口资产无法直接复用，导入效率与数据一致性受限。现在扩展导入来源可以在不改变现有导入体验的前提下，降低迁移成本并提升日常维护效率。

## What Changes

- 完善导入入口中已展示但未落地的类型：实现 `Postman`、`cURL`、`Apifox`（与当前页面一致）。
- 本次新增 `HAR` 类型导入，作为当前迭代唯一新增候选来源。
- 新增“导入来源评估”机制：基于使用频率、社区与规范成熟度、文档完整度筛选后续可扩展来源；当前结论为**仅新增 `HAR`**，其他候选延后。
- 为已选导入类型建立与 Swagger 一致的导入流程（解析 -> 预览/确认 -> 规范化 -> 入库），保证交互与数据结构一致。
- 为不同来源补充文档映射规则，统一转换为系统既有接口规范，避免新增独立存储格式。
- 导入实现按来源拆分为独立 Vue 文件，并抽取解析后“展示与选择导入”为公用组件，便于后续扩展。
- 解析后的统一结果结构通过独立 TypeScript 类型文件定义，保障多来源行为一致。

## 候选导入来源对比（供选型）

说明：**使用频率**指在联调、文档、Mock、迁移等场景中的常见程度（相对分级）；**社区与规范成熟度**指公开规范稳定性、工具/解析生态、版本碎片化风险（相对分级）。分级均为 `高 / 中高 / 中 / 低`，便于排序而非精确统计。

| 来源 | 典型输入形态 | 使用频率 | 社区与规范成熟度 | 与当前 REST/HTTP 模型契合度 | 实施复杂度（相对） | 建议阶段 |
| --- | --- | --- | --- | --- | --- | --- |
| OpenAPI / Swagger | YAML / JSON | 高（规范层） | 高 | 高 | 低 | **已实现** |
| 手动录入 | 表单 | 高 | 高（产品内建） | 高 | 低 | **已实现** |
| **Postman Collection** | JSON（v2.0 / v2.1） | 高 | 高 | 高 | 中 | **P0（与入口一致，建议默认做）** |
| **cURL** | 单行或多行文本 | 高 | 中（语法变体多，需约定支持子集） | 高 | 中 | **P0（与入口一致，建议默认做）** |
| **Apifox** | 项目/数据导出 JSON | 中高（国内团队常见） | 中高（导出格式可能随版本调整） | 高 | 中高 | **P0（与入口一致，建议默认做）** |
| HAR | JSON（Chrome 等导出） | 高 | 高 | 高（偏单次请求快照） | 中 | P1 候选 |
| Insomnia | JSON（v4 等） | 中高 | 中高 | 高 | 中 | P1 候选 |
| Bruno | `.bru` / 集合目录 | 中 | 中（生态在增长） | 高 | 中 | P2 候选 |
| Hoppscotch | 集合导出 JSON | 中 | 中 | 高 | 中 | P2 候选 |
| GraphQL | SDL / introspection JSON | 中高 | 高 | **低**（与 REST 资源模型差异大，需单独建模或大幅映射） | 高 | 另立项评估 |
| RAML | YAML | 中低 | 中 | 中高 | 中高 | P2 候选（存量项目） |
| WSDL / SOAP | XML | 低（存量） | 高（规范成熟） | 中（协议与 REST 不同） | 高 | 按需（垂直场景） |

### 选型建议（默认值，可按业务调整）

- **P0（与现有「导入 API 数据」入口对齐）**：Postman、cURL、Apifox —— 先闭环当前 UI 承诺，投入产出比最高。
- **P1（高频、规范清晰）**：HAR、Insomnia —— 与 Postman 部分重叠，但来源不同，适合作为「第二梯队」。
- **P2（有需求再做）**：Bruno、Hoppscotch、RAML。
- **单独评估**：GraphQL（产品是否以 REST 为主）、WSDL/SOAP（是否服务政企存量）。

### 选型确认（已定稿）

- [x] 保留 P0：Postman、cURL、Apifox
- [x] 本次新增 P1：`HAR`
- [ ] 暂不新增：`Insomnia`、`Bruno`、`Hoppscotch`、`RAML`
- [ ] 暂不纳入本次：`GraphQL`、`WSDL/SOAP`

## Capabilities

### New Capabilities
- `multi-source-api-import`: 支持从多种常见接口定义来源导入并统一转换为系统接口模型。
- `import-source-evaluation`: 建立导入来源选型标准与候选优先级，指导后续扩展范围。

### Modified Capabilities
- 无

## 参考文档与规范链接

为保证导入实现与社区规范保持一致，本次涉及的来源类型建议参考以下官方或权威文档：

- **Postman Collection**
  - Collection v2.1 Schema 文档：[`https://schema.postman.com/collection/json/v2.1.0/draft-07/docs/index.html`](https://schema.postman.com/collection/json/v2.1.0/draft-07/docs/index.html)
- **cURL**
  - 官方工具手册（man page）：[`https://curl.se/docs/manpage.html`](https://curl.se/docs/manpage.html)
- **Apifox**
  - 导出接口 / 导出数据说明：[`https://docs.apifox.com/export-data`](https://docs.apifox.com/export-data)
- **HAR (HTTP Archive)**
  - HAR 1.2 规范：[`http://www.softwareishard.com/blog/har-12-spec/`](http://www.softwareishard.com/blog/har-12-spec/)
  - W3C HAR 草案：[`https://w3c.github.io/web-performance/specs/HAR/Overview.html`](https://w3c.github.io/web-performance/specs/HAR/Overview.html)

## Impact

- 受影响前端模块：`mcp-gateway-web/src/pages/apiManage/import.vue` 与 `mcp-gateway-web/src/pages/apiManage/importTypes/*`。
- 受影响导入解析与保存逻辑：现有 Swagger 导入复用的规范化与落库流程。
- 对外接口与已有导入行为保持兼容，不引入破坏性变更。
