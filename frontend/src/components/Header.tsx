import React, { useState } from 'react';
import { Button, Space, Input, Modal, List, message, Popconfirm } from 'antd';
import {
  PlusOutlined,
  FolderOpenOutlined,
  SaveOutlined,
  BugOutlined,
  UserOutlined,
  LogoutOutlined,
  DeleteOutlined,
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
  const [loggedIn, setLoggedIn] = useState(() => localStorage.getItem('ia_logged_in') === 'true');
  const [username, setUsername] = useState('');
  const [loginModalOpen, setLoginModalOpen] = useState(false);

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

  const handleDeleteWorkflow = async (id: string, name: string) => {
    try {
      const response = await fetch(`/api/workflows/${id}`, { method: 'DELETE' });
      const result = await response.json();
      if (result.success) {
        message.success(`工作流"${name}"已删除`);
        setWorkflows((prev) => prev.filter((w) => w.id !== id));
      } else {
        message.error(result.message || '删除失败');
      }
    } catch (error) {
      message.error('删除工作流失败');
    }
  };

  const handleLogin = () => {
    if (!username.trim()) {
      message.warning('请输入用户名');
      return;
    }
    localStorage.setItem('ia_logged_in', 'true');
    localStorage.setItem('ia_username', username.trim());
    setLoggedIn(true);
    setLoginModalOpen(false);
    message.success('登录成功');
  };

  const handleLogout = () => {
    localStorage.removeItem('ia_logged_in');
    localStorage.removeItem('ia_username');
    setLoggedIn(false);
    setUsername('');
    message.success('已登出');
  };

  const displayUsername = loggedIn ? (localStorage.getItem('ia_username') || 'admin') : '';

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
          {loggedIn ? (
            <>
              <Space>
                <UserOutlined />
                {displayUsername}
              </Space>
              <Button type="text" icon={<LogoutOutlined />} onClick={handleLogout}>
                登出
              </Button>
            </>
          ) : (
            <Button type="primary" icon={<UserOutlined />} onClick={() => setLoginModalOpen(true)}>
              登录
            </Button>
          )}
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
                <Popconfirm
                  title="确定删除此工作流？"
                  description="删除后不可恢复"
                  onConfirm={() => handleDeleteWorkflow(item.id, item.name)}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button type="link" danger icon={<DeleteOutlined />}>
                    删除
                  </Button>
                </Popconfirm>,
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

      <Modal
        title="登录"
        open={loginModalOpen}
        onOk={handleLogin}
        onCancel={() => setLoginModalOpen(false)}
        okText="登录"
        cancelText="取消"
        destroyOnClose
      >
        <div style={{ padding: '16px 0' }}>
          <Input
            prefix={<UserOutlined />}
            placeholder="请输入用户名"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            onPressEnter={handleLogin}
            size="large"
          />
          <div style={{ color: '#999', fontSize: 12, marginTop: 8 }}>
            💡 输入任意用户名即可登录（演示模式）
          </div>
        </div>
      </Modal>
    </>
  );
};

export default Header;
