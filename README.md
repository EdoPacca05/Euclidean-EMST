# α-EMST — Euclidean Minimum Spanning Tree with a Distance Constraint

A Java implementation of the **α-Euclidean Minimum Spanning Tree** problem: given a set of points in the plane, compute a minimum-weight spanning tree in which every edge has length at most a given threshold **α**, or report that no such tree exists.

This project was developed for the *Dati e Algoritmi 1* course (2025-26) and follows the assignment specification, including exact I/O formatting requirements.

## Problem statement

Let `D = {p1, p2, ..., pn}` be a set of points in the plane with integer coordinates, and no duplicates. The Euclidean distance between two points is:

```
dist(pi, pj) = sqrt((xi - xj)^2 + (yi - yj)^2)
```

A **spanning tree** connects all `n` points using `n - 1` edges. Its weight is the sum of the Euclidean lengths of its edges. Given a real parameter **α > 1**, the goal is to find a spanning tree of minimum total weight in which **every edge has weight ≤ α** (an *α-EMST*), or to determine that no such tree exists.

A natural motivating scenario: `D` represents house locations in a city, and the goal is to design a minimum-cost fiber cable network connecting all houses, where α caps the maximum cable length of a single link to avoid signal degradation.

## Algorithm

The program implements a grid-accelerated variant of **Prim's algorithm**:

1. Grow a tree starting from an arbitrary point.
2. At each step, greedily attach the closest point not yet in the tree.
3. If the closest available point is farther than α, no valid α-EMST exists and the program reports failure.

**Efficiency.** A naive implementation of this strategy takes `O(n^3)` time, and maintaining running distances for all non-tree points brings it down to `O(n^2)`. This implementation goes further, using a **priority queue** together with a **uniform spatial grid** whose cell size equals α:

- Points are bucketed into grid cells of side α.
- Any two points at Euclidean distance ≤ α must lie in the same cell or in one of its 8 neighboring cells, so scanning only the 3×3 block of cells around a point is guaranteed to find every point within reach.
- The priority queue always extracts the globally closest not-yet-connected point next (lazy-deletion Prim's algorithm), so the first time a popped point's best known distance exceeds α, the algorithm can immediately conclude that no valid tree exists.

This keeps each relaxation step close to `O(1)` on average, giving an overall running time of **`O(n log n)`** for constant α — enough to handle inputs with hundreds of thousands of points in well under a minute.

## Project structure

```
.
├── EMST.java          # Main program: parsing, algorithm, and output formatting
├── README.md
└── test-data/         # Sample inputs and reference outputs
    ├── input_n10.txt
    ├── input_n1000.txt
    ├── input_n100000.txt
    ├── output_n10_a3.txt
    ├── output_n10_a4.txt
    ├── output_n1000_a10.txt
    ├── output_n1000_a15.txt
    ├── output_n100000_a30.txt
    └── output_n100000_a40.txt
```

The implementation only relies on the `java.lang`, `java.io`, and `java.util` packages.

## Build

```bash
javac EMST.java
```

## Usage

```bash
java EMST <path-to-input-file> <alpha>
```

- `<path-to-input-file>`: path to a text file listing the points, one per line, formatted as `(x,y)` with integer coordinates.
- `<alpha>`: the maximum allowed edge weight (a real number, α > 1).

### Output format

- If no spanning tree with all edges of weight ≤ α exists, the program prints:
  ```
  FAIL
  ```
- Otherwise, it prints the total weight of the computed tree, followed — **only when the input has at most 10 points** — by the list of tree edges, one per line, formatted as `(x1,y1)(x2,y2)`.

## Example

Input (`test-data/input_n10.txt`):

```
(0,1)
(3,2)
(2,5)
(3,5)
(3,7)
(5,5)
(6,4)
(5,2)
(6,6)
(2,8)
```

Running with α = 4:

```bash
$ java EMST test-data/input_n10.txt 4
16.64
(0,1)(3,2)
(3,2)(5,2)
(5,2)(6,4)
(6,4)(5,5)
(5,5)(6,6)
(5,5)(3,5)
(3,5)(2,5)
(3,5)(3,7)
(3,7)(2,8)
```

Running with the stricter α = 3, no valid spanning tree exists:

```bash
$ java EMST test-data/input_n10.txt 3
FAIL
```

## Testing

The `test-data/` folder contains larger instances (`n = 1000` and `n = 100000` points) together with their expected outputs, used to validate both the correctness and the efficiency of the implementation for constant α on large inputs.

## Authors

Edoardo Paccagnella

## License

This project is provided for educational purposes as part of a university coursework assignment.
