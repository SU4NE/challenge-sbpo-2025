package org.sbpo2025.challenge;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.apache.commons.lang3.time.StopWatch;

public class SolverInputData {
    public static final long MAX_RUNTIME = 600000;
    private final Random random = new Random();
    private final List<Map<Integer, Integer>> orders;
    private final List<Map<Integer, Integer>> aisles;
    private final CSRMatrix aislesMatrix;
    private final Map<Integer, Integer> stock;
    private final boolean[] validOrders;
    private final int[] ordersSum;
    private final int nItems;
    private final int nOrders;
    private final int nAisles;
    private final int waveSizeLB;
    private final int waveSizeUB;

    public SolverInputData(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems,
            int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nOrders = orders.size();
        this.nAisles = aisles.size();
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        this.validOrders = new boolean[nOrders];
        this.ordersSum = new int[nOrders];
        this.stock = MapUtils.mergeListMap(aisles, nItems);
        this.aislesMatrix = new CSRMatrix(aisles, true, true, nItems);

        processOrders();
    }

    public List<Map<Integer, Integer>> getOrders() {
        return orders;
    }

    public long getRemainingTime(StopWatch stopWatch) {
        long elapsed = stopWatch.getTime(TimeUnit.MILLISECONDS);
        return Math.max(MAX_RUNTIME - elapsed, 0);
    }

    public int getNOrders() {
        return nOrders;
    }

    public int getNAisles() {
        return nAisles;
    }

    public List<Map<Integer, Integer>> getAisles() {
        return aisles;
    }

    public Random getRandom() {
        return random;
    }

    public int getnItems() {
        return nItems;
    }

    public int[] getOrdersSum() {
        return ordersSum;
    }

    public boolean[] getValidOrders() {
        return validOrders;
    }

    public int getWaveSizeLB() {
        return waveSizeLB;
    }

    public int getWaveSizeUB() {
        return waveSizeUB;
    }

    public Map<Integer, Integer> getStock() {
        return stock;
    }

    public CSRMatrix getAislesMatrix() {
        return aislesMatrix;
    }

    /**
     * Processes all orders in parallel to check if they are valid and calculate
     * the total number of units in each order. The validity of an order is
     * determined by checking if the order contains items that are not in stock
     * or if the total number of units in the order exceeds the wave size upper
     * bound.
     *
     * @see #validOrders
     * @see #ordersSum
     */
    private void processOrders() {
        IntStream.range(0, nOrders).parallel().forEach(i -> {
            Map<Integer, Integer> order = orders.get(i);
            boolean isValid = true;
            int sum = 0;

            for (Map.Entry<Integer, Integer> entry : order.entrySet()) {
                int availableStock = stock.getOrDefault(entry.getKey(), 0);
                if (availableStock < entry.getValue() || entry.getValue() > waveSizeUB) {
                    isValid = false;
                    break;
                }
                sum += entry.getValue();
            }

            validOrders[i] = isValid;
            ordersSum[i] = sum;
        });
    }
}