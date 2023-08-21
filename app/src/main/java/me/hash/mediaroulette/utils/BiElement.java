package me.hash.mediaroulette.utils;

import java.util.Objects;

// Will probably be needed in future
@Deprecated
public class BiElement<T, U> {
    private final T first;
    private final U second;

    public BiElement(T first, U second) {
        this.first = first;
        this.second = second;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BiElement<?, ?> biElement = (BiElement<?, ?>) o;
        return Objects.equals(first, biElement.first) &&
                Objects.equals(second, biElement.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return "BiElement{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}

