package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class Util {
    /**
     * Converts a Set of Integers to a BitSet of the specified size.
     * Each element in the Set is used to set the corresponding bit in the BitSet.
     * If the Set is empty, the BitSet is returned with all bits set to false.
     *
     * @param set  the Set of Integers to convert
     * @param size the size of the resulting BitSet
     * @return a BitSet representing the Set
     */
    public static BitSet convertSetToBitSet(Set<Integer> set, int size) {
        BitSet bitSet = new BitSet(size);
        for (Integer index : set) {
            bitSet.set(index);
        }
        return bitSet;
    }

    /**
     * Mutates a double array by setting elements to 0.5 if their corresponding
     * bit in the input BitSet is set and the element is less than 0.5, or if the
     * bit is not set and the element is greater than or equal to 0.5. The
     * mutated value is then adjusted by adding a random value between 0 and
     * 0.5 if the input BitSet bit is set, or subtracting such a value if the
     * bit is not set. If the input BitSet is null, the input array is returned
     * as is.
     * 
     * @param binaryOrders A BitSet with the same length as the input array.
     * @param inputArray   The double array to be mutated.
     * @param size         The length of the input array.
     * @param random       A Random instance used to generate random numbers.
     *                     If null, no random adjustment is made.
     * @return the mutated double array.
     */
    public static double[] mutateDoubleArrayFromBitSet(BitSet binaryOrders, double[] inputArray, int size,
            Random random) {
        double[] outputArray = inputArray.clone();
        for (int i = 0; i < size; i++) {
            if ((binaryOrders.get(i) && outputArray[i] <= 0.5) || (!binaryOrders.get(i) && outputArray[i] >= 0.5)) {
                outputArray[i] = 0.5;
                if (random != null) {
                    double adjustment = random.nextDouble() * 0.5;
                    outputArray[i] += binaryOrders.get(i) ? adjustment : -adjustment;
                    outputArray[i] = Math.min(Math.max(outputArray[i], 0.0), 1.0);
                }
                continue;
            }
        }
        return outputArray;
    }

    /**
     * Converts a BitSet to a Set of Integers where each element in the Set
     * corresponds to an index of a set bit in the BitSet.
     *
     * @param bitSet the BitSet to convert
     * @return a Set of Integers representing the indices of set bits in the BitSet
     */
    public static Set<Integer> convertBitSetToSet(BitSet bitSet) {
        Set<Integer> set = new HashSet<>();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1))
            set.add(i);
        return set;
    }

    /**
     * Returns a random index i in the given BitSet such that the bit at index i is
     * not set. If no such index exists, returns -1.
     * 
     * @param bitSet the BitSet to search
     * @param random a Random object to use for generating the random index
     * @return a random index i such that the bit at index i is not set, or -1 if no
     *         such index exists
     */
    public static int getRandomClearIndex(BitSet bitSet, int size, Random random) {
        int falseCount = size - bitSet.cardinality();
        int target = random.nextInt(falseCount);
        int count = 0;

        for (int i = bitSet.nextClearBit(0); i < size; i = bitSet.nextClearBit(i + 1)) {
            if (count == target)
                return i;
            count++;
        }
        return -1;
    }

    /**
     * Converts a BitSet to a double array in-place by setting each element of
     * the array to 1.0 if the corresponding bit in the BitSet is set, and 0.0
     * otherwise. The size of the array must be at least as large as the size of
     * the BitSet.
     *
     * @param bitSet the BitSet to convert
     * @param array  the double array to convert into
     * @param size   the size of the double array
     */
    public static void convertBitSetToDoubleArrayInplace(BitSet bitSet, double[] array, int size) {
        for (int i = 0; i < size; i++)
            array[i] = bitSet.get(i) ? 1.0 : 0.0;
    }

    public static BitSet convertDoubleArrayToBitSet(double[] array, int size, double base) {
        BitSet bitSet = new BitSet(size);
        for (int i = 0; i < size; i++)
            bitSet.set(i, array[i] >= base);
        return bitSet;
    }

    /**
     * Converts a double array of the specified size to a BitSet. Each element in
     * the
     * array is interpreted as a boolean value, where 0.0 represents false and any
     * other value represents true. The resulting BitSet is populated with the
     * corresponding boolean values from the array.
     * 
     * @param array the double array to convert
     * @param size  the size of the double array
     * @return a BitSet representing the array
     */
    public static BitSet convertDoubleArrayToBitSet(double[] array, int size) {
        return convertDoubleArrayToBitSet(array, size, 0.5);
    }

    /**
     * Converts a BitSet to a double array of the specified size.
     * Each element in the resulting array is set to 1.0 if the corresponding
     * bit in the BitSet is set, and 0.0 otherwise.
     *
     * @param bitSet the BitSet to convert
     * @param size   the size of the resulting double array
     * @return a double array representing the BitSet
     */
    public static double[] convertBitSetToDoubleArray(BitSet bitSet, int size) {
        double[] array = new double[size];
        for (int i = 0; i < size; i++)
            array[i] = bitSet.get(i) ? 1.0 : 0.0;
        return array;
    }

    /**
     * Transposes a list of maps into a list of maps, where each inner map
     * contains the keys from the outer list and the values from the inner
     * list. The number of items in the inner list is given by nItems. The
     * result is a list of nItems maps, where each map has m keys, where m is
     * the size of the outer list.
     * 
     * @param listMaps The list of maps to be transposed.
     * @param nItems   The number of items in the inner list.
     * @return A list of nItems maps, where each map has m keys.
     */
    public static List<Map<Integer, Integer>> transposeListMap(List<Map<Integer, Integer>> listMaps, int nItems) {
        List<Map<Integer, Integer>> transposed = new ArrayList<>(nItems);
        int m = listMaps.size();

        for (int i = 0; i < nItems; i++) {
            transposed.add(new HashMap<>());
            for (int j = 0; j < m; j++) {
                Map<Integer, Integer> map = listMaps.get(j);
                if (map.containsKey(i))
                    transposed.get(i).put(j, map.get(i));
            }
        }

        return transposed;
    }

    /**
     * Checks if the total number of units picked is within the bounds specified
     * in the input data.
     *
     * @param inputData        The input data containing the lower and upper bounds
     *                         for
     *                         the wave size.
     * @param totalUnitsPicked The total number of units that have been picked.
     * @return true if the total units picked is within the specified bounds, false
     *         otherwise.
     */
    public static boolean rangeCheck(SolverInputData inputData, int totalUnitsPicked) {
        return totalUnitsPicked >= inputData.getWaveSizeLB() && totalUnitsPicked <= inputData.getWaveSizeUB();
    }

    /**
     * Checks if a given solution is feasible based on the input data and the
     * solution details.
     * A solution is considered feasible if the total number of units picked is
     * within the bounds
     * specified by the input data, and all required units for selected orders do
     * not exceed the
     * available units in the aisles.
     *
     * @param inputData         The input data containing constraints and available
     *                          stock.
     * @param challengeSolution The solution to be verified for feasibility,
     *                          including selected orders,
     *                          visited aisles, and the total units picked.
     * @return true if the solution is feasible, false otherwise.
     */
    public static boolean isSolutionFeasible(SolverInputData inputData, ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        int totalUnitsPicked = challengeSolution.totalUnitsPicked();

        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty())
            return false;

        return rangeCheck(inputData, totalUnitsPicked)
                && !MapUtils.isMapGreater(challengeSolution.UnitsPicked(), challengeSolution.UnitsAvailable());
    }

    /**
     * Extracts the indices of all set bits in the given BitSet and returns them as
     * a Set of Integers.
     *
     * @param bitSet the BitSet to extract the indices from
     * @return a Set of Integers representing the indices of the set bits in the
     *         given BitSet
     */
    public static Set<Integer> extractIndices(BitSet bitSet) {
        Set<Integer> indices = new HashSet<>();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            indices.add(i);
        }
        return indices;
    }
}
