package com.github.disc99.kuroko.util;

import java.util.function.Function;
import java.util.function.Predicate;


/**
 * Predicates
 */
public final class Predicates {

    /**
     * Converts {@link Function} to {@link Predicate}
     * 
     * @param <T> the type of input
     * @param function {@link Function}
     * @return {@link Predicate}
     */
    public static final <T> Predicate<T> fromFunction(final Function<T, Boolean> function) {
        return input -> {
            Boolean bool = function.apply(input);
            if (bool == null) {
                return false;
            }
            return bool.booleanValue() ? true : false;
        };
    }

    private Predicates() {
    }

}
