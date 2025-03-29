package org.sbpo2025.challenge.heuristics;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sbpo2025.challenge.ChallengeSolution;
import org.sbpo2025.challenge.SolverInputData;
import org.sbpo2025.challenge.Util;

public class Individual implements Cloneable {
    public final SolverInputData inputData;
    public double[] orders;
    public double[] aisles;

    public Individual(SolverInputData inputData) {
        this.inputData = inputData;
        this.orders = new double[inputData.getNOrders()];
        this.aisles = new double[inputData.getNAisles()];
    }

    public Individual(SolverInputData inputData, boolean random) {
        this(inputData);
        if (random) {
            for (int i = 0; i < orders.length; i++)
                orders[i] = inputData.getRandom().nextDouble();
            for (int i = 0; i < aisles.length; i++)
                aisles[i] = inputData.getRandom().nextDouble();
        }
    }

    public Individual(SolverInputData inputData, Set<Integer> orders, Set<Integer> aisles) {
        this(inputData, false);
        orders.forEach(i -> this.orders[i] = 1);
        aisles.forEach(i -> this.aisles[i] = 1);
    }

    /**
     * Converts the orders and aisles to binary BitSets and returns them in an
     * array of size 2. The first element of the array is a BitSet representing
     * the orders, and the second element is a BitSet representing the aisles.
     *
     * @return a 2-element array of BitSets, where the first element is the orders
     *         and the second element is the aisles
     */
    public BitSet[] getBinary() {
        BitSet[] binary = new BitSet[2];
        binary[0] = Util.convertDoubleArrayToBitSet(orders, inputData.getNOrders());
        binary[1] = Util.convertDoubleArrayToBitSet(aisles, inputData.getNAisles());
        return binary;
    }

    /**
     * Evaluates the objective function for this individual.
     *
     * @return the objective function value
     */
    public double evaluate() {
        BitSet[] binary = getBinary();
        return HeuristicUtils.objective_function(binary[0], binary[1], inputData);
    }

    /**
     * Clips the values of orders and aisles to be within the range [0, 1].
     * This is useful for ensuring that the values are valid for the
     * toChallengeSolution() method.
     */
    public void clip() {
        for (int i = 0; i < orders.length; i++)
            orders[i] = Math.min(Math.max(orders[i], 0), 1);
        for (int i = 0; i < aisles.length; i++)
            aisles[i] = Math.min(Math.max(aisles[i], 0), 1);
    }

    /**
     * Converts the Individual to a ChallengeSolution.
     * 
     * @return a ChallengeSolution with the orders and aisles selected by the
     *         Individual, and the corresponding total number of units picked.
     */
    public ChallengeSolution toChallengeSolution() {
        BitSet[] binary = getBinary();

        Set<Integer> orders = Util.extractIndices(binary[0]);
        Set<Integer> aisles = Util.extractIndices(binary[1]);

        Map<Integer, Integer> unitsPicked = HeuristicUtils.computeRequired(binary[0], inputData.getOrders());
        Map<Integer, Integer> unitsAvailable = HeuristicUtils.computeRequired(binary[1], inputData.getAisles());

        int totalUnitsPicked = orders.stream().mapToInt(i -> inputData.getOrdersSum()[i]).sum();

        return new ChallengeSolution(orders, aisles, unitsPicked, unitsAvailable, totalUnitsPicked);
    }

    /**
     * Clones the Individual and its arrays, to ensure that the clones are 
     * independent of the original Individual.
     * 
     * @return a new Individual, which is a clone of this Individual.
     */
    @Override
    public Individual clone() {
        try {
            Individual cloned = (Individual) super.clone();

            cloned.orders = this.orders.clone();
            cloned.aisles = this.aisles.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Omg", e);
        }
    }
}
