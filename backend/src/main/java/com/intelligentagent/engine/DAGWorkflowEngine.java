package com.intelligentagent.engine;

import com.intelligentagent.engine.model.*;
import com.intelligentagent.engine.processor.NodeProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DAGWorkflowEngine {

    private final List<NodeProcessor> processorList;
    private final Map<String, NodeProcessor> processorMap = new ConcurrentHashMap<>();

    private Map<String, NodeProcessor> getProcessorMap() {
        if (processorMap.isEmpty()) {
            for (NodeProcessor processor : processorList) {
                processorMap.put(processor.getType(), processor);
            }
            log.info("Registered node processors: {}", processorMap.keySet());
        }
        return processorMap;
    }

    public WorkflowExecutionResult execute(WorkflowDefinition definition, String input) {
        long startTime = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();
        Map<String, NodeContext> nodeContexts = new LinkedHashMap<>();

        logs.add("🚀 DAG工作流引擎启动...");
        logs.add(String.format("📊 节点数: %d, 边数: %d", definition.getNodes().size(), definition.getEdges().size()));

        try {
            Map<String, WorkflowNode> nodeMap = definition.getNodes().stream()
                    .collect(Collectors.toMap(WorkflowNode::getId, n -> n));

            List<String> sortedNodeIds = topologicalSort(definition, nodeMap);
            logs.add("📋 拓扑排序完成: " + sortedNodeIds.stream()
                    .map(id -> nodeMap.get(id).getData().getLabel())
                    .collect(Collectors.joining(" → ")));

            Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();
            nodeOutputs.put("__input__", Map.of("input", input, "text", input));

            for (String nodeId : sortedNodeIds) {
                WorkflowNode node = nodeMap.get(nodeId);
                long nodeStart = System.currentTimeMillis();

                logs.add(String.format("⚙️ 执行节点: %s (%s)", node.getData().getLabel(), node.getType()));

                try {
                    Map<String, Object> inputData = collectInputData(nodeId, definition, nodeOutputs, input);

                    NodeProcessor processor = getProcessorMap().get(node.getType());
                    if (processor == null) {
                        logs.add(String.format("⚠️ 未找到处理器: %s, 跳过", node.getType()));
                        continue;
                    }

                    Map<String, Object> config = node.getData().getConfig() != null
                            ? node.getData().getConfig() : new HashMap<>();
                    if (node.getData().getType() != null) {
                        config.putIfAbsent("provider", node.getData().getType());
                    }

                    Map<String, Object> outputData = processor.process(inputData, config);
                    nodeOutputs.put(nodeId, outputData);

                    long nodeDuration = System.currentTimeMillis() - nodeStart;
                    nodeContexts.put(nodeId, NodeContext.builder()
                            .nodeId(nodeId)
                            .nodeType(node.getType())
                            .label(node.getData().getLabel())
                            .inputData(inputData)
                            .outputData(outputData)
                            .executionTimeMs(nodeDuration)
                            .status("SUCCESS")
                            .build());

                    logs.add(String.format("✅ 节点完成: %s (%dms)", node.getData().getLabel(), nodeDuration));

                } catch (Exception e) {
                    long nodeDuration = System.currentTimeMillis() - nodeStart;
                    nodeContexts.put(nodeId, NodeContext.builder()
                            .nodeId(nodeId)
                            .nodeType(node.getType())
                            .label(node.getData().getLabel())
                            .executionTimeMs(nodeDuration)
                            .status("FAILED")
                            .errorMessage(e.getMessage())
                            .build());

                    logs.add(String.format("❌ 节点失败: %s - %s", node.getData().getLabel(), e.getMessage()));
                    throw new RuntimeException("节点执行失败: " + node.getData().getLabel(), e);
                }
            }

            String outputText = extractFinalOutput(sortedNodeIds, nodeOutputs, nodeMap);
            String audioUrl = extractAudioUrl(nodeOutputs);

            long totalDuration = System.currentTimeMillis() - startTime;
            logs.add(String.format("🎉 工作流执行完成! 耗时: %dms", totalDuration));

            return WorkflowExecutionResult.builder()
                    .success(true)
                    .outputText(outputText)
                    .audioUrl(audioUrl)
                    .logs(logs)
                    .nodeContexts(nodeContexts)
                    .totalDurationMs(totalDuration)
                    .build();

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            logs.add(String.format("❌ 工作流执行失败: %s", e.getMessage()));

            return WorkflowExecutionResult.builder()
                    .success(false)
                    .logs(logs)
                    .nodeContexts(nodeContexts)
                    .totalDurationMs(totalDuration)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private List<String> topologicalSort(WorkflowDefinition definition, Map<String, WorkflowNode> nodeMap) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (WorkflowNode node : definition.getNodes()) {
            inDegree.put(node.getId(), 0);
            adjacency.put(node.getId(), new ArrayList<>());
        }

        for (WorkflowEdge edge : definition.getEdges()) {
            adjacency.computeIfAbsent(edge.getSource(), k -> new ArrayList<>()).add(edge.getTarget());
            inDegree.merge(edge.getTarget(), 1, Integer::sum);
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);

            for (String neighbor : adjacency.getOrDefault(current, List.of())) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        if (result.size() != definition.getNodes().size()) {
            throw new RuntimeException("工作流中存在循环依赖，无法执行");
        }

        return result;
    }

    private Map<String, Object> collectInputData(String nodeId, WorkflowDefinition definition,
                                                  Map<String, Map<String, Object>> nodeOutputs, String input) {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("input", input);

        List<WorkflowEdge> incomingEdges = definition.getEdges().stream()
                .filter(e -> e.getTarget().equals(nodeId))
                .toList();

        for (WorkflowEdge edge : incomingEdges) {
            Map<String, Object> sourceOutput = nodeOutputs.get(edge.getSource());
            if (sourceOutput != null) {
                inputData.putAll(sourceOutput);
            }
        }

        return inputData;
    }

    private String extractFinalOutput(List<String> sortedNodeIds, Map<String, Map<String, Object>> nodeOutputs,
                                       Map<String, WorkflowNode> nodeMap) {
        for (int i = sortedNodeIds.size() - 1; i >= 0; i--) {
            String nodeId = sortedNodeIds.get(i);
            WorkflowNode node = nodeMap.get(nodeId);
            Map<String, Object> output = nodeOutputs.get(nodeId);

            if ("outputNode".equals(node.getType()) && output != null) {
                return getStr(output, "text", "");
            }
        }

        for (int i = sortedNodeIds.size() - 1; i >= 0; i--) {
            String nodeId = sortedNodeIds.get(i);
            Map<String, Object> output = nodeOutputs.get(nodeId);
            if (output != null && output.containsKey("text")) {
                return getStr(output, "text", "");
            }
        }

        return "";
    }

    private String extractAudioUrl(Map<String, Map<String, Object>> nodeOutputs) {
        for (Map<String, Object> output : nodeOutputs.values()) {
            if (output.containsKey("audioUrl") && output.get("audioUrl") != null) {
                return output.get("audioUrl").toString();
            }
        }
        return null;
    }

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
