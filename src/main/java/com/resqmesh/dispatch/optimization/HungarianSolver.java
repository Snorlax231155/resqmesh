package com.resqmesh.dispatch.optimization;

import java.util.Arrays;

public class HungarianSolver {

    public int[] solve(double[][] costMatrix) {
        int n = costMatrix.length;
        if (n == 0) return new int[0];
        int m = costMatrix[0].length;
        
        int size = Math.max(n, m);
        double[][] cost = new double[size + 1][size + 1];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i < n && j < m) {
                    cost[i + 1][j + 1] = costMatrix[i][j];
                } else {
                    cost[i + 1][j + 1] = 0.0;
                }
            }
        }

        double[] u = new double[size + 1];
        double[] v = new double[size + 1];
        int[] p = new int[size + 1];
        int[] way = new int[size + 1];

        for (int i = 1; i <= size; i++) {
            p[0] = i;
            int j0 = 0;
            double[] minv = new double[size + 1];
            Arrays.fill(minv, Double.POSITIVE_INFINITY);
            boolean[] used = new boolean[size + 1];

            do {
                used[j0] = true;
                int i0 = p[j0], j1 = 0;
                double delta = Double.POSITIVE_INFINITY;

                for (int j = 1; j <= size; j++) {
                    if (!used[j]) {
                        double cur = cost[i0][j] - u[i0] - v[j];
                        if (cur < minv[j]) {
                            minv[j] = cur;
                            way[j] = j0;
                        }
                        if (minv[j] < delta) {
                            delta = minv[j];
                            j1 = j;
                        }
                    }
                }

                for (int j = 0; j <= size; j++) {
                    if (used[j]) {
                        u[p[j]] += delta;
                        v[j] -= delta;
                    } else {
                        minv[j] -= delta;
                    }
                }
                j0 = j1;
            } while (p[j0] != 0);

            do {
                int j1 = way[j0];
                p[j0] = p[j1];
                j0 = j1;
            } while (j0 != 0);
        }

        int[] result = new int[n];
        for (int j = 1; j <= size; j++) {
            if (p[j] != 0 && p[j] <= n) {
                result[p[j] - 1] = j - 1;
            }
        }
        
        return result;
    }
}
