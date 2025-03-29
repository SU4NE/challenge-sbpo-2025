package org.sbpo2025.challenge;

import com.google.ortools.Loader;
import com.google.ortools.linearsolver.MPSolver;
import com.google.ortools.linearsolver.MPVariable;
import com.google.ortools.linearsolver.MPConstraint;
import com.google.ortools.linearsolver.MPObjective;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.sbpo2025.challenge.heuristics.HeuristicUtils;

public class ILPWaveSolver {

    /**
     * Solves the given SolverInputData using an ILP solver and returns the
     * best ChallengeSolution found within the given remainingTime.
     *
     * @param inputData     the input data to solve
     * @param remainingTime the maximum computation time available
     * @return the best ChallengeSolution found
     */
    public static ChallengeSolution solveILP(SolverInputData inputData, long remainingTime) {
        Loader.loadNativeLibraries();

        MPSolver solver = MPSolver.createSolver("CBC_MIXED_INTEGER_PROGRAMMING");
        if (solver == null) {
            return new ChallengeSolution(new HashSet<>(), new HashSet<>(), new HashMap<>(), new HashMap<>(), 0);
        }
        solver.setTimeLimit(remainingTime);
        int nOrders = inputData.getNOrders();
        int nAisles = inputData.getAisles().size();

        MPVariable[] x = new MPVariable[nOrders];
        for (int o = 0; o < nOrders; o++) {
            x[o] = solver.makeIntVar(0, 1, "x_" + o);
        }

        MPVariable[] y = new MPVariable[nAisles];
        for (int a = 0; a < nAisles; a++) {
            y[a] = solver.makeIntVar(0, 1, "y_" + a);
        }

        int[] ordersSum = inputData.getOrdersSum();
        MPConstraint lbConstraint = solver.makeConstraint(inputData.getWaveSizeLB(), Double.POSITIVE_INFINITY, "LB");
        MPConstraint ubConstraint = solver.makeConstraint(-Double.POSITIVE_INFINITY, inputData.getWaveSizeUB(), "UB");
        for (int o = 0; o < nOrders; o++) {
            lbConstraint.setCoefficient(x[o], ordersSum[o]);
            ubConstraint.setCoefficient(x[o], ordersSum[o]);
        }

        int nItems = inputData.getnItems();
        for (int i = 0; i < nItems; i++) {
            MPConstraint itemConstraint = solver.makeConstraint(Double.NEGATIVE_INFINITY, 0, "item_" + i);
            for (int o = 0; o < nOrders; o++) {
                int uoi = inputData.getOrders().get(o).getOrDefault(i, 0);
                itemConstraint.setCoefficient(x[o], uoi);
            }
            for (int a = 0; a < nAisles; a++) {
                int uai = inputData.getAisles().get(a).getOrDefault(i, 0);
                itemConstraint.setCoefficient(y[a], -uai);
            }
        }

        double M = 1e-3;
        MPObjective objective = solver.objective();
        for (int o = 0; o < nOrders; o++) {
            objective.setCoefficient(x[o], ordersSum[o]);
        }
        for (int a = 0; a < nAisles; a++) {
            objective.setCoefficient(y[a], -M);
        }
        objective.setMaximization();

        MPSolver.ResultStatus resultStatus = solver.solve();
        if (resultStatus != MPSolver.ResultStatus.OPTIMAL && resultStatus != MPSolver.ResultStatus.FEASIBLE) {
            return new ChallengeSolution(new HashSet<>(), new HashSet<>(), new HashMap<>(), new HashMap<>(), 0);
        }

        Set<Integer> ordersSelected = new HashSet<>();
        for (int o = 0; o < nOrders; o++) {
            if (x[o].solutionValue() > 0.5) {
                ordersSelected.add(o);
            }
        }
        Set<Integer> aislesSelected = new HashSet<>();
        for (int a = 0; a < nAisles; a++) {
            if (y[a].solutionValue() > 0.5) {
                aislesSelected.add(a);
            }
        }

        Map<Integer, Integer> unitsPicked = HeuristicUtils.computeRequired(
                Util.convertSetToBitSet(ordersSelected, nOrders), inputData.getOrders());
        Map<Integer, Integer> unitsAvailable = HeuristicUtils.computeRequired(
                Util.convertSetToBitSet(aislesSelected, nAisles), inputData.getAisles());
        int totalUnitsPicked = ordersSelected.stream().mapToInt(o -> ordersSum[o]).sum();

        return new ChallengeSolution(ordersSelected, aislesSelected, unitsPicked, unitsAvailable, totalUnitsPicked);
    }
}
