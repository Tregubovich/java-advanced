package info.kgeorgiy.ja.tregubovich.iterative;

import info.kgeorgiy.java.advanced.iterative.AdvancedIP;
import info.kgeorgiy.java.advanced.iterative.NewListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IterativeParallelism implements NewListIP, AdvancedIP {
    private ParallelMapper mapper;

    public IterativeParallelism() {

    }

    public IterativeParallelism(final ParallelMapper mapper) {
        this.mapper = mapper;
    }


    @Override
    public <T> int[] indices(final int threads, final List<? extends T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return indices(threads, list, predicate, 1);
    }

    @Override
    public <T> int[] indices(final int threads, final List<? extends T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                step,
                IntStream.empty(),
                indices -> indices.get().filter(i -> predicate.test(list.get(i))),
                IntStream::concat
        ).toArray();
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return filter(threads, list, predicate, 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> filter(final int threads, final List<? extends T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        // :NOTE: unchecked
        return (List<T>) parallelProcess(threads, list, s -> s.filter(predicate), step);
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> list, final Function<? super T, ? extends U> function) throws InterruptedException {
        return map(threads, list, function, 1);
    }

    @Override
    public <T, R> List<R> map(final int threads, final List<? extends T> list, final Function<? super T, ? extends R> function, final int step) throws InterruptedException {
        return parallelProcess(threads, list, s -> s.map(function), step);
    }

    public <T, R> List<R> parallelProcess(final int threads,
            final List<? extends T> list,
            final Function<Stream<T>, Stream<R>> action,
            final int step
    ) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                step,
                new ArrayList<>(),
                indices -> action.apply(indices.get().mapToObj(list::get))
                        .toList(),
                (a, b) -> {
                    a.addAll(b);
                    return a;
                }
        );
    }

    @Override
    public <T> T reduce(final int threads, final List<T> list, final T identity, final BinaryOperator<T> operator, final int step) throws InterruptedException {
        return mapReduce(threads, list, Function.identity(), identity, operator, step);
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> list, final Function<T, R> lift, final R identity, final BinaryOperator<R> operator, final int step) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                step,
                identity,
                (indices) -> indices.get().mapToObj(list::get).map(lift).reduce(identity, operator),
                operator
        );
    }

    @Override
    public <T> int argMax(final int threads, final List<T> list, final Comparator<? super T> comparator) throws InterruptedException {
        return argMax(threads, list, comparator, 1);
    }

    @Override
    public <T> int argMax(final int threads, final List<T> list, final Comparator<? super T> comparator, final int step) throws InterruptedException {
        return indexBy(threads, list, intStream -> intStream.reduce((a, b) ->
                comparator.compare(list.get(b), list.get(a)) > 0 ? b : a
        ).orElse(0), step, 0);
    }

    @Override
    public <T> int argMin(final int threads, final List<T> list, final Comparator<? super T> comparator) throws InterruptedException {
        return argMin(threads, list, comparator, 1);
    }

    @Override
    public <T> int argMin(final int threads, final List<T> list, final Comparator<? super T> comparator, final int step) throws InterruptedException {
        return argMax(threads, list, comparator.reversed(), step);
    }

    @Override
    public <T> int indexOf(final int threads, final List<T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return indexOf(threads, list, predicate, 1);
    }

    @Override
    public <T> int indexOf(final int threads, final List<T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        return indexOf(threads, list, predicate, step, true);
    }

    @Override
    public <T> int lastIndexOf(final int threads, final List<T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return lastIndexOf(threads, list, predicate, 1);
    }

    @Override
    public <T> int lastIndexOf(final int threads, final List<T> list, final Predicate<? super T> predicate, final int step) throws InterruptedException {
        return indexOf(threads, list, predicate, step, false);
    }

    private <T> int indexOf(final int threads, final List<T> list, final Predicate<? super T> predicate, final int step, final boolean isFirst) throws InterruptedException {
        return indexBy(threads, list, intStream -> intStream
                .filter(idx -> idx != -1 && predicate.test(list.get(idx)))
                .reduce((a, b) -> (isFirst ? a : b))
                .orElse(-1), step, -1);
    }

    private <T> int indexBy(final int threads,
                            final List<T> list,
                            final Function<IntStream, Integer> action,
                            final int step,
                            final int zeroValue) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                step,
                zeroValue,
                indices -> action.apply(indices.get()),
                (a, b) -> action.apply(IntStream.of(a, b))
        );
    }

    @Override
    public <T> long sumIndices(final int threads, final List<? extends T> list, final Predicate<? super T> predicate) throws InterruptedException {
        return sumIndices(threads, list, predicate, 1);
    }

    @Override
    public <T> long sumIndices(final int threads,
                               final List<? extends T> list,
                               final Predicate<? super T> predicate,
                               final int step) throws InterruptedException {
        return parallelReduce(
                threads,
                list.size(),
                step,
                0L,
                (indices) -> indices.get().filter(i -> predicate.test(list.get(i))).asLongStream().sum(),
                Long::sum
        );
    }

    private <R> R parallelReduce(int threads,
                                 final int n,
                                 final int step,
                                 final R zeroValue,
                                 final Function<Supplier<IntStream>, R> function,
                                 final BinaryOperator<R> merge)
            throws InterruptedException {
        Objects.requireNonNull(function);
        Objects.requireNonNull(merge);

        if (n == 0) {
            return zeroValue;
        }

        final int nStep = (n + step - 1) / step;
        threads = Math.min(threads, nStep);
        final List<Supplier<IntStream>> streams = getSuppliers(threads, step, nStep);

        final List<R> res;
        if (mapper != null) {
            res = mapper.map(function, streams);
        } else {
            res = map(threads, function, streams);
        }
        return res.stream().reduce(zeroValue, merge);
    }

    private static List<Supplier<IntStream>> getSuppliers(final int threads, final int step, final int nStep) {
        final int chunkSize = nStep / threads;
        final int remaining = nStep % threads;

        final List<Supplier<IntStream>> streams = new ArrayList<>();
        int next = 0;
        for (int t = 0; t < threads; t++) {
            final int size = chunkSize + (t < remaining ? 1 : 0);
            final int start = next;
            streams.add(() -> IntStream.iterate(start * step, i -> i < (start + size) * step, i -> i + step));
            next += size;
        }
        return streams;
    }

    private static <R> List<R> map(final int threads,
                                   final Function<Supplier<IntStream>, R> function,
                                   final List<Supplier<IntStream>> streams) throws InterruptedException {
        final List<R> res = new ArrayList<>(Collections.nCopies(threads, null));
        final List<Thread> workers = new ArrayList<>(Collections.nCopies(threads, null));
        final List<RuntimeException> ex = new ArrayList<>(Collections.nCopies(threads, null));
        IntStream.range(0, threads)
                .forEach(idx -> workers.set(idx, new Thread(() -> {
                    try {
                        res.set(idx, function.apply(streams.get(idx)));
                    } catch (final RuntimeException e) {
                        ex.set(idx, e);
                    }
                })));
        workers.forEach(Thread::start);

        InterruptedException interruptedException = null;
        for (final Thread w : workers) {
            try {
                w.join();
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
        return ex.stream().reduce(null, (finalException, e) -> {
            if (finalException == null) {
                return e;
            } else if (e != null) {
                finalException.addSuppressed(e);
            }
            return finalException;
        });
    }
}
