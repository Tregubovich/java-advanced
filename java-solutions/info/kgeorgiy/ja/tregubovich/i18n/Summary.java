package info.kgeorgiy.ja.tregubovich.i18n;


import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.ToDoubleFunction;

public class Summary<T> {
    private int amount = 0;
    private final Set<T> distinct = new HashSet<>();

    private T min = null;
    private T max = null;
    private T minLength = null;
    private T maxLength = null;
    private double total = 0;

    private final Comparator<T> comparator;
    private final Comparator<T> lengthComparator;
    private final ToDoubleFunction<T> metricFunction;

    public Summary(final Comparator<T> comparator, final Comparator<T> lengthComparator, final ToDoubleFunction<T> numberFunction) {
        this.comparator = comparator;
        this.lengthComparator = lengthComparator;
        this.metricFunction = numberFunction;
    }

    void addValue(final T value) {
        amount++;
        distinct.add(value);

        min = min == null ? value : comparator.min(min, value);
        max = max == null ? value : comparator.max(max, value);

        minLength = lengthComparator == null ? null : (minLength == null ? value : lengthComparator.min(minLength, value));
        maxLength = lengthComparator == null ? null : (maxLength == null ? value : lengthComparator.max(maxLength, value));

        total += metricFunction.applyAsDouble(value);
    }

    public int getAmount() {
        return amount;
    }

    public int getDistinctAmount() {
        return distinct.size();
    }

    public T getMin() {
        return min;
    }

    public T getMax() {
        return max;
    }

    public T getMinLength() {
        return minLength;
    }

    public T getMaxLength() {
        return maxLength;
    }

    public double getAverage() {
        return total / amount;
    }
}
//
///mnt/repo/java-solutions/info/kgeorgiy/ja/tregubovich/i18n/Summary.java:33: error: cannot find symbol
//        min = min == null ? value : comparator.min(min, value);
//                                              ^
//symbol:   method min(T,T)
//location: variable comparator of type Comparator<T>
//where T is a type-variable:
//T extends Object declared in class Summary
///mnt/repo/java-solutions/info/kgeorgiy/ja/tregubovich/i18n/Summary.java:34: error: cannot find symbol
//        max = max == null ? value : comparator.max(max, value);
//                                              ^
//symbol:   method max(T,T)
//location: variable comparator of type Comparator<T>
//where T is a type-variable:
//T extends Object declared in class Summary
///mnt/repo/java-solutions/info/kgeorgiy/ja/tregubovich/i18n/Summary.java:36: error: cannot find symbol
//        minLength = lengthComparator == null ? null : (minLength == null ? value : lengthComparator.min(minLength, value));
//                                                                                                   ^
//symbol:   method min(T,T)
//location: variable lengthComparator of type Comparator<T>
//where T is a type-variable:
//T extends Object declared in class Summary
///mnt/repo/java-solutions/info/kgeorgiy/ja/tregubovich/i18n/Summary.java:37: error: cannot find symbol
//        maxLength = lengthComparator == null ? null : (maxLength == null ? value : lengthComparator.max(maxLength, value));
//                                                                                                   ^
//symbol:   method max(T,T)
//location: variable lengthComparator of type Comparator<T>
//where T is a type-variable:
//T extends Object declared in class Summary
//4 errors