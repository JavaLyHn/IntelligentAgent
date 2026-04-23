import React, { useCallback, useRef } from 'react';
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  addEdge,
  useReactFlow,
  ReactFlowProvider,
} from '@xyflow/react';
import '@xyflow/react/dist/style.css';
import { useWorkflowStore } from '../store/workflowStore';
import { nodeTypes } from '../nodes/CustomNodes';
import { NodeType } from '../types/workflow';

const WorkflowCanvas: React.FC = () => {
  const reactFlowWrapper = useRef<HTMLDivElement>(null);
  const { screenToFlowPosition } = useReactFlow();
  const {
    nodes,
    edges,
    onNodesChange,
    onEdgesChange,
    addNode,
    setEdges,
    setSelectedNode,
  } = useWorkflowStore();

  const onConnect = useCallback(
    (params: any) =>
      setEdges((eds) =>
        addEdge({ ...params, animated: true, style: { stroke: '#1890ff', strokeWidth: 2 } }, eds)
      ),
    [setEdges]
  );

  const onDragOver = useCallback((event: React.DragEvent) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
  }, []);

  const onDrop = useCallback(
    (event: React.DragEvent) => {
      event.preventDefault();

      const nodeType = event.dataTransfer.getData('nodeType');
      const label = event.dataTransfer.getData('label');
      const category = event.dataTransfer.getData('category') as NodeType;

      if (!nodeType || !screenToFlowPosition || !reactFlowWrapper.current) return;

      const position = screenToFlowPosition({
        x: event.clientX,
        y: event.clientY,
      });

      let newNodeType = 'llmNode';
      let defaultLabel = label;

      switch (category) {
        case NodeType.INPUT:
          newNodeType = 'inputNode';
          break;
        case NodeType.LLM:
          newNodeType = 'llmNode';
          break;
        case NodeType.TOOL:
          newNodeType = 'toolNode';
          break;
        case NodeType.OUTPUT:
          newNodeType = 'outputNode';
          break;
      }

      const newNode = {
        id: `${newNodeType}-${Date.now()}`,
        type: newNodeType,
        position,
        data: {
          label: defaultLabel,
          type: nodeType,
          category,
          config: {},
        },
      };

      addNode(newNode);
    },
    [screenToFlowPosition, addNode]
  );

  const onNodeClick = useCallback((_event: React.MouseEvent, node: any) => {
    setSelectedNode(node.id);
  }, [setSelectedNode]);

  const onPaneClick = useCallback(() => {
    setSelectedNode(null);
  }, [setSelectedNode]);

  return (
    <div ref={reactFlowWrapper} style={{ width: '100%', height: '100%' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onConnect={onConnect}
        onDrop={onDrop}
        onDragOver={onDragOver}
        onNodeClick={onNodeClick}
        onPaneClick={onPaneClick}
        nodeTypes={nodeTypes}
        fitView
        snapToGrid
        snapGrid={[15, 15]}
        defaultEdgeOptions={{
          animated: true,
          style: { stroke: '#1890ff', strokeWidth: 2 },
        }}
      >
        <Controls
          style={{
            bottom: 30,
            left: 16,
          }}
        />
        <MiniMap
          nodeStrokeWidth={3}
          zoomable
          pannable
          style={{
            width: 180,
            height: 120,
            bottom: 30,
            right: 320,
          }}
        />
        <Background gap={20} size={1} color="#e8e8e8" />
      </ReactFlow>
    </div>
  );
};

const Canvas: React.FC = () => {
  return (
    <ReactFlowProvider>
      <WorkflowCanvas />
    </ReactFlowProvider>
  );
};

export default Canvas;
