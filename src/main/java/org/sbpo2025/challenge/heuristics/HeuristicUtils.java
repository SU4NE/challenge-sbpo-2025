package org.sbpo2025.challenge.heuristics;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.sbpo2025.challenge.CSRMatrix;
import org.sbpo2025.challenge.MapUtils;
import org.sbpo2025.challenge.SolverInputData;
import org.sbpo2025.challenge.Util;

public class HeuristicUtils {

    /**
     * Repairs a given solution by removing orders if the total number of units
     * picked exceeds the upper bound, and adding orders if the total number of
     * units picked is below the lower bound. If the number of units available
     * in the aisles is not sufficient to satisfy the required units picked, it
     * adds aisles to the solution.
     *
     * @param individual the individual to repair
     * @param inputData  the input data
     */
    public static void repairSolution(Individual individual, SolverInputData inputData) {
        BitSet orders = Util.convertDoubleArrayToBitSet(individual.orders, inputData.getNOrders());
        BitSet aisles = Util.convertDoubleArrayToBitSet(individual.aisles, inputData.getNAisles());
        int totalUnitsPicked = recalcTotalUnits(orders, inputData);

        while (totalUnitsPicked > inputData.getWaveSizeUB() && orders.cardinality() > 0) {
            int orderToRemove = orders.nextSetBit(0);
            orders.clear(orderToRemove);
            totalUnitsPicked = recalcTotalUnits(orders, inputData);
        }
        while (totalUnitsPicked < inputData.getWaveSizeLB()) {
            int candidate = Util.getRandomClearIndex(orders, inputData.getNOrders(), inputData.getRandom());
            if (candidate == -1)
                break;
            orders.set(candidate);
            totalUnitsPicked = recalcTotalUnits(orders, inputData);
        }
        if (aisles.cardinality() == 0) {
            aisles.set(inputData.getRandom().nextInt(inputData.getNAisles()));
        }

        Map<Integer, Integer> requiredUnits = computeRequired(orders, inputData.getOrders());
        Map<Integer, Integer> availableUnits = computeRequired(aisles, inputData.getAisles());
        if (MapUtils.isMapGreater(requiredUnits, availableUnits)) {
            Set<Integer> additionalAisles = HeuristicUtils.selectAisles(requiredUnits, inputData,
                    inputData.getRandom().nextBoolean());
            for (Integer a : additionalAisles) {
                aisles.set(a);
            }
        }

        individual.orders = Util.convertBitSetToDoubleArray(orders, inputData.getNOrders());
        individual.aisles = Util.convertBitSetToDoubleArray(aisles, inputData.getNAisles());
    }

    /**
     * Recalculates the total number of units picked given the orders.
     * 
     * @param orders    the orders to consider
     * @param inputData the input data
     * @return the total number of units picked
     */
    private static int recalcTotalUnits(BitSet orders, SolverInputData inputData) {
        int total = 0;
        for (int i = orders.nextSetBit(0); i >= 0; i = orders.nextSetBit(i + 1)) {
            total += inputData.getOrdersSum()[i];
        }
        return total;
    }

    /**
     * Computes the objective function for a given set of orders and aisles.
     * The objective value is determined as the ratio of total units picked to
     * the number of aisles visited, with penalties applied for exceeding or
     * falling short of the wave size bounds, and for insufficient aisle items
     * to fulfill the required orders.
     *
     * @param orders    A BitSet representing the selected orders.
     * @param aisles    A BitSet representing the visited aisles.
     * @param inputData The input data containing orders, aisles, and constraints.
     * @return The calculated objective function value, adjusted for penalties.
     */
    public static double objective_function(BitSet orders, BitSet aisles, SolverInputData inputData) {
        int totalUnitsPicked = 0;
        for (int i = orders.nextSetBit(0); i >= 0; i = orders.nextSetBit(i + 1)) {
            totalUnitsPicked += inputData.getOrdersSum()[i];
        }

        double baseObjective = (aisles.cardinality() > 0) ? (double) totalUnitsPicked / aisles.cardinality() : 0;

        double penalty = 0.0;
        double lambda = 1.0;

        if (totalUnitsPicked > inputData.getWaveSizeUB() || totalUnitsPicked < inputData.getWaveSizeLB()) {
            double media = (inputData.getWaveSizeUB() + inputData.getWaveSizeLB()) / 2.0;
            penalty += lambda * Math.abs(totalUnitsPicked - media);
        }

        if (aisles.cardinality() == 0) {
            penalty += lambda * totalUnitsPicked;
        }

        Map<Integer, Integer> requiredUnits = HeuristicUtils.computeRequired(orders, inputData.getOrders());
        Map<Integer, Integer> unitsAvailableInAisles = HeuristicUtils.computeRequired(aisles, inputData.getAisles());
        if (MapUtils.isMapGreater(requiredUnits, unitsAvailableInAisles)) {
            penalty += lambda * totalUnitsPicked;
        }

        return (penalty == 0) ? baseObjective : baseObjective - penalty;
    }

    /**
     * Computes the total number of units required for each item in the given
     * solution.
     *
     * @param solution the binary solution
     * @return a map where the key is the item index and the value is the total
     *         number of units required
     */
    public static Map<Integer, Integer> computeRequired(BitSet solution, List<Map<Integer, Integer>> listMaps) {
        Map<Integer, Integer> mapSolution = new HashMap<>();
        for (int orderIndex = solution.nextSetBit(0); orderIndex >= 0; orderIndex = solution
                .nextSetBit(orderIndex + 1))
            MapUtils.mergeMapInPlace(mapSolution, listMaps.get(orderIndex));

        return mapSolution;
    }

    /**
     * Given a map of the required units for each item, returns a set of aisle
     * indices that are required to fulfill the demand. The selection of aisles
     * is done randomly, with the probability of each aisle being selected
     * proportional to its total contribution to the fulfillment of the demand.
     *
     * @param requiredUnits the required units for each item
     * @param inputData     the input data containing orders, aisles, and
     *                      constraints
     * @param random        a Random instance used for generating random numbers
     * @return a set of aisle indices that are required to fulfill the demand
     */
    public static Set<Integer> selectAislesRandomized(Map<Integer, Integer> requiredUnits, SolverInputData inputData,
            Random random) {
        Set<Integer> selectedAisles = new HashSet<>();
        Map<Integer, Integer> remainingUnits = new HashMap<>(requiredUnits);
        Set<Integer> availableAisles = IntStream.range(0, inputData.getAisles().size())
                .boxed()
                .collect(Collectors.toSet());

        CSRMatrix matrix = inputData.getAislesMatrix();

        while (!remainingUnits.isEmpty()) {
            Map<Integer, Integer> contributions = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : remainingUnits.entrySet()) {
                int item = entry.getKey();
                int needed = entry.getValue();
                int start = matrix.row_ptr[item];
                int end = matrix.row_ptr[item + 1];
                for (int idx = start; idx < end; idx++) {
                    int aisle = matrix.col[idx];
                    if (!availableAisles.contains(aisle))
                        continue;
                    int quantity = matrix.items[idx];
                    int contribute = Math.min(quantity, needed);
                    contributions.merge(aisle, contribute, Integer::sum);
                }
            }

            int totalContribution = contributions.values().stream().mapToInt(Integer::intValue).sum();
            if (totalContribution == 0)
                break;

            int chosenAisle = weightedRandomSelection(contributions, random);
            if (chosenAisle == -1)
                break;

            selectedAisles.add(chosenAisle);
            availableAisles.remove(chosenAisle);

            Map<Integer, Integer> aisleMap = inputData.getAisles().get(chosenAisle);
            for (Map.Entry<Integer, Integer> e : aisleMap.entrySet()) {
                int item = e.getKey();
                if (remainingUnits.containsKey(item)) {
                    int newNeed = remainingUnits.get(item) - e.getValue();
                    if (newNeed <= 0)
                        remainingUnits.remove(item);
                    else
                        remainingUnits.put(item, newNeed);
                }
            }
        }
        return selectedAisles;
    }

    /**
     * Given a map from objects to their weights, returns an object randomly
     * selected from the map with probability proportional to the weight.
     * If the total weight of all objects is 0, returns -1.
     * 
     * @param contributions the map from objects to weights
     * @param random        the random number generator
     * @return the randomly selected object, or -1 if no object was selected
     */
    private static int weightedRandomSelection(Map<Integer, Integer> contributions, Random random) {
        int total = contributions.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0)
            return -1;
        int r = random.nextInt(total) + 1;
        for (Map.Entry<Integer, Integer> entry : contributions.entrySet()) {
            r -= entry.getValue();
            if (r <= 0)
                return entry.getKey();
        }
        return -1;
    }

    /**
     * Given a map of the required units for each item, returns a set of aisle
     * indices that are required to fulfill the demand. The selection of aisles
     * is done greedily, with each aisle being selected if it provides the
     * highest contribution to the fulfillment of the demand at the current
     * iteration.
     *
     * @param requiredUnits the required units for each item
     * @param inputData     the input data containing orders, aisles, and
     *                      constraints
     * @return a set of aisle indices that are required to fulfill the demand
     */
    public static Set<Integer> selectAisles(Map<Integer, Integer> requiredUnits, SolverInputData inputData) {
        Set<Integer> selectedAisles = new HashSet<>();
        Map<Integer, Integer> remainingUnits = new HashMap<>(requiredUnits);
        Set<Integer> availableAisles = IntStream.range(0, inputData.getAisles().size())
                .boxed()
                .collect(Collectors.toSet());

        CSRMatrix matrix = inputData.getAislesMatrix();

        while (!remainingUnits.isEmpty()) {
            Map<Integer, Integer> contributions = new HashMap<>();
            for (Map.Entry<Integer, Integer> entry : remainingUnits.entrySet()) {
                int item = entry.getKey();
                int needed = entry.getValue();
                int start = matrix.row_ptr[item];
                int end = matrix.row_ptr[item + 1];
                for (int idx = start; idx < end; idx++) {
                    int aisle = matrix.col[idx];
                    if (!availableAisles.contains(aisle))
                        continue;
                    int quantity = matrix.items[idx];
                    int contribute = Math.min(quantity, needed);
                    contributions.merge(aisle, contribute, Integer::sum);
                }
            }

            int bestAisle = -1;
            int bestContribution = 0;
            for (Map.Entry<Integer, Integer> entry : contributions.entrySet()) {
                if (entry.getValue() > bestContribution) {
                    bestContribution = entry.getValue();
                    bestAisle = entry.getKey();
                }
            }

            if (bestAisle == -1 || bestContribution == 0)
                break;

            selectedAisles.add(bestAisle);
            availableAisles.remove(bestAisle);

            Map<Integer, Integer> aisleMap = inputData.getAisles().get(bestAisle);
            for (Map.Entry<Integer, Integer> e : aisleMap.entrySet()) {
                int item = e.getKey();
                if (remainingUnits.containsKey(item)) {
                    int newNeed = remainingUnits.get(item) - e.getValue();
                    if (newNeed <= 0) {
                        remainingUnits.remove(item);
                    } else {
                        remainingUnits.put(item, newNeed);
                    }
                }
            }
        }
        return selectedAisles;
    }

    /**
     * Selects a set of aisles required to fulfill the given units for each item.
     * This method can operate in either deterministic or randomized mode.
     * 
     * @param requiredUnits A map where the key is the item index and the value is
     *                      the number of units required for each item.
     * @param inputData     The input data containing aisles and constraints.
     * @param randomAisles  A boolean flag indicating whether to use a randomized
     *                      approach for aisle selection.
     * @return A set of aisle indices that are required to fulfill the given units.
     */
    public static Set<Integer> selectAisles(Map<Integer, Integer> requiredUnits, SolverInputData inputData,
            boolean randomAisles) {
        if (randomAisles)
            return selectAislesRandomized(requiredUnits, inputData, inputData.getRandom());

        return selectAisles(requiredUnits, inputData);
    }

    /**
     * Given a map of units picked for each item, computes the total units
     * available in the required aisles.
     *
     * @param unitsPicked the units picked for each item
     * @param inputData   the input data
     * @return a map of the total units available in the required aisles for each
     *         item
     */
    public static Map<Integer, Integer> computeUnitsAvailableInAisles(
            Set<Integer> requiredAisleIndices,
            SolverInputData inputData) {
        Map<Integer, Integer> unitsAvailableInAisles = new HashMap<>();
        for (int requiredAisleIndex : requiredAisleIndices) {
            Map<Integer, Integer> aisleUnits = inputData.getAisles().get(requiredAisleIndex);
            unitsAvailableInAisles = MapUtils.mergeMap(unitsAvailableInAisles, aisleUnits);
        }
        return unitsAvailableInAisles;
    }
}
