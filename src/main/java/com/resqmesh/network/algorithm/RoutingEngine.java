package com.resqmesh.network.algorithm;

import com.resqmesh.network.dto.RouteResponse;
import com.resqmesh.network.model.GraphEdge;
import com.resqmesh.network.model.GraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoutingEngine {
    private static final Logger log = LoggerFactory.getLogger(RoutingEngine.class);
    private static final double MAX_TRAVEL_SPEED_KM_PER_MIN = 60.0 / 60.0; // 60 km/h = 1 km/min

    private int nodeCount;
    private int edgeCount;

    // Dense index mappings
    private Map<String, Integer> nodeIdToIndex = new HashMap<>();
    private String[] indexToNodeId;

    // CSR Arrays
    private int[] nodeOffsets;
    private int[] edgeTargets;
    private double[] edgeWeights;
    private double[] nodeLat;
    private double[] nodeLon;
    private String[] edgeIds;

    // Reverse index: edgeIndex -> set of mission IDs
    private Map<Integer, Set<java.util.UUID>> activeMissionsPerEdge = new ConcurrentHashMap<>();
    private Map<String, Integer> edgeIdToIndex = new HashMap<>();

    public synchronized void buildGraph(List<GraphNode> nodes, List<GraphEdge> edges, Map<String, Double> latitudes, Map<String, Double> longitudes) {
        this.nodeCount = nodes.size();
        this.indexToNodeId = new String[nodeCount];
        this.nodeLat = new double[nodeCount];
        this.nodeLon = new double[nodeCount];

        for (int i = 0; i < nodeCount; i++) {
            String id = nodes.get(i).getId();
            nodeIdToIndex.put(id, i);
            indexToNodeId[i] = id;
            if (latitudes != null && longitudes != null) {
                nodeLat[i] = latitudes.getOrDefault(id, 0.0);
                nodeLon[i] = longitudes.getOrDefault(id, 0.0);
            }
        }

        // Count out-degrees
        int[] outDegree = new int[nodeCount];
        for (GraphEdge e : edges) {
            Integer srcIdx = nodeIdToIndex.get(e.getSourceNodeId());
            if (srcIdx != null) {
                outDegree[srcIdx]++;
            }
        }

        // Build offsets
        this.nodeOffsets = new int[nodeCount + 1];
        int offset = 0;
        for (int i = 0; i < nodeCount; i++) {
            nodeOffsets[i] = offset;
            offset += outDegree[i];
        }
        nodeOffsets[nodeCount] = offset; // Total edges

        this.edgeCount = edges.size();
        this.edgeTargets = new int[edgeCount];
        this.edgeWeights = new double[edgeCount];
        this.edgeIds = new String[edgeCount];

        // Fill CSR arrays
        int[] currentOffset = Arrays.copyOf(nodeOffsets, nodeCount);
        for (GraphEdge e : edges) {
            Integer srcIdx = nodeIdToIndex.get(e.getSourceNodeId());
            Integer dstIdx = nodeIdToIndex.get(e.getDestinationNodeId());
            if (srcIdx != null && dstIdx != null) {
                int pos = currentOffset[srcIdx]++;
                edgeTargets[pos] = dstIdx;
                edgeWeights[pos] = e.getTravelTimeMinutes();
                edgeIds[pos] = e.getId();
                edgeIdToIndex.put(e.getId(), pos);
            }
        }
        
        log.info("Graph built in CSR format. Nodes: {}, Edges: {}", nodeCount, edgeCount);
    }

    public synchronized void closeEdge(String edgeId) {
        Integer idx = edgeIdToIndex.get(edgeId);
        if (idx != null) {
            edgeWeights[idx] = Double.POSITIVE_INFINITY;
        }
    }

    public synchronized void setEdgeWeight(String edgeId, double weight) {
        Integer idx = edgeIdToIndex.get(edgeId);
        if (idx != null) {
            edgeWeights[idx] = weight;
        }
    }

    public Set<java.util.UUID> getMissionsOnEdge(String edgeId) {
        Integer idx = edgeIdToIndex.get(edgeId);
        if (idx != null && activeMissionsPerEdge.containsKey(idx)) {
            return new HashSet<>(activeMissionsPerEdge.get(idx));
        }
        return Collections.emptySet();
    }

    public void registerMissionRoute(java.util.UUID missionId, List<String> routeEdgeIds) {
        for (String edgeId : routeEdgeIds) {
            Integer idx = edgeIdToIndex.get(edgeId);
            if (idx != null) {
                activeMissionsPerEdge.computeIfAbsent(idx, k -> ConcurrentHashMap.newKeySet()).add(missionId);
            }
        }
    }
    
    public void unregisterMissionRoute(java.util.UUID missionId, List<String> routeEdgeIds) {
        for (String edgeId : routeEdgeIds) {
            Integer idx = edgeIdToIndex.get(edgeId);
            if (idx != null && activeMissionsPerEdge.containsKey(idx)) {
                activeMissionsPerEdge.get(idx).remove(missionId);
            }
        }
    }

    // Dijkstra implementation - primitive priority queue logic
    public RouteResponse dijkstra(String sourceNodeId, String destinationNodeId) {
        return runAStar(sourceNodeId, destinationNodeId, false);
    }
    
    // Bidirectional A* Implementation (Forward A* for now)
    public RouteResponse bidirectionalAStar(String sourceNodeId, String destinationNodeId) {
        return runAStar(sourceNodeId, destinationNodeId, true);
    }
    
    private RouteResponse runAStar(String sourceNodeId, String destinationNodeId, boolean useHeuristic) {
        if (nodeCount == 0) return new RouteResponse(false, null, Collections.emptyList(), Collections.emptyList());
        Integer src = nodeIdToIndex.get(sourceNodeId);
        Integer dst = nodeIdToIndex.get(destinationNodeId);
        if (src == null || dst == null) return new RouteResponse(false, null, Collections.emptyList(), Collections.emptyList());
        if (src.equals(dst)) return new RouteResponse(true, 0, List.of(sourceNodeId), Collections.emptyList());

        double[] gScore = new double[nodeCount];
        Arrays.fill(gScore, Double.POSITIVE_INFINITY);
        int[] prevNode = new int[nodeCount];
        int[] prevEdge = new int[nodeCount];
        Arrays.fill(prevNode, -1);
        Arrays.fill(prevEdge, -1);
        
        gScore[src] = 0;
        
        double[] fScore = new double[nodeCount];
        Arrays.fill(fScore, Double.POSITIVE_INFINITY);
        fScore[src] = useHeuristic ? heuristic(src, dst) : 0;
        
        int[] heap = new int[nodeCount];
        int[] pos = new int[nodeCount];
        Arrays.fill(pos, -1);
        int heapSize = 0;
        
        heap[0] = src;
        pos[src] = 0;
        heapSize++;
        
        while (heapSize > 0) {
            int u = heap[0];
            heapSize--;
            heap[0] = heap[heapSize];
            if (heapSize > 0) pos[heap[0]] = 0;
            pos[u] = -1;
            heapifyDown(0, heap, pos, fScore, heapSize);
            
            if (u == dst) break;
            if (gScore[u] == Double.POSITIVE_INFINITY) break;
            
            int start = nodeOffsets[u];
            int end = nodeOffsets[u + 1];
            for (int i = start; i < end; i++) {
                int v = edgeTargets[i];
                double w = edgeWeights[i];
                if (w == Double.POSITIVE_INFINITY) continue;
                
                double tentativeG = gScore[u] + w;
                if (tentativeG < gScore[v]) {
                    prevNode[v] = u;
                    prevEdge[v] = i;
                    gScore[v] = tentativeG;
                    fScore[v] = tentativeG + (useHeuristic ? heuristic(v, dst) : 0);
                    
                    if (pos[v] == -1) {
                        heap[heapSize] = v;
                        pos[v] = heapSize;
                        heapSize++;
                        heapifyUp(heapSize - 1, heap, pos, fScore);
                    } else {
                        heapifyUp(pos[v], heap, pos, fScore);
                    }
                }
            }
        }
        
        if (gScore[dst] == Double.POSITIVE_INFINITY) {
            return new RouteResponse(false, null, Collections.emptyList(), Collections.emptyList());
        }
        
        return reconstructPath(dst, gScore[dst], prevNode, prevEdge);
    }
    
    public Map<String, Double> multiSourceDijkstra(List<String> sourceNodeIds) {
        if (nodeCount == 0) return Collections.emptyMap();
        double[] dist = new double[nodeCount];
        Arrays.fill(dist, Double.POSITIVE_INFINITY);
        
        int[] heap = new int[nodeCount];
        int[] pos = new int[nodeCount];
        Arrays.fill(pos, -1);
        int heapSize = 0;
        
        for (String srcId : sourceNodeIds) {
            Integer src = nodeIdToIndex.get(srcId);
            if (src != null) {
                dist[src] = 0.0;
                heap[heapSize] = src;
                pos[src] = heapSize;
                heapSize++;
            }
        }
        
        // Ensure heap is valid if multiple sources added
        for (int i = (heapSize / 2) - 1; i >= 0; i--) {
            heapifyDown(i, heap, pos, dist, heapSize);
        }
        
        while (heapSize > 0) {
            int u = heap[0];
            heapSize--;
            heap[0] = heap[heapSize];
            if (heapSize > 0) pos[heap[0]] = 0;
            pos[u] = -1;
            heapifyDown(0, heap, pos, dist, heapSize);
            
            if (dist[u] == Double.POSITIVE_INFINITY) break;
            
            int start = nodeOffsets[u];
            int end = nodeOffsets[u + 1];
            for (int i = start; i < end; i++) {
                int v = edgeTargets[i];
                double w = edgeWeights[i];
                if (w == Double.POSITIVE_INFINITY) continue;
                
                double newDist = dist[u] + w;
                if (newDist < dist[v]) {
                    dist[v] = newDist;
                    
                    if (pos[v] == -1) {
                        heap[heapSize] = v;
                        pos[v] = heapSize;
                        heapSize++;
                        heapifyUp(heapSize - 1, heap, pos, dist);
                    } else {
                        heapifyUp(pos[v], heap, pos, dist);
                    }
                }
            }
        }
        
        Map<String, Double> result = new HashMap<>();
        for (int i = 0; i < nodeCount; i++) {
            if (dist[i] != Double.POSITIVE_INFINITY) {
                result.put(indexToNodeId[i], dist[i]);
            }
        }
        return result;
    }
    
    private double heuristic(int u, int v) {
        double lat1 = Math.toRadians(nodeLat[u]);
        double lon1 = Math.toRadians(nodeLon[u]);
        double lat2 = Math.toRadians(nodeLat[v]);
        double lon2 = Math.toRadians(nodeLon[v]);
        
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;
        
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                   Math.cos(lat1) * Math.cos(lat2) * 
                   Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distKm = 6371.0 * c;
        
        return distKm / MAX_TRAVEL_SPEED_KM_PER_MIN;
    }

    private RouteResponse reconstructPath(int dst, double totalWeight, int[] prevNode, int[] prevEdge) {
        List<String> nodePath = new ArrayList<>();
        List<String> roadPath = new ArrayList<>();
        
        int current = dst;
        while (current != -1) {
            nodePath.add(indexToNodeId[current]);
            int edgeIdx = prevEdge[current];
            if (edgeIdx != -1) {
                roadPath.add(edgeIds[edgeIdx]);
            }
            current = prevNode[current];
        }
        
        Collections.reverse(nodePath);
        Collections.reverse(roadPath);
        
        return new RouteResponse(true, (int) Math.round(totalWeight), nodePath, roadPath);
    }

    private void heapifyUp(int i, int[] heap, int[] pos, double[] keys) {
        int u = heap[i];
        while (i > 0) {
            int p = (i - 1) / 2;
            int parent = heap[p];
            if (keys[u] >= keys[parent]) break;
            heap[i] = parent;
            pos[parent] = i;
            i = p;
        }
        heap[i] = u;
        pos[u] = i;
    }

    private void heapifyDown(int i, int[] heap, int[] pos, double[] keys, int heapSize) {
        int u = heap[i];
        while (true) {
            int left = 2 * i + 1;
            int right = 2 * i + 2;
            int smallest = i;
            
            if (left < heapSize && keys[heap[left]] < keys[u]) {
                smallest = left;
            }
            if (right < heapSize && keys[heap[right]] < keys[heap[smallest]]) {
                smallest = right;
            }
            
            if (smallest == i) break;
            
            int v = heap[smallest];
            heap[i] = v;
            pos[v] = i;
            i = smallest;
        }
        heap[i] = u;
        pos[u] = i;
    }
    
    public int getNodeCount() { return nodeCount; }
    public int getEdgeCount() { return edgeCount; }
    
    // Legacy support since calculateShortestPath was used by NetworkService
    public RouteResponse calculateShortestPath(List<GraphNode> nodes, List<GraphEdge> edges, String sourceNodeId, String destinationNodeId) {
        return dijkstra(sourceNodeId, destinationNodeId);
    }
    
    public String getRandomNodeId(java.util.Random random) {
        if (nodeCount == 0) return null;
        return indexToNodeId[random.nextInt(nodeCount)];
    }
}
