# MCP Gateway：零代码改造，把现有 API 发布为 MCP 服务

> 上篇讲了 MCP 协议与网关设计思路；本篇介绍落地实现 **MCP Gateway**：导入 OpenAPI/Swagger 文档即可一键发布 MCP 服务，大模型通过标准协议直接调用你的接口，业务侧零侵入。Spring Boot + MCP SDK，GitHub 已开源，欢迎 Star。

---
![MCP Gateway.gif](https://mmbiz.qpic.cn/sz_mmbiz_gif/k4T981hROvzPicfFiaNys8MRmnK3wKsCxJkn1wXiaIwE578KDM5DS7vowLYShgnMmhstBfib3rqiacpWgLokjEeiaRnQSP01oAR8EBvG6icTdpXf3o/640?wx_fmt=gif&amp;from=appmsg)


## 一、MCP Gateway 是什么

**MCP Gateway** 是一个基于 Spring Boot 的 MCP 服务网关，核心做一件事：**把 OpenAPI/Swagger 文档自动转换成 MCP 工具**，让大模型通过标准 MCP 协议（`tools/list`、`tools/call`）直接调用你的业务接口，而无需改业务代码。

你可以把它理解成「API 与 MCP 之间的翻译官」：在管理端导入或手动录入 API 文档，配置好转发地址与认证，发布后就会得到一个独立的 MCP Server；客户端（如 Claude Desktop、Cursor、自研 AI 应用）按 MCP 规范连上来，就能把文档里的接口当作「工具」来调用。原有后端保持 REST/OpenAPI 形态不变，零侵入。

项目已在 GitHub 开源，地址：**https://github.com/fanzhongwei/mcp-gateway**，欢迎 Star 与使用。

---

## 二、核心能力与特性

- **API 文档来源灵活**：支持通过 **OpenAPI/Swagger**（URL 或本地上传）导入，也支持在管理端**手动录入**接口信息；录入或导入后统一转换为 MCP 工具定义，配置驱动、零侵入业务服务。（Postman、cURL、Apifox 等导入方式正在开发中。）
- **标准协议**：完整支持 MCP 的 `tools/list`、`tools/call` 等能力，兼容主流 MCP 客户端。
- **多服务 / 多租户**：按「服务」维度管理多套 API-Docs，每个服务对应一个 MCP Server 端点（如按 `serviceId` 路由），便于区分业务域或租户。
- **认证与鉴权**：支持 Bearer Token 等认证方式，网关侧统一校验后再转发到后端，与上篇设计中的「自研传输入口 + 统一鉴权」一致。
- **技术栈**：Spring Boot 3.x、SpringDoc OpenAPI、官方 MCP Java SDK（协议层复用），便于与现有 Java 技术体系集成。

---

## 三、快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- Node.js 18+（构建前端时需要）
- PostgreSQL 14+（用于管理端数据存储）

### 构建与运行

在项目根目录执行：

```bash
mvn clean package
```

然后启动服务：

```bash
java -jar mcp-gateway-server/target/mcp-gateway-server.jar
```

### 使用流程

启动服务后访问管理端，按以下步骤即可完成从 API 文档发布到在 MCP 客户端中使用的全流程。

#### 1. 创建业务系统、维护环境

- 在管理端进入**业务系统**管理，新建一个业务系统（例如「订单服务」「用户中心」），用于归类将要暴露为 MCP 的接口。
- 为每个业务系统维护**环境**（如开发、测试、生产）：在对应业务系统下进入「环境管理」，添加环境并填写**环境名称**、**Base URL**（该环境下 API 的根地址）等。后续导入或录入的接口会按「业务系统 + 环境」维度管理，发布 MCP 时也会按环境转发请求。

#### 2. 导入 API 文档

- **方式一：导入文档**  
  在**接口管理**中选择已创建的业务系统及环境，选择「导入」→ 选择 **OpenAPI/Swagger**（支持填写文档 URL 或本地上传），按页面提示完成导入，系统会解析并生成接口列表。
- **方式二：手动录入**  
  选择「手动录入」，逐项填写接口的路径、方法、摘要、参数（Query/Header/Body）等，保存后同样进入该业务系统下的接口列表。

同一业务系统下可同时存在导入与手动录入的接口，可编辑、删除或补充。

#### 3. 创建并发布 MCP Server

- 在**MCP 服务**中点击「创建 MCP 服务」，填写服务名称、描述等基本信息。
- **配置资源组合**：为当前 MCP 服务选择要暴露的「业务系统 + 环境」组合，并勾选该组合下要作为 MCP 工具暴露的接口（可多选）；可为接口或组合设置便于大模型识别的名称。
- **服务端点**（该 MCP Server 对外提供的 URL 路径）和**访问令牌**（Access Token，客户端调用时需携带）由系统自动生成，不可修改；发布后可在服务详情中查看与复制。
- 保存后点击**发布**。发布成功后，该 MCP Server 处于运行中状态，即可被 MCP 客户端连接。

#### 4. 如何在 MCP 客户端上使用

在管理端 **MCP 服务** 列表点击 **查看** 打开服务详情，详情页提供 **访问令牌**、**服务端点**、**客户端配置** 三项复制按钮，复制后填入客户端即可。

**配置示例**（将 `url`、令牌换为详情页复制的值）：

- **Cursor**（`mcpServers` 下）：
```json
"mcp-gateway-http": {
  "timeout": 60,
  "type": "streamableHttp",
  "url": "https://your-gateway-host/mcp-gateway/your-service-endpoint",
  "headers": { "Authorization": "Bearer your-access-token" }
}
```

- **Claude Desktop**（`claude_desktop_config.json` 的 `mcp_servers` 下）：
```json
"mcp-gateway-http": {
  "url": "https://your-gateway-host/mcp-gateway/your-service-endpoint",
  "api_key": "your-access-token"
}
```

---

## 四、适用场景与小结

### 适用场景

- **存量 API 快速接入 MCP**：已有大量 REST/OpenAPI 接口，希望**不改业务代码**就接入 Claude、Cursor 等 MCP 生态，让大模型能够直接调用你的接口。
- **多服务 / 多租户统一出口**：需要按业务域或租户暴露多套 API，每个 MCP Server 对应一个端点与令牌，便于隔离与权限控制。
- **配置驱动、少写适配代码**：希望用导入文档 + 勾选接口的方式把 OpenAPI 转成 MCP 工具，避免为每个接口手写 MCP 适配层。
- **内部工具与自研 AI 应用**：企业内已有 OpenAPI/Swagger 文档的内部系统，希望通过统一网关暴露给内部 AI 助手或自研 MCP 客户端，便于检索、调用与审计。
- **多环境与灰度发布**：同一业务系统配置开发/测试/生产等环境，按环境发布不同 MCP 服务或切换转发的 Base URL，方便在 AI 侧做联调与发布验证。

### 小结

上篇从协议与传输层拆解了「为什么要自研网关、为何推荐无状态」；本篇介绍的 **MCP Gateway** 即是该思路的落地实现：从导入或录入 API 文档、配置资源组合与认证，到发布 MCP Server、在客户端复制配置使用，全流程打通，业务侧零侵入，让传统 REST 服务也能无缝对接大模型生态。若你正打算把现有 API 暴露给 Claude、Cursor 或自研 AI 应用，欢迎试用并反馈。

---

**项目地址**：https://github.com/fanzhongwei/mcp-gateway  

欢迎使用、提 Issue 和 PR，如果对你有帮助，也欢迎给个 Star。
