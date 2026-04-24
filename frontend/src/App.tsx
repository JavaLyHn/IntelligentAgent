import React from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Canvas from './components/Canvas';
import ConfigPanel from './components/ConfigPanel';
import DebugDrawer from './components/DebugDrawer';

const App: React.FC = () => {
  return (
    <div className="app-container">
      <Header />
      <div className="app-body">
        <Sidebar />
        <div className="canvas-wrapper">
          <Canvas />
        </div>
        <ConfigPanel />
      </div>
      <DebugDrawer />
    </div>
  );
};

export default App;
