package info.kgeorgiy.ja.tregubovich.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.iterative.NewListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class IterativeParallelism implements NewListIP, AdvancedIP {
    // :NOTE: access modifier
    ParallelMapper mapper;

    public IterativeParallelism() {

    }

    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int[] indices(
            final int threads,
            final List<? extends T> list,
            final Predicate<? super T> predicate
    ) throws InterruptedException {
        return indices(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int[] indices(final int threads, final List<? extends T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                IntStream.empty(),
                indices -> indices.filter(i -> predicate.test(list.get(i))),
                IntStream::concat,
                step
        ).toArray();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return filter(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                new ArrayList<>(),
                (indices) -> indices
                        .filter(i -> predicate.test(list.get(i)))
                        .mapToObj(list::get)
                        .collect(Collectors.toCollection(ArrayList::new)),
                // :NOTE: use streams
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
    public <T, U> List<U> map(final int threads, final List<? extends T> list, final Function<? super T, ? extends U> function) throws InterruptedException {
        return map(threads, list, function, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(final int threads, final List<? extends T> list, final Function<? super T, ? extends R> function, final int step) throws InterruptedException {
        final int n = (list.size() + step - 1) / step;
        final List<R> res = new ArrayList<>(Collections.nCopies(n, null));
        parallelReduce(
                threads,
                list.size(),
                null,
                (indices) -> {
                    // :NOTE: + Mapper
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
    public <T> T reduce(final int threads, final List<T> list, final T identity, final BinaryOperator<T> operator, final int step) throws InterruptedException {
        return mapReduce(threads, list, Function.identity(), identity, operator, step);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> R mapReduce(final int threads, final List<T> list, final Function<T, R> lift, final R identity, final BinaryOperator<R> operator, final int step) throws InterruptedException {
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
    public <T> int argMax(final int threads, final List<T> list, final Comparator<? super T> comparator) throws InterruptedException {
        return argMax(threads, list, comparator, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMax(final int threads, final List<T> list, final Comparator<? super T> comparator, final int step) throws InterruptedException {
        return indexBy(threads, list, (a, b) -> comparator.compare(list.get(b), list.get(a)) > 0, step, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMin(final int threads, final List<T> list, final Comparator<? super T> comparator) throws InterruptedException {
        return argMin(threads, list, comparator, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int argMin(final int threads, final List<T> list, final Comparator<? super T> comparator, final int step) throws InterruptedException {
        return indexBy(threads, list, (a, b) -> comparator.compare(list.get(b), list.get(a)) < 0, step, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int indexOf(final int threads, final List<T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return indexOf(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int indexOf(final int threads, final List<T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        return indexBy(threads, list, (a, b) -> a == -1 && (b == -1 || predicate.test(list.get(b))), step, -1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int lastIndexOf(final int threads, final List<T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return lastIndexOf(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> int lastIndexOf(final int threads, final List<T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        return indexBy(threads, list, (_, b) -> b != -1 && predicate.test(list.get(b)), step, -1);
    }

    private <T> int indexBy(final int threads, final List<T> list, final BiPredicate<Integer, Integer> compare, final int step, final int zeroValue) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                zeroValue,
                // :NOTE: simplify
                (indices) -> indices.reduce(zeroValue, (best, i) -> compare.test(best, i) ? i : best),
                (a, b) -> compare.test(a, b) ? b : a,
                step
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long sumIndices(final int threads, final List<? extends T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return sumIndices(threads, list, predicate, 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> long sumIndices(final int threads, final List<? extends T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                0L,
                (indices) -> indices.filter(i -> predicate.test(list.get(i))).asLongStream().sum(),
                Long::sum,
                step
        );
    }

    private <R> R parallelReduce(int threads, final int n, final R zeroValue, final Function<IntStream, R> function, final BinaryOperator<R> merge, final int step)
            throws InterruptedException {
        Objects.requireNonNull(function);
        Objects.requireNonNull(merge);

        if (n == 0) {
            return zeroValue;
        }

        final int nStep = (n + step - 1) / step;
        threads = Math.min(threads, nStep);
        final int chunkSize = nStep / threads;
        final int remaining = nStep % threads;

        // :NOTE: Supplier<IntStream>
        final List<IntStream> streams = new ArrayList<>();
        int next = 0;
        for (int t = 0; t < threads; t++) {
            final int size = chunkSize + (t < remaining ? 1 : 0);
            final int start = next;
            streams.add(IntStream.iterate(start * step, i -> i < (start + size) * step, i -> i + step));
            next += size;
        }

        if (mapper != null) {
            return mapper.map(function, streams).stream().reduce(zeroValue, merge);
        }

        final List<R> res = map(threads, function, streams);
        // :NOTE: copy-paste
        return res.stream().reduce(zeroValue, merge);
    }

    private static <R> List<R> map(
            final int threads,
            final Function<IntStream, R> function,
            final List<IntStream> streams
    ) throws InterruptedException {
        final List<R> res = new ArrayList<>(Collections.nCopies(threads, null));
        // :NOTE: array
        final Thread[] workers = new Thread[threads];
        final List<RuntimeException> ex = new ArrayList<>(Collections.nCopies(threads, null));
        // :NOTE: stream-per-action
        for (int t = 0; t < threads; t++) {
            final int idx = t;
            workers[t] = new Thread(() -> {
                try {
                    res.set(idx, function.apply(streams.get(idx)));
                } catch (final RuntimeException e) {
                    ex.set(idx, e);
                }
            });
            workers[t].start();
        }

        InterruptedException interruptedException = null;
        for (final Thread w : workers) {
            try {
                w.join(); // :NOTE: IE
            } catch (final InterruptedException e) {
                if (interruptedException == null) {
                    interruptedException = e;
                } else {
                    interruptedException.addSuppressed(e);
                }
            }
        }

        final RuntimeException suppressedExceptions = suppressEx(ex);
        if (interruptedException != null) {
            if (suppressedExceptions != null) {
                interruptedException.addSuppressed(suppressedExceptions);
            }
            throw interruptedException;
        }
        if (suppressedExceptions != null) {
            throw suppressedExceptions;
        }
        return res;
    }

    private static RuntimeException suppressEx(final List<RuntimeException> ex) {
        // :NOTE: reduce
        RuntimeException finalException = null;
        for (final RuntimeException e : ex) {
            if (finalException == null) {
                finalException = e;
            } else if (e != null) {
                finalException.addSuppressed(e);
            }
        }
        return finalException;
    }
}
