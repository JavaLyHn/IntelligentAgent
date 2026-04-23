import React from 'react';
import { Layout } from 'antd';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Canvas from './components/Canvas';
import ConfigPanel from './components/ConfigPanel';
import DebugDrawer from './components/DebugDrawer';

const { Content } = Layout;

const App: React.FC = () => {
  return (
    <Layout className="app-container">
      <Header />
      <Layout>
        <Sidebar />
        <Content style={{ flex: 1, position: 'relative' }}>
          <Canvas />
        </Content>
        <ConfigPanel />
      </Layout>
      <DebugDrawer />
    </Layout>
  );
};

export default App;
