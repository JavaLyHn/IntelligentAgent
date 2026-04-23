import { create } from 'zustand';
import { type Node, type Edge, applyNodeChanges, applyEdgeChanges } from '@xyflow/react';
import type { NodeChange, EdgeChange } from '@xyflow/react';

interface WorkflowState {
  nodes: Node[];
  edges: Edge[];
  selectedNodeId: string | null;
  isDebugOpen: boolean;
  debugInput: string;
  debugOutput: string;
  isExecuting: boolean;
  currentWorkflowId: string | null;
  currentWorkflowName: string;

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
}

export const useWorkflowStore = create<WorkflowState>((set, get) => ({
  nodes: [],
  edges: [],
  selectedNodeId: null,
  isDebugOpen: false,
  debugInput: '',
  debugOutput: '',
  isExecuting: false,
  currentWorkflowId: null,
  currentWorkflowName: '未命名工作流',

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
        node.id === nodeId ? { ...node, data: { ...node.data, ...data } } : node
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
          nodes: wf.nodes || [],
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
      nodes: [],
      edges: [],
      currentWorkflowId: null,
      currentWorkflowName: '未命名工作流',
      selectedNodeId: null,
      debugInput: '',
      debugOutput: '',
    });
  },
}));
