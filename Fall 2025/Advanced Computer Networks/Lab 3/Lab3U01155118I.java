import java.io.*;
import java.util.*;

public class Lab3U01155118I {
    // Inner class representing a connection (edge) between two storages
    static class Edge {
        int a; // one endpoint (storage ID)
        int b; // another endpoint (storage ID)
        int w; // bandwidth (edge weight)

        Edge(int a, int b, int w) {
            this.a = a;
            this.b = b;
            this.w = w;
        }
    }

    // Disjoint Set Union (DSU) or Union-Find structure
    // Used to determine connectivity efficiently
    static class DSU {
        int[] parent; // parent[i] stores the representative (root) of node i
        int[] rank; // rank[i] stores tree depth to optimize union operation

        DSU(int n) {
            parent = new int[n + 1]; // index 1..N
            rank = new int[n + 1];
            for (int i = 1; i <= n; i++)
                parent[i] = i; // initially, each node is its own parent
        }

        // Find representative (root) of set that x belongs to (with path compression)
        int find(int x) {
            if (parent[x] != x)
                parent[x] = find(parent[x]);
            return parent[x];
        }

        // Union two sets; attach smaller tree under larger (by rank)
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
        // Use fast input reading from storage.txt
        BufferedReader br = new BufferedReader(new FileReader("storage.txt"));

        // Read first line: number of storages (N) and links (M)
        String[] firstLine = br.readLine().trim().split(" ");
        int N = Integer.parseInt(firstLine[0]); // total number of storages
        int M = Integer.parseInt(firstLine[1]); // total number of bidirectional links

        // Read second line: initial storage of each file
        String[] secondLine = br.readLine().trim().split(" ");
        int[] storageOfFile = new int[N + 1]; // storageOfFile[i] = storage where file i is currently located
        for (int i = 1; i <= N; i++)
            storageOfFile[i] = Integer.parseInt(secondLine[i - 1]);

        // Read next M lines: each link with (a, b, w)
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < M; i++) {
            String[] parts = br.readLine().trim().split(" ");
            int a = Integer.parseInt(parts[0]); // endpoint 1
            int b = Integer.parseInt(parts[1]); // endpoint 2
            int w = Integer.parseInt(parts[2]); // bandwidth
            edges.add(new Edge(a, b, w)); // store edge info
        }
        br.close(); // done reading input

        // Check if all files are already in their rightful storage
        boolean alreadyCorrect = true;
        for (int i = 1; i <= N; i++) {
            if (storageOfFile[i] != i) {
                alreadyCorrect = false;
                break;
            }
        }

        if (alreadyCorrect) {
            // If no moves needed, output -1 as per problem statement
            PrintWriter pw = new PrintWriter("storageout.txt");
            pw.println(-1);
            pw.close();
            return;
        }

        // Sort all edges in decreasing order of bandwidth (from widest to narrowest)
        edges.sort((e1, e2) -> Integer.compare(e2.w, e1.w));

        // Initialize DSU for N storages
        DSU dsu = new DSU(N);

        // Iterate over edges from highest bandwidth to lowest
        // We'll simulate "activating" links in this order until every file can reach
        // its rightful storage
        for (Edge e : edges) {
            dsu.union(e.a, e.b); // connect the two storages via this link

            boolean allConnected = true;
            // Check if for every file i, its current storage and rightful storage are
            // connected
            for (int i = 1; i <= N; i++) {
                if (dsu.find(storageOfFile[i]) != dsu.find(i)) {
                    allConnected = false;
                    break;
                }
            }

            // Once all files are in the same connected components as their rightful
            // storage,
            // the current edge’s bandwidth is the minimal one in this configuration.
            if (allConnected) {
                PrintWriter pw = new PrintWriter("storageout.txt");
                pw.println(e.w); // output the maximum minimal bandwidth
                pw.close();
                return;
            }
        }

        // Should not happen per the problem guarantee, but just in case:
        PrintWriter pw = new PrintWriter("storageout.txt");
        pw.println(-1);
        pw.close();
    }
}