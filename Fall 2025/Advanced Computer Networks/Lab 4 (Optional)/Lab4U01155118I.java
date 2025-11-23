import java.io.*;
import java.util.*;

public class Lab4U01155118I {

    // simple edge class to hold destination node and cost.
    static class Edge {
        int to;
        long cost;

        Edge(int to, long cost) {
            this.to = to;
            this.cost = cost;
        }
    }

    static int nodeCount, edgeCount;
    static ArrayList<Edge>[] graph; // normal graph
    static ArrayList<Edge>[] reversed; // reversed graph for backward dijkstra
    // large value to represent "infinity"
    static long INF = (long) 1e18;

    public static void main(String[] args) throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        /**
         * using tokenizer because it is little faster than split
         * for large inputs and simpler than full fledged scanner
         */
        StringTokenizer inputTokens = new StringTokenizer(input.readLine());
        nodeCount = Integer.parseInt(inputTokens.nextToken());
        edgeCount = Integer.parseInt(inputTokens.nextToken());
        // adjacency lists
        graph = new ArrayList[nodeCount + 1];
        reversed = new ArrayList[nodeCount + 1];
        for (int i = 1; i <= nodeCount; i++) {
            graph[i] = new ArrayList<>();
            reversed[i] = new ArrayList<>();
        }
        // storing all edges in arrays so we can easily rebuild the shortest path
        int[] from = new int[edgeCount];
        int[] to = new int[edgeCount];
        long[] cost = new long[edgeCount];
        for (int i = 0; i < edgeCount; i++) {
            inputTokens = new StringTokenizer(input.readLine());
            from[i] = Integer.parseInt(inputTokens.nextToken());
            to[i] = Integer.parseInt(inputTokens.nextToken());
            cost[i] = Long.parseLong(inputTokens.nextToken());

            graph[from[i]].add(new Edge(to[i], cost[i]));
            reversed[to[i]].add(new Edge(from[i], cost[i]));
        }
        // shortest distances from source (node 1)
        long[] distFromSource = dijkstra(graph, 1);
        // for shortest distances to destination/node n (via reversed graph)
        long[] distToDestination = dijkstra(reversed, nodeCount);
        long shortestDistance = distFromSource[nodeCount];
        // build shortest path DAG (Direct Acyclic Graph)
        ArrayList<Integer>[] dag = new ArrayList[nodeCount + 1];
        int[] indegree = new int[nodeCount + 1];
        for (int i = 1; i <= nodeCount; i++) {
            dag[i] = new ArrayList<>();
        }
        for (int i = 0; i < edgeCount; i++) {
            // only include edges that lie on at least one shortest path
            if (distFromSource[from[i]] + cost[i] + distToDestination[to[i]] == shortestDistance) {
                dag[from[i]].add(to[i]);
                indegree[to[i]]++;
            }
        }
        // and the topological sort on shortest path DAG
        int[] topoOrder = new int[nodeCount + 1];
        int topoIndex = 0;
        Queue<Integer> topoQueue = new ArrayDeque<>();
        for (int i = 1; i <= nodeCount; i++) {
            // only nodes that appear on some shortest path should enter the queue
            if (indegree[i] == 0 && distFromSource[i] + distToDestination[i] == shortestDistance) {
                topoQueue.add(i);
            }
        }
        // our classic BFS based topological sort
        while (!topoQueue.isEmpty()) {
            int currentNode = topoQueue.poll();
            topoOrder[topoIndex++] = currentNode;

            for (int nextNode : dag[currentNode]) {
                indegree[nextNode]--;
                if (indegree[nextNode] == 0) {
                    topoQueue.add(nextNode);
                }
            }
        }
        // count number of shortest paths from source to each node
        long[] waysFromSource = new long[nodeCount + 1];
        if (distFromSource[1] + distToDestination[1] == shortestDistance) {
            waysFromSource[1] = 1;
        }
        for (int i = 0; i < topoIndex; i++) {
            int u = topoOrder[i];
            for (int v : dag[u]) {
                waysFromSource[v] += waysFromSource[u];
            }
        }
        // count number of shortest paths from each node to destination
        long[] waysToDestination = new long[nodeCount + 1];
        waysToDestination[nodeCount] = 1; // destination has 1 trivial way to end at itself ofcourse
        for (int i = topoIndex - 1; i >= 0; i--) {
            int u = topoOrder[i];
            for (int v : dag[u]) {
                waysToDestination[u] += waysToDestination[v];
            }
        }
        // identifying mandatory nodes
        long totalShortestPaths = waysFromSource[nodeCount];
        ArrayList<Integer> mandatoryNodes = new ArrayList<>();
        for (int i = 1; i <= nodeCount; i++) {
            // a node is mandatory if every shortest path goes through it
            if (waysFromSource[i] > 0 &&
                    waysToDestination[i] > 0 &&
                    waysFromSource[i] * waysToDestination[i] == totalShortestPaths) {

                mandatoryNodes.add(i);
            }
        }
        // sort nodes before printing
        Collections.sort(mandatoryNodes);
        // the output
        System.out.println(mandatoryNodes.size());
        for (int i = 0; i < mandatoryNodes.size(); i++) {
            System.out.print(mandatoryNodes.get(i));
            if (i + 1 < mandatoryNodes.size())
                System.out.print(" ");
        }
    }

    /**
     * Dijkstra using a priority queue
     */
    static long[] dijkstra(ArrayList<Edge>[] graph, int startNode) {
        long[] dist = new long[nodeCount + 1];
        Arrays.fill(dist, INF);
        PriorityQueue<long[]> pq = new PriorityQueue<>(Comparator.comparingLong(a -> a[1]));
        dist[startNode] = 0;
        pq.add(new long[] { startNode, 0 });
        while (!pq.isEmpty()) {
            long[] current = pq.poll();
            int node = (int) current[0];
            long distance = current[1];
            if (distance != dist[node])
                continue;
            for (Edge edge : graph[node]) {
                long newDistance = distance + edge.cost;
                if (newDistance < dist[edge.to]) {
                    dist[edge.to] = newDistance;
                    pq.add(new long[] { edge.to, newDistance });
                }
            }
        }
        return dist;
    }
}
