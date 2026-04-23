import React, { useState } from 'react';
import { Button, Space, Input, Modal, List, message } from 'antd';
import {
  PlusOutlined,
  FolderOpenOutlined,
  SaveOutlined,
  BugOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons';
import { useWorkflowStore } from '../store/workflowStore';

const Header: React.FC = () => {
  const {
    setIsDebugOpen,
    isDebugOpen,
    currentWorkflowName,
    setCurrentWorkflowName,
    newWorkflow,
    saveWorkflow,
    loadWorkflow,
  } = useWorkflowStore();

  const [loadModalOpen, setLoadModalOpen] = useState(false);
  const [workflows, setWorkflows] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const handleNew = () => {
    newWorkflow();
    message.success('已创建新工作流');
  };

  const handleSave = async () => {
    await saveWorkflow();
    message.success('工作流已保存');
  };

  const handleLoad = async () => {
    setLoading(true);
    try {
      const response = await fetch('/api/workflows');
      const result = await response.json();
      if (result.success && result.data) {
        setWorkflows(result.data);
        setLoadModalOpen(true);
      }
    } catch (error) {
      message.error('加载工作流列表失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSelectWorkflow = async (id: string) => {
    await loadWorkflow(id);
    setLoadModalOpen(false);
    message.success('工作流已加载');
  };

  return (
    <>
      <div
        style={{
          height: 56,
          background: '#fff',
          borderBottom: '1px solid #e8e8e8',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 24px',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div
            style={{
              fontSize: 18,
              fontWeight: 600,
              color: '#1890ff',
              display: 'flex',
              alignItems: 'center',
              gap: 8,
            }}
          >
            🤖 Intelligent Agent
          </div>
          <Input
            value={currentWorkflowName}
            onChange={(e) => setCurrentWorkflowName(e.target.value)}
            variant="borderless"
            style={{ width: 180, fontWeight: 500 }}
          />
        </div>

        <Space>
          <Button icon={<PlusOutlined />} type="default" onClick={handleNew}>
            新建
          </Button>
          <Button icon={<FolderOpenOutlined />} onClick={handleLoad} loading={loading}>
            加载
          </Button>
          <Button type="primary" icon={<SaveOutlined />} onClick={handleSave}>
            保存
          </Button>
          <Button
            type="primary"
            icon={<BugOutlined />}
            onClick={() => setIsDebugOpen(!isDebugOpen)}
            style={{
              backgroundColor: isDebugOpen ? '#52c41a' : '#1890ff',
            }}
          >
            调试
          </Button>
        </Space>

        <Space>
          <Space>
            <UserOutlined />
            admin
          </Space>
          <Button type="text" icon={<LogoutOutlined />}>
            登出
          </Button>
        </Space>
      </div>

      <Modal
        title="加载工作流"
        open={loadModalOpen}
        onCancel={() => setLoadModalOpen(false)}
        footer={null}
        width={600}
      >
        <List
          dataSource={workflows}
          renderItem={(item: any) => (
            <List.Item
              actions={[
                <Button type="link" onClick={() => handleSelectWorkflow(item.id)}>
                  加载
                </Button>,
              ]}
            >
              <List.Item.Meta
                title={item.name}
                description={`版本: ${item.version} | 更新: ${item.updatedAt || '-'}`}
              />
            </List.Item>
          )}
          locale={{ emptyText: '暂无已保存的工作流' }}
        />
      </Modal>
    </>
  );
};

export default Header;
