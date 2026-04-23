import React, { useState } from 'react';
import { Drawer, Button, Input, Space, message, Typography, Tag } from 'antd';
import {
  PlayCircleOutlined,
  ClearOutlined,
  AudioOutlined,
  CheckCircleOutlined,
  BugOutlined,
} from '@ant-design/icons';
import { useWorkflowStore } from '../store/workflowStore';

const { TextArea } = Input;
const { Text, Paragraph } = Typography;

const DebugDrawer: React.FC = () => {
  const {
    isDebugOpen,
    setIsDebugOpen,
    debugInput,
    setDebugInput,
    debugOutput,
    setDebugOutput,
    isExecuting,
    setIsExecuting,
    nodes,
    edges,
    currentWorkflowId,
  } = useWorkflowStore();

  const [audioUrl, setAudioUrl] = useState<string | null>(null);
  const [executionLog, setExecutionLog] = useState<string[]>([]);

  const addLog = (log: string) => {
    setExecutionLog((prev) => [...prev, `[${new Date().toLocaleTimeString()}] ${log}`]);
  };

  const handleExecute = async () => {
    if (!debugInput.trim()) {
      message.warning('请输入测试文本');
      return;
    }

    setIsExecuting(true);
    setExecutionLog([]);
    setAudioUrl(null);

    try {
      addLog('🚀 开始执行工作流...');
      addLog(`📝 输入: ${debugInput.substring(0, 50)}${debugInput.length > 50 ? '...' : ''}`);

      const response = await fetch('/api/workflow/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          workflowId: currentWorkflowId,
          input: debugInput,
          nodes,
          edges,
        }),
      });

      const result = await response.json();

      if (result.success && result.data) {
        const data = result.data;
        addLog('✅ 工作流执行成功');

        if (data.logs) {
          data.logs.forEach((log: string) => addLog(log));
        }

        if (data.outputText) {
          setDebugOutput(data.outputText);
          addLog(`📤 输出: ${data.outputText.substring(0, 50)}...`);
        }

        if (data.audioUrl) {
          setAudioUrl(data.audioUrl);
          addLog('🎵 音频生成完成');
        }

        if (data.durationMs) {
          addLog(`⏱️ 执行耗时: ${data.durationMs}ms`);
        }

        message.success('执行成功！');
      } else {
        addLog(`❌ 错误: ${result.message || result.data?.errorMessage || '执行失败'}`);
        message.error(result.message || '执行失败');
      }
    } catch (error: any) {
      addLog(`❌ 网络错误: ${error.message}`);
      message.error('网络请求失败，请确认后端服务已启动');
    } finally {
      setIsExecuting(false);
    }
  };

  const handleClear = () => {
    setDebugInput('');
    setDebugOutput('');
    setAudioUrl(null);
    setExecutionLog([]);
  };

  return (
    <Drawer
      title={
        <Space>
          <BugOutlined />
          <span>工作流调试</span>
          {isExecuting && <Tag color="processing">执行中...</Tag>}
        </Space>
      }
      placement="right"
      onClose={() => setIsDebugOpen(false)}
      open={isDebugOpen}
      width={450}
      styles={{
        body: { padding: 16, display: 'flex', flexDirection: 'column', gap: 16 },
      }}
    >
      <div>
        <Text strong style={{ marginBottom: 8, display: 'block' }}>
          📝 测试输入
        </Text>
        <TextArea
          value={debugInput}
          onChange={(e) => setDebugInput(e.target.value)}
          placeholder="请输入要处理的文本，例如：介绍一下人工智能的发展历史..."
          rows={4}
          disabled={isExecuting}
        />
      </div>

      <Space style={{ width: '100%', justifyContent: 'flex-end' }}>
        <Button icon={<ClearOutlined />} onClick={handleClear} disabled={isExecuting}>
          清空
        </Button>
        <Button
          type="primary"
          icon={<PlayCircleOutlined />}
          onClick={handleExecute}
          loading={isExecuting}
          size="large"
        >
          执行测试
        </Button>
      </Space>

      {executionLog.length > 0 && (
        <div>
          <Text strong style={{ marginBottom: 8, display: 'block' }}>
            📋 执行日志
          </Text>
          <div
            style={{
              background: '#1e1e1e',
              color: '#d4d4d4',
              padding: 12,
              borderRadius: 8,
              maxHeight: 180,
              overflowY: 'auto',
              fontFamily: 'Consolas, Monaco, monospace',
              fontSize: 12,
              lineHeight: 1.6,
            }}
          >
            {executionLog.map((log, index) => (
              <div key={index} style={{ marginBottom: 2 }}>{log}</div>
            ))}
          </div>
        </div>
      )}

      {debugOutput && (
        <div>
          <Text strong style={{ marginBottom: 8, display: 'block' }}>
            📤 模型输出
          </Text>
          <Paragraph
            style={{
              background: '#f0fff4',
              padding: 12,
              borderRadius: 8,
              borderLeft: '3px solid #52c41a',
              maxHeight: 200,
              overflowY: 'auto',
              whiteSpace: 'pre-wrap',
            }}
          >
            {debugOutput}
          </Paragraph>
        </div>
      )}

      {audioUrl && (
        <div>
          <Text strong style={{ marginBottom: 8, display: 'block' }}>
            <AudioOutlined /> 音频输出
          </Text>
          <div
            style={{
              background: '#e6f7ff',
              padding: 16,
              borderRadius: 8,
              textAlign: 'center',
              border: '2px dashed #1890ff',
            }}
          >
            <audio controls src={audioUrl} style={{ width: '100%' }} autoPlay>
              您的浏览器不支持音频播放
            </audio>
            <div style={{ marginTop: 8, color: '#1890ff' }}>
              <CheckCircleOutlined /> AI播客已就绪，点击播放按钮收听
            </div>
          </div>
        </div>
      )}
    </Drawer>
  );
};

export default DebugDrawer;
