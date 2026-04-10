package info.kgeorgiy.ja.tregubovich.iterative;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

public class ParallelMapperImpl implements ParallelMapper {
    private final Thread[] workers;
    private final Queue<Runnable> tasks;

    private volatile boolean closed = false;

    /**
     * Constructor for ParallelMapperImpl
     *
     * @param threads number of threads for mapping
     */
    public ParallelMapperImpl(int threads) {
        workers = new Thread[threads];
        tasks = new LinkedList<>();
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(() -> {
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
            });
            workers[t].start();
        }
    }

    private static class Counter {
        int remain;

        Counter(int remain) {
            this.remain = remain;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list) throws InterruptedException {
        int n = list.size();
        List<R> res = new ArrayList<>(Collections.nCopies(n, null));
        Counter cnt = new Counter(list.size());
        final List<RuntimeException> ex = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < n; i++) {
            int idx = i;
            synchronized (tasks) {
                if (closed) {
                    throw new IllegalStateException("ParallelMapper was closed");
                }
                tasks.add(() -> {
                    try {
                        res.set(idx, function.apply(list.get(idx)));
                    } catch (RuntimeException exception) {
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
            for (RuntimeException e : ex) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        closed = true;
        synchronized (tasks) {
            tasks.notifyAll();
        }
        for (Thread t : workers) {
            t.interrupt();
        }
        for (Thread t : workers) {
            try {
                t.join();
            } catch (InterruptedException ignored) {
            }
        }
    }
}
