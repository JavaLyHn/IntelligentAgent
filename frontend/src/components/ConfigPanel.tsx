import React, { useEffect, useState } from 'react';
import {
  Form, Input, Select, Button, Space, InputNumber, Divider,
  Tag, Card, Slider, message,
} from 'antd';
import {
  PlusOutlined, DeleteOutlined, SaveOutlined,
  EyeInvisibleOutlined, EyeOutlined,
} from '@ant-design/icons';
import { useWorkflowStore } from '../store/workflowStore';
import { LLMProvider, ToolType } from '../types/workflow';

const { Option } = Select;
const { TextArea } = Input;
const { Password } = Input;

interface DynamicParam {
  name: string;
  paramType: 'input' | 'reference';
  value: string;
}

interface OutputParamConfig {
  name: string;
  type: string;
  description: string;
}

const DEFAULT_SYSTEM_PROMPT = `# 角色
你是一位专业的广播节目编辑，负责制作一档名为"AI电台"的节目。你的任务是将用户提供的原始内容改编为适合单口相声播客节目的逐字稿。
# 任务
将原始内容分解为若干主题或问题，确保每段对话涵盖关键点，并自然过渡。
# 注意点
确保对话语言口语化、易懂。
对于专业术语或复杂概念，使用简单明了的语言进行解释，使听众更易理解。
保持对话节奏轻松、有趣，并加入适当的幽默和互动，以提高听众的参与感。
注意：我会直接将你生成的内容朗读出来，不要输出口播稿以外的东西，不要带格式，
# 示例
欢迎收听AI电台，今天咱们的节目一定让你们大开眼界！
没错！今天的主题绝对精彩，快搬小板凳听好哦！
那么，今天我们要讨论的内容是……
# 原始内容：{{input}}`;

const DEEPSEEK_MODELS = ['deepseek-chat', 'deepseek-coder', 'deepseek-reasoner'];
const TONGYI_MODELS = ['qwen-plus', 'qwen-turbo', 'qwen-max', 'qwen-long'];
const OPENAI_MODELS = ['gpt-4o', 'gpt-4o-mini', 'gpt-4-turbo', 'gpt-3.5-turbo'];

const PARAM_TYPE_OPTIONS = ['string', 'audio'];

const ConfigPanel: React.FC = () => {
  const { nodes, selectedNodeId, updateNodeData, edges } = useWorkflowStore();
  const [form] = Form.useForm();
  const selectedNode = nodes.find((n) => n.id === selectedNodeId);

  const [llmInputParams, setLlmInputParams] = useState<DynamicParam[]>([]);
  const [outputParams, setOutputParams] = useState<DynamicParam[]>([]);
  const [answerTemplate, setAnswerTemplate] = useState('');
  const [saving, setSaving] = useState(false);

  const [llmOutputParams, setLlmOutputParams] = useState<OutputParamConfig[]>([]);
  const [inputOutputParams, setInputOutputParams] = useState<OutputParamConfig[]>([]);
  const [outputNodeOutputParams, setOutputNodeOutputParams] = useState<OutputParamConfig[]>([]);
  const [ttsInputParams, setTtsInputParams] = useState<DynamicParam[]>([]);
  const [ttsOutputParams, setTtsOutputParams] = useState<OutputParamConfig[]>([]);
  const [outputInputSource, setOutputInputSource] = useState<string>('');

  useEffect(() => {
    if (selectedNode) {
      const config = (selectedNode.data.config || {}) as Record<string, any>;
      form.setFieldsValue({ label: selectedNode.data.label as string, type: selectedNode.data.type as string, ...config });
      if (selectedNode.type === 'llmNode') {
        setLlmInputParams(config.inputParams || [{ name: 'input', paramType: 'reference', value: '' }]);
        setLlmOutputParams(config.outputParamConfigs || [{ name: 'text', type: 'string', description: '大模型生成的文本内容' }]);
      }
      if (selectedNode.type === 'inputNode') {
        setInputOutputParams(config.outputParamConfigs || [{ name: 'text', type: 'string', description: '传递给下游节点的文本内容' }]);
      }
      if (selectedNode.type === 'outputNode') {
        setOutputParams(config.outputParams || []);
        setAnswerTemplate(config.answerTemplate || '');
        setOutputNodeOutputParams(config.outputParamConfigs || [{ name: 'text', type: 'string', description: '最终输出内容' }]);
        setOutputInputSource(config.inputSource || '');
      }
      if (selectedNode.type === 'toolNode') {
        setTtsInputParams(config.inputParams || [
          { name: 'text', paramType: 'reference', value: '' },
          { name: 'voice', paramType: 'input', value: 'Cherry' },
          { name: 'language_type', paramType: 'input', value: 'Auto' },
          { name: 'api_key', paramType: 'input', value: '' },
          { name: 'model', paramType: 'input', value: 'qwen3-tts-flash' },
        ]);
        setTtsOutputParams(config.outputParamConfigs || [
          { name: 'voice_url', type: 'audio', description: '阿里百炼TTS生成的音频URL' },
          { name: 'text', type: 'string', description: '原始文本内容' },
        ]);
      }
    }
  }, [selectedNode, form]);

  if (!selectedNode) {
    return (
      <div style={{ width: 320, background: '#fff', borderLeft: '1px solid #e8e8e8', padding: 20, display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#999' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 48, marginBottom: 12 }}>⚙️</div>
          <div style={{ fontSize: 15, fontWeight: 500 }}>节点配置</div>
          <div style={{ fontSize: 12, marginTop: 8 }}>点击画布中的节点进行配置</div>
        </div>
      </div>
    );
  }

  const isInputNode = selectedNode.type === 'inputNode';
  const isOutputNode = selectedNode.type === 'outputNode';
  const isLLMNode = selectedNode.type === 'llmNode';
  const isTTSNode = selectedNode.type === 'toolNode';

  const handleSave = () => {
    form.validateFields().then((values) => {
      setSaving(true);
      try {
        const updateData: Record<string, any> = { label: values.label, config: { ...values } };
        if (isLLMNode) {
          updateData.config.inputParams = llmInputParams;
          updateData.config.outputParamConfigs = llmOutputParams;
        }
        if (isInputNode) {
          updateData.config.outputParamConfigs = inputOutputParams;
        }
        if (isOutputNode) {
          updateData.config.outputParams = outputParams;
          updateData.config.answerTemplate = answerTemplate;
          updateData.config.outputParamConfigs = outputNodeOutputParams;
          updateData.config.inputSource = outputInputSource;
        }
        if (isTTSNode) {
          updateData.config.inputParams = ttsInputParams;
          updateData.config.outputParamConfigs = ttsOutputParams;
        }
        updateNodeData(selectedNodeId!, updateData);
        message.success('配置保存成功');
      } catch (e) {
        message.error('配置保存失败');
      } finally {
        setSaving(false);
      }
    }).catch(() => {
      message.warning('请检查配置项是否填写正确');
    });
  };

  const getUpstreamNodeOutputs = () => {
    if (!selectedNodeId) return [];
    const incomingEdges = edges.filter((e) => e.target === selectedNodeId);
    const result: { nodeId: string; label: string; field: string }[] = [];
    for (const edge of incomingEdges) {
      const sn = nodes.find((n) => n.id === edge.source);
      if (sn) {
        const snConfig = (sn.data.config || {}) as Record<string, any>;
        const snOutputParamConfigs: OutputParamConfig[] = snConfig.outputParamConfigs || [];

        if (snOutputParamConfigs.length > 0) {
          for (const op of snOutputParamConfigs) {
            if (op.name) result.push({ nodeId: sn.id, label: sn.data.label as string, field: op.name });
          }
        } else if (sn.type === 'toolNode') {
          result.push({ nodeId: sn.id, label: sn.data.label as string, field: 'voice_url' });
          result.push({ nodeId: sn.id, label: sn.data.label as string, field: 'text' });
        } else {
          result.push({ nodeId: sn.id, label: sn.data.label as string, field: 'text' });
        }
      }
    }
    return result;
  };

  const getUpstreamNodes = () => {
    if (!selectedNodeId) return [];
    const incomingEdges = edges.filter((e) => e.target === selectedNodeId);
    return incomingEdges.map((edge) => {
      const sn = nodes.find((n) => n.id === edge.source);
      return sn ? { nodeId: sn.id, label: sn.data.label as string, type: sn.type } : null;
    }).filter(Boolean) as { nodeId: string; label: string; type: string }[];
  };

  const upstreamOutputs = getUpstreamNodeOutputs();
  const upstreamNodes = getUpstreamNodes();

  const addLlmInputParam = () => setLlmInputParams([...llmInputParams, { name: '', paramType: 'reference', value: '' }]);
  const removeLlmInputParam = (i: number) => { const p = [...llmInputParams]; p.splice(i, 1); setLlmInputParams(p); };
  const updateLlmInputParam = (i: number, f: keyof DynamicParam, v: any) => {
    const p = [...llmInputParams]; p[i] = { ...p[i], [f]: v }; if (f === 'paramType') p[i].value = ''; setLlmInputParams(p);
  };

  const addOutputParam = () => setOutputParams([...outputParams, { name: '', paramType: 'input', value: '' }]);
  const removeOutputParam = (i: number) => { const p = [...outputParams]; p.splice(i, 1); setOutputParams(p); };
  const updateOutputParam = (i: number, f: keyof DynamicParam, v: any) => {
    const p = [...outputParams]; p[i] = { ...p[i], [f]: v }; if (f === 'paramType') p[i].value = ''; setOutputParams(p);
  };

  const addTtsInputParam = () => setTtsInputParams([...ttsInputParams, { name: '', paramType: 'input', value: '' }]);
  const removeTtsInputParam = (i: number) => { const p = [...ttsInputParams]; p.splice(i, 1); setTtsInputParams(p); };
  const updateTtsInputParam = (i: number, f: keyof DynamicParam, v: any) => {
    const p = [...ttsInputParams]; p[i] = { ...p[i], [f]: v }; if (f === 'paramType') p[i].value = ''; setTtsInputParams(p);
  };

  const createOutputParamCRUD = (
    params: OutputParamConfig[],
    setParams: React.Dispatch<React.SetStateAction<OutputParamConfig[]>>,
  ) => ({
    add: () => setParams([...params, { name: '', type: 'string', description: '' }]),
    remove: (i: number) => { const p = [...params]; p.splice(i, 1); setParams(p); },
    update: (i: number, f: keyof OutputParamConfig, v: any) => {
      const p = [...params]; p[i] = { ...p[i], [f]: v }; setParams(p);
    },
  });

  const llmOutputCRUD = createOutputParamCRUD(llmOutputParams, setLlmOutputParams);
  const inputOutputCRUD = createOutputParamCRUD(inputOutputParams, setInputOutputParams);
  const outputNodeOutputCRUD = createOutputParamCRUD(outputNodeOutputParams, setOutputNodeOutputParams);
  const ttsOutputCRUD = createOutputParamCRUD(ttsOutputParams, setTtsOutputParams);

  const inputVarStyle: React.CSSProperties = { background: '#f0f5ff', padding: '8px 12px', borderRadius: 6, border: '1px solid #d6e4ff', marginBottom: 8 };
  const outputVarStyle: React.CSSProperties = { background: '#f6ffed', padding: '8px 12px', borderRadius: 6, border: '1px solid #b7eb8f', marginBottom: 8 };

  const getModelOptions = () => {
    const provider = form.getFieldValue('provider') || (selectedNode.data.type as string);
    switch (provider) {
      case 'deepseek': return DEEPSEEK_MODELS;
      case 'tongyi': return TONGYI_MODELS;
      default: return OPENAI_MODELS;
    }
  };

  const renderDynamicParamCard = (
    param: DynamicParam,
    index: number,
    onUpdate: (i: number, f: keyof DynamicParam, v: any) => void,
    onRemove: (i: number) => void,
  ) => (
    <Card key={index} size="small" style={{ marginBottom: 6, borderLeft: `3px solid ${param.paramType === 'input' ? '#1890ff' : '#52c41a'}` }} styles={{ body: { padding: '6px 10px' } }}>
      <Space direction="vertical" style={{ width: '100%' }} size={4}>
        <Input placeholder="参数名" size="small" value={param.name} onChange={(e) => onUpdate(index, 'name', e.target.value)} addonBefore="名" />
        <Space style={{ width: '100%' }} size={4}>
          <Select size="small" value={param.paramType} onChange={(v) => onUpdate(index, 'paramType', v)} style={{ width: 80 }}>
            <Option value="input">输入</Option>
            <Option value="reference">引用</Option>
          </Select>
          {param.paramType === 'input' ? (
            <Input size="small" placeholder="输入值" value={param.value} onChange={(e) => onUpdate(index, 'value', e.target.value)} style={{ flex: 1 }} />
          ) : (
            <Select size="small" placeholder="选择引用源" value={param.value || undefined} onChange={(v) => onUpdate(index, 'value', v)} style={{ flex: 1 }} allowClear>
              {upstreamOutputs.map((uo) => (
                <Option key={`${uo.nodeId}.${uo.field}`} value={`${uo.nodeId}.${uo.field}`}>
                  {uo.label}.{uo.field}
                </Option>
              ))}
            </Select>
          )}
          <Button type="text" danger size="small" icon={<DeleteOutlined />} onClick={() => onRemove(index)} />
        </Space>
      </Space>
    </Card>
  );

  const renderOutputParamCard = (
    param: OutputParamConfig,
    index: number,
    onUpdate: (i: number, f: keyof OutputParamConfig, v: any) => void,
    onRemove: (i: number) => void,
  ) => (
    <Card key={index} size="small" style={{ marginBottom: 6, borderLeft: '3px solid #52c41a' }} styles={{ body: { padding: '6px 10px' } }}>
      <Space direction="vertical" style={{ width: '100%' }} size={4}>
        <Space style={{ width: '100%' }} size={4}>
          <Input placeholder="变量名" size="small" value={param.name} onChange={(e) => onUpdate(index, 'name', e.target.value)} style={{ flex: 1 }} />
          <Select size="small" value={param.type} onChange={(v) => onUpdate(index, 'type', v)} style={{ width: 90 }}>
            {PARAM_TYPE_OPTIONS.map((t) => (
              <Option key={t} value={t}>{t}</Option>
            ))}
          </Select>
          <Button type="text" danger size="small" icon={<DeleteOutlined />} onClick={() => onRemove(index)} />
        </Space>
        <Input placeholder="描述（可选）" size="small" value={param.description} onChange={(e) => onUpdate(index, 'description', e.target.value)} />
      </Space>
    </Card>
  );

  const renderOutputParamSection = (
    params: OutputParamConfig[],
    crud: { add: () => void; remove: (i: number) => void; update: (i: number, f: keyof OutputParamConfig, v: any) => void },
    emptyText: string = '点击"添加"按钮创建输出参数',
  ) => (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
        <span style={{ fontSize: 12, fontWeight: 500 }}>输出参数</span>
        <Button type="dashed" icon={<PlusOutlined />} size="small" onClick={crud.add}>添加</Button>
      </div>
      {params.length === 0 && (
        <div style={{ textAlign: 'center', padding: 16, background: '#fafafa', borderRadius: 6, border: '1px dashed #d9d9d9', color: '#999', fontSize: 11 }}>
          {emptyText}
        </div>
      )}
      {params.map((param, index) => renderOutputParamCard(param, index, crud.update, crud.remove))}
      {params.filter((p) => p.name).length > 0 && (
        <div style={{ background: '#fff7e6', padding: 8, borderRadius: 6, border: '1px solid #ffd591', fontSize: 11, color: '#d46b08', marginTop: 4 }}>
          🔗 下游节点可通过 <strong>{'{'}{selectedNode!.id}.{params[0]?.name || 'text'}{'}'}</strong> 引用此节点的输出
        </div>
      )}
    </>
  );

  return (
    <div style={{ width: 320, background: '#fff', borderLeft: '1px solid #e8e8e8', padding: 16, overflowY: 'auto', height: 'calc(100vh - 56px)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 16 }}>
        <h3 style={{ margin: 0, fontSize: 15, fontWeight: 600 }}>节点配置</h3>
        <Tag color="blue">{isInputNode ? '输入' : isOutputNode ? '输出' : isLLMNode ? '大模型' : isTTSNode ? '超拟人音频' : (selectedNode.data.type as string)}</Tag>
      </div>

      <Form form={form} layout="vertical" size="small">
        <Form.Item label="节点名称" name="label" rules={[{ required: true, message: '请输入节点名称' }]}>
          <Input placeholder="节点名称" />
        </Form.Item>

        {isInputNode && (
          <>
            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输入变量</Divider>
            <div style={inputVarStyle}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontWeight: 500, fontSize: 13 }}>user_input</span>
                <Space size={4}><Tag color="blue" style={{ margin: 0 }}>String</Tag><Tag color="red" style={{ margin: 0 }}>必要</Tag></Space>
              </div>
              <div style={{ color: '#666', fontSize: 11, marginTop: 4 }}>用户本轮的输入内容</div>
            </div>
            <div style={{ background: '#f6f0ff', padding: 10, borderRadius: 6, border: '1px solid #d3adf7', fontSize: 11, color: '#722ed1' }}>
              💡 输入节点定义工作流入口，调试时输入的文本作为 <code style={{ background: '#fff', padding: '1px 4px', borderRadius: 3 }}>user_input</code> 传递给下游
            </div>
            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输出配置</Divider>
            {renderOutputParamSection(inputOutputParams, inputOutputCRUD)}
          </>
        )}

        {isOutputNode && (
          <>
            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输入源选择</Divider>
            {upstreamNodes.length > 0 ? (
              <Select
                placeholder="选择输入源（默认使用所有上游节点）"
                value={outputInputSource || undefined}
                onChange={(v) => setOutputInputSource(v)}
                style={{ width: '100%' }}
                allowClear
              >
                {upstreamNodes.map((un) => (
                  <Option key={un.nodeId} value={un.nodeId}>
                    {un.label} ({un.type === 'toolNode' ? '超拟人音频' : un.type === 'llmNode' ? '大模型' : un.type === 'inputNode' ? '输入' : un.type})
                  </Option>
                ))}
              </Select>
            ) : (
              <div style={{ textAlign: 'center', padding: 12, background: '#fafafa', borderRadius: 6, border: '1px dashed #d9d9d9', color: '#999', fontSize: 11 }}>
                请先连接上游节点
              </div>
            )}
            <div style={{ color: '#999', fontSize: 11, marginTop: 4 }}>
              💡 选择指定上游节点后，将优先使用该节点的输出作为输入数据
            </div>

            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输入变量</Divider>
            {upstreamOutputs.length > 0 ? upstreamOutputs.map((uo, i) => (
              <div key={i} style={inputVarStyle}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <span style={{ fontWeight: 500, fontSize: 13 }}>{uo.label}.{uo.field}</span>
                  <Tag color={uo.field === 'voice_url' ? 'green' : 'blue'} style={{ margin: 0 }}>{uo.field === 'voice_url' ? 'Audio' : 'String'}</Tag>
                </div>
              </div>
            )) : (
              <div style={{ textAlign: 'center', padding: 12, background: '#fafafa', borderRadius: 6, border: '1px dashed #d9d9d9', color: '#999', fontSize: 11 }}>
                请先连接上游节点
              </div>
            )}

            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输出配置</Divider>
            {renderOutputParamSection(outputNodeOutputParams, outputNodeOutputCRUD)}

            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>数据采集配置</Divider>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <span style={{ fontSize: 12, fontWeight: 500 }}>采集参数</span>
              <Button type="dashed" icon={<PlusOutlined />} size="small" onClick={addOutputParam}>添加</Button>
            </div>
            {outputParams.length === 0 && (
              <div style={{ textAlign: 'center', padding: 16, background: '#fafafa', borderRadius: 6, border: '1px dashed #d9d9d9', color: '#999', fontSize: 11 }}>
                点击"添加"按钮创建采集参数
              </div>
            )}
            {outputParams.map((param, index) => renderDynamicParamCard(param, index, updateOutputParam, removeOutputParam))}
            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>回答内容配置</Divider>
            <TextArea rows={3} placeholder="使用 {{参数名}} 引用参数，如：{{result}}" value={answerTemplate} onChange={(e) => setAnswerTemplate(e.target.value)} style={{ fontFamily: 'monospace', fontSize: 12 }} />
            <div style={{ color: '#999', fontSize: 11, marginTop: 4 }}>💡 使用 {'{{参数名}}'} 引用上方定义的参数</div>
            {outputParams.filter((p) => p.name).length > 0 && (
              <div style={{ marginTop: 6 }}>
                <Space wrap size={4}>
                  {outputParams.filter((p) => p.name).map((p, i) => (
                    <Tag key={i} color="blue" style={{ cursor: 'pointer', fontSize: 11 }} onClick={() => setAnswerTemplate((answerTemplate || '') + `{{${p.name}}}`)}>
                      {'{{' + p.name + '}}'}
                    </Tag>
                  ))}
                </Space>
              </div>
            )}
          </>
        )}

        {isLLMNode && (
          <>
            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输入配置</Divider>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <span style={{ fontSize: 12, fontWeight: 500 }}>输入参数</span>
              <Button type="dashed" icon={<PlusOutlined />} size="small" onClick={addLlmInputParam}>添加</Button>
            </div>
            {llmInputParams.map((param, index) => renderDynamicParamCard(param, index, updateLlmInputParam, removeLlmInputParam))}
            <div style={{ color: '#999', fontSize: 11, marginBottom: 4 }}>
              💡 在提示词模板中使用 {'{{参数名}}'} 引用上方定义的参数
            </div>
            {llmInputParams.filter((p) => p.name).length > 0 && (
              <div style={{ marginBottom: 8 }}>
                <Space wrap size={4}>
                  {llmInputParams.filter((p) => p.name).map((p, i) => (
                    <Tag key={i} color="purple" style={{ cursor: 'pointer', fontSize: 11 }} onClick={() => {
                      const current = form.getFieldValue('userPrompt') || '';
                      form.setFieldsValue({ userPrompt: current + `{{${p.name}}}` });
                    }}>
                      {'{{' + p.name + '}}'}
                    </Tag>
                  ))}
                </Space>
              </div>
            )}

            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输出配置</Divider>
            {renderOutputParamSection(llmOutputParams, llmOutputCRUD)}

            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>模型接口配置</Divider>
            <Form.Item label="提供商" name="provider" rules={[{ required: true, message: '请选择提供商' }]}>
              <Select placeholder="选择提供商">
                <Option value={LLMProvider.DEEPSEEK}>DeepSeek</Option>
                <Option value={LLMProvider.TONGYI}>通义千问</Option>
                <Option value="openai">OpenAI</Option>
                <Option value={LLMProvider.AI_PING}>AI Ping</Option>
                <Option value={LLMProvider.ZHIPU}>智谱</Option>
              </Select>
            </Form.Item>
            <Form.Item
              label="模型接口地址"
              name="baseUrl"
              rules={[
                { required: true, message: '请输入接口地址' },
                { pattern: /^https?:\/\/.+/, message: '请输入有效的HTTP/HTTPS地址' },
              ]}
            >
              <Input placeholder="https://api.deepseek.com/v1" />
            </Form.Item>
            <Form.Item label="API密钥" name="apiKey">
              <Password placeholder="sk-xxxxxxxxxxxxxxxx" iconRender={(visible) => visible ? <EyeOutlined /> : <EyeInvisibleOutlined />} />
            </Form.Item>
            <Form.Item label="模型名称" name="model" rules={[{ required: true, message: '请选择或输入模型名称' }]}>
              <Select placeholder="选择或输入模型名称" showSearch allowClear>
                {getModelOptions().map((m) => (
                  <Option key={m} value={m}>{m}</Option>
                ))}
              </Select>
            </Form.Item>

            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>参数配置</Divider>
            <Form.Item label="Temperature" name="temperature">
              <Slider min={0} max={1} step={0.1} defaultValue={0.7} marks={{ 0: '0', 0.5: '0.5', 1: '1' }} />
            </Form.Item>
            <Form.Item label="最大Token" name="maxTokens">
              <InputNumber min={1} max={8000} defaultValue={2000} style={{ width: '100%' }} />
            </Form.Item>

            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>提示词配置</Divider>
            <Form.Item label="系统提示词" name="systemPrompt">
              <TextArea rows={4} placeholder="系统提示词..." style={{ fontSize: 12 }} />
            </Form.Item>
            <Form.Item label="用户提示词模板" name="userPrompt">
              <TextArea rows={6} placeholder={'{{input}}'} style={{ fontFamily: 'monospace', fontSize: 12 }} />
            </Form.Item>
            <Button
              type="link"
              size="small"
              style={{ padding: 0, marginBottom: 8, fontSize: 11 }}
              onClick={() => {
                form.setFieldsValue({ systemPrompt: DEFAULT_SYSTEM_PROMPT, userPrompt: '{{input}}' });
                message.info('已填充默认提示词模板');
              }}
            >
              📋 填充默认播客提示词模板
            </Button>
          </>
        )}

        {isTTSNode && (
          <>
            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输入配置</Divider>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
              <span style={{ fontSize: 12, fontWeight: 500 }}>输入参数</span>
              <Button type="dashed" icon={<PlusOutlined />} size="small" onClick={addTtsInputParam}>添加</Button>
            </div>
            {ttsInputParams.map((param, index) => (
              <Card key={index} size="small" style={{ marginBottom: 6, borderLeft: `3px solid ${param.paramType === 'input' ? '#1890ff' : '#52c41a'}` }} styles={{ body: { padding: '6px 10px' } }}>
                <Space direction="vertical" style={{ width: '100%' }} size={4}>
                  <Space style={{ width: '100%' }} size={4}>
                    <Input placeholder="参数名" size="small" value={param.name} onChange={(e) => updateTtsInputParam(index, 'name', e.target.value)} style={{ width: 90 }} />
                    <Select size="small" value={param.paramType} onChange={(v) => updateTtsInputParam(index, 'paramType', v)} style={{ width: 80 }}>
                      <Option value="input">输入</Option>
                      <Option value="reference">引用</Option>
                    </Select>
                    <Button type="text" danger size="small" icon={<DeleteOutlined />} onClick={() => removeTtsInputParam(index)} />
                  </Space>
                  {param.name === 'voice' ? (
                    <Select size="small" value={param.value || 'Cherry'} onChange={(v) => updateTtsInputParam(index, 'value', v)} style={{ width: '100%' }}>
                      <Option value="Cherry">Cherry（芊悦）</Option>
                      <Option value="Serena">Serena（苏瑶）</Option>
                      <Option value="Ethan">Ethan（晨煦）</Option>
                    </Select>
                  ) : param.name === 'language_type' ? (
                    <Select size="small" value={param.value || 'Auto'} onChange={(v) => updateTtsInputParam(index, 'value', v)} style={{ width: '100%' }}>
                      <Option value="Auto">Auto</Option>
                      <Option value="Chinese">Chinese</Option>
                      <Option value="English">English</Option>
                    </Select>
                  ) : param.name === 'api_key' ? (
                    <Password size="small" placeholder="输入API密钥" value={param.value} onChange={(e) => updateTtsInputParam(index, 'value', e.target.value)} iconRender={(visible) => visible ? <EyeOutlined /> : <EyeInvisibleOutlined />} />
                  ) : param.name === 'model' ? (
                    <Input size="small" placeholder="模型名称" value={param.value || 'qwen3-tts-flash'} onChange={(e) => updateTtsInputParam(index, 'value', e.target.value)} />
                  ) : param.paramType === 'input' ? (
                    <Input size="small" placeholder="输入值" value={param.value} onChange={(e) => updateTtsInputParam(index, 'value', e.target.value)} />
                  ) : (
                    <Select size="small" placeholder="选择引用源" value={param.value || undefined} onChange={(v) => updateTtsInputParam(index, 'value', v)} style={{ width: '100%' }} allowClear>
                      {upstreamOutputs.map((uo) => (
                        <Option key={`${uo.nodeId}.${uo.field}`} value={`${uo.nodeId}.${uo.field}`}>
                          {uo.label}.{uo.field}
                        </Option>
                      ))}
                    </Select>
                  )}
                </Space>
              </Card>
            ))}

            <Divider orientation="left" style={{ fontSize: 12, margin: '12px 0 8px' }}>输出配置</Divider>
            {renderOutputParamSection(ttsOutputParams, ttsOutputCRUD)}
          </>
        )}

        <Divider />
        <Button type="primary" block icon={<SaveOutlined />} loading={saving} onClick={handleSave}>
          保存配置
        </Button>
      </Form>
    </div>
  );
};

export default ConfigPanel;
