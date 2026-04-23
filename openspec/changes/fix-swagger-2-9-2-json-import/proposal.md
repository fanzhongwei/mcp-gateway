## Why

当前 Swagger API JSON（2.9.2）在导入流程中失败，导致用户无法从该来源完成接口导入。该问题直接影响现有导入能力可用性，需要在不破坏已支持 OpenAPI 导入行为的前提下尽快修复。

## What Changes

- 修复 Swagger 2.9.2 JSON 的解析兼容性，使其能够进入现有统一导入流程。
- 保持现有 OpenAPI 导入路径、解析结果结构与确认流程不变，避免回归。
- 对 Swagger 与 OpenAPI 输入分流逻辑进行边界明确化，确保兼容处理只作用于 Swagger 2.x 相关输入。
- 补充覆盖 Swagger 2.9.2 成功导入与 OpenAPI 无回归的测试用例。

## Capabilities

### New Capabilities
- 无

### Modified Capabilities
- `multi-source-api-import`: 调整“导入流程与 Swagger 保持一致”相关需求，明确 Swagger 2.x（含 2.9.2 JSON）兼容导入要求，并要求不影响既有 OpenAPI 导入行为。

## Impact

- 影响代码：`mcp-gateway-web` 中 API 导入来源识别与 Swagger/OpenAPI 解析适配逻辑。
- 影响测试：需新增/调整 Swagger 2.9.2 与 OpenAPI 回归测试。
- 外部接口与依赖：不新增外部依赖，不改变对外 API 形态。
