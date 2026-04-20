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
        IntStream.range(0, threads).forEach(t -> workers.set(t, new Thread(() -> {
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
            throw ex.stream().reduce(null, (finalException, e) -> {
                if (finalException == null) {
                    return e;
                } else if (e != null) {
                    finalException.addSuppressed(e);
                }
                return finalException;
            });
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

//=== test96_stepPerformance()
//argMax
//test96_stepPerformance() done in 5ms
//ERROR: Test AdvancedMapperTest.test01_argMax() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test01_argMax(ScalarIPTest.java:26)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test02_argMin() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test02_argMin(ScalarIPTest.java:31)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test03_indexOf() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testS(BaseIPTest.java:132)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test03_indexOf(ScalarIPTest.java:36)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test04_lastIndexOf() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testS(BaseIPTest.java:132)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test04_lastIndexOf(ScalarIPTest.java:41)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test05_sumIndices() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testS(BaseIPTest.java:132)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test05_sumIndices(ScalarIPTest.java:46)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test10_sleepPerformance() failed: >>> AdvancedMapperTest advanced for info.kgeorgiy.ja.tregubovich.iterative.IterativeParallelism / === test10_sleepPerformance() / argMax / Warm up (1/3) / java.lang.IllegalMonitorStateException: current thread is not owner
//
//info.kgeorgiy.java.advanced.base.ContextException: >>> AdvancedMapperTest advanced for info.kgeorgiy.ja.tregubovich.iterative.IterativeParallelism / === test10_sleepPerformance() / argMax / Warm up (1/3) / java.lang.IllegalMonitorStateException: current thread is not owner
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:79)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.context(Context.java:67)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.measure(ScalarIPTest.java:280)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.lambda$speedup$0(ScalarIPTest.java:298)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:75)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.context(Context.java:67)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.speedup(ScalarIPTest.java:293)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.test(ScalarIPTest.java:311)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testPerformance(ScalarIPTest.java:235)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test10_sleepPerformance(ScalarIPTest.java:72)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ListIPTest.test10_sleepPerformance(ListIPTest.java:21)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//Caused by: java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.measureTime(ScalarIPTest.java:326)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.lambda$measure$0(ScalarIPTest.java:284)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:75)
//        ... 13 more
//ERROR: Test AdvancedMapperTest.test11_burnPerformance() failed: >>> AdvancedMapperTest advanced for info.kgeorgiy.ja.tregubovich.iterative.IterativeParallelism / === test11_burnPerformance() / argMax / Warm up (1/3) / java.lang.IllegalMonitorStateException: current thread is not owner
//
//info.kgeorgiy.java.advanced.base.ContextException: >>> AdvancedMapperTest advanced for info.kgeorgiy.ja.tregubovich.iterative.IterativeParallelism / === test11_burnPerformance() / argMax / Warm up (1/3) / java.lang.IllegalMonitorStateException: current thread is not owner
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:79)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.context(Context.java:67)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.measure(ScalarIPTest.java:280)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.lambda$speedup$0(ScalarIPTest.java:298)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:75)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.context(Context.java:67)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.speedup(ScalarIPTest.java:293)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.test(ScalarIPTest.java:311)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testPerformance(ScalarIPTest.java:235)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test11_burnPerformance(ScalarIPTest.java:80)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//Caused by: java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.measureTime(ScalarIPTest.java:326)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.lambda$measure$0(ScalarIPTest.java:284)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:75)
//        ... 12 more
//ERROR: Test AdvancedMapperTest.test12_sleepPerformance() failed: >>> AdvancedMapperTest advanced for info.kgeorgiy.ja.tregubovich.iterative.IterativeParallelism / === test12_sleepPerformance() / filter / Warm up (1/3) / java.lang.IllegalMonitorStateException: current thread is not owner
//
//info.kgeorgiy.java.advanced.base.ContextException: >>> AdvancedMapperTest advanced for info.kgeorgiy.ja.tregubovich.iterative.IterativeParallelism / === test12_sleepPerformance() / filter / Warm up (1/3) / java.lang.IllegalMonitorStateException: current thread is not owner
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:79)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.context(Context.java:67)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.measure(ScalarIPTest.java:280)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.lambda$speedup$0(ScalarIPTest.java:298)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:75)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.context(Context.java:67)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.speedup(ScalarIPTest.java:293)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.test(ScalarIPTest.java:311)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testPerformance(ScalarIPTest.java:235)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ListIPTest.test12_sleepPerformance(ListIPTest.java:30)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//Caused by: java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.measureTime(ScalarIPTest.java:326)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest$PerformanceTest.lambda$measure$0(ScalarIPTest.java:284)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:75)
//        ... 12 more
//ERROR: Test AdvancedMapperTest.test51_indices() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ListIPTest.test51_indices(ListIPTest.java:38)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test52_filter() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ListIPTest.test52_filter(ListIPTest.java:52)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test53_map() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ListIPTest.test53_map(ListIPTest.java:57)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test54_mapMaximum() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ListIPTest.test54_mapMaximum(ListIPTest.java:62)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test61_null() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testExceptionS(BaseIPTest.java:104)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testException(BaseIPTest.java:124)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ListIPTest.test61_null(ListIPTest.java:77)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test62_rethrowAllNPE() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.createInstance(ListMapperTest.java:78)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.createInstance(ListMapperTest.java:21)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testExceptionS(BaseIPTest.java:104)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testException(BaseIPTest.java:124)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.test62_rethrowAllNPE(ListMapperTest.java:29)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.test62_rethrowAllNPE(AdvancedMapperTest.java:27)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test62_rethrowNPE() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testExceptionS(BaseIPTest.java:104)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testException(BaseIPTest.java:124)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ListIPTest.test62_rethrowNPE(ListIPTest.java:88)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test63_rethrowAllIOBE() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.createInstance(ListMapperTest.java:78)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.createInstance(ListMapperTest.java:21)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testExceptionS(BaseIPTest.java:104)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testException(BaseIPTest.java:124)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.test62_rethrowAllNPE(ListMapperTest.java:29)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.test63_rethrowAllIOBE(AdvancedMapperTest.java:32)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test64_rethrowSingleNPE() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.createInstance(ListMapperTest.java:78)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.createInstance(ListMapperTest.java:21)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.testExceptionS(BaseIPTest.java:104)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.test64_rethrowSingleNPE(ListMapperTest.java:51)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.test64_rethrowSingleNPE(AdvancedMapperTest.java:37)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test71_afterClose() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.createInstance(ListMapperTest.java:78)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.ListMapperTest.test71_afterClose(ListMapperTest.java:71)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.test71_afterClose(AdvancedMapperTest.java:42)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test71_reduce() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.AdvancedIPTest.test71_reduce(AdvancedIPTest.java:27)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test72_mapReduceInteger() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.AdvancedIPTest.testMR(AdvancedIPTest.java:37)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.AdvancedIPTest.test72_mapReduceInteger(AdvancedIPTest.java:46)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test72_parallelClose() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.test72_parallelClose(AdvancedMapperTest.java:48)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test73_mapReduceString() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.AdvancedIPTest.testMR(AdvancedIPTest.java:37)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.AdvancedIPTest.test73_mapReduceString(AdvancedIPTest.java:54)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test91_stepArgMax() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test91_stepArgMax(ScalarIPTest.java:88)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test92_stepArgMin() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test92_stepArgMin(ScalarIPTest.java:93)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test93_indexOf() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test93_indexOf(ScalarIPTest.java:98)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test94_lastIndexOf() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test94_lastIndexOf(ScalarIPTest.java:103)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test95_sumIndices() failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:78)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.BaseIPTest.test(BaseIPTest.java:65)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStep(ScalarIPTest.java:190)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test95_sumIndices(ScalarIPTest.java:118)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//ERROR: Test AdvancedMapperTest.test96_stepPerformance() failed: >>> AdvancedMapperTest advanced for info.kgeorgiy.ja.tregubovich.iterative.IterativeParallelism / === test96_stepPerformance() / argMax / java.lang.IllegalMonitorStateException: current thread is not owner
//
//info.kgeorgiy.java.advanced.base.ContextException: >>> AdvancedMapperTest advanced for info.kgeorgiy.ja.tregubovich.iterative.IterativeParallelism / === test96_stepPerformance() / argMax / java.lang.IllegalMonitorStateException: current thread is not owner
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:79)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.context(Context.java:67)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.context(Context.java:50)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.testStepPerformance(ScalarIPTest.java:154)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.test96_stepPerformance(ScalarIPTest.java:145)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//Caused by: java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.instance(TestHelper.java:19)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:60)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.createInstance(AdvancedMapperTest.java:18)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.measureTime(ScalarIPTest.java:326)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.measureTime(ScalarIPTest.java:171)
//at info.kgeorgiy.java.advanced.iterative/info.kgeorgiy.java.advanced.iterative.ScalarIPTest.lambda$testStepPerformance$0(ScalarIPTest.java:156)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.lambda$context$0(Context.java:51)
//at info.kgeorgiy.java.advanced.base/info.kgeorgiy.java.advanced.base.Context.checked(Context.java:75)
//        ... 7 more
//ERROR: Test AdvancedMapperTest.AdvancedMapperTest failed: current thread is not owner
//
//java.lang.IllegalMonitorStateException: current thread is not owner
//at java.base/java.lang.Object.notifyAll(Native Method)
//at info.kgeorgiy.ja.tregubovich.iterative.ParallelMapperImpl.close(ParallelMapperImpl.java:100)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.TestHelper.close(TestHelper.java:39)
//at info.kgeorgiy.java.advanced.mapper/info.kgeorgiy.java.advanced.mapper.AdvancedMapperTest.close(AdvancedMapperTest.java:70)
//at java.base/java.lang.reflect.Method.invoke(Method.java:565)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)
//at java.base/java.util.Collections$UnmodifiableCollection.forEach(Collections.java:1118)
//at java.base/java.util.ArrayList.forEach(ArrayList.java:1604)