import { create } from 'zustand';
import { type Node, type Edge, applyNodeChanges, applyEdgeChanges } from '@xyflow/react';
import type { NodeChange, EdgeChange } from '@xyflow/react';

const DEFAULT_NODES: Node[] = [
  {
    id: 'input-node-1',
    type: 'inputNode',
    position: { x: 250, y: 50 },
    data: {
      label: '输入',
      type: 'input',
      category: 'input',
      config: {
        variableName: 'user_input',
        variableType: 'String',
        description: '用户本轮的输入内容',
        required: true,
      },
    },
  },
  {
    id: 'output-node-1',
    type: 'outputNode',
    position: { x: 250, y: 500 },
    data: {
      label: '输出',
      type: 'output',
      category: 'output',
      config: {
        outputParams: [],
        answerTemplate: '',
      },
    },
  },
];

export interface NodeExecutionState {
  nodeId: string;
  nodeType: string;
  label: string;
  status: 'pending' | 'running' | 'success' | 'failed';
  durationMs?: number;
  outputData?: Record<string, any>;
  errorMessage?: string;
}

export interface ExecutionEvent {
  type: string;
  nodeId?: string;
  nodeType?: string;
  label?: string;
  status?: string;
  message?: string;
  durationMs?: number;
  data?: Record<string, any>;
}

interface WorkflowState {
  nodes: Node[];
  edges: Edge[];
  selectedNodeId: string | null;
  isDebugOpen: boolean;
  debugInput: string;
  debugOutput: string;
  isExecuting: boolean;
  executionResult: Record<string, any> | null;
  currentWorkflowId: string | null;
  currentWorkflowName: string;

  nodeExecutionStates: Record<string, NodeExecutionState>;
  executionLogs: string[];
  currentExecutingNodeId: string | null;

  onNodesChange: (changes: NodeChange[]) => void;
  onEdgesChange: (changes: EdgeChange[]) => void;
  addNode: (node: Node) => void;
  updateNodeData: (nodeId: string, data: Record<string, any>) => void;
  deleteNode: (nodeId: string) => void;
  setSelectedNode: (nodeId: string | null) => void;
  setIsDebugOpen: (open: boolean) => void;
  setDebugInput: (input: string) => void;
  setDebugOutput: (output: string) => void;
  setIsExecuting: (executing: boolean) => void;
  setNodes: (nodes: Node[]) => void;
  setEdges: (edges: Edge[]) => void;
  setCurrentWorkflowId: (id: string | null) => void;
  setCurrentWorkflowName: (name: string) => void;
  loadWorkflow: (id: string) => Promise<void>;
  saveWorkflow: () => Promise<void>;
  newWorkflow: () => void;
  executeWorkflow: (input: string) => Promise<void>;
}

export const useWorkflowStore = create<WorkflowState>((set, get) => ({
  nodes: DEFAULT_NODES,
  edges: [],
  selectedNodeId: null,
  isDebugOpen: false,
  debugInput: '',
  debugOutput: '',
  isExecuting: false,
  executionResult: null,
  currentWorkflowId: null,
  currentWorkflowName: '未命名工作流',
  nodeExecutionStates: {},
  executionLogs: [],
  currentExecutingNodeId: null,

  onNodesChange: (changes) => {
    set({ nodes: applyNodeChanges(changes, get().nodes) });
  },

  onEdgesChange: (changes) => {
    set({ edges: applyEdgeChanges(changes, get().edges) });
  },

  addNode: (node) => {
    set({ nodes: [...get().nodes, node] });
  },

  updateNodeData: (nodeId, data) => {
    set({
      nodes: get().nodes.map((node) =>
        node.id === nodeId
          ? { ...node, data: { ...node.data, ...data, config: { ...(node.data.config || {}), ...(data.config || {}) } } }
          : node
      ),
    });
  },

  deleteNode: (nodeId) => {
    set({
      nodes: get().nodes.filter((n) => n.id !== nodeId),
      edges: get().edges.filter((e) => e.source !== nodeId && e.target !== nodeId),
      selectedNodeId: get().selectedNodeId === nodeId ? null : get().selectedNodeId,
    });
  },

  setSelectedNode: (nodeId) => {
    set({ selectedNodeId: nodeId });
  },

  setIsDebugOpen: (open) => {
    set({ isDebugOpen: open });
  },

  setDebugInput: (input) => {
    set({ debugInput: input });
  },

  setDebugOutput: (output) => {
    set({ debugOutput: output });
  },

  setIsExecuting: (executing) => {
    set({ isExecuting: executing });
  },

  setNodes: (nodes) => {
    set({ nodes });
  },

  setEdges: (edges) => {
    set({ edges });
  },

  setCurrentWorkflowId: (id) => {
    set({ currentWorkflowId: id });
  },

  setCurrentWorkflowName: (name) => {
    set({ currentWorkflowName: name });
  },

  loadWorkflow: async (id) => {
    try {
      const response = await fetch(`/api/workflows/${id}`);
      const result = await response.json();
      if (result.success && result.data) {
        const wf = result.data;
        set({
          nodes: wf.nodes && wf.nodes.length > 0 ? wf.nodes : DEFAULT_NODES,
          edges: wf.edges || [],
          currentWorkflowId: wf.id,
          currentWorkflowName: wf.name,
        });
      }
    } catch (error) {
      console.error('加载工作流失败:', error);
    }
  },

  saveWorkflow: async () => {
    const { nodes, edges, currentWorkflowId, currentWorkflowName } = get();
    const payload = {
      name: currentWorkflowName,
      nodes,
      edges,
    };

    try {
      let response;
      if (currentWorkflowId) {
        response = await fetch(`/api/workflows/${currentWorkflowId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });
      } else {
        response = await fetch('/api/workflows', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload),
        });
      }

      const result = await response.json();
      if (result.success && result.data) {
        set({ currentWorkflowId: result.data.id });
      }
    } catch (error) {
      console.error('保存工作流失败:', error);
    }
  },

  newWorkflow: () => {
    set({
      nodes: DEFAULT_NODES,
      edges: [],
      currentWorkflowId: null,
      currentWorkflowName: '未命名工作流',
      selectedNodeId: null,
      debugInput: '',
      debugOutput: '',
      executionResult: null,
      nodeExecutionStates: {},
      executionLogs: [],
      currentExecutingNodeId: null,
    });
  },

  executeWorkflow: async (input: string) => {
    const { nodes, edges, currentWorkflowId } = get();

    const initialStates: Record<string, NodeExecutionState> = {};
    nodes.forEach((n) => {
      initialStates[n.id] = {
        nodeId: n.id,
        nodeType: n.type || 'unknown',
        label: (n.data as any)?.label || n.id,
        status: 'pending',
      };
    });

    set({
      isExecuting: true,
      executionResult: null,
      nodeExecutionStates: initialStates,
      executionLogs: [],
      currentExecutingNodeId: null,
    });

    const payload = {
      workflowId: currentWorkflowId,
      input,
      nodes: nodes.map((n) => ({
        id: n.id,
        type: n.type,
        position: n.position,
        data: n.data,
      })),
      edges: edges.map((e) => ({
        id: e.id,
        source: e.source,
        target: e.target,
        sourceHandle: e.sourceHandle,
        targetHandle: e.targetHandle,
      })),
    };

    try {
      const response = await fetch('/api/workflow/execute/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('无法获取响应流');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || '';

        let currentEventType = '';
        for (const line of lines) {
          if (line.startsWith('event:')) {
            currentEventType = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            const jsonStr = line.slice(5).trim();
            if (jsonStr) {
              try {
                const event: ExecutionEvent = JSON.parse(jsonStr);
                handleExecutionEvent(event, set, get);
              } catch (e) {
                console.warn('Failed to parse SSE event:', jsonStr, e);
              }
            }
          }
        }
      }

      if (buffer.trim()) {
        const remainingLines = buffer.split('\n');
        let currentEventType = '';
        for (const line of remainingLines) {
          if (line.startsWith('event:')) {
            currentEventType = line.slice(6).trim();
          } else if (line.startsWith('data:')) {
            const jsonStr = line.slice(5).trim();
            if (jsonStr) {
              try {
                const event: ExecutionEvent = JSON.parse(jsonStr);
                handleExecutionEvent(event, set, get);
              } catch (e) {
                console.warn('Failed to parse SSE event:', jsonStr, e);
              }
            }
          }
        }
      }
    } catch (error: any) {
      console.error('工作流执行异常:', error);
      set({
        executionResult: {
          success: false,
          errorMessage: error.message || '网络请求失败，请检查后端服务是否启动',
          logs: get().executionLogs,
        },
        isExecuting: false,
        currentExecutingNodeId: null,
      });
    }
  },
}));

function handleExecutionEvent(
  event: ExecutionEvent,
  set: (partial: Partial<WorkflowState> | ((state: WorkflowState) => Partial<WorkflowState>)) => void,
  get: () => WorkflowState
) {
  const state = get();

  switch (event.type) {
    case 'workflow_started': {
      set({
        executionLogs: [...state.executionLogs, '🚀 工作流开始执行...'],
      });
      break;
    }

    case 'node_started': {
      if (!event.nodeId) break;
      const updatedStates = { ...state.nodeExecutionStates };
      updatedStates[event.nodeId] = {
        ...updatedStates[event.nodeId],
        nodeId: event.nodeId,
        nodeType: event.nodeType || 'unknown',
        label: event.label || event.nodeId,
        status: 'running',
      };
      set({
        nodeExecutionStates: updatedStates,
        currentExecutingNodeId: event.nodeId,
        executionLogs: [...state.executionLogs, `⚙️ ${event.label || event.nodeId} 开始执行...`],
      });
      break;
    }

    case 'node_completed': {
      if (!event.nodeId) break;
      const updatedStates = { ...state.nodeExecutionStates };
      updatedStates[event.nodeId] = {
        ...updatedStates[event.nodeId],
        status: 'success',
        durationMs: event.durationMs,
        outputData: event.data,
      };
      set({
        nodeExecutionStates: updatedStates,
        executionLogs: [...state.executionLogs, `✅ ${event.label || event.nodeId} 执行完成 (${event.durationMs || 0}ms)`],
      });
      break;
    }

    case 'node_failed': {
      if (!event.nodeId) break;
      const updatedStates = { ...state.nodeExecutionStates };
      updatedStates[event.nodeId] = {
        ...updatedStates[event.nodeId],
        status: 'failed',
        durationMs: event.durationMs,
        errorMessage: event.data?.error as string || event.message,
      };
      set({
        nodeExecutionStates: updatedStates,
        executionLogs: [...state.executionLogs, `❌ ${event.label || event.nodeId} 执行失败: ${event.message}`],
      });
      break;
    }

    case 'log': {
      if (event.message) {
        set({
          executionLogs: [...state.executionLogs, event.message],
        });
      }
      break;
    }

    case 'workflow_completed': {
      const data = event.data || {};
      const result: Record<string, any> = {
        success: data.success ?? true,
        outputText: data.outputText || '',
        audioUrl: data.audioUrl || '',
        ttsStatus: data.ttsStatus || '',
        ttsError: data.ttsError || '',
        durationMs: event.durationMs,
        logs: [...state.executionLogs, event.message || '🎉 工作流执行完成'],
      };

      if (!data.success && data.ttsError) {
        result.errorMessage = data.ttsError;
      }

      set({
        executionResult: result,
        isExecuting: false,
        currentExecutingNodeId: null,
      });
      break;
    }

    case 'workflow_failed': {
      set({
        executionResult: {
          success: false,
          errorMessage: event.message || '工作流执行失败',
          logs: [...state.executionLogs, `❌ ${event.message || '工作流执行失败'}`],
        },
        isExecuting: false,
        currentExecutingNodeId: null,
      });
      break;
    }
  }
}
