package org.sbpo2025.challenge.heuristics.iwoa;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.time.StopWatch;

import org.sbpo2025.challenge.ChallengeSolution;
import org.sbpo2025.challenge.DeterministicSolutions;
import org.sbpo2025.challenge.ILPWaveSolver;
import org.sbpo2025.challenge.SolverInputData;
import org.sbpo2025.challenge.heuristics.HeuristicUtils;
import org.sbpo2025.challenge.heuristics.Individual;

public class IWOA {
    public final SolverInputData inputData;

    public IWOA(SolverInputData inputData) {
        this.inputData = inputData;
    }

    /**
     * Calls {@link #iwoaSolution(StopWatch, int)} with a default population size
     * of 10.
     * 
     * @param stopWatch a stopwatch to track the computation time
     * @return the best solution found
     */
    public ChallengeSolution iwoaSolution(StopWatch stopWatch) {
        return iwoaSolution(stopWatch, 10);
    }

    /**
     * Implements the Improved Whale Optimization Algorithm (IWOA) to find a
     * solution to the challenge. The algorithm works by initializing a population
     * of candidate solutions and then iteratively improving the best solution by
     * applying the IWOA operators. The algorithm terminates when the maximum
     * allowed
     * computation time is reached.
     * 
     * @param stopWatch      a stopwatch to track the computation time
     * @param populationSize the size of the population
     * @return the best solution found
     */
    public ChallengeSolution iwoaSolution(StopWatch stopWatch, int populationSize) {
        Individual[] population = new Individual[populationSize];
        long remainingTime = inputData.getRemainingTime(stopWatch);
        long allocatedTimeForILP = remainingTime / 2;

        for (int i = 0; i < populationSize; i++) {
            switch (i) {
                case 0:
                    ChallengeSolution solution = ILPWaveSolver.solveILP(inputData, allocatedTimeForILP);
                    population[i] = new Individual(inputData, solution.orders(), solution.aisles());
                    break;
                case 3:
                    solution = DeterministicSolutions.decreasingTotalSolution(inputData);
                    population[i] = new Individual(inputData, solution.orders(), solution.aisles());
                    break;

                case 4:
                    solution = DeterministicSolutions.decreasingAffortSolution(inputData);
                    population[i] = new Individual(inputData, solution.orders(), solution.aisles());
                    break;

                default:
                    population[i] = new Individual(inputData, true);
                    break;
            }
        }

        double maxFitness = Double.MIN_VALUE;
        int leaderIndex = 0;
        for (int i = 0; i < populationSize; i++) {
            double currentFitness = population[i].evaluate();
            if (currentFitness > maxFitness) {
                maxFitness = currentFitness;
                leaderIndex = i;
            }
        }

        Individual leader = population[leaderIndex].clone();

        double b = 1;
        while (inputData.getRemainingTime(stopWatch) > 10) {
            double elapsedSec = stopWatch.getTime(TimeUnit.MILLISECONDS);
            double a = 2 - (2 * elapsedSec / SolverInputData.MAX_RUNTIME);

            for (int i = 0; i < populationSize; i++) {
                Individual individual = population[i];
                if (inputData.getRandom().nextBoolean()) {
                    double A = 2 * a * inputData.getRandom().nextDouble() - a;
                    double C = 2 * inputData.getRandom().nextDouble();

                    if (Math.abs(A) < 1) {
                        for (int j = 0; j < inputData.getNOrders(); j++) {
                            double D = Math.abs(C * leader.orders[j] - individual.orders[j]);
                            individual.orders[j] = leader.orders[j] - A * D;
                        }
                        for (int j = 0; j < inputData.getNAisles(); j++) {
                            double D = Math.abs(C * leader.aisles[j] - individual.aisles[j]);
                            individual.aisles[j] = leader.aisles[j] - A * D;
                        }
                    } else {
                        Individual candidate = population[inputData.getRandom().nextInt(0, populationSize)];

                        for (int j = 0; j < inputData.getNOrders(); j++) {
                            double D = Math.abs(C * candidate.orders[j] - individual.orders[j]);
                            individual.orders[j] = candidate.orders[j] - A * D;
                        }
                        for (int j = 0; j < inputData.getNAisles(); j++) {
                            double D = Math.abs(C * candidate.aisles[j] - individual.aisles[j]);
                            individual.aisles[j] = candidate.aisles[j] - A * D;
                        }
                    }
                } else {
                    for (int j = 0; j < inputData.getNOrders(); j++) {
                        double D = Math.abs(leader.orders[j] - individual.orders[j]);
                        double l = inputData.getRandom().nextDouble(-1, 1.1);
                        l = l > 1 ? 1 : l;
                        individual.orders[j] = D * Math.exp(b * l) * Math.cos(2 * Math.PI * l) + leader.orders[j];
                    }
                    for (int j = 0; j < inputData.getNAisles(); j++) {
                        double D = Math.abs(leader.aisles[j] - individual.aisles[j]);
                        double l = inputData.getRandom().nextDouble(-1, 1.1);
                        l = l > 1 ? 1 : l;
                        individual.aisles[j] = D * Math.exp(b * l) * Math.cos(2 * Math.PI * l) + leader.aisles[j];
                    }
                }

                individual.clip();
                HeuristicUtils.repairSolution(individual, inputData);
                double currentFitness = individual.evaluate();
                if (currentFitness > maxFitness) {
                    maxFitness = currentFitness;
                    leader = individual.clone();
                }
            }
        }

        return leader.toChallengeSolution();
    }
}
