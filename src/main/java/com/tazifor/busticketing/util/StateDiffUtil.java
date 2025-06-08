package com.tazifor.busticketing.util;

import com.tazifor.busticketing.model.BookingState;

import java.lang.reflect.Field;
import java.util.Objects;

public class StateDiffUtil {

    public static String prettyPrintDiff(BookingState oldState, BookingState newState) {
        StringBuilder sb = new StringBuilder();
        Field[] fields = BookingState.class.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object oldVal = field.get(oldState);
                Object newVal = field.get(newState);

                if (!Objects.equals(oldVal, newVal)) {
                    sb.append(String.format(" - %s: [%s] → [%s]%n", field.getName(), toStr(oldVal), toStr(newVal)));
                }
            } catch (IllegalAccessException e) {
                // You might log or ignore
            }
        }

        return sb.toString().isEmpty() ? "(no changes)" : sb.toString();
    }

    private static String toStr(Object val) {
        return val == null ? "null" : val.toString();
    }
}
