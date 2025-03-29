package org.sbpo2025.challenge;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapUtils {
    /**
     * Merges all the maps in the given list into a single map, using the sum of
     * the values for each key as the value in the merged map.
     *
     * @param listMaps The list of maps to be merged.
     * @param nItems   The number of items in the maps (used to initialize the
     *                 size of the merged map).
     * @return The merged map.
     */
    public static Map<Integer, Integer> mergeListMap(List<Map<Integer, Integer>> listMaps, int nItems) {
        Map<Integer, Integer> merged = new HashMap<>(nItems);
        for (Map<Integer, Integer> map : listMaps) {
            for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
                merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        return merged;
    }

    /**
     * Merges two maps into a single map. The resulting map contains all the keys
     * from both input maps, with values summed for keys present in both maps.
     *
     * @param map1 The first map to be merged.
     * @param map2 The second map to be merged.
     * @return A new map containing the combined entries from both input maps,
     *         with values summed where keys overlap.
     */
    public static Map<Integer, Integer> mergeMap(Map<Integer, Integer> map1, Map<Integer, Integer> map2) {
        Map<Integer, Integer> merged = new HashMap<>(map1);
        for (Map.Entry<Integer, Integer> entry : map2.entrySet()) {
            merged.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        return merged;
    }

    /**
     * Removes the entries of the second map from the first map by subtracting
     * the values of matching keys. If the resulting value for any key is less
     * than or equal to zero, that key is removed from the result.
     *
     * @param map1 The map from which entries will be subtracted.
     * @param map2 The map whose entries will be subtracted from the first map.
     * @return A new map containing the result of the subtraction, with keys
     *         removed if their resulting values are less than or equal to zero.
     */
    public static Map<Integer, Integer> removeMap(Map<Integer, Integer> map1, Map<Integer, Integer> map2) {
        Map<Integer, Integer> result = new HashMap<>(map1);

        for (Map.Entry<Integer, Integer> entry : map2.entrySet()) {
            result.merge(entry.getKey(), -entry.getValue(), Integer::sum);
        }

        result.values().removeIf(value -> value <= 0);

        return result;
    }

    /**
     * Modifies the target map by adding the entries of the source map to it.
     * The values of matching keys are summed. If a key is present in the source
     * map but not the target map, it is added to the target map with the value
     * from the source map.
     *
     * @param target The map to which the entries of the source map will be added.
     * @param source The map whose entries will be added to the target map.
     */
    public static void mergeMapInPlace(Map<Integer, Integer> target, Map<Integer, Integer> source) {
        for (Map.Entry<Integer, Integer> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    /**
     * Modifies the target map by subtracting the entries of the subtrahend map
     * from it. The values of matching keys are subtracted. If a key is present in
     * the subtrahend map but not the target map, or if the resulting value for
     * any key is less than or equal to zero, that key is removed from the
     * target map.
     *
     * @param target     The map from which the entries of the subtrahend map will
     *                   be
     *                   subtracted.
     * @param subtrahend The map whose entries will be subtracted from the target
     *                   map.
     */
    public static void removeMapInPlace(Map<Integer, Integer> target, Map<Integer, Integer> subtrahend) {
        for (Map.Entry<Integer, Integer> entry : subtrahend.entrySet()) {
            target.merge(entry.getKey(), -entry.getValue(), Integer::sum);
            if (target.get(entry.getKey()) <= 0) {
                target.remove(entry.getKey());
            }
        }
    }

    /**
     * Returns true if the first map is greater than the second map in the sense
     * that for each key in the first map, the value in the first map is greater
     * than the value in the second map. If a key is present in the first map but
     * not the second, the value of that key in the first map is considered to be
     * greater than the value of that key in the second map.
     *
     * @param map1 The first map.
     * @param map2 The second map.
     * @return True if the first map is greater than the second map, false
     *         otherwise.
     */
    public static boolean isMapGreater(Map<Integer, Integer> map1, Map<Integer, Integer> map2) {
        for (Map.Entry<Integer, Integer> entry : map1.entrySet()) {
            if (map2.getOrDefault(entry.getKey(), 0) < entry.getValue())
                return true;
        }
        return false;
    }

    /**
     * Returns true if the given maps have at least one key in common, false
     * otherwise.
     * 
     * @param map1 The first map.
     * @param map2 The second map.
     * @return True if the given maps have at least one key in common, false
     *         otherwise.
     */
    public static boolean hasCommonKey(Map<Integer, Integer> map1, Map<Integer, Integer> map2) {
        for (Integer key : map2.keySet()) {
            if (map1.containsKey(key)) {
                return true;
            }
        }
        return false;
    }
}
