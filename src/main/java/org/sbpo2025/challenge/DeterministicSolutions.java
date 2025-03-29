package org.sbpo2025.challenge;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.sbpo2025.challenge.heuristics.HeuristicUtils;

public class DeterministicSolutions {
    /**
     * Given a SolverInputData, return a ChallengeSolution that is the best one
     * in terms of the number of units picked, using a deterministic algorithm.
     * The algorithm works by iterating over the orders in decreasing order of
     * the total number of units in the order, and for each order, adding it to
     * the solution if it does not make the total number of units picked exceed
     * the upper bound, and if the number of units picked is less than the lower
     * bound, trying to add aisles to the solution to reach the lower bound.
     * <p>
     * The algorithm uses a HashSet to keep track of the aisles that have already
     * been added to the solution.
     * <p>
     * The algorithm returns the best solution it found, or an empty solution if
     * it did not find any solution.
     *
     * @param inputData the input data
     * @return the best solution found
     */
    public static ChallengeSolution decreasingTotalSolution(SolverInputData inputData) {
        Integer[] ordersIndices = IntStream.range(0, inputData.getNOrders())
                .boxed()
                .sorted(Comparator.comparingInt(i -> -inputData.getOrdersSum()[i]))
                .toArray(Integer[]::new);

        // Create a set of all aisles indices
        HashSet<Integer> aislesIndices = IntStream.range(0, inputData.getAisles().size())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));

        // Start with an empty solution
        ChallengeSolution bestSolution = new ChallengeSolution(new HashSet<>(), new HashSet<>(), new HashMap<>(),
                new HashMap<>(), 0);

        // Iterate over the orders in decreasing order of the total number of units
        for (Integer i : ordersIndices) {
            if (!inputData.getValidOrders()[i])
                continue;

            // Calculate the new total number of units picked if we add the order to the
            // solution
            int newTotalUnits = bestSolution.totalUnitsPicked() + inputData.getOrdersSum()[i];

            // If the new total number of units picked is greater than the upper bound, skip
            // it
            if (newTotalUnits > inputData.getWaveSizeUB())
                continue;

            // Create a new solution with the new order added
            Set<Integer> ordersNewSolution = new HashSet<>(bestSolution.orders());
            Set<Integer> aislesNewSolution = new HashSet<>(bestSolution.aisles());
            Map<Integer, Integer> unitsNewPicked = MapUtils.mergeMap(bestSolution.UnitsPicked(),
                    inputData.getOrders().get(i));
            Map<Integer, Integer> unitsNewAvailable = new HashMap<>(bestSolution.UnitsAvailable());

            ordersNewSolution.add(i);

            // If the new total number of units picked is less than the lower bound, try to
            // add aisles to the solution to reach the lower bound
            if (newTotalUnits < inputData.getWaveSizeLB()) {
                if (!MapUtils.isMapGreater(unitsNewPicked, inputData.getStock())) {
                    bestSolution = new ChallengeSolution(ordersNewSolution, aislesNewSolution,
                            unitsNewPicked, unitsNewAvailable, newTotalUnits);
                }
                continue;
            }

            // Calculate the new available units if we add the order to the solution
            Map<Integer, Integer> unitsNewAvailableNew = MapUtils.removeMap(unitsNewPicked, unitsNewAvailable);
            boolean change = unitsNewAvailableNew.size() == 0;

            // If there are new available units, try to add aisles to the solution to reach
            // the lower bound
            if (!change) {
                Set<Integer> newAislesIndices = new HashSet<>();
                for (Integer aisle : unitsNewAvailableNew.keySet()) {
                    List<Integer> validIndices = inputData.getAislesMatrix().getAllItems(aisle, aislesIndices,
                            unitsNewAvailableNew.get(aisle),
                            true);
                    if (validIndices == null) {
                        newAislesIndices = null;
                        break;
                    }
                    newAislesIndices.addAll(validIndices);
                }

                // If there are new aisles indices, add them to the solution
                if (newAislesIndices != null) {
                    change = true;
                    for (Integer j : newAislesIndices) {
                        aislesNewSolution.add(j);
                        unitsNewAvailable = MapUtils.mergeMap(unitsNewAvailable, inputData.getAisles().get(j));
                    }
                }
            }

            // If there was a change in the solution, update the best solution
            if (change) {
                bestSolution = new ChallengeSolution(ordersNewSolution, aislesNewSolution,
                        unitsNewPicked, unitsNewAvailable, newTotalUnits);
                aislesIndices.removeAll(aislesNewSolution);
            }
        }

        return bestSolution;
    }

    /**
     * Determines the best ChallengeSolution using a deterministic algorithm
     * that prioritizes orders with the highest objective value. The objective
     * value is calculated as the ratio of total units in the order to the number
     * of required aisles. The algorithm iterates over the orders sorted in
     * descending order of this objective value, adding orders to the solution
     * if they do not exceed the upper bound of total units picked. If the total
     * units picked is below the lower bound, it attempts to add aisles to satisfy
     * the lower bound requirements.
     * 
     * The function returns the best feasible solution found or an empty solution
     * if none are feasible.
     *
     * @param inputData the input data containing orders, aisles, and constraints
     * @return the best ChallengeSolution found
     */
    public static ChallengeSolution decreasingAffortSolution(SolverInputData inputData) {
        // Create a list of order indices sorted by the objective value in descending
        // order
        Integer[] ordersIndices = IntStream.range(0, inputData.getNOrders())
                .boxed()
                .sorted(Comparator.comparingDouble(i -> -((double) inputData.getOrdersSum()[i]
                        / HeuristicUtils.selectAisles(inputData.getOrders().get(i), inputData).size())))
                .toArray(Integer[]::new);

        // Create a set of all aisles
        HashSet<Integer> aislesIndices = IntStream.range(0, inputData.getAisles().size())
                .boxed()
                .collect(Collectors.toCollection(HashSet::new));

        // Initialize an empty solution
        ChallengeSolution bestSolution = new ChallengeSolution(new HashSet<>(), new HashSet<>(), new HashMap<>(),
                new HashMap<>(), 0);

        // Iterate over orders sorted by the objective value
        for (Integer i : ordersIndices) {
            if (!inputData.getValidOrders()[i])
                continue;

            int newTotalUnits = bestSolution.totalUnitsPicked() + inputData.getOrdersSum()[i];

            if (newTotalUnits > inputData.getWaveSizeUB())
                continue;

            Set<Integer> ordersNewSolution = new HashSet<>(bestSolution.orders());
            Set<Integer> aislesNewSolution = new HashSet<>(bestSolution.aisles());
            Map<Integer, Integer> unitsNewPicked = MapUtils.mergeMap(bestSolution.UnitsPicked(),
                    inputData.getOrders().get(i));
            Map<Integer, Integer> unitsNewAvailable = new HashMap<>(bestSolution.UnitsAvailable());

            ordersNewSolution.add(i);

            if (newTotalUnits < inputData.getWaveSizeLB()) {
                if (!MapUtils.isMapGreater(unitsNewPicked, inputData.getStock())) {
                    bestSolution = new ChallengeSolution(ordersNewSolution, aislesNewSolution,
                            unitsNewPicked, unitsNewAvailable, newTotalUnits);
                }
                continue;
            }

            Map<Integer, Integer> unitsNewAvailableNew = MapUtils.removeMap(unitsNewPicked, unitsNewAvailable);
            boolean change = unitsNewAvailableNew.size() == 0;

            if (!change) {
                Set<Integer> newAislesIndices = new HashSet<>();
                for (Integer aisle : unitsNewAvailableNew.keySet()) {
                    List<Integer> validIndices = inputData.getAislesMatrix().getAllItems(aisle, aislesIndices,
                            unitsNewAvailableNew.get(aisle),
                            true);
                    if (validIndices == null) {
                        newAislesIndices = null;
                        break;
                    }
                    newAislesIndices.addAll(validIndices);
                }

                if (newAislesIndices != null) {
                    change = true;
                    for (Integer j : newAislesIndices) {
                        aislesNewSolution.add(j);
                        unitsNewAvailable = MapUtils.mergeMap(unitsNewAvailable, inputData.getAisles().get(j));
                    }
                }
            }

            if (change) {
                bestSolution = new ChallengeSolution(ordersNewSolution, aislesNewSolution,
                        unitsNewPicked, unitsNewAvailable, newTotalUnits);
                aislesIndices.removeAll(aislesNewSolution);
            }
        }

        return bestSolution;
    }

}
