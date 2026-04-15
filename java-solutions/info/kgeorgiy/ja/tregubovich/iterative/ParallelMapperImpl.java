package info.kgeorgiy.ja.tregubovich.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    private final Queue<Runnable> tasks;

    private volatile boolean closed = false;

    /**
     * Constructor for ParallelMapperImpl
     *
     * @param threads number of threads for mapping
     */
    public ParallelMapperImpl(final int threads) {
        workers = new ArrayList<>(Collections.nCopies(threads, null));
        tasks = new LinkedList<>();
         IntStream.range(0, threads).forEach(t-> workers.set(t, new Thread(() -> {
            Runnable task;
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    synchronized (tasks) {
                        while (tasks.isEmpty() && !closed) {
                            tasks.wait();
                        }
                        if (tasks.isEmpty()) {
                            return;
                        }
                        task = tasks.poll();
                    }
                    task.run();
                } catch (InterruptedException _) {
                    break;
                }
            }
        })));
        workers.forEach(Thread::start);
    }

    private static class Counter {
        int remain;

        Counter(final int remain) {
            this.remain = remain;
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> function, final List<? extends T> list) throws InterruptedException {
        final int n = list.size();
        final List<R> res = new ArrayList<>(Collections.nCopies(n, null));
        final Counter cnt = new Counter(list.size());
        final List<RuntimeException> ex = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            final int idx = i;
            synchronized (tasks) {
                if (closed) {
                    throw new IllegalStateException("ParallelMapper was closed");
                }
                tasks.add(() -> {
                    try {
                        res.set(idx, function.apply(list.get(idx)));
                    } catch (final RuntimeException exception) {
                        ex.add(exception);
                    }
                    synchronized (cnt) {
                        cnt.remain--;
                        cnt.notify();
                    }
                });
                tasks.notify();
            }
        }
        synchronized (cnt) {
            while (cnt.remain > 0 && !closed) {
                cnt.wait();
            }
        }
        if (!ex.isEmpty()) {
            RuntimeException exception = null;
            for (final RuntimeException e : ex) {
                if (exception != null) {
                    exception.addSuppressed(e);
                } else {
                    exception = e;
                }
            }
            throw exception;
        }
        if (closed) {
            throw new IllegalStateException("ParallelMapper was closed");
        }
        return res;
    }

    @Override
    public void close() {
        closed = true;
        for (final Thread t : workers) {
            t.interrupt();
        }
        for (final Thread t : workers) {
            try {
                t.join();
            } catch (final InterruptedException ignored) {
            }
        }
    }
}
