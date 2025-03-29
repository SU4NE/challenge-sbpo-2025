package org.sbpo2025.challenge;

import java.util.Set;
import java.util.Map;

public record ChallengeSolution(
        Set<Integer> orders,
        Set<Integer> aisles,
        Map<Integer, Integer> UnitsPicked, // quantidade de cada item somando todos os pedidos
        Map<Integer, Integer> UnitsAvailable, // quantidade de cada item somando todos os corredores da solução
        int totalUnitsPicked) {

    /**
     * Computes the objective function of the solution, which is the total number
     * of units picked divided by the number of visited aisles.
     *
     * @return The objective function value.
     */
    public double computeObjectiveFunction() {
        int totalUnitsPicked = this.totalUnitsPicked();
        int numVisitedAisles = this.aisles().size();

        if (numVisitedAisles == 0)
            return 0.0;

        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
