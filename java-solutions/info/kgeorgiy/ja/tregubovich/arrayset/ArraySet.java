package info.kgeorgiy.ja.tregubovich.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E>, List<E> {

    private final Comparator<? super E> comparator;
    private final List<E> elements;

    public ArraySet() {
        this(new ArrayList<>());
    }

    public ArraySet(Collection<E> collection) {
        this(collection, null);
    }

    public ArraySet(Collection<E> collection, Comparator<? super E> cmp) {
        Set<E> distinct = new TreeSet<>(cmp);
        distinct.addAll(collection);
        this.elements = new ArrayList<>(distinct);
        this.comparator = cmp;
    }

    private ArraySet(List<E> collection, Comparator<? super E> cmp) {
        elements = collection;
        comparator = cmp;
    }

    @Override
    public E lower(E t) {
        return less(t, false);
    }

    @Override
    public E floor(E t) {
        return less(t, true);
    }

    private E less(E t, boolean inclusive) {
        try {
            return headSet(t, inclusive).last();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public E ceiling(E t) {
        return greater(t, true);
    }

    @Override
    public E higher(E t) {
        return greater(t, false);
    }

    private E greater(E t, boolean inclusive) {
        try {
            return tailSet(t, inclusive).first();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    @Override
    public E pollFirst() {
        error();
        return null;
    }

    @Override
    public E pollLast() {
        error();
        return null;
    }

    private class ArraySetIterator implements ListIterator<E> {
        private int index;

        public ArraySetIterator(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return index < size();
        }

        @Override
        public E next() {
            return elements.get(index++);
        }

        @Override
        public boolean hasPrevious() {
            return index > 0;
        }

        @Override
        public E previous() {
            return elements.get(--index);
        }

        @Override
        public int nextIndex() {
            return index + 1;
        }

        @Override
        public int previousIndex() {
            return index - 1;
        }

        @Override
        public void remove() {
            error();
        }

        @Override
        public void set(E e) {
            error();
        }

        @Override
        public void add(E e) {
            error();
        }
    }

    private class ForwardIterator extends ArraySetIterator {
        public ForwardIterator(int index) {
            super(index);
        }
    }

    private class BackwardIterator extends ArraySetIterator {
        public BackwardIterator(int index) {
            super(index);
        }

        @Override
        public boolean hasNext() {
            return hasPrevious();
        }

        @Override
        public E next() {
            return previous();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new ForwardIterator(0);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(elements.reversed(), Collections.reverseOrder(comparator));
    }

    @Override
    public E get(int index) {
        return elements.get(index);
    }

    @Override
    public E set(int index, E element) {
        return null;
    }

    @Override
    public void add(int index, E element) {
    }

    @Override
    public E remove(int index) {
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int indexOf(Object o) {
        int ind = Collections.binarySearch(elements, (E) o, comparator);
        if (ind < 0) {
            return -1;
        }
        return ind;
    }

    @Override
    public int lastIndexOf(Object o) {
        return indexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return new ArraySetIterator(0);
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return new ArraySetIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return elements.subList(fromIndex, toIndex);
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new BackwardIterator(size());
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException(fromElement + " > " + toElement);
        }
        return headSet(toElement, toInclusive).tailSet(
                fromElement,
                fromInclusive
        );
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int index = getIndex(toElement, inclusive);
        return new ArraySet<>(elements.subList(0, index), comparator);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        int index = getIndex(fromElement, !inclusive);
        return new ArraySet<>(elements.subList(index, size()), comparator);
    }

    private int getIndex(E toElement, boolean inclusive) {
        int index = Collections.binarySearch(elements, toElement, comparator);
        if (index < 0) {
            index = -index - 1;
        }
        if (inclusive && index < size() && compare(elements.get(index), toElement) == 0) {
            index++;
        }
        return index;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E removeFirst() {
        return NavigableSet.super.removeFirst();
    }

    @Override
    public E removeLast() {
        return NavigableSet.super.removeLast();
    }

    @Override
    public ArraySet<E> reversed() {
        return (ArraySet<E>) descendingSet();
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @Override
    public E first() {
        return elements.getFirst();
    }

    @Override
    public E last() {
        return elements.getLast();
    }

    @Override
    public Spliterator<E> spliterator() {
        return NavigableSet.super.spliterator();
    }

    @Override
    public void addFirst(E e) {
        NavigableSet.super.addFirst(e);
    }

    @Override
    public void addLast(E e) {
        NavigableSet.super.addLast(e);
    }

    @Override
    public E getFirst() {
        return NavigableSet.super.getFirst();
    }

    @Override
    public E getLast() {
        return NavigableSet.super.getLast();
    }

    @Override
    public int size() {
        return elements.size();
    }

    @SuppressWarnings("unchecked")
    private int compare(E e1, E e2) {
        if (comparator != null) {
            return comparator.compare(e1, e2);
        } else {
            return ((Comparable<E>) e1).compareTo(e2);
        }
    }

    @Override
    public boolean add(E e) {
        error();
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        error();
        return false;
    }

    private void error() {
        throw new UnsupportedOperationException("ArraySet is immutable");
    }
}
