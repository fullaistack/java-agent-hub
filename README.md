# java-agent-hub

基于 **AgentScope Java 2.0** 构建的企业级多租户 **RAG + Harness Agent + MCP 工具平台**。

面向 Java 后端开发者：一套代码讲清楚「多租户数据隔离 → 文档解析 → 向量化 → 混合检索 → Agent 编排 → MCP 工具调用 → 全链路可观测」的企业级 LLM 应用架构，配套自媒体教程开源。

## 功能清单

| 模块 | 能力 |
| --- | --- |
| 多租户隔离 | 租户/用户/知识库权限隔离；MySQL 由 MyBatis-Plus 租户插件自动追加 `tenant_id`，PGVector 显式携带 `tenant_id`，文件存储按租户目录物理隔离 |
| 知识库 | 文档上传即返回、异步解析（MinerU 对接 + Tika 本地降级）、文本切分、向量化、向量入库（PGVector + HNSW） |
| RAG | 向量检索、关键字检索、RRF 混合检索、重排、过滤条件支持 |
| Harness Agent | 内置 ReAct 编排引擎 + AgentScope 2.0 HarnessAgent 双引擎，支持多轮对话与窗口记忆管理 |
| MCP 工具 | 内置 MCP Client（stdio / sse / streamable-http），连接 MCP Server 后工具自动注册，Agent 按需调用 |
| LLM 路由 | 多模型配置，兼容 Ollama、OpenAI 格式接口，内置 Mock 模型零依赖跑通全链路 |
| 链路监控 | Agent Hook + AOP 埋点，token 消耗、调用耗时、异常统计落库，对接 OpenTelemetry（Jaeger/Prometheus） |
| 部署 | Docker Compose 一键启动所有依赖 + 应用，默认 local 模式（进程内异步 + 本地存储 + Mock 模型） |

## 架构

```
接入层 gateway    → 租户认证拦截器（API Key / 登录态）、traceId 透传
业务服务层         → tenant / knowledge / llm 路由 / mcp 管理
Agent 核心层      → HarnessAgentEngine（builtin ReAct）⇄ AgentScopeAdapter（agentscope Harness）
插件层            → parser（MinerU/Tika）、rag（检索/重排）、mcp（工具桥接）
存储层            → MySQL（业务）、PGVector（向量）、MinIO/本地（文件）
异步层 task       → RocketMQ（doc-parse / chunk-embed Topic）或本地事件降级
```

关键设计：

1. **严格租户隔离**：所有表带 `tenant_id`，由 `TenantLineInnerInterceptor` 自动过滤（无需手写 WHERE）；向量检索 SQL 显式拼 `tenant_id`；`TenantContext`（ThreadLocal）经 `TaskDecorator` 透传异步线程。
2. **异步流水线**：`UPLOADED → PARSING → PARSED → EMBEDDING → READY`，上传接口立即返回，解析/向量化由 MQ 消费，失败可重试。
3. **双引擎**：`agenthub.agent.engine=builtin` 内置零依赖 ReAct 循环；`=agentscope` 切换 AgentScope 2.0 HarnessAgent（带工程化中间件体系），工具经 `AgentScopeToolBridge` 桥接。

## 快速启动

### 方式一：Docker Compose 一键启动（推荐）

```bash
# 1. 构建应用镜像
mvn clean package -DskipTests && docker build -t agenthub:1.0.0 .

# 2. 启动全部依赖（MySQL/PGVector/RocketMQ/MinIO/OTel/Jaeger）
docker compose up -d

# 3. 查看日志
docker compose logs -f agenthub
```

### 方式二：IDE 本地运行（只起数据库）

```bash
docker compose up -d mysql pgvector
# IDEA 直接运行 AgenthubApplication，或：
mvn spring-boot:run
```

默认配置即"最小可运行 demo"：`MQ_TYPE=local`（进程内异步）、`STORAGE_TYPE=local`（本地磁盘）、`mock-chat`/`mock-embed`（内置 Mock 模型），**无需任何外部 LLM 与 MQ 即可跑通全链路**。

### 体验流程（curl）

```bash
# 1. 登录（种子租户 demo / admin / admin123）
curl -X POST http://localhost:8080/api/v1/tenant/login \
  -H "Content-Type: application/json" \
  -d '{"tenantCode":"demo","username":"admin","password":"admin123"}'

# 之后所有请求带租户认证头（或用 API Key: ahk_demo1234567890abcdef）
AUTH='X-Tenant-Id: 1'

# 2. 创建知识库（默认已有 id=1 的"企业产品手册"）
curl -X POST http://localhost:8080/api/v1/kb \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"name":"产品文档","description":"内部产品文档库"}'

# 3. 上传文档（返回后轮询状态，READY 即可检索）
curl -X POST http://localhost:8080/api/v1/kb/1/documents \
  -H "$AUTH" -F "file=@./docs/demo.md"
curl http://localhost:8080/api/v1/documents/1 -H "$AUTH"

# 4. Agent 对话（RAG 检索由 Agent 按需调用）
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"kbId":1,"message":"产品的退款政策是什么？"}'
```

## 租户 RAG 流程

```
用户上传文档
   │  TenantContext(tenantId) → storage 路径含租户ID（物理隔离）
   ▼
document 落库（UPLOADED）→ MQ: parse 任务（携 tenantId）
   │
   ▼
解析消费者：PARSING → MinerU/Tika 解析 → parsed_text 落库（PARSED）→ MQ: embed 任务
   │
   ▼
向量化消费者：EMBEDDING → 分块 → embedding（Ollama/OpenAI/Mock）→ PGVector 批量写入（含 tenant_id）→ READY
   │
   ▼
Agent 对话：rag_search 工具 → EmbeddingRouter → HybridRetriever（向量 + 关键字 RRF 融合）
   │  全部检索 SQL 强制 tenant_id = 当前租户
   ▼
重排 → 拼装上下文（标注来源文档）→ LLM 生成回答
```

## MCP 接入示例

```bash
# 1. 注册一个 Streamable HTTP 类型的 MCP Server（如部署在 9001 端口的服务）
curl -X POST http://localhost:8080/api/v1/mcp/servers \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{
        "name": "weather",
        "transportType": "streamable-http",
        "baseUrl": "http://localhost:9001",
        "endpoint": "/mcp",
        "enabled": true
      }'

# 2. 建立连接并把 Server 工具注册进 Agent（工具名: mcp__weather__get_forecast）
curl -X POST http://localhost:8080/api/v1/mcp/servers/1/connect -H "$AUTH"

# 3. 查看已注册工具
curl http://localhost:8080/api/v1/mcp/servers/1/tools -H "$AUTH"

# 4. Agent 对话中自然触发：LLM 判断需要查询天气时自动调用 MCP 工具
curl -X POST http://localhost:8080/api/v1/agent/chat \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{"message":"帮我查一下北京明天的天气"}'
```

支持三种传输：`stdio`（npx/uvx/java -jar 子进程）、`sse`、`streamable-http`（推荐）。

## 可观测性

- **埋点落库**：`GET /api/v1/tracing/stats` 查看按类型聚合的耗时/token 统计；`GET /api/v1/tracing/calls` 查看明细。
- **链路**：Micrometer Tracing → OTLP → Collector → Jaeger（`http://localhost:16686`），Span 带 `tenant.id` 标签，可按租户过滤。
- **指标**：Collector 暴露 Prometheus 端点（`:9464`），应用 Actuator 也暴露 `prometheus` 指标。

## 切换到真实依赖（生产形态）

```yaml
# application.yml 或环境变量
MQ_TYPE: rocketmq          # RocketMQ 异步任务
STORAGE_TYPE: minio        # 对象存储
LLM_DEFAULT_MODEL: qwen2.5:7b     # 真实 LLM（Ollama/OpenAI 兼容）
EMBEDDING_MODEL: ollama-embed     # 真实向量化模型
AGENT_ENGINE: agentscope          # 切换 AgentScope 2.0 Harness 引擎
PARSER_PROVIDER: mineru           # 高精度解析（GPU）
```

模型 URI 格式：AgentScope 引擎使用 `{provider}:{model}`（如 `ollama:qwen2.5:7b`），依赖 `agentscope-extensions-model-openai/ollama` 模块。

## 项目结构

```
com.agenthub
├── gateway        # 网关：租户拦截器、链路拦截器、异步线程配置
├── tenant         # 租户、用户、角色、API Key、资源隔离
├── knowledge      # 知识库管理、文档上传、文档任务
├── parser         # 文档解析模块（MinerU 对接 + Tika 降级）
├── rag            # RAG 核心：分块、向量化、混合检索、重排、PGVector
├── agent          # AgentScope Harness 封装、双引擎编排、记忆、默认提示词
├── mcp            # MCP Client 模块、工具发现与注册调用
├── llm            # LLM 模型路由、多模型适配、token 统计
├── tracing        # 链路监控、OpenTelemetry 埋点、指标统计
├── common         # 公共枚举、异常、工具类
└── task           # 异步任务：RocketMQ 生产者/消费者 + 本地事件降级
```

## 自媒体教程链接（占位）

- 教程系列一：多租户数据隔离的三种实现与 MyBatis-Plus 租户插件原理 → `https://blog.example.com/agenthub/multi-tenant`
- 教程系列二：RAG 异步流水线：MinerU + 分块 + PGVector 实战 → `https://blog.example.com/agenthub/rag-pipeline`
- 教程系列三：AgentScope Harness 与内置 ReAct 引擎对比 → `https://blog.example.com/agenthub/harness`
- 教程系列四：MCP 协议与 Java SDK 2.0 实战 → `https://blog.example.com/agenthub/mcp`
- 教程系列五：OpenTelemetry 全链路埋点落地 → `https://blog.example.com/agenthub/otel`
- B 站 / 视频号 / 公众号：`@java-agent-hub`（占位，发布后替换）

## 默认提示词

**RAG 助手**（知识库问答）：

> 你是专业知识库助手，基于检索到的文档内容回答用户问题。
> 1. 只使用参考文档中的信息回答，如果文档没有相关内容，直接说明不知道，不要编造；
> 2. 回答时标注引用来源文档名称；
> 3. 语言简洁，条理清晰；
> 4. 禁止幻觉，不要自己编造事实。

**Agent（Harness）**（工具调用编排）：

> 你是可以调用工具的智能助手，拥有检索知识库、调用MCP工具的能力。
> 1. 先判断用户问题是否需要检索知识库，需要则调用RAG检索工具；
> 2. 判断是否需要调用MCP工具，按需调用；
> 3. 拿到工具返回结果后再整理回答；
> 4. 不要编造工具返回结果，不知道就如实告知用户；
> 5. 回答尽量贴合企业内部业务场景，简洁专业。

代码实现见 `agent/prompt/Prompts.java`。

## 技术栈

Java 17 · Spring Boot 3.3 · AgentScope Java 2.0（agentscope-harness）· MCP Java SDK 2.0 · MyBatis-Plus 3.5（租户插件）· MySQL 8 · PostgreSQL 16 + pgvector（HNSW）· RocketMQ 5 · MinIO · Apache Tika · Micrometer Tracing + OpenTelemetry · Docker Compose

## License

Apache-2.0
