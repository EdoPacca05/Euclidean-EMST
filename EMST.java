import java.io.*;
import java.util.*;

/**
 * Computes the alpha-constrained Euclidean Minimum Spanning Tree (alpha-EMST)
 * of a set of points in the plane.
 * <p>
 * Given a set D of n points with integer coordinates and a real parameter
 * alpha &gt; 1, the alpha-EMST is a minimum-weight spanning tree of D in which
 * every edge has Euclidean length at most alpha. If no such spanning tree
 * exists, the program reports failure.
 * <p>
 * The algorithm is a variant of Prim's minimum spanning tree strategy: at
 * each step it greedily attaches to the tree the closest not-yet-connected
 * point, and it fails as soon as the closest available point is farther than
 * alpha away (which is correct because a priority queue always extracts
 * points in non-decreasing order of distance from the current tree).
 * <p>
 * To avoid the O(n^2) cost of a naive Prim implementation, candidate points
 * are looked up through a uniform spatial grid whose cell size equals alpha.
 * Since any point within Euclidean distance alpha of a given point must lie
 * in the same cell or in one of its 8 neighboring cells, only the 3x3 block
 * of cells around a point needs to be scanned to find every relevant
 * candidate. This keeps each relaxation step close to O(1) on average,
 * yielding an overall O(n log n) running time for constant alpha.
 * <p>
 * Command-line usage:
 * <pre>
 *     java EMST &lt;path-to-input-file&gt; &lt;alpha&gt;
 * </pre>
 * The input file must contain one point per line, formatted as
 * {@code (x,y)} with integer coordinates.
 * <p>
 * Output:
 * <ul>
 *   <li>{@code FAIL} if no spanning tree with all edges of weight &le; alpha
 *       exists;</li>
 *   <li>otherwise, the total weight of the computed tree, followed
 *       (only when n &le; 10) by the list of its edges, one per line, in the
 *       format {@code (x1,y1)(x2,y2)}.</li>
 * </ul>
 */
public class EMST {

    /**
     * A point in the plane with integer coordinates and a unique identifier.
     * The identifier corresponds to the point's index in the input file and
     * is used to index auxiliary arrays (distances, parents, visited flags).
     */
    static class Point {
        int x, y;
        int id;

        Point(int x, int y, int id) {
            this.x = x;
            this.y = y;
            this.id = id;
        }

        /**
         * Returns the Euclidean distance between this point and {@code other}.
         * Coordinate differences are widened to {@code long} before squaring
         * to avoid integer overflow.
         */
        double distanceTo(Point other) {
            long dx = (long) this.x - other.x;
            long dy = (long) this.y - other.y;
            return Math.sqrt(dx * dx + dy * dy);
        }

        @Override
        public String toString() {
            return "(" + x + "," + y + ")";
        }
    }

    /**
     * An undirected edge of the spanning tree, connecting two points, with
     * its precomputed Euclidean weight.
     */
    static class Edge {
        Point p1, p2;
        double weight;

        Edge(Point p1, Point p2, double weight) {
            this.p1 = p1;
            this.p2 = p2;
            this.weight = weight;
        }

        /** Formats the edge as {@code (x1,y1)(x2,y2)}, as required by the output spec. */
        @Override
        public String toString() {
            return "(" + p1.x + "," + p1.y + ")(" + p2.x + "," + p2.y + ")";
        }
    }

    /**
     * An entry of the priority queue used by Prim's algorithm: a candidate
     * point together with the current best known distance from the growing
     * tree. Entries are ordered by increasing distance.
     */
    static class Node implements Comparable<Node> {
        int pointId;
        double distance;

        Node(int pointId, double distance) {
            this.pointId = pointId;
            this.distance = distance;
        }

        @Override
        public int compareTo(Node other) {
            if (this.distance < other.distance) return -1;
            if (this.distance > other.distance) return 1;
            return 0;
        }
    }

    /**
     * Program entry point.
     *
     * @param args {@code args[0]} is the path to the input file, {@code args[1]}
     *             is the alpha threshold (a real number &gt; 1)
     * @throws IOException if the input file cannot be read
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            return;
        }

        String filename = args[0];
        double alpha = Double.parseDouble(args[1]);

        List<Point> points = readPoints(filename);
        if (points == null || points.isEmpty()) {
            System.out.println("FAIL");
            return;
        }

        List<Edge> mst = computeAlphaEMST(points, alpha);

        if (mst == null) {
            System.out.println("FAIL");
        } else {
            double totalWeight = 0;
            for (int i = 0; i < mst.size(); i++) {
                Edge e = mst.get(i);
                totalWeight += e.weight;
            }

            // Print the total weight as an integer when it is numerically
            // indistinguishable from one (avoids spurious ".0000000001" noise
            // from floating-point summation), otherwise print the full value.
            if (Math.abs(totalWeight - Math.round(totalWeight)) < 1e-9) {
                System.out.println((long) Math.round(totalWeight));
            } else {
                System.out.println(totalWeight);
            }

            // The edge list is only required in the output for small inputs.
            if (points.size() <= 10) {
                for (int i = 0; i < mst.size(); i++) {
                    System.out.println(mst.get(i));
                }
            }
        }
    }

    /**
     * Reads the input points from a text file, one point per line, in the
     * format {@code (x,y)}. Blank lines are skipped. Each point is assigned
     * an identifier equal to its (zero-based) position among the non-blank
     * lines, in reading order.
     *
     * @param filename path to the input file
     * @return the list of parsed points, in file order
     * @throws IOException if the file cannot be opened or read
     */
    static List<Point> readPoints(String filename) throws IOException {
        List<Point> points = new ArrayList<>();
        Scanner scanner = new Scanner(new File(filename));
        int id = 0;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            line = line.replace("(", "").replace(")", "");
            String[] parts = line.split(",");

            int x = Integer.parseInt(parts[0].trim());
            int y = Integer.parseInt(parts[1].trim());
            points.add(new Point(x, y, id++));
        }
        scanner.close();
        return points;
    }

    /**
     * Computes an alpha-EMST of the given points using a grid-accelerated
     * variant of Prim's algorithm.
     * <p>
     * Points are bucketed into square cells of side {@code alpha}, indexed by
     * their grid coordinates. Since any two points at Euclidean distance at
     * most {@code alpha} necessarily lie in the same cell or in adjacent
     * cells, scanning the 3x3 neighborhood of a point's cell is guaranteed to
     * find every point within reach.
     * <p>
     * The algorithm grows a tree from an arbitrary starting point using a
     * priority queue keyed by distance to the current tree (lazy-deletion
     * Prim's algorithm). Because the queue always extracts the globally
     * closest not-yet-connected point first, the very first time a popped
     * point's best known distance exceeds {@code alpha} it is safe to
     * conclude that no valid alpha-EMST exists, and the algorithm returns
     * {@code null} immediately.
     *
     * @param points the input points (must be non-null and non-empty)
     * @param alpha  the maximum allowed edge weight
     * @return the list of edges of the alpha-EMST, in the order they were
     *         added to the tree; an empty list if there is a single point;
     *         or {@code null} if no spanning tree respecting the alpha
     *         constraint exists
     */
    static List<Edge> computeAlphaEMST(List<Point> points, double alpha) {
        int n = points.size();
        if (n == 0) return null;
        if (n == 1) return new ArrayList<>();

        // Spatial index: maps a grid cell ("gx:gy") to the points inside it.
        Map<String, List<Point>> grid = new HashMap<>();

        for (int i = 0; i < points.size(); i++) {
            Point p = points.get(i);

            int gx = (int) (p.x / alpha);
            int gy = (int) (p.y / alpha);
            String cellKey = gx + ":" + gy;

            List<Point> cellList = grid.get(cellKey);
            if (cellList == null) {
                cellList = new ArrayList<>();
                grid.put(cellKey, cellList);
            }
            cellList.add(p);
        }

        List<Edge> mst = new ArrayList<>();
        double[] minDist = new double[n];       // best known distance from the tree to each point
        Arrays.fill(minDist, Double.MAX_VALUE);
        int[] parent = new int[n];               // tree parent achieving minDist, for edge reconstruction
        Arrays.fill(parent, -1);
        boolean[] visited = new boolean[n];      // whether a point has already been added to the tree

        PriorityQueue<Node> pq = new PriorityQueue<>();

        // Start the tree from an arbitrary point (index 0).
        minDist[0] = 0;
        pq.offer(new Node(0, 0));

        int visitedCount = 0;

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            int u = current.pointId;

            // Lazy deletion: skip stale queue entries for already-visited points.
            if (visited[u]) continue;

            // The queue pops points in non-decreasing order of distance to the
            // tree, so if the closest available point is already farther than
            // alpha, every remaining point is too: no valid tree exists.
            if (minDist[u] > alpha && u != 0) {
                return null;
            }

            visited[u] = true;
            visitedCount++;

            if (parent[u] != -1) {
                Point p1 = points.get(parent[u]);
                Point p2 = points.get(u);
                mst.add(new Edge(p1, p2, minDist[u]));
            }

            Point uPoint = points.get(u);

            int uGx = (int) (uPoint.x / alpha);
            int uGy = (int) (uPoint.y / alpha);

            // Relax distances to every unvisited point in the 3x3 block of
            // grid cells around u; this covers all points within distance
            // alpha of u.
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {

                    String neighborKey = (uGx + dx) + ":" + (uGy + dy);
                    List<Point> cellPoints = grid.get(neighborKey);

                    if (cellPoints != null) {
                        for (int i = 0; i < cellPoints.size(); i++) {
                            Point v = cellPoints.get(i);

                            if (visited[v.id]) continue;

                            double dist = uPoint.distanceTo(v);
                            if (dist < minDist[v.id]) {
                                minDist[v.id] = dist;
                                parent[v.id] = u;
                                pq.offer(new Node(v.id, dist));
                            }
                        }
                    }
                }
            }
        }

        // Defensive check: covers the case of a point that never received any
        // candidate edge within alpha (e.g. an isolated outlier far from
        // every other point), which the queue could otherwise empty without
        // ever popping a >alpha distance for it.
        if (visitedCount < n) {
            return null;
        }

        return mst;
    }
}
