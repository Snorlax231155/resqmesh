package com.resqmesh;

import com.resqmesh.network.controller.BenchmarkController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

@SpringBootTest
@ActiveProfiles("test")
class BenchmarkRunTest {

    @Autowired
    private BenchmarkController benchmarkController;

    @Test
    void printBenchmark() {
        Map<String, Object> results = benchmarkController.runBenchmark();
        System.out.println("================ BENCHMARK RESULTS ================");
        System.out.println(results);
        System.out.println("===================================================");
    }
}
