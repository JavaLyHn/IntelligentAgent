# 🤖 IntelligentAgent - AI 智能体工作流平台

<div align="center">

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://reactjs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.3-blue.svg)](https://www.typescriptlang.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

**一个可视化的 AI Agent 工作流编排平台，支持 LLM 节点、TTS 工具节点的拖拽编排，实时调试与音频输出**

[功能特性](#-功能特性) • [快速开始](#-快速开始) • [架构设计](#-架构设计) • [API 文档](#-api-文档)

</div>

---

## 📸 项目截图

> 可视化工作流编辑器，支持拖拽式节点编排

## ✨ 功能特性

### 🔷 可视化流程编辑
- **拖拽式节点编排** - 从左侧节点库拖拽节点到画布，直观构建 AI 工作流
- **实时连线配置** - 通过连线定义数据流向，支持多输入多输出
- **智能吸附对齐** - 节点自动对齐，优雅的网格布局

### 🔷 丰富的节点类型

| 节点类型 | 图标 | 功能描述 |
|---------|------|---------|
| **输入节点** | 📥 | 定义工作流输入参数，支持变量名、类型、描述配置 |
| **LLM 节点** | 🤖 | 大模型推理节点，支持 OpenAI、DeepSeek、通义千问 |
| **TTS 节点** | 🎙️ | 超拟人音频合成，集成阿里百炼 qwen3-tts-flash |
| **输出节点** | 📤 | 定义工作流输出，支持模板渲染和音频播放 |

### 🔷 实时执行状态
- **SSE 流式推送** - 节点执行状态实时更新，无需等待
- **进度可视化** - 每个节点显示执行状态（等待中→执行中→已完成/失败）
- **实时日志流** - 终端风格日志展示，自动滚动到最新

### 🔷 智能音频处理
- **长文本分段** - 超过 600 字符自动按标点智能分段
- **多段音频合并** - 多个 TTS 片段自动合并为单个 WAV 文件
- **MinIO 持久化** - 音频文件上传至对象存储，永久可访问

### 🔷 工作流管理
- **持久化存储** - 工作流配置保存至 MySQL，支持版本管理
- **执行记录追溯** - 完整的执行历史，包含输入输出和耗时
- **一键导入导出** - 支持工作流配置的导入导出

---

## 🛠 技术栈

### 前端
| 技术 | 版本 | 用途 |
|------|------|------|
| React | 18.3 | UI 框架 |
| TypeScript | 5.3 | 类型安全 |
| ReactFlow | 12.0 | 流程图编辑器 |
| Ant Design | 5.13 | UI 组件库 |
| Zustand | 4.5 | 状态管理 |
| Vite | 5.2 | 构建工具 |

### 后端
| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.5 | 后端框架 |
| JDK | 17 | 运行时环境 |
| MyBatis-Plus | 3.5.7 | ORM 框架 |
| MySQL | 8.x | 关系型数据库 |
| MinIO | 8.5.9 | 对象存储 |
| OkHttp | 4.12.0 | HTTP 客户端 |

### AI 服务集成
- **OpenAI API** - GPT-4o 等模型
- **DeepSeek API** - deepseek-chat 模型
- **阿里百炼 API** - qwen-plus LLM + qwen3-tts-flash TTS

---

## 🚀 快速开始

### 前置条件

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.x
- MinIO (可选，用于音频存储)

### 1. 克隆项目

```bash
git clone https://github.com/your-username/IntelligentAgent.git
cd IntelligentAgent
```

### 2. 配置数据库

创建 MySQL 数据库：
```sql
CREATE DATABASE intelligent_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

表结构会自动创建（通过 `schema.sql`）。

### 3. 配置后端

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/intelligent_agent
    username: root
    password: your-password

llm:
  providers:
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:sk-your-key}
    tongyi:
      api-key: ${TONGYI_API_KEY:sk-your-key}

minio:
  endpoint: http://localhost:9000
  access-key: admin
  secret-key: your-secret
  bucket-name: intelligentagent
```

### 4. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务运行在 http://localhost:4000

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端应用运行在 http://localhost:5173

### 6. 访问应用

打开浏览器访问 http://localhost:5173

---

## 📁 项目结构

```
IntelligentAgent/
├── frontend/                              # React 前端应用
│   ├── src/
│   │   ├── components/
│   │   │   ├── Canvas.tsx                 # ReactFlow 画布
│   │   │   ├── ConfigPanel.tsx            # 节点配置面板
│   │   │   ├── DebugDrawer.tsx            # 调试抽屉（SSE 实时状态）
│   │   │   ├── Header.tsx                 # 顶部工具栏
│   │   │   └── Sidebar.tsx                # 左侧节点库
│   │   ├── nodes/
│   │   │   └── CustomNodes.tsx            # 自定义节点组件
│   │   ├── store/
│   │   │   └── workflowStore.ts           # Zustand 状态管理
│   │   └── types/
│   │       └── workflow.ts                # TypeScript 类型定义
│   ├── package.json
│   └── vite.config.ts
│
├── backend/                               # Spring Boot 后端服务
│   ├── src/main/java/com/intelligentagent/
│   │   ├── config/                        # 配置类
│   │   │   ├── CorsConfig.java
│   │   │   ├── MinioConfig.java
│   │   │   └── MinioProperties.java
│   │   ├── controller/                    # REST 控制器
│   │   │   ├── WorkflowController.java
│   │   │   └── WorkflowExecuteController.java  # SSE 流式执行
│   │   ├── dto/                           # 数据传输对象
│   │   ├── engine/                        # ⭐ DAG 工作流引擎
│   │   │   ├── DAGWorkflowEngine.java     # 核心引擎
│   │   │   ├── model/                     # 引擎模型
│   │   │   │   ├── ExecutionEvent.java    # SSE 事件模型
│   │   │   │   ├── WorkflowDefinition.java
│   │   │   │   └── WorkflowExecutionResult.java
│   │   │   ├── processor/                 # 节点处理器
│   │   │   │   ├── NodeProcessor.java     # 处理器接口
│   │   │   │   ├── InputNodeProcessor.java
│   │   │   │   ├── LLMNodeProcessor.java
│   │   │   │   ├── TTSNodeProcessor.java  # TTS + 音频合并
│   │   │   │   └── OutputNodeProcessor.java
│   │   │   ├── llm/                       # LLM 适配层
│   │   │   │   ├── OpenAICompatibleProvider.java
│   │   │   │   └── BailianTTSProvider.java
│   │   │   └── util/                      # 工具类
│   │   │       ├── TextSegmenter.java     # 文本分段
│   │   │       └── AudioMerger.java       # WAV 合并
│   │   ├── entity/                        # 数据实体
│   │   ├── mapper/                        # MyBatis Mapper
│   │   └── service/                       # 业务服务
│   │       └── MinioService.java          # MinIO 文件服务
│   ├── src/main/resources/
│   │   ├── application.yml                # 配置文件
│   │   └── db/schema.sql                  # 数据库建表脚本
│   └── pom.xml
│
└── README.md
```

---

## 🏗 架构设计

### DAG 工作流引擎

```
┌─────────────────────────────────────────────────────────────┐
│                    DAGWorkflowEngine                         │
├─────────────────────────────────────────────────────────────┤
│  1. 解析工作流定义 (nodes + edges)                           │
│  2. BFS 查找可达节点（过滤孤立节点）                          │
│  3. 拓扑排序确定执行顺序（Kahn 算法）                         │
│  4. 逐节点执行：                                             │
│     ├── 收集上游输出作为输入                                 │
│     ├── 查找 NodeProcessor 处理器                           │
│     ├── 执行 processor.process()                            │
│     └── 推送 SSE 事件（node_started → node_completed）       │
│  5. 提取最终结果（outputText + audioUrl）                    │
│  6. 推送 workflow_completed 事件                             │
└─────────────────────────────────────────────────────────────┘
```

### SSE 实时执行流程

```
前端                                后端
  │                                   │
  │  POST /api/workflow/execute/stream│
  │──────────────────────────────────>│
  │                                   │
  │  event: workflow_started          │
  │<──────────────────────────────────│
  │                                   │
  │  event: node_started (LLM)        │
  │<──────────────────────────────────│
  │  ... 节点执行中 ...                │
  │  event: node_completed (LLM)      │
  │<──────────────────────────────────│
  │                                   │
  │  event: node_started (TTS)        │
  │<──────────────────────────────────│
  │  ... TTS 调用 + 音频合并 ...       │
  │  event: node_completed (TTS)      │
  │<──────────────────────────────────│
  │                                   │
  │  event: workflow_completed        │
  │<──────────────────────────────────│
  │                                   │
```

### TTS 音频处理流程

```
输入文本 (>600字符)
    │
    ▼
TextSegmenter.segment() ──→ 多个文本分段
    │
    ▼
每段调用 BailianTTSProvider.synthesize()
    │
    ▼
MinioService.downloadBytes() ──→ 多个音频 byte[]
    │
    ▼
AudioMerger.mergeAudioFiles() ──→ 合并后的 WAV
    │
    ▼
MinioService.uploadBytes() ──→ 公开可访问的 URL
```

---

## 📖 API 文档

### 工作流管理

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/workflows` | 创建工作流 |
| `PUT` | `/api/workflows/{id}` | 更新工作流 |
| `GET` | `/api/workflows/{id}` | 获取工作流详情 |
| `GET` | `/api/workflows` | 列出所有工作流 |
| `DELETE` | `/api/workflows/{id}` | 删除工作流 |

### 工作流执行

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/workflow/execute` | 同步执行工作流 |
| `POST` | `/api/workflow/execute/stream` | SSE 流式执行 |

### SSE 事件类型

| 事件类型 | 说明 | 数据字段 |
|---------|------|---------|
| `workflow_started` | 工作流开始 | `nodeCount`, `edgeCount` |
| `node_started` | 节点开始执行 | `nodeId`, `nodeType`, `label` |
| `node_completed` | 节点执行完成 | `nodeId`, `durationMs`, `data` |
| `node_failed` | 节点执行失败 | `nodeId`, `message` |
| `workflow_completed` | 工作流完成 | `success`, `outputText`, `audioUrl` |
| `workflow_failed` | 工作流失败 | `message` |

### 请求示例

**执行工作流:**
```bash
curl -X POST http://localhost:4000/api/workflow/execute/stream \
  -H "Content-Type: application/json" \
  -H "Accept: text/event-stream" \
  -d '{
    "workflowId": "wf-001",
    "input": "你好，请介绍一下你自己",
    "nodes": [...],
    "edges": [...]
  }'
```

---

## 🔧 扩展开发

### 添加新的节点处理器

1. 实现 `NodeProcessor` 接口：

```java
@Component
public class MyCustomProcessor implements NodeProcessor {
    
    @Override
    public String getType() {
        return "customNode";
    }
    
    @Override
    public Map<String, Object> process(Map<String, Object> inputData, 
                                        Map<String, Object> config) {
        // 处理逻辑
        return Map.of("result", "...");
    }
}
```

2. 引擎会自动注册并匹配 `type="customNode"` 的节点

### 添加新的 LLM 提供商

在 `application.yml` 中添加配置：

```yaml
llm:
  providers:
    my-provider:
      base-url: https://api.example.com/v1
      api-key: ${MY_API_KEY:}
      default-model: model-name
```

在 `OpenAICompatibleProvider` 中添加 case 分支即可。

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 License

本项目基于 [MIT](LICENSE) 许可证开源。

---

## 🙏 致谢

- [ReactFlow](https://reactflow.dev/) - 强大的流程图编辑库
- [Ant Design](https://ant.design/) - 企业级 UI 组件库
- [Spring Boot](https://spring.io/projects/spring-boot) - 简化 Spring 开发
- [阿里百炼](https://bailian.console.aliyun.com/) - 超拟人 TTS 服务

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star！⭐**

Made with ❤️ by IntelligentAgent Team

</div>
