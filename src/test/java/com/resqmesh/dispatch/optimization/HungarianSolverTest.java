package com.resqmesh.dispatch.optimization;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class HungarianSolverTest {

    @Test
    public void testSquareMatrix() {
        double[][] cost = {
                { 82, 83, 69, 92 },
                { 77, 37, 49, 92 },
                { 11, 69, 5, 86 },
                { 8, 9, 98, 23 }
        };

        HungarianSolver solver = new HungarianSolver();
        int[] assignment = solver.solve(cost);
        
        // Expected optimal assignment minimizing cost:
        // Worker 1 (Row 0) -> Task 3 (Col 2) : 69
        // Worker 2 (Row 1) -> Task 2 (Col 1) : 37
        // Worker 3 (Row 2) -> Task 1 (Col 0) : 11
        // Worker 4 (Row 3) -> Task 4 (Col 3) : 23
        // Total cost: 69 + 37 + 11 + 23 = 140
        assertArrayEquals(new int[]{2, 1, 0, 3}, assignment);
    }

    @Test
    public void testAsymmetricMatrixMoreAgents() {
        // 2 missions, 3 agents
        double[][] cost = {
                { 10, 20, 30 },
                { 40, 50, 10 }
        };

        HungarianSolver solver = new HungarianSolver();
        int[] assignment = solver.solve(cost);
        
        // Mission 0 -> Agent 0 (cost 10)
        // Mission 1 -> Agent 2 (cost 10)
        assertArrayEquals(new int[]{0, 2}, assignment);
    }

    @Test
    public void testAsymmetricMatrixMoreMissions() {
        // 3 missions, 2 agents
        double[][] cost = {
                { 10, 50 },
                { 40, 20 },
                { 60, 60 }
        };

        HungarianSolver solver = new HungarianSolver();
        int[] assignment = solver.solve(cost);
        
        // The algorithm pads to 3x3.
        // M0 -> A0 (10)
        // M1 -> A1 (20)
        // M2 -> A2 (dummy, 0)
        // The result returned for M0 is 0, M1 is 1, M2 is 2 (which is out of bounds for agents, so caller needs to ignore if > available agents)
        // Wait, the return array length is `n` (number of missions), and the values are the column indices (agents).
        assertArrayEquals(new int[]{0, 1, 2}, assignment);
    }
}
