import React, { memo } from 'react';
import { Handle, Position, NodeProps } from '@xyflow/react';

interface BaseNodeData {
  label: string;
  type: string;
  [key: string]: any;
}

const InputNode = memo(({ data }: NodeProps) => (
  <div
    style={{
      padding: '12px 20px',
      borderRadius: 8,
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      color: '#fff',
      minWidth: 140,
      textAlign: 'center',
      boxShadow: '0 2px 8px rgba(102, 126, 234, 0.3)',
    }}
  >
    <Handle type="target" position={Position.Top} style={{ background: '#667eea' }} />
    <div style={{ fontWeight: 600 }}>{data.label || '输入'}</div>
    <Handle type="source" position={Position.Bottom} style={{ background: '#667eea' }} />
  </div>
));

InputNode.displayName = 'InputNode';

const LLMNode = memo(({ data }: NodeProps<BaseNodeData>) => {
  const colors: Record<string, { bg: string; border: string }> = {
    deepseek: { bg: '#fff0f5', border: '#ff6b9d' },
    tongyi: { bg: '#fffbe6', border: '#faad14' },
    aiping: { bg: '#fff1f0', border: '#ff4d4f' },
    zhipu: { bg: '#f9f0ff', border: '#722ed1' },
  };

  const color = colors[data.type] || { bg: '#f0f5ff', border: '#1890ff' };

  return (
    <div
      style={{
        padding: '14px 24px',
        borderRadius: 8,
        background: color.bg,
        border: `2px solid ${color.border}`,
        minWidth: 150,
        textAlign: 'center',
        boxShadow: `0 2px 8px ${color.border}33`,
      }}
    >
      <Handle type="target" position={Position.Top} style={{ background: color.border }} />
      <div style={{ fontWeight: 600, color: '#333' }}>
        {data.label || '大模型'}
      </div>
      <Handle type="source" position={Position.Bottom} style={{ background: color.border }} />
    </div>
  );
});

LLMNode.displayName = 'LLMNode';

const ToolNode = memo(({ data }: NodeProps<BaseNodeData>) => (
  <div
    style={{
      padding: '14px 20px',
      borderRadius: 8,
      background: 'linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%)',
      border: '2px solid #1890ff',
      minWidth: 160,
      textAlign: 'center',
      boxShadow: '0 2px 8px rgba(24, 144, 255, 0.3)',
    }}
  >
    <Handle type="target" position={Position.Top} style={{ background: '#1890ff' }} />
    <div style={{ fontWeight: 600, color: '#0050b3' }}>
      🎙️ {data.label || '工具'}
    </div>
    <Handle type="source" position={Position.Bottom} style={{ background: '#1890ff' }} />
  </div>
));

ToolNode.displayName = 'ToolNode';

const OutputNode = memo(({ data }: NodeProps) => (
  <div
    style={{
      padding: '12px 20px',
      borderRadius: 8,
      background: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
      color: '#fff',
      minWidth: 140,
      textAlign: 'center',
      boxShadow: '0 2px 8px rgba(82, 196, 26, 0.3)',
    }}
  >
    <Handle type="target" position={Position.Top} style={{ background: '#52c41a' }} />
    <div style={{ fontWeight: 600 }}>{data.label || '输出'}</div>
  </div>
));

OutputNode.displayName = 'OutputNode';

export const nodeTypes = {
  inputNode: InputNode,
  llmNode: LLMNode,
  toolNode: ToolNode,
  outputNode: OutputNode,
};

export default nodeTypes;
