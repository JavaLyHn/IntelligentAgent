import React, { useEffect } from 'react';
import { Form, Input, Select, Button, Card, Space, InputNumber, Divider } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { useWorkflowStore } from '../store/workflowStore';
import { LLMProvider, ToolType } from '../types/workflow';

const { Option } = Select;
const { TextArea } = Input;

const ConfigPanel: React.FC = () => {
  const { nodes, selectedNodeId, updateNodeData } = useWorkflowStore();
  const [form] = Form.useForm();
  const selectedNode = nodes.find((n) => n.id === selectedNodeId);

  useEffect(() => {
    if (selectedNode) {
      form.setFieldsValue({
        label: selectedNode.data.label,
        type: selectedNode.data.type,
        ...selectedNode.data.config,
      });
    }
  }, [selectedNode, form]);

  if (!selectedNode) {
    return (
      <div
        style={{
          width: 300,
          background: '#fff',
          borderLeft: '1px solid #e8e8e8',
          padding: 20,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: '#999',
        }}
      >
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>⚙️</div>
          <div>节点配置</div>
          <div style={{ fontSize: 12, marginTop: 8 }}>选择一个节点进行配置</div>
        </div>
      </div>
    );
  }

  const handleSave = () => {
    const values = form.getFieldsValue();
    updateNodeData(selectedNodeId!, {
      label: values.label,
      config: values,
    });
  };

  const isLLMNode = [LLMProvider.DEEPSEEK, LLMProvider.TONGYI, LLMProvider.AI_PING, LLMProvider.ZHIPU].includes(
    selectedNode.data.type as LLMProvider
  );

  const isTTSNode = selectedNode.data.type === ToolType.TTS_AUDIO;

  return (
    <div
      style={{
        width: 320,
        background: '#fff',
        borderLeft: '1px solid #e8e8e8',
        padding: 20,
        overflowY: 'auto',
        height: 'calc(100vh - 56px)',
      }}
    >
      <h3 style={{ marginBottom: 20, fontSize: 16, fontWeight: 600 }}>节点配置</h3>

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSave}
      >
        <Form.Item label="节点 ID">
          <Input value={selectedNode.id} disabled />
        </Form.Item>

        <Form.Item label="节点类型" name="type">
          <Input disabled />
        </Form.Item>

        <Divider>输出配置</Divider>

        <Form.Item label="输出变量">
          <Space style={{ width: '100%' }}>
            <Form.Item name={['output', 'name']} noStyle>
              <Input placeholder="output" style={{ width: 120 }} />
            </Form.Item>
            <Form.Item name={['output', 'type']} noStyle>
              <Select defaultValue="引用" style={{ width: 80 }}>
                    <Option value="引用">引用</Option>
                    <Option value="字符串">字符串</Option>
                  </Select>
                </Form.Item>
              </Space>
            </Form.Item>

            <Form.Item name={['output', 'source']} noStyle>
              <Select
                placeholder="选择数据源"
                style={{ width: '100%' }}
                allowClear
              >
                {isTTSNode && (
                  <Option value="tts_audioUrl">超拟人音频合成.audioUrl</Option>
                )}
                {isLLMNode && (
                  <Option value="llm_output">大模型输出.text</Option>
                )}
              </Select>
            </Form.Item>

          {isLLMNode && (
            <>
              <Divider>模型参数</Divider>
              
              <Form.Item label="提供商" name="provider">
                <Select defaultValue={selectedNode.data.type}>
                  <Option value={LLMProvider.DEEPSEEK}>DeepSeek</Option>
                  <Option value={LLMProvider.TONGYI}>通义千问</Option>
                  <Option value={LLMProvider.AI_PING}>AI Ping</Option>
                  <Option value={LLMProvider.ZHIPU}>智谱</Option>
                </Select>
              </Form.Item>

              <Form.Item label="模型" name="model">
                <Input placeholder="deepseek-chat" />
              </Form.Item>

              <Form.Item label="Temperature" name="temperature">
                <InputNumber min={0} max={2} step={0.1} defaultValue={0.7} style={{ width: '100%' }} />
              </Form.Item>

              <Form.Item label="最大Token" name="maxTokens">
                <InputNumber min={1} max={8000} defaultValue={2000} style={{ width: '100%' }} />
              </Form.Item>

              <Form.Item label="系统提示词" name="systemPrompt">
                <TextArea rows={3} placeholder="你是一个有帮助的AI助手..." />
              </Form.Item>

              <Form.Item label="用户提示词模板" name="userPrompt">
                <TextArea rows={3} placeholder="{{input}}" />
              </Form.Item>
            </>
          )}

          {isTTSNode && (
            <>
              <Divider>音频参数</Divider>
              
              <Form.Item label="音色" name="voiceType">
                <Select defaultValue="female-1">
                  <Option value="female-1">女声-温柔</Option>
                  <Option value="female-2">女声-活泼</Option>
                  <Option value="male-1">男声-磁性</Option>
                  <Option value="male-2">男声-稳重</Option>
                </Select>
              </Form.Item>

              <Form.Item label="语速" name="speed">
                <InputNumber min={0.5} max={2} step={0.1} defaultValue={1} style={{ width: '100%' }} />
              </Form.Item>

              <Form.Item label="音调" name="pitch">
                <InputNumber min={-12} max={12} step={1} defaultValue={0} style={{ width: '100%' }} />
              </Form.Item>

              <Form.Item label="音量" name="volume">
                <InputNumber min={0} max={1} step={0.1} defaultValue={0.8} style={{ width: '100%' }} />
              </Form.Item>
            </>
          )}

          <Divider />

          <Form.Item label="回答内容配置">
            <TextArea
              rows={4}
              placeholder='{{output}}'
              value={JSON.stringify(selectedNode.data.config || {}, null, 2)}
            />
            <div style={{ color: '#999', fontSize: 12, marginTop: 6 }}>
              💡 提示: 使用 {{参数名}} 引用上游定义的参数
            </div>
          </Form.Item>

          <Button type="primary" htmlType="submit" block size="large">
            保存配置
          </Button>
        </Form>
      </Form>
    </div>
  );
};

export default ConfigPanel;
