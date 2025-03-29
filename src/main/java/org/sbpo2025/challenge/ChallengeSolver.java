package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;
import org.sbpo2025.challenge.heuristics.iwoa.IWOA;

import java.util.*;

public class ChallengeSolver {
    public final SolverInputData inputData;

    public ChallengeSolver(
            List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB,
            int waveSizeUB) {
        inputData = new SolverInputData(orders, aisles, nItems, waveSizeLB, waveSizeUB);
    }

    public ChallengeSolution solve(StopWatch stopWatch) {
        IWOA iwoa = new IWOA(inputData);
        return iwoa.iwoaSolution(stopWatch);
    }

}
