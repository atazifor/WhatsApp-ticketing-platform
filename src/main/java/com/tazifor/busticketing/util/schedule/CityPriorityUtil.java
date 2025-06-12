package com.tazifor.busticketing.util.schedule;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CityPriorityUtil {
    public static final int DEFAULT_VALUE = 99;

    private static final Map<String, Integer> CITY_PRIORITIES = Map.of(
        "douala", 1,
        "yaounde", 2
        // All other cities get default priority of 99
    );

    public static List<String> sortCitiesByPriority(Collection<String> cities) {
        return cities.stream()
            .sorted(Comparator.comparingInt(city ->
                CITY_PRIORITIES.getOrDefault(city.toLowerCase(), DEFAULT_VALUE)))
            .collect(Collectors.toList());
    }
}
