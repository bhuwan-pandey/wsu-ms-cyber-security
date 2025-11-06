import java.io.*;
import java.util.*;

public class Lab3U01155118I {
    /**
     * `Edge` class to represent a bidirectional link between two storages.
     * just using the OOP way to store edge information.
     */
    static class Edge {
        // endpoints (storage IDs)
        int vertex1;
        int vertex2;
        // bandwidth weight
        int weight;

        Edge(int vertex1, int vertex2, int weight) {
            this.vertex1 = vertex1;
            this.vertex2 = vertex2;
            this.weight = weight;
        }
    }

    /**
     * instead of using loops in unstructured way, we use DSU to manage connected
     * components efficiently. This allows us to quickly union sets in more of a
     * "professional" manner.
     */
    static class DSU {
        int[] parent; // parent[i] stores the representative (root) of node i
        int[] rank; // rank[i] stores tree depth to optimize union operation

        DSU(int n) {
            parent = new int[n + 1];
            rank = new int[n + 1];
            for (int i = 1; i <= n; i++) {
                // initially, each node is its own parent
                parent[i] = i;
            }
        }

        // find root of set that parent belongs to
        int find(int x) {
            if (parent[x] != x)
                parent[x] = find(parent[x]);
            return parent[x];
        }

        // attach trees by rand
        void union(int x, int y) {
            int rx = find(x), ry = find(y);
            if (rx == ry)
                return; // already in the same set
            if (rank[rx] < rank[ry])
                parent[rx] = ry;
            else if (rank[rx] > rank[ry])
                parent[ry] = rx;
            else {
                parent[ry] = rx;
                rank[rx]++;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        BufferedReader inputReader = new BufferedReader(new FileReader("storage.txt"));
        // read first line. i.e. number of storages (N) and links (M)
        String[] firstLine = inputReader.readLine().trim().split(" ");
        int N = Integer.parseInt(firstLine[0]); // total number of storages
        int M = Integer.parseInt(firstLine[1]); // total number of bidirectional links
        // read second line
        String[] secondLine = inputReader.readLine().trim().split(" ");
        int[] storageOfFile = new int[N + 1];
        for (int i = 1; i <= N; i++)
            storageOfFile[i] = Integer.parseInt(secondLine[i - 1]);

        // Read next M lines: each link with (a, b, w)
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            String[] parts = inputReader.readLine().trim().split(" ");
            int vertex1 = Integer.parseInt(parts[0]); // endpoint 1
            int vertex2 = Integer.parseInt(parts[1]); // endpoint 2
            int weight = Integer.parseInt(parts[2]); // bandwidth
            edges.add(new Edge(vertex1, vertex2, weight)); // store edge info
        }
        inputReader.close();

        // check if all files are already in their correct storage
        boolean alreadyCorrect = true;
        for (int i = 1; i <= N; i++) {
            if (storageOfFile[i] != i) {
                alreadyCorrect = false;
                break;
            }
        }
        if (alreadyCorrect) {
            // if no moves needed, output -1 as per problem statement
            PrintWriter outputWriter = new PrintWriter("storageout.txt");
            outputWriter.println(-1);
            outputWriter.close();
            return;
        }

        // sort all edges in decreasing order of bandwidth
        edges.sort((e1, e2) -> Integer.compare(e2.weight, e1.weight));
        // initializing DSU for N storages
        DSU dsu = new DSU(N);

        /**
         * iterate over edges from highest bandwidth to lowest. just simulating
         * "activating" links in this order until every file can reach its correct
         * storage.
         */
        for (Edge edge : edges) {
            // connect the two vertices via this link
            dsu.union(edge.vertex1, edge.vertex2);
            boolean allConnected = true;
            /**
             * Check if for every file i, its current storage and correct storage are
             * connected.
             */
            for (int i = 1; i <= N; i++) {
                if (dsu.find(storageOfFile[i]) != dsu.find(i)) {
                    allConnected = false;
                    break;
                }
            }

            /**
             * if all files can reach their correct storage, output the current edge's
             * bandwidth.
             */
            if (allConnected) {
                PrintWriter outputWriter = new PrintWriter("storageout.txt");
                outputWriter.println(edge.weight); // output the maximum minimal bandwidth
                outputWriter.close();
                return;
            }
        }

        // self explanatory...
        PrintWriter outputWriter = new PrintWriter("storageout.txt");
        outputWriter.println(-1);
        outputWriter.close();
    }
}