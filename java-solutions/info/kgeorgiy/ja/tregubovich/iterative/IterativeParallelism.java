package info.kgeorgiy.ja.tregubovich.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.iterative.NewListIP;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.*;

public class IterativeParallelism implements NewListIP, AdvancedIP {

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int[] indices(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return indices(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int[] indices(int threads, List<? extends T> list, Predicate<? super T> predicate, int step) throws InterruptedException {
        return parallelProcessing(threads, list, (idx, cur) -> {
            T el = list.get(idx);
            if (predicate.test(el)) cur.add(idx);
        }, step).stream().mapToInt(idx -> (int) idx).toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return filter(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(int threads, List<? extends T> list, Predicate<? super T> predicate, int step) throws InterruptedException {
        return parallelProcessing(threads, list, (idx, cur) -> {
            T el = list.get(idx);
            if (predicate.test(el)) cur.add(el);
        }, step);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, U> List<U> map(int threads, List<? extends T> list, Function<? super T, ? extends U> function) throws InterruptedException {
        return map(threads, list, function, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(int threads, List<? extends T> list, Function<? super T, ? extends R> function, int step) throws InterruptedException {
        return parallelProcessing(threads, list, (idx, cur) -> cur.add(function.apply(list.get(idx))), step);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T reduce(int threads, List<T> list, T identity, BinaryOperator<T> operator, int step) throws InterruptedException {
        return mapReduce(threads, list, Function.identity(), identity, operator, step);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> R mapReduce(int threads, List<T> list, Function<T, R> lift, R identity, BinaryOperator<R> operator, int step) throws InterruptedException {
        class State {
            R value = identity;
        }
        return parallelReduce(
                threads,
                list.size(),
                State::new,
                (idx, cur) -> cur.value = operator.apply(cur.value, lift.apply(list.get(idx))),
                (l, r) -> {
                    l.value = operator.apply(l.value, r.value);
                    return l;
                },
                step).value;
    }

    private <T, E> List<E> parallelProcessing(int threads, List<? extends T> list, BiConsumer<Integer, List<E>> consumer, int step) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                ArrayList::new,
                consumer,
                //note -- O(n)?
                (a, b) -> {
                    a.addAll(b);
                    return a;
                },
                step
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMax(int threads, List<T> list, Comparator<? super T> comparator) throws InterruptedException {
        return argMax(threads, list, comparator, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMax(int threads, List<T> list, Comparator<? super T> comparator, int step) throws InterruptedException {
        return indexBy(threads, list, (a, b) -> comparator.compare(list.get(b), list.get(a)) > 0, step, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMin(int threads, List<T> list, Comparator<? super T> comparator) throws InterruptedException {
        return argMin(threads, list, comparator, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMin(int threads, List<T> list, Comparator<? super T> comparator, int step) throws InterruptedException {
        return indexBy(threads, list, (a, b) -> comparator.compare(list.get(b), list.get(a)) < 0, step, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int indexOf(int threads, List<T> list, Predicate<? super T> predicate) throws InterruptedException {
        return indexOf(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int indexOf(int threads, List<T> list, Predicate<? super T> predicate, int step) throws InterruptedException {
        return indexBy(threads, list, (a, b) -> a == -1 && (b == -1 || predicate.test(list.get(b))), step, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int lastIndexOf(int threads, List<T> list, Predicate<? super T> predicate) throws InterruptedException {
        return lastIndexOf(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int lastIndexOf(int threads, List<T> list, Predicate<? super T> predicate, int step) throws InterruptedException {
        return indexBy(threads, list, (_, b) -> b != -1 && predicate.test(list.get(b)), step, -1);
    }

    /**
     * {@inheritDoc}
     */
    private <T> int indexBy(int threads, List<T> list, BiPredicate<Integer, Integer> compare, int step, int zeroValue) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                () -> new int[]{zeroValue},
                (i, cur) -> {
                    if (compare.test(cur[0], i)) cur[0] = i;
                },
                (l, r) -> compare.test(l[0], r[0]) ? r : l,
                step
        )[0];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long sumIndices(int threads, List<? extends T> list, Predicate<? super T> predicate) throws InterruptedException {
        return sumIndices(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long sumIndices(int threads, List<? extends T> list, Predicate<? super T> predicate, int step) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                () -> new long[]{0},
                (i, cur) -> {
                    if (predicate.test(list.get(i))) cur[0] += i;
                },
                (l, r) -> new long[]{l[0] + r[0]},
                step
        )[0];
    }

    private <R> R parallelReduce(int threads, int n, Supplier<R> defaultValue, BiConsumer<Integer, R> consumer, BinaryOperator<R> merge, int step)
            throws InterruptedException {
        Objects.requireNonNull(defaultValue);
        Objects.requireNonNull(consumer);
        Objects.requireNonNull(merge);
        if (n == 0) {
            return defaultValue.get();
        }

        int nStep = (n + step - 1) / step;
        int chunkSize = (nStep + threads - 1) / threads;
        threads = Math.min(threads, nStep);
        List<R> ans = new ArrayList<>();
        Thread[] workers = new Thread[threads];
        List<RuntimeException> ex = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            ex.add(null);
            int start = t * chunkSize;
            int finish = Math.min((t + 1) * chunkSize, nStep);
            R cur = defaultValue.get();
            ans.add(cur);
            int curIdx = t;
            workers[t] = new Thread(() -> {
                try {
                    for (int i = start; i < finish; i++) {
                        int k = i * step;
                        consumer.accept(k, cur);
                    }
                } catch (RuntimeException e) {
                    ex.set(curIdx, e);
                }
            });
            workers[t].start();
        }

        for (Thread w : workers) {
            try {
                w.join();
            } catch (InterruptedException interruptedException) {
                RuntimeException finalException = supressEx(ex);
                interruptedException.addSuppressed(finalException);
                for (Thread w1 : workers) {
                    w1.interrupt();
                }
                throw interruptedException;
            }
        }
        RuntimeException finalException = supressEx(ex);
        if (finalException != null) {
            throw finalException;
        }

        R res = ans.getFirst();
        for (int i = 1; i < ans.size(); i++) {
            res = merge.apply(res, ans.get(i));
        }
        return res;
    }

    private static RuntimeException supressEx(List<RuntimeException> ex) {
        RuntimeException finalException = null;
        for (RuntimeException e : ex) {
            if (finalException == null) {
                finalException = e;
            } else if (e != null) {
                finalException.addSuppressed(e);
            }
        }
        return finalException;
    }
}
