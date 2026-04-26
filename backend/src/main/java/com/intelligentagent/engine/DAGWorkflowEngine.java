package com.intelligentagent.engine;

import com.intelligentagent.engine.model.*;
import com.intelligentagent.engine.processor.NodeProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
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
        return execute(definition, input, null);
    }

    public WorkflowExecutionResult execute(WorkflowDefinition definition, String input,
                                            Consumer<ExecutionEvent> eventConsumer) {
        long startTime = System.currentTimeMillis();
        List<String> logs = new ArrayList<>();
        Map<String, NodeContext> nodeContexts = new LinkedHashMap<>();

        logs.add("🚀 DAG工作流引擎启动...");
        logs.add(String.format("📊 节点数: %d, 边数: %d", definition.getNodes().size(), definition.getEdges().size()));

        emitEvent(eventConsumer, ExecutionEvent.workflowStarted(
                definition.getNodes().size(), definition.getEdges().size()));

        try {
            Map<String, WorkflowNode> nodeMap = definition.getNodes().stream()
                    .collect(Collectors.toMap(WorkflowNode::getId, n -> n));

            Set<String> reachableNodeIds = findReachableNodes(definition);
            logs.add(String.format("🔗 可达节点数: %d (共 %d 个节点)",
                    reachableNodeIds.size(), definition.getNodes().size()));

            List<WorkflowNode> reachableNodes = definition.getNodes().stream()
                    .filter(n -> reachableNodeIds.contains(n.getId()))
                    .collect(Collectors.toList());

            WorkflowDefinition filteredDefinition = WorkflowDefinition.builder()
                    .nodes(reachableNodes)
                    .edges(definition.getEdges().stream()
                            .filter(e -> reachableNodeIds.contains(e.getSource()) && reachableNodeIds.contains(e.getTarget()))
                            .collect(Collectors.toList()))
                    .build();

            List<String> sortedNodeIds = topologicalSort(filteredDefinition, nodeMap);
            String orderStr = sortedNodeIds.stream()
                    .map(id -> nodeMap.get(id).getData().getLabel())
                    .collect(Collectors.joining(" → "));
            logs.add("📋 执行顺序: " + orderStr);
            emitEvent(eventConsumer, ExecutionEvent.log("📋 执行顺序: " + orderStr));

            Map<String, Map<String, Object>> nodeOutputs = new ConcurrentHashMap<>();
            nodeOutputs.put("__input__", Map.of("input", input, "text", input));

            for (String nodeId : sortedNodeIds) {
                WorkflowNode node = nodeMap.get(nodeId);
                long nodeStart = System.currentTimeMillis();

                logs.add(String.format("⚙️ 执行节点: %s (%s)", node.getData().getLabel(), node.getType()));
                emitEvent(eventConsumer, ExecutionEvent.nodeStarted(nodeId, node.getType(), node.getData().getLabel()));

                try {
                    Map<String, Object> inputData = collectInputData(nodeId, definition, nodeOutputs, input);

                    NodeProcessor processor = getProcessorMap().get(node.getType());
                    if (processor == null) {
                        logs.add(String.format("⚠️ 未找到处理器: %s, 跳过", node.getType()));
                        emitEvent(eventConsumer, ExecutionEvent.log("⚠️ 未找到处理器: " + node.getType()));
                        continue;
                    }

                    Map<String, Object> config = node.getData().getConfig() != null
                            ? new HashMap<>(node.getData().getConfig()) : new HashMap<>();
                    if (node.getData().getType() != null) {
                        config.putIfAbsent("provider", node.getData().getType());
                    }

                    Map<String, Object> outputData = processor.process(inputData, config);
                    nodeOutputs.put(nodeId, outputData);

                    Map<String, Object> allContextData = new HashMap<>(inputData);
                    allContextData.putAll(outputData);
                    nodeOutputs.put("node_" + nodeId, allContextData);

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
                    emitEvent(eventConsumer, ExecutionEvent.nodeCompleted(
                            nodeId, node.getType(), node.getData().getLabel(), nodeDuration, outputData));

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
                    emitEvent(eventConsumer, ExecutionEvent.nodeFailed(
                            nodeId, node.getType(), node.getData().getLabel(), nodeDuration, e.getMessage()));
                    throw new RuntimeException("节点执行失败: " + node.getData().getLabel(), e);
                }
            }

            String outputText = extractFinalOutput(sortedNodeIds, nodeOutputs, nodeMap);
            String audioUrl = extractAudioUrl(nodeOutputs);
            String[] ttsInfo = extractTtsStatus(nodeOutputs);

            long totalDuration = System.currentTimeMillis() - startTime;
            logs.add(String.format("🎉 工作流执行完成! 耗时: %dms", totalDuration));

            WorkflowExecutionResult result = WorkflowExecutionResult.builder()
                    .success(true)
                    .outputText(outputText)
                    .audioUrl(audioUrl)
                    .ttsStatus(ttsInfo[0])
                    .ttsError(ttsInfo[1])
                    .logs(logs)
                    .nodeContexts(nodeContexts)
                    .totalDurationMs(totalDuration)
                    .build();

            emitEvent(eventConsumer, ExecutionEvent.workflowCompleted(
                    true, outputText, audioUrl, ttsInfo[0], ttsInfo[1], totalDuration));

            return result;

        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime;
            logs.add(String.format("❌ 工作流执行失败: %s", e.getMessage()));

            emitEvent(eventConsumer, ExecutionEvent.workflowCompleted(
                    false, null, null, null, e.getMessage(), totalDuration));

            return WorkflowExecutionResult.builder()
                    .success(false)
                    .logs(logs)
                    .nodeContexts(nodeContexts)
                    .totalDurationMs(totalDuration)
                    .errorMessage(e.getMessage())
                    .build();
        }
    }

    private void emitEvent(Consumer<ExecutionEvent> eventConsumer, ExecutionEvent event) {
        if (eventConsumer != null) {
            try {
                eventConsumer.accept(event);
            } catch (Exception e) {
                log.warn("Failed to emit event: {}", e.getMessage());
            }
        }
    }

    private Set<String> findReachableNodes(WorkflowDefinition definition) {
        Map<String, List<String>> adjacency = new HashMap<>();
        Set<String> allNodeIds = definition.getNodes().stream()
                .map(WorkflowNode::getId)
                .collect(Collectors.toSet());

        for (String nodeId : allNodeIds) {
            adjacency.put(nodeId, new ArrayList<>());
        }
        for (WorkflowEdge edge : definition.getEdges()) {
            if (adjacency.containsKey(edge.getSource())) {
                adjacency.get(edge.getSource()).add(edge.getTarget());
            }
        }

        Set<String> targetIds = definition.getEdges().stream()
                .map(WorkflowEdge::getTarget)
                .collect(Collectors.toSet());

        List<String> startNodes = definition.getNodes().stream()
                .filter(n -> n.getType().equals("inputNode") || !targetIds.contains(n.getId()))
                .map(WorkflowNode::getId)
                .collect(Collectors.toList());

        if (startNodes.isEmpty() && !definition.getNodes().isEmpty()) {
            startNodes.add(definition.getNodes().get(0).getId());
        }

        Set<String> reachable = new HashSet<>();
        Queue<String> queue = new LinkedList<>(startNodes);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (reachable.contains(current)) continue;
            reachable.add(current);

            for (String next : adjacency.getOrDefault(current, List.of())) {
                if (!reachable.contains(next)) {
                    queue.add(next);
                }
            }
        }

        return reachable;
    }

    private List<String> topologicalSort(WorkflowDefinition definition, Map<String, WorkflowNode> nodeMap) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (WorkflowNode node : definition.getNodes()) {
            inDegree.put(node.getId(), 0);
            adjacency.put(node.getId(), new ArrayList<>());
        }

        for (WorkflowEdge edge : definition.getEdges()) {
            if (adjacency.containsKey(edge.getSource()) && inDegree.containsKey(edge.getTarget())) {
                adjacency.get(edge.getSource()).add(edge.getTarget());
                inDegree.merge(edge.getTarget(), 1, Integer::sum);
            }
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

            Map<String, Object> sourceContext = nodeOutputs.get("node_" + edge.getSource());
            if (sourceContext != null) {
                inputData.put("node_" + edge.getSource(), sourceContext);
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
            if (output.containsKey("voice_url") && output.get("voice_url") != null) {
                String url = output.get("voice_url").toString();
                if (!url.isEmpty()) return url;
            }
            if (output.containsKey("audioUrl") && output.get("audioUrl") != null) {
                String url = output.get("audioUrl").toString();
                if (!url.isEmpty()) return url;
            }
        }
        return null;
    }

    private String[] extractTtsStatus(Map<String, Map<String, Object>> nodeOutputs) {
        for (Map<String, Object> output : nodeOutputs.values()) {
            if (output.containsKey("ttsStatus")) {
                String status = output.get("ttsStatus") != null ? output.get("ttsStatus").toString() : "";
                String error = output.get("ttsError") != null ? output.get("ttsError").toString() : "";
                return new String[]{status, error};
            }
        }
        return new String[]{"", ""};
    }

    private String getStr(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultVal;
    }
}
