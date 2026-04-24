import React, { memo } from 'react';
import { Handle, Position, NodeProps } from '@xyflow/react';

interface BaseNodeData {
  label: string;
  type: string;
  [key: string]: any;
}

const InputNode = memo(({ data, selected }: NodeProps) => (
  <div
    style={{
      padding: '14px 24px',
      borderRadius: 10,
      background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
      color: '#fff',
      minWidth: 160,
      textAlign: 'center',
      boxShadow: selected
        ? '0 0 0 2px #1890ff, 0 4px 12px rgba(102, 126, 234, 0.4)'
        : '0 2px 8px rgba(102, 126, 234, 0.3)',
      transition: 'box-shadow 0.2s',
    }}
  >
    <div style={{ fontSize: 18, marginBottom: 4 }}>📥</div>
    <div style={{ fontWeight: 600, fontSize: 14 }}>{data.label || '输入'}</div>
    <div style={{ fontSize: 11, opacity: 0.8, marginTop: 2 }}>user_input</div>
    <Handle type="source" position={Position.Bottom} style={{ background: '#667eea', width: 10, height: 10 }} />
  </div>
));

InputNode.displayName = 'InputNode';

const LLMNode = memo(({ data, selected }: NodeProps<BaseNodeData>) => {
  const colors: Record<string, { bg: string; border: string; icon: string }> = {
    deepseek: { bg: '#fff0f5', border: '#ff6b9d', icon: '🌸' },
    tongyi: { bg: '#fffbe6', border: '#faad14', icon: '☀️' },
    aiping: { bg: '#fff1f0', border: '#ff4d4f', icon: '🚀' },
    zhipu: { bg: '#f9f0ff', border: '#722ed1', icon: '💎' },
  };

  const color = colors[data.type] || { bg: '#f0f5ff', border: '#1890ff', icon: '🤖' };

  return (
    <div
      style={{
        padding: '14px 24px',
        borderRadius: 10,
        background: color.bg,
        border: `2px solid ${color.border}`,
        minWidth: 160,
        textAlign: 'center',
        boxShadow: selected
          ? `0 0 0 2px #1890ff, 0 4px 12px ${color.border}55`
          : `0 2px 8px ${color.border}33`,
        transition: 'box-shadow 0.2s',
      }}
    >
      <Handle type="target" position={Position.Top} style={{ background: color.border, width: 10, height: 10 }} />
      <div style={{ fontSize: 18, marginBottom: 4 }}>{color.icon}</div>
      <div style={{ fontWeight: 600, color: '#333', fontSize: 14 }}>
        {data.label || '大模型'}
      </div>
      <Handle type="source" position={Position.Bottom} style={{ background: color.border, width: 10, height: 10 }} />
    </div>
  );
});

LLMNode.displayName = 'LLMNode';

const ToolNode = memo(({ data, selected }: NodeProps<BaseNodeData>) => (
  <div
    style={{
      padding: '14px 20px',
      borderRadius: 10,
      background: 'linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%)',
      border: '2px solid #1890ff',
      minWidth: 170,
      textAlign: 'center',
      boxShadow: selected
        ? '0 0 0 2px #1890ff, 0 4px 12px rgba(24, 144, 255, 0.4)'
        : '0 2px 8px rgba(24, 144, 255, 0.3)',
      transition: 'box-shadow 0.2s',
    }}
  >
    <Handle type="target" position={Position.Top} style={{ background: '#1890ff', width: 10, height: 10 }} />
    <div style={{ fontSize: 18, marginBottom: 4 }}>🎙️</div>
    <div style={{ fontWeight: 600, color: '#0050b3', fontSize: 14 }}>
      {data.label || '工具'}
    </div>
    <Handle type="source" position={Position.Bottom} style={{ background: '#1890ff', width: 10, height: 10 }} />
  </div>
));

ToolNode.displayName = 'ToolNode';

const OutputNode = memo(({ data, selected }: NodeProps) => (
  <div
    style={{
      padding: '14px 24px',
      borderRadius: 10,
      background: 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)',
      color: '#fff',
      minWidth: 160,
      textAlign: 'center',
      boxShadow: selected
        ? '0 0 0 2px #1890ff, 0 4px 12px rgba(82, 196, 26, 0.4)'
        : '0 2px 8px rgba(82, 196, 26, 0.3)',
      transition: 'box-shadow 0.2s',
    }}
  >
    <Handle type="target" position={Position.Top} style={{ background: '#52c41a', width: 10, height: 10 }} />
    <div style={{ fontSize: 18, marginBottom: 4 }}>📤</div>
    <div style={{ fontWeight: 600, fontSize: 14 }}>{data.label || '输出'}</div>
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
