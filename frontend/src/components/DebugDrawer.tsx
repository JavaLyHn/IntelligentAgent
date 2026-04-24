import React, { useState, useEffect, useRef } from 'react';
import { Drawer, Input, Button, Tag, Alert, Divider, message } from 'antd';
import {
  PlayCircleOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined,
  SoundOutlined,
  LoadingOutlined,
  MinusCircleOutlined,
} from '@ant-design/icons';
import { useWorkflowStore, NodeExecutionState } from '../store/workflowStore';

const { TextArea } = Input;

const NODE_TYPE_LABELS: Record<string, string> = {
  inputNode: '输入节点',
  llmNode: 'LLM节点',
  toolNode: 'TTS节点',
  outputNode: '输出节点',
};

const STATUS_CONFIG: Record<string, { icon: React.ReactNode; color: string; bg: string; text: string }> = {
  pending: { icon: <MinusCircleOutlined />, color: '#d9d9d9', bg: '#fafafa', text: '等待中' },
  running: { icon: <LoadingOutlined spin />, color: '#1677ff', bg: '#e6f4ff', text: '执行中' },
  success: { icon: <CheckCircleOutlined />, color: '#52c41a', bg: '#f6ffed', text: '已完成' },
  failed: { icon: <CloseCircleOutlined />, color: '#ff4d4f', bg: '#fff2f0', text: '失败' },
};

const TTS_STATUS_MAP: Record<string, { label: string; color: string; icon: React.ReactNode }> = {
  success: { label: 'TTS调用成功', color: 'success', icon: <CheckCircleOutlined /> },
  demo: { label: 'TTS演示模式', color: 'warning', icon: <WarningOutlined /> },
  error: { label: 'TTS调用失败', color: 'error', icon: <CloseCircleOutlined /> },
  no_api_key: { label: '未配置API密钥', color: 'warning', icon: <WarningOutlined /> },
  no_text: { label: '无输入文本', color: 'default', icon: <WarningOutlined /> },
  skipped: { label: 'TTS未执行', color: 'default', icon: <WarningOutlined /> },
};

const DebugDrawer: React.FC = () => {
  const {
    isDebugOpen,
    setIsDebugOpen,
    executeWorkflow,
    executionResult,
    isExecuting,
    nodeExecutionStates,
    executionLogs,
    currentExecutingNodeId,
  } = useWorkflowStore();

  const [inputText, setInputText] = useState('');
  const logsEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (logsEndRef.current) {
      logsEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [executionLogs]);

  const handleExecute = async () => {
    if (!inputText.trim()) {
      message.warning('请输入测试文本');
      return;
    }
    await executeWorkflow(inputText);
  };

  const result = executionResult as any;
  const ttsStatus = result?.ttsStatus || '';
  const ttsError = result?.ttsError || '';
  const audioUrl = result?.audioUrl || result?.voice_url || '';

  const nodeStatesArray = Object.values(nodeExecutionStates);

  const renderNodeProgress = () => {
    if (nodeStatesArray.length === 0) return null;

    return (
      <div style={{ marginBottom: 16 }}>
        <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>
          节点执行进度
        </Divider>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
          {nodeStatesArray.map((nodeState) => {
            const config = STATUS_CONFIG[nodeState.status] || STATUS_CONFIG.pending;
            const isCurrent = nodeState.nodeId === currentExecutingNodeId;
            const typeLabel = NODE_TYPE_LABELS[nodeState.nodeType] || nodeState.nodeType;

            return (
              <div
                key={nodeState.nodeId}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 8,
                  padding: '8px 12px',
                  borderRadius: 8,
                  background: isCurrent ? config.bg : '#fafafa',
                  border: isCurrent ? `1px solid ${config.color}30` : '1px solid #f0f0f0',
                  transition: 'all 0.3s ease',
                }}
              >
                <span style={{ color: config.color, fontSize: 16 }}>{config.icon}</span>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                    <span style={{ fontSize: 13, fontWeight: 500 }}>{nodeState.label}</span>
                    <Tag
                      color={nodeState.status === 'running' ? 'processing' : nodeState.status === 'success' ? 'success' : nodeState.status === 'failed' ? 'error' : 'default'}
                      style={{ fontSize: 11, lineHeight: '18px', padding: '0 4px', margin: 0 }}
                    >
                      {config.text}
                    </Tag>
                    <span style={{ fontSize: 10, color: '#999' }}>{typeLabel}</span>
                  </div>
                  {nodeState.durationMs != null && nodeState.status !== 'running' && nodeState.status !== 'pending' && (
                    <div style={{ fontSize: 11, color: '#999', marginTop: 2 }}>
                      耗时: {nodeState.durationMs}ms
                    </div>
                  )}
                  {nodeState.errorMessage && (
                    <div style={{ fontSize: 11, color: '#ff4d4f', marginTop: 2, wordBreak: 'break-all' }}>
                      {nodeState.errorMessage}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    );
  };

  const renderTtsStatus = () => {
    if (!ttsStatus || ttsStatus === 'skipped') return null;

    if (ttsStatus === 'success') {
      return (
        <Alert
          type="success"
          message="超拟人音频合成调用成功"
          description="百炼TTS服务已成功生成音频，可在下方播放器中收听"
          showIcon
          icon={<CheckCircleOutlined />}
          style={{ marginBottom: 12 }}
        />
      );
    }

    if (ttsStatus === 'error') {
      return (
        <Alert
          type="error"
          message="超拟人音频合成调用失败"
          description={ttsError || '未知错误'}
          showIcon
          icon={<CloseCircleOutlined />}
          style={{ marginBottom: 12 }}
        />
      );
    }

    if (ttsStatus === 'no_api_key') {
      return (
        <Alert
          type="warning"
          message="未配置API密钥"
          description="请在超拟人音频节点中配置百炼API密钥，或检查application.yml中的tongyi.api-key配置"
          showIcon
          icon={<WarningOutlined />}
          style={{ marginBottom: 12 }}
        />
      );
    }

    if (ttsStatus === 'demo') {
      return (
        <Alert
          type="warning"
          message="TTS演示模式"
          description="当前为演示模式，未实际调用百炼TTS服务。请配置API密钥以获取真实音频"
          showIcon
          icon={<WarningOutlined />}
          style={{ marginBottom: 12 }}
        />
      );
    }

    if (ttsStatus === 'no_text') {
      return (
        <Alert
          type="info"
          message="无输入文本"
          description="超拟人音频节点未接收到文本输入，跳过TTS调用"
          showIcon
          style={{ marginBottom: 12 }}
        />
      );
    }

    return null;
  };

  const renderAudioPlayer = () => {
    if (!audioUrl) return null;

    return (
      <div style={{ marginBottom: 16 }}>
        <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>
          <SoundOutlined /> 音频输出
        </Divider>
        <div style={{
          background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          borderRadius: 12,
          padding: 16,
          color: '#fff',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
            <div style={{
              width: 48,
              height: 48,
              borderRadius: '50%',
              background: 'rgba(255,255,255,0.2)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 24,
            }}>
              🎙️
            </div>
            <div>
              <div style={{ fontSize: 14, fontWeight: 600 }}>AI 播客音频</div>
              <div style={{ fontSize: 11, opacity: 0.8 }}>点击播放按钮收听</div>
            </div>
          </div>
          <audio
            controls
            src={audioUrl}
            style={{ width: '100%', borderRadius: 8 }}
            onError={(e) => {
              console.error('Audio playback error:', e);
            }}
          >
            您的浏览器不支持音频播放
          </audio>
        </div>
      </div>
    );
  };

  const renderExecutionLogs = () => {
    if (executionLogs.length === 0) return null;

    return (
      <div style={{ marginBottom: 16 }}>
        <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>
          执行日志
        </Divider>
        <div style={{
          background: '#1a1a2e',
          borderRadius: 8,
          padding: 12,
          maxHeight: 200,
          overflowY: 'auto',
          fontFamily: 'monospace',
        }}>
          {executionLogs.map((log, idx) => (
            <div key={idx} style={{
              fontSize: 11,
              color: log.includes('❌') ? '#ff6b6b' : log.includes('✅') ? '#51cf66' : log.includes('⚙️') ? '#74c0fc' : '#adb5bd',
              lineHeight: 1.8,
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
            }}>
              {log}
            </div>
          ))}
          <div ref={logsEndRef} />
        </div>
      </div>
    );
  };

  return (
    <Drawer
      title="🔧 工作流调试"
      placement="right"
      onClose={() => setIsDebugOpen(false)}
      open={isDebugOpen}
      width={420}
      styles={{ body: { padding: 16 } }}
    >
      <div style={{ marginBottom: 16 }}>
        <div style={{ fontSize: 13, fontWeight: 500, marginBottom: 8 }}>输入测试文本</div>
        <TextArea
          rows={4}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="输入测试文本，如：今天天气怎么样？"
          onPressEnter={(e) => {
            if (!e.shiftKey) {
              e.preventDefault();
              handleExecute();
            }
          }}
        />
        <Button
          type="primary"
          icon={<PlayCircleOutlined />}
          loading={isExecuting}
          onClick={handleExecute}
          block
          style={{ marginTop: 8 }}
        >
          {isExecuting ? '执行中...' : '执行工作流'}
        </Button>
      </div>

      {renderNodeProgress()}

      {renderExecutionLogs()}

      {result && (
        <>
          <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>
            执行结果
          </Divider>

          {result.success ? (
            <Tag color="success" style={{ marginBottom: 8 }}>✅ 执行成功</Tag>
          ) : (
            <Alert
              type="error"
              message="执行失败"
              description={result.errorMessage || '未知错误'}
              showIcon
              style={{ marginBottom: 8 }}
            />
          )}

          {renderTtsStatus()}

          {result.outputText && (
            <div style={{ marginBottom: 12 }}>
              <div style={{ fontSize: 12, fontWeight: 500, marginBottom: 4 }}>输出文本</div>
              <div style={{
                background: '#f5f5f5',
                padding: 12,
                borderRadius: 8,
                fontSize: 13,
                lineHeight: 1.6,
                whiteSpace: 'pre-wrap',
                maxHeight: 200,
                overflowY: 'auto',
              }}>
                {result.outputText}
              </div>
            </div>
          )}

          {renderAudioPlayer()}

          {result.durationMs != null && (
            <div style={{ fontSize: 11, color: '#999', marginBottom: 8 }}>
              总耗时: {result.durationMs}ms
            </div>
          )}
        </>
      )}
    </Drawer>
  );
};

export default DebugDrawer;
