package info.kgeorgiy.ja.tregubovich.arrayset;

import java.util.*;

public class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {

    private final Comparator<? super E> comparator;
    private final List<E> elements;

    public ArraySet() {
        this(new ArrayList<>());
    }

    public ArraySet(Collection<E> collection) {
        this(collection, null);
    }

    @SuppressWarnings("unchecked")
    public ArraySet(Collection<E> collection, Comparator<? super E> cmp) {
        this.comparator =
            cmp != null
                ? cmp
                : (Comparator<? super E>) Comparator.naturalOrder();
        List<E> listOfElements = new ArrayList<>(collection);
        listOfElements.sort(comparator);
        elements = new ArrayList<>();
        for (E e : listOfElements) {
            if (
                elements.isEmpty() ||
                comparator.compare(e, elements.getLast()) != 0
            ) {
                elements.addLast(e);
            }
        }
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
        return first();
    }

    @Override
    public E pollLast() {
        return last();
    }

    private abstract class ArraySetIterator implements Iterator<E> {

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

        protected boolean hasPrev() {
            return index > 0;
        }

        protected E prev() {
            return elements.get(--index);
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
            return hasPrev();
        }

        @Override
        public E next() {
            return prev();
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new ForwardIterator(0);
    }

    @Override
    public NavigableSet<E> descendingSet() {
        return new ArraySet<>(elements, comparator.reversed());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new BackwardIterator(size());
    }

    @Override
    public NavigableSet<E> subSet(
        E fromElement,
        boolean fromInclusive,
        E toElement,
        boolean toInclusive
    ) {
        if (comparator.compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException(fromElement + " > " + toElement);
        }
        return headSet(toElement, toInclusive).tailSet(
            fromElement,
            fromInclusive
        );
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        int index = Collections.binarySearch(elements, toElement, comparator);
        if (index < 0) {
            index = -index - 1;
        }
        if (
            inclusive &&
            index < size() &&
            comparator.compare(elements.get(index), toElement) == 0
        ) {
            index++;
        }
        return new ArraySet<>(elements.subList(0, index), comparator);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        int index = Collections.binarySearch(elements, fromElement, comparator);
        if (index < 0) {
            index = -index - 1;
        }
        if (
            !inclusive &&
            index < size() &&
            comparator.compare(elements.get(index), fromElement) == 0
        ) {
            index++;
        }
        return new ArraySet<>(elements.subList(index, size()), comparator);
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

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return Collections.binarySearch(elements, (E) o, comparator) >= 0;
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
    public int size() {
        return elements.size();
    }
}
