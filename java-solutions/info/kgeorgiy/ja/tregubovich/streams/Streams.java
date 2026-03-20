package info.kgeorgiy.ja.tregubovich.streams;

import info.kgeorgiy.java.advanced.streams.AdvancedStreams;
import info.kgeorgiy.java.advanced.streams.Trees;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;

public class Streams implements AdvancedStreams {
    private static class TreeSpliterator<N, T> implements Spliterator<T> {
        private final Deque<N> stack = new ArrayDeque<>();
        private final Function<N, T> leafExtractor;
        private final Function<N, List<N>> childrenExtractor;
        private long remaining;
        private final int characteristics;

        TreeSpliterator(N root, Function<N, T> leafExtractor, Function<N, List<N>> childrenExtractor, long size, int characteristics) {
            stack.push(root);
            this.leafExtractor = leafExtractor;
            this.childrenExtractor = childrenExtractor;
            this.remaining = size;
            this.characteristics = characteristics;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            while (!stack.isEmpty()) {
                N node = stack.pop();
                T value = leafExtractor.apply(node);
                if (value != null) {
                    action.accept(value);
                    remaining--;
                    return true;
                }
                List<N> children = childrenExtractor.apply(node);
                for (N child : children.reversed()) {
                    stack.push(child);
                }
            }
            return false;
        }

        @Override
        public Spliterator<T> trySplit() {
            N node = stack.pop();
            return new TreeSpliterator<>(node, leafExtractor, childrenExtractor, Long.MAX_VALUE, characteristics);
        }

        @Override
        public long estimateSize() {
            return remaining;
        }

        @Override
        public int characteristics() {
            return characteristics;
        }
    }

    @Override
    public <T> Spliterator<T> binaryTreeSpliterator(Trees.Binary<T> root) {
        return new TreeSpliterator<>(
                root,
                node -> node instanceof Trees.Leaf<T>(T value) ? value : null,
                node -> (node instanceof Trees.Binary.Branch<T>(
                        Trees.Binary<T> left, Trees.Binary<T> right
                ) ? List.of(left, right) : List.of()),
                Long.MAX_VALUE,
                Spliterator.IMMUTABLE | Spliterator.ORDERED
        );
    }

    @Override
    public <T> Spliterator<T> sizedBinaryTreeSpliterator(Trees.SizedBinary<T> root) {
        return new TreeSpliterator<>(
                root,
                node -> node instanceof Trees.Leaf<T>(T value) ? value : null,
                node -> (node instanceof Trees.SizedBinary.Branch<T> b ? List.of(b.left(), b.right()) : List.of()),
                root.size(),
                Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.ORDERED
        );
    }

    @Override
    public <T> Spliterator<T> naryTreeSpliterator(Trees.Nary<T> root) {
        return new TreeSpliterator<>(
                root,
                node -> node instanceof Trees.Leaf<T>(T value) ? value : null,
                node -> (node instanceof Trees.Nary.Node<T>(List<Trees.Nary<T>> children) ? children : List.of()),
                Long.MAX_VALUE,
                Spliterator.IMMUTABLE | Spliterator.ORDERED
        );
    }

    private static class NestedSpliterator<T> implements Spliterator<T> {
        private final Spliterator<? extends Iterable<T>> spliterator;
        private Iterator<T> current = null;

        NestedSpliterator(Spliterator<? extends Iterable<T>> spliterator) {
            this.spliterator = spliterator;
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            while (true) {
                if (current != null && current.hasNext()) {
                    action.accept(current.next());
                    return true;
                }
                current = null;
                if (!spliterator.tryAdvance(list -> current = list.iterator())) {
                    return false;
                }
            }
        }

        @Override
        public Spliterator<T> trySplit() {
            Spliterator<? extends Iterable<T>> split = spliterator.trySplit();
            return split == null ? null : new NestedSpliterator<>(split);
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return spliterator.characteristics() & ~Spliterator.SIZED & ~Spliterator.SUBSIZED & ~Spliterator.IMMUTABLE;
        }
    }

    @Override
    public <T> Spliterator<T> nestedBinaryTreeSpliterator(Trees.Binary<List<T>> tree) {
        return new NestedSpliterator<>(binaryTreeSpliterator(tree));
    }

    @Override
    public <T> Spliterator<T> nestedSizedBinaryTreeSpliterator(Trees.SizedBinary<List<T>> tree) {
        return new NestedSpliterator<>(sizedBinaryTreeSpliterator(tree));
    }

    @Override
    public <T> Spliterator<T> nestedNaryTreeSpliterator(Trees.Nary<List<T>> tree) {
        return new NestedSpliterator<>(naryTreeSpliterator(tree));
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> first() {
        return Collectors.reducing((l, _) -> l);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> last() {
        return Collectors.reducing((_, r) -> r);
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> middle() {
        return Collector.of(
                ArrayList<T>::new,
                ArrayList::add,
                (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                },
                list -> list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() / 2))
        );
    }

    private static class Prefix {
        StringBuilder sb;

        void extend(CharSequence cs) {
            if (sb == null) {
                sb = new StringBuilder(cs);
            } else {
                int len = Math.min(sb.length(), cs.length());
                for (int i = 0; i < len; i++) {
                    if (sb.charAt(i) != cs.charAt(i)) {
                        sb.setLength(i);
                        return;
                    }
                }
                sb.setLength(len);
            }
        }

        Prefix union(Prefix other) {
            extend(other.sb);
            return this;
        }

        @Override
        public String toString() {
            return sb == null ? "" : sb.toString();
        }
    }

    @Override
    public Collector<CharSequence, ?, String> commonPrefix() {
        return Collector.of(
                Prefix::new,
                Prefix::extend,
                Prefix::union,
                Prefix::toString
        );
    }

    @Override
    public Collector<CharSequence, ?, String> commonSuffix() {
        return Collector.of(
                Prefix::new,
                (p, cs) -> p.extend(new StringBuilder(cs).reverse()),
                Prefix::union,
                p -> new StringBuilder(p.toString()).reverse().toString()
        );
    }

    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringPrefixes() {
        return stringCutter((cs, idx) -> cs.subSequence(0, idx));
    }

    @Override
    public Gatherer<CharSequence, ?, CharSequence> stringSuffixes() {
        return stringCutter((cs, idx) -> cs.subSequence(cs.length() - idx, cs.length()));
    }

    private Gatherer<CharSequence, ?, CharSequence> stringCutter(BiFunction<CharSequence, Integer, CharSequence> cutter) {
        return Gatherer.of(
                Gatherer.Integrator.of((_, input, output) -> {
                    for (int i = 1; i <= input.length(); i++) {
                        if (!output.push(cutter.apply(input, i))) {
                            return false;
                        }
                    }
                    return true;
                })
        );
    }

    @Override
    public <T> Gatherer<T, ?, T> distinctPrefix() {
        return distinctPrefixBy(Function.identity());
    }

    @Override
    public <T> Collector<T, ?, List<T>> head(int k) {
        return listCutter(k, List::removeLast);
    }

    @Override
    public <T> Collector<T, ?, List<T>> tail(int k) {
        return listCutter(k, List::removeFirst);
    }

    private <T> Collector<T, ?, List<T>> listCutter(int k, Function<List<T>, ?> action) {
        return Collector.of(
                LinkedList::new,
                (curTail, el) -> {
                    curTail.add(el);
                    if (curTail.size() > k) {
                        action.apply(curTail);
                    }
                },
                (l, _) -> l,
                Collector.Characteristics.IDENTITY_FINISH
        );
    }

    @Override
    public <T> Collector<T, ?, Optional<T>> kth(int k) {
        class State {
            T value = null;
            int idx = 0;
        }
        return Collector.of(
                State::new,
                (s, t) -> {
                    if (s.idx == k) {
                        s.value = t;
                    }
                    s.idx++;
                },
                (a, b) -> {
                    if (a.idx <= k && a.idx + b.idx > k) {
                        a.value = b.value;
                    }
                    a.idx += b.idx;
                    return a;
                },
                s -> Optional.ofNullable(s.value)
        );
    }

    @Override
    public <T> Gatherer<T, int[], T> nth(int n) {
        return ithOfN(n - 1, n);
    }

    @Override
    public <T> Gatherer<T, int[], T> ithOfN(int i, int n) {
        return Gatherer.of(
                () -> new int[1],
                (state, element, downstream) -> {
                    if (state[0] == i) {
                        if (!downstream.push(element)) {
                            return false;
                        }
                    }
                    state[0] = (state[0] + 1) % n;
                    return true;
                },
                (l, r) -> {
                    l[0] += r[0];
                    return l;
                },
                (_, _) -> {
                }
        );
    }

    @Override
    public <T, K> Gatherer<T, ?, T> distinctPrefixBy(Function<? super T, K> function) {
        return Gatherer.ofSequential(
                LinkedHashSet<K>::new,
                (state, el, downstream) -> {
                    K key = function.apply(el);
                    if (state.add(key)) {
                        downstream.push(el);
                        return true;
                    }
                    return false;
                }
        );
    }

    @Override
    public <T> Collector<T, ?, List<T>> distinctBy(Function<? super T, ?> mapper) {
        return Collectors.collectingAndThen(
                Collectors.toMap(
                        mapper,
                        Function.identity(),
                        (a, _) -> a,
                        LinkedHashMap::new
                ),
                m -> new ArrayList<>(m.values())
        );
    }

    @Override
    public <T> Collector<T, ?, List<T>> minimums(Comparator<? super T> comparator) {
        return Collectors.collectingAndThen(
                Collectors.groupingBy(Function.identity(), () -> new TreeMap<>(comparator), Collectors.toList()),
                m -> m.isEmpty() ? List.of() : m.firstEntry().getValue());
    }

    @Override
    public <T> Collector<T, ?, List<T>> maximums(Comparator<? super T> comparator) {
        return minimums(comparator.reversed());
    }
}
