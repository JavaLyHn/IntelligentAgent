# Intelligent Agent - AI工作流平台

## 项目简介
一个可视化的 AI Agent 工作流编辑和执行平台，支持大模型节点、工具节点的拖拽编排，以及实时调试和音频输出功能。

## 技术栈

### 前端
- **React 18** + TypeScript
- **ReactFlow 12** - 流程图编辑器
- **Ant Design 5** - UI组件库
- **Zustand** - 状态管理
- **Vite** - 构建工具

### 后端
- **Spring Boot 3.2** + JDK 17
- **Spring Data JPA** - 数据持久化
- **H2 Database** - 嵌入式数据库（可切换MySQL）
- **OkHttp** - HTTP客户端（LLM API调用）
- **Lombok** - 代码简化

### 工作流引擎
- 自研轻量级 DAG 图执行引擎
- 拓扑排序 + 逐节点执行 + 数据流转
- 可扩展的节点处理器架构

## 功能特性

✅ **可视化流程编辑**
- 拖拽式节点添加（左侧节点库）
- 实时连线配置数据流向
- 支持多种自定义节点类型

✅ **丰富的节点类型**
- 🤖 大模型节点：OpenAI、DeepSeek、通义千问
- 🛠️ 工具节点：超拟人音频合成(TTS)
- 📥 输入/📤 输出节点

✅ **节点配置面板**
- 动态参数配置
- 模型参数调节（Temperature、MaxTokens等）
- 音频参数设置（音色、语速、音调等）

✅ **调试抽屉**
- 实时输入测试文本
- 执行日志展示
- 模型输出预览
- 音频播放功能

✅ **工作流持久化**
- 工作流配置保存/加载
- 执行记录查询
- 版本管理

## 快速开始

### 前置条件
- JDK 17+
- Maven 3.8+
- Node.js 18+

### 1. 启动后端（Spring Boot）
```bash
cd backend
mvn spring-boot:run
```
后端服务运行在 http://localhost:4000

### 2. 启动前端
```bash
cd frontend
npm install
npm run dev
```
前端应用运行在 http://localhost:3000

### 3. 访问应用
打开浏览器访问: http://localhost:3000

## 使用指南

### 创建工作流
1. 从左侧节点库拖拽节点到画布
2. 点击节点进行参数配置
3. 连接各节点形成完整的数据流
4. 点击"保存"按钮保存工作流
5. 点击"调试"按钮打开调试面板
6. 输入测试文本，点击"执行测试"
7. 查看模型输出和生成的音频

### 配置API密钥
在 `backend/src/main/resources/application.yml` 中配置：
```yaml
llm:
  providers:
    openai:
      api-key: ${OPENAI_API_KEY:sk-your-key}
    deepseek:
      api-key: ${DEEPSEEK_API_KEY:sk-your-key}
    tongyi:
      api-key: ${TONGYI_API_KEY:sk-your-key}
```

或通过环境变量设置：
```bash
export OPENAI_API_KEY=sk-your-key
export DEEPSEEK_API_KEY=sk-your-key
export TONGYI_API_KEY=sk-your-key
```

> ⚠️ 不配置API密钥时，系统将使用演示模式返回模拟数据

## 项目结构

```
IntelligentAgent/
├── frontend/                          # React前端应用
│   ├── src/
│   │   ├── components/
│   │   │   ├── Header.tsx             # 顶部工具栏（新建/加载/保存/调试）
│   │   │   ├── Sidebar.tsx            # 左侧节点库
│   │   │   ├── Canvas.tsx             # 中间画板（ReactFlow）
│   │   │   ├── ConfigPanel.tsx        # 右侧配置面板
│   │   │   └── DebugDrawer.tsx        # 调试抽屉
│   │   ├── nodes/
│   │   │   └── CustomNodes.tsx        # 自定义节点组件
│   │   ├── store/
│   │   │   └── workflowStore.ts       # Zustand状态管理
│   │   └── types/
│   │       └── workflow.ts            # TypeScript类型定义
│   ├── package.json
│   ├── vite.config.ts
│   └── tsconfig.json
│
├── backend/                           # Spring Boot后端服务
│   ├── src/main/java/com/intelligentagent/
│   │   ├── IntelligentAgentApplication.java  # 启动类
│   │   ├── config/
│   │   │   └── CorsConfig.java               # CORS配置
│   │   ├── controller/
│   │   │   └── WorkflowController.java       # REST API
│   │   ├── dto/
│   │   │   ├── ApiResponse.java              # 统一响应
│   │   │   ├── WorkflowDTO.java              # 工作流数据传输
│   │   │   ├── ExecuteRequestDTO.java        # 执行请求
│   │   │   ├── ExecuteResponseDTO.java       # 执行响应
│   │   │   └── ExecutionRecordDTO.java       # 执行记录
│   │   ├── engine/
│   │   │   ├── DAGWorkflowEngine.java        # ⭐ DAG工作流引擎核心
│   │   │   ├── model/                        # 引擎模型
│   │   │   │   ├── WorkflowDefinition.java
│   │   │   │   ├── WorkflowNode.java
│   │   │   │   ├── WorkflowEdge.java
│   │   │   │   ├── NodeContext.java
│   │   │   │   └── WorkflowExecutionResult.java
│   │   │   ├── processor/                    # 节点处理器
│   │   │   │   ├── NodeProcessor.java        # 处理器接口
│   │   │   │   ├── InputNodeProcessor.java
│   │   │   │   ├── OutputNodeProcessor.java
│   │   │   │   ├── LLMNodeProcessor.java
│   │   │   │   └── TTSNodeProcessor.java
│   │   │   └── llm/                          # LLM适配层
│   │   │       ├── LLMProvider.java          # 提供商接口
│   │   │       ├── LLMRequest.java
│   │   │       ├── LLMResponse.java
│   │   │       └── OpenAICompatibleProvider.java  # OpenAI兼容适配
│   │   ├── entity/
│   │   │   ├── WorkflowConfig.java           # 工作流配置实体
│   │   │   └── ExecutionRecord.java          # 执行记录实体
│   │   ├── repository/
│   │   │   ├── WorkflowConfigRepository.java
│   │   │   └── ExecutionRecordRepository.java
│   │   └── service/
│   │       └── WorkflowService.java          # 业务逻辑层
│   ├── src/main/resources/
│   │   └── application.yml                  # 配置文件
│   └── pom.xml
│
└── package.json                       # 根配置
```

## API接口

### 工作流管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workflows | 创建工作流 |
| PUT | /api/workflows/{id} | 更新工作流 |
| GET | /api/workflows/{id} | 获取工作流 |
| GET | /api/workflows | 列出所有工作流 |
| DELETE | /api/workflows/{id} | 删除工作流 |

### 工作流执行

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/workflow/execute | 执行工作流 |

### 执行记录

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/workflows/{id}/executions | 获取执行记录 |
| GET | /api/executions/{id} | 获取单条记录 |

### 请求示例

**执行工作流:**
```json
POST /api/workflow/execute
{
  "workflowId": "optional-workflow-id",
  "input": "用户输入的文本",
  "nodes": [...],
  "edges": [...]
}
```

**响应:**
```json
{
  "success": true,
  "data": {
    "success": true,
    "outputText": "模型输出的内容",
    "audioUrl": "音频URL",
    "logs": ["执行日志..."],
    "durationMs": 1234
  }
}
```

## DAG工作流引擎设计

### 核心流程
1. **解析工作流定义** - 将前端传来的节点和边转换为内部模型
2. **拓扑排序** - 基于DAG边关系进行拓扑排序，确定执行顺序
3. **循环检测** - 如果存在循环依赖则抛出异常
4. **逐节点执行** - 按拓扑排序结果依次执行各节点
5. **数据流转** - 每个节点从上游节点收集输入数据，处理后将输出传递给下游
6. **结果汇总** - 从输出节点提取最终结果

### 扩展节点处理器
1. 实现 `NodeProcessor` 接口
2. 添加 `@Component` 注解
3. 引擎自动注册并匹配节点类型

### 扩展LLM提供商
1. 在 `application.yml` 中添加新提供商配置
2. 在 `OpenAICompatibleProvider.getConfig()` 中添加新的 case

## License
MIT
