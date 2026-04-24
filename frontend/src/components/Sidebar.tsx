import React, { useState } from 'react';
import { Collapse, Card, Tag } from 'antd';
import {
  RobotOutlined,
  ToolOutlined,
  DragOutlined,
} from '@ant-design/icons';
import { useWorkflowStore } from '../store/workflowStore';
import { NodeType, LLMProvider, ToolType } from '../types/workflow';

const llmNodes = [
  { type: LLMProvider.DEEPSEEK, label: 'DeepSeek', icon: '🌸', color: '#ff6b9d', bgColor: '#fff0f5' },
  { type: LLMProvider.TONGYI, label: '通义千问', icon: '☀️', color: '#faad14', bgColor: '#fffbe6' },
  { type: LLMProvider.AI_PING, label: 'AI Ping', icon: '🚀', color: '#ff4d4f', bgColor: '#fff1f0' },
  { type: LLMProvider.ZHIPU, label: '智谱', icon: '💎', color: '#722ed1', bgColor: '#f9f0ff' },
];

const toolNodes = [
  { type: ToolType.TTS_AUDIO, label: '超拟人音频合成', icon: '🎙️', color: '#1890ff', bgColor: '#e6f7ff' },
];

const Sidebar: React.FC = () => {
  const [activeKey, setActiveKey] = useState<string[]>(['llm', 'tool']);

  const handleDragStart = (
    e: React.DragEvent,
    nodeType: string,
    label: string,
    nodeCategory: NodeType
  ) => {
    e.dataTransfer.setData('nodeType', nodeType);
    e.dataTransfer.setData('label', label);
    e.dataTransfer.setData('category', nodeCategory);
  };

  const collapseItems = [
    {
      key: 'llm',
      label: (
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <RobotOutlined style={{ color: '#1890ff' }} />
          大模型节点
        </span>
      ),
      children: (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {llmNodes.map((node) => (
            <Card
              key={node.type}
              size="small"
              draggable
              onDragStart={(e) => handleDragStart(e, node.type, node.label, NodeType.LLM)}
              style={{
                cursor: 'grab',
                borderLeft: `3px solid ${node.color}`,
                background: node.bgColor,
                transition: 'all 0.2s',
              }}
              className="node-card"
              hoverable
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 20 }}>{node.icon}</span>
                <span style={{ fontWeight: 500 }}>{node.label}</span>
              </div>
            </Card>
          ))}
        </div>
      ),
    },
    {
      key: 'tool',
      label: (
        <span style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <ToolOutlined style={{ color: '#52c41a' }} />
          工具节点
        </span>
      ),
      children: (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {toolNodes.map((node) => (
            <Card
              key={node.type}
              size="small"
              draggable
              onDragStart={(e) => handleDragStart(e, node.type, node.label, NodeType.TOOL)}
              style={{
                cursor: 'grab',
                borderLeft: `3px solid ${node.color}`,
                background: node.bgColor,
                transition: 'all 0.2s',
              }}
              hoverable
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontSize: 20 }}>{node.icon}</span>
                <span style={{ fontWeight: 500 }}>{node.label}</span>
              </div>
            </Card>
          ))}
        </div>
      ),
    },
  ];

  return (
    <div
      style={{
        width: 260,
        background: '#fff',
        borderRight: '1px solid #e8e8e8',
        padding: 16,
        overflowY: 'auto',
        height: '100%',
      }}
    >
      <h3 style={{ marginBottom: 16, fontSize: 15, fontWeight: 600 }}>节点库</h3>

      <Collapse
        activeKey={activeKey}
        onChange={(keys) => setActiveKey(keys as string[])}
        ghost
        items={collapseItems}
      />

      <div
        style={{
          marginTop: 16,
          padding: 12,
          background: '#fafafa',
          borderRadius: 8,
          border: '1px dashed #d9d9d9',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: '#999' }}>
          <DragOutlined />
          <span style={{ fontSize: 12 }}>拖拽节点到画布中使用</span>
        </div>
      </div>

      <style>{`
        .node-card:hover {
          transform: translateX(4px);
          box-shadow: 0 2px 12px rgba(0,0,0,0.1);
        }
      `}</style>
    </div>
  );
};

export default Sidebar;
