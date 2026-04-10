package info.kgeorgiy.ja.tregubovich.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.iterative.NewListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IterativeParallelism implements NewListIP, AdvancedIP {
    ParallelMapper mapper;

    public IterativeParallelism() {

    }

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }


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
        return parallelReduce(
                threads,
                list.size(),
                IntStream.empty(),
                (indices) -> indices.filter(i -> predicate.test(list.get(i))),
                IntStream::concat,
                step).toArray();
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
        return parallelReduce(
                threads,
                list.size(),
                new ArrayList<>(),
                (indices) -> indices
                        .filter(i -> predicate.test(list.get(i)))
                        .mapToObj(list::get)
                        .collect(Collectors.toCollection(ArrayList::new)),
                (a, b) -> {
                    a.addAll(b);
                    return a;
                },
                step);
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
        int n = (list.size() + step - 1) / step;
        List<R> res = new ArrayList<>(Collections.nCopies(n, null));
        parallelReduce(
                threads,
                list.size(),
                null,
                (indices) -> {
                    indices.forEach(i -> res.set(i / step, function.apply(list.get(i))));
                    return null;
                },
                (_, _) -> null,
                step
        );
        return res;
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
        return parallelReduce(
                threads,
                list.size(),
                identity,
                (indices) -> indices.mapToObj(list::get).map(lift).reduce(identity, operator),
                operator,
                step);
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

    private <T> int indexBy(int threads, List<T> list, BiPredicate<Integer, Integer> compare, int step, int zeroValue) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                zeroValue,
                (indices) -> indices.reduce(zeroValue, (best, i) -> compare.test(best, i) ? i : best),
                (a, b) -> compare.test(a, b) ? b : a,
                step
        );
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
                0L,
                (indices) -> indices.filter(i -> predicate.test(list.get(i))).asLongStream().sum(),
                Long::sum,
                step
        );
    }

    private <R> R parallelReduce(int threads, int n, R zeroValue, Function<IntStream, R> function, BinaryOperator<R> merge, int step)
            throws InterruptedException {
        Objects.requireNonNull(function);
        Objects.requireNonNull(merge);

        if (n == 0) {
            return zeroValue;
        }

        int nStep = (n + step - 1) / step;
        threads = Math.min(threads, nStep);
        int chunkSize = nStep / threads;
        int remaining = nStep % threads;

        List<IntStream> streams = new ArrayList<>();
        int next = 0;
        for (int t = 0; t < threads; t++) {
            int size = chunkSize + (t < remaining ? 1 : 0);
            int start = next;
            streams.add(IntStream.iterate(start * step, i -> i < (start + size) * step, i -> i + step));
            next += size;
        }

        if (mapper != null) {
            return mapper.map(function, streams).stream().reduce(zeroValue, merge);
        }

        List<R> res = new ArrayList<>(Collections.nCopies(threads, null));
        Thread[] workers = new Thread[threads];
        List<RuntimeException> ex = new ArrayList<>(Collections.nCopies(threads, null));
        for (int t = 0; t < threads; t++) {
            int idx = t;
            workers[t] = new Thread(() -> {
                try {
                    res.set(idx, function.apply(streams.get(idx)));
                } catch (RuntimeException e) {
                    ex.set(idx, e);
                }
            });
            workers[t].start();
        }

        InterruptedException interruptedException = null;
        for (Thread w : workers) {
            try {
                w.join();
            } catch (InterruptedException e) {
                if (interruptedException == null) {
                    interruptedException = e;
                } else {
                    interruptedException.addSuppressed(e);
                }
            }
        }

        RuntimeException suppressedExceptions = suppressEx(ex);
        if (interruptedException != null) {
            if (suppressedExceptions != null) {
                interruptedException.addSuppressed(suppressedExceptions);
            }
            throw interruptedException;
        }
        if (suppressedExceptions != null) {
            throw suppressedExceptions;
        }
        return res.stream().reduce(zeroValue, merge);
    }

    private static RuntimeException suppressEx(List<RuntimeException> ex) {
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
