package info.kgeorgiy.ja.tregubovich.student;

import info.kgeorgiy.java.advanced.student.AdvancedQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.GroupName;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedQuery {
    private static final Comparator<Student> NAME_COMPARATOR = Comparator
            .comparing(Student::lastName, Comparator.reverseOrder())
            .thenComparing(Student::firstName, Comparator.reverseOrder())
            .thenComparingInt(Student::id);

    @Override
    public List<Group> getGroupsByName(final Collection<Student> collection) {
        return getGroupBy(collection, NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(final Collection<Student> collection) {
        return getGroupBy(collection, Comparator.comparingInt(Student::id));
    }

    private List<Group> getGroupBy(final Collection<Student> collection, final Comparator<Student> cmp) {
        return collection
                .stream()
                .collect(Collectors.groupingBy(Student::groupName))
                .entrySet()
                .stream()
                .map(entry -> new Group(
                        entry.getKey(),
                        sortStudentBy(entry.getValue(), cmp)
                ))
                .sorted(Comparator.comparing(Group::name))
                .toList();
    }

    @Override
    public GroupName getLargestGroup(final Collection<Student> collection) {
        return getLargestGroupBy(collection, Collectors.counting(), Long::longValue, Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroupFirstName(final Collection<Student> collection) {
        return getLargestGroupBy(collection, Collectors.mapping(Student::firstName, Collectors.toSet()), Set::size, Comparator.reverseOrder());

    }

    private <T> GroupName getLargestGroupBy(final Collection<Student> collection, final Collector<Student, ?, T> collector, final ToLongFunction<T> extractor, final Comparator<GroupName> groupCmp) {
        return collection.stream()
                .collect(Collectors.groupingBy(Student::groupName, collector))
                .entrySet()
                .stream()
                .max(Comparator
                        .comparingLong((Map.Entry<GroupName, T> e) -> extractor.applyAsLong(e.getValue()))
                        .thenComparing(Map.Entry::getKey, groupCmp))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public List<String> getFirstNames(final List<Student> list) {
        return getField(list, Student::firstName);
    }

    @Override
    public List<String> getLastNames(final List<Student> list) {
        return getField(list, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(final List<Student> list) {
        return getField(list, Student::groupName);
    }

    @Override
    public List<String> getFullNames(final List<Student> list) {
        return getField(list, student -> student.firstName() + " " + student.lastName());
    }

    private <T> List<T> getField(final List<Student> list, final Function<Student, T> extractor) {
        return list.stream().map(extractor).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(final List<Student> list) {
        return list.stream().map(Student::firstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(final List<Student> list) {
        return list.stream()
                .max(Comparator.comparingInt(Student::id))
                .map(Student::firstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(final Collection<Student> collection) {
        return sortStudentBy(collection, Comparator.comparingInt(Student::id));
    }

    @Override
    public List<Student> sortStudentsByName(final Collection<Student> collection) {
        return sortStudentBy(collection, NAME_COMPARATOR);
    }

    private List<Student> sortStudentBy(final Collection<Student> collection, final Comparator<Student> cmp) {
        return collection.stream().sorted(cmp).toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(final Collection<Student> collection, final String s) {
        return findStudentBy(collection, Student::firstName, s);
    }

    @Override
    public List<Student> findStudentsByLastName(final Collection<Student> collection, final String s) {
        return findStudentBy(collection, Student::lastName, s);
    }

    @Override
    public List<Student> findStudentsByGroup(final Collection<Student> collection, final GroupName groupName) {
        return findStudentBy(collection, Student::groupName, groupName);
    }

    private <T> List<Student> findStudentBy(final Collection<Student> collection, final Function<Student, T> field, final T temp) {
        return collection.stream().filter(s1 -> field.apply(s1).equals(temp)).sorted(NAME_COMPARATOR).toList();
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(final Collection<Student> collection, final GroupName groupName) {
        return collection
                .stream()
                .filter(s1 -> s1.groupName().equals(groupName))
                .collect(Collectors.toMap(Student::lastName, Student::firstName, BinaryOperator.minBy(String::compareTo)));
    }

    @Override
    public GroupName getMaxGroup(final Collection<Student> students, final String name) {
        return students
                .stream()
                .filter(s -> s.firstName().equals(name))
                .collect(Collectors.groupingBy(Student::groupName, Collectors.counting()))
                .entrySet()
                .stream()
                .max(Comparator
                        .comparingLong(Map.Entry<GroupName, Long>::getValue)
                        .thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public List<String> getFirstNames(final Collection<Student> students, final int[] ids) {
        return getByIndices(students, ids, Student::firstName);
    }

    @Override
    public List<String> getLastNames(final Collection<Student> students, final int[] ids) {
        return getByIndices(students, ids, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(final Collection<Student> students, final int[] ids) {
        return getByIndices(students, ids, Student::groupName);
    }

    @Override
    public List<String> getFullNames(final Collection<Student> students, final int[] ids) {
        return getByIndices(students, ids, s -> s.firstName() + " " + s.lastName());
    }

    private <T> List<T> getByIndices(final Collection<Student> students, final int[] ids, final Function<Student, T> extractor) {
        return Arrays
                .stream(ids)
                .mapToObj(id -> extractor.apply(
                        students
                                .stream()
                                .filter(s -> s.id() == id)
                                .findFirst()
                                .orElse(null)
                ))
                .toList();
    }
}
