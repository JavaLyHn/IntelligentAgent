export enum NodeType {
  INPUT = 'input',
  LLM = 'llm',
  TOOL = 'tool',
  OUTPUT = 'output',
}

export enum LLMProvider {
  DEEPSEEK = 'deepseek',
  TONGYI = 'tongyi',
  AI_PING = 'aiping',
  ZHIPU = 'zhipu',
}

export enum ToolType {
  TTS_AUDIO = 'tts_audio',
}

export interface NodeData {
  label: string;
  type: NodeType | LLMProvider | ToolType;
  config?: Record<string, any>;
  [key: string]: any;
}

export interface WorkflowNode {
  id: string;
  type: string;
  position: { x: number; y: number };
  data: NodeData;
}

export interface WorkflowEdge {
  id: string;
  source: string;
  target: string;
  sourceHandle?: string;
  targetHandle?: string;
}

export interface WorkflowData {
  nodes: WorkflowNode[];
  edges: WorkflowEdge[];
}

export interface LLMNodeConfig {
  provider: LLMProvider;
  model: string;
  temperature: number;
  maxTokens: number;
  systemPrompt: string;
  userPrompt: string;
}

export interface TTSNodeConfig {
  voiceType: string;
  speed: number;
  pitch: number;
  volume: number;
  textInput: string;
}
