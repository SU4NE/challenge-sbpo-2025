package org.sbpo2025.challenge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class CSRMatrix {
    public int[] items;
    public int[] col;
    public int[] row_ptr;

    public CSRMatrix(List<Map<Integer, Integer>> elements, boolean transpose, boolean sort_items, int nItems) {
        if (transpose)
            elements = Util.transposeListMap(elements, nItems);

        List<Integer> itemsList = new ArrayList<>();
        List<Integer> colList = new ArrayList<>();
        List<Integer> rowPtrList = new ArrayList<>();

        rowPtrList.add(0);

        int count = 0;

        for (Map<Integer, Integer> element : elements) {
            List<int[]> tempList = new ArrayList<>();

            for (Map.Entry<Integer, Integer> entry : element.entrySet()) {
                tempList.add(new int[] { entry.getValue(), entry.getKey() });
            }

            if (sort_items) {
                tempList.sort(Comparator.comparingInt(a -> a[0]));
            }

            for (int[] pair : tempList) {
                itemsList.add(pair[0]);
                colList.add(pair[1]);
                count++;
            }
            rowPtrList.add(count);
        }

        this.items = itemsList.stream().mapToInt(Integer::intValue).toArray();
        this.col = colList.stream().mapToInt(Integer::intValue).toArray();
        this.row_ptr = rowPtrList.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Returns all items in the given row that are in the given set, limited by the
     * given total.
     * If the total is not null, it is subtracted by the sum of the items in the
     * result.
     * If the total is 0 or negative, the function returns null.
     * If the decreasing parameter is true, the items are traversed in decreasing
     * order.
     * 
     * @param row        the row index
     * @param contain    the set of items to contain
     * @param total      the total to limit the result by
     * @param decreasing whether to traverse the items in decreasing order
     * @return a list of items, or null if the total is 0 or negative
     */
    public List<Integer> getAllItems(int row, HashSet<Integer> contain, Integer total,
            boolean decreasing) {
        List<Integer> result = new ArrayList<>();

        for (int i = row_ptr[row]; i < row_ptr[row + 1]; i++) {
            int j = i;
            if (decreasing)
                j = row_ptr[row + 1] - 1 - (i - row_ptr[row]);

            if (!contain.contains(col[j]))
                continue;

            if (total != null)
                total -= items[j];

            result.add(col[j]);

            if (total <= 0)
                break;
        }

        if (total != null && total > 0)
            return null;

        return result;
    }
}
