package com.gathin.tvhome;
import java.util.*;
import java.util.function.Function;
import java.util.function.ToLongFunction;
/** Select newest per publisher/content link before applying per-app limits. Ties retain provider order. */
final class ShelfSelection {
    static <T> List<T> newest(List<T> rows, Function<T,String> key, ToLongFunction<T> time, Set<String> seen) {
        List<T> sorted = new ArrayList<>(rows);
        sorted.sort((a,b) -> Long.compare(time.applyAsLong(b),time.applyAsLong(a)));
        List<T> result = new ArrayList<>();
        for (T row : sorted) if (seen.add(key.apply(row))) result.add(row);
        return result;
    }
    private ShelfSelection() {}
}
