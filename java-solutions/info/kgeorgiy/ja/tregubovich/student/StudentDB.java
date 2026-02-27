package info.kgeorgiy.ja.tregubovich.student;

import info.kgeorgiy.java.advanced.student.*;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StudentDB implements AdvancedQuery {
    private final Comparator<Student> NAME_COMPARATOR = Comparator
            .comparing(Student::lastName, Comparator.reverseOrder())
            .thenComparing(Student::firstName, Comparator.reverseOrder())
            .thenComparingInt(Student::id);

    @Override
    public List<Group> getGroupsByName(Collection<Student> collection) {
        return getGroupBy(collection, NAME_COMPARATOR);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> collection) {
        return getGroupBy(collection, Comparator.comparingInt(Student::id));
    }

    private List<Group> getGroupBy(Collection<Student> collection, Comparator<Student> cmp) {
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
    public GroupName getLargestGroup(Collection<Student> collection) {
        return getLargestGroupBy(collection, Collectors.counting(), Long::longValue, Comparator.naturalOrder());
    }

    @Override
    public GroupName getLargestGroupFirstName(Collection<Student> collection) {
        return getLargestGroupBy(collection, Collectors.mapping(Student::firstName, Collectors.toSet()), Set::size, Comparator.reverseOrder());

    }

    private <T> GroupName getLargestGroupBy(Collection<Student> collection, Collector<Student, ?, T> collector, ToLongFunction<T> extractor, Comparator<GroupName> groupCmp) {
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
    public List<String> getFirstNames(List<Student> list) {
        return getField(list, Student::firstName);
    }

    @Override
    public List<String> getLastNames(List<Student> list) {
        return getField(list, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(List<Student> list) {
        return getField(list, Student::groupName);
    }

    @Override
    public List<String> getFullNames(List<Student> list) {
        return getField(list, student -> student.firstName() + " " + student.lastName());
    }

    private <T> List<T> getField(List<Student> list, Function<Student, T> extractor) {
        return list.stream().map(extractor).toList();
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> list) {
        return list.stream().map(Student::firstName).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public String getMaxStudentFirstName(List<Student> list) {
        return list.stream()
                .max(Comparator.comparingInt(Student::id))
                .map(Student::firstName)
                .orElse("");
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> collection) {
        return sortStudentBy(collection, Comparator.comparingInt(Student::id));
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> collection) {
        return sortStudentBy(collection, NAME_COMPARATOR);
    }

    private List<Student> sortStudentBy(Collection<Student> collection, Comparator<Student> cmp) {
        return collection.stream().sorted(cmp).toList();
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> collection, String s) {
        return findStudentBy(collection, Student::firstName, s);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> collection, String s) {
        return findStudentBy(collection, Student::lastName, s);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> collection, GroupName groupName) {
        return findStudentBy(collection, Student::groupName, groupName);
    }

    private <T> List<Student> findStudentBy(Collection<Student> collection, Function<Student, T> field, T temp) {
        return collection.stream().filter(s1 -> field.apply(s1).equals(temp)).sorted(NAME_COMPARATOR).toList();
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> collection, GroupName groupName) {
        return collection
                .stream()
                .filter(s1 -> s1.groupName().equals(groupName))
                .collect(Collectors.toMap(Student::lastName, Student::firstName, BinaryOperator.minBy(String::compareTo)));
    }

    @Override
    public GroupName getMaxGroup(Collection<Student> students, String name) {
        return students
                .stream()
                .collect(Collectors
                        .groupingBy(Student::groupName, Collectors
                                .mapping(Student::firstName, Collectors.filtering(s -> s.equals(name), Collectors.counting()))))
                .entrySet()
                .stream()
                .filter(e -> e.getValue() > 0)
                .max(Comparator.comparingLong(Map.Entry<GroupName, Long>::getValue).thenComparing(Map.Entry::getKey, Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    @Override
    public List<String> getFirstNames(Collection<Student> students, int[] ids) {
        return getByIndices(students, ids, Student::firstName);
    }

    @Override
    public List<String> getLastNames(Collection<Student> students, int[] ids) {
        return getByIndices(students, ids, Student::lastName);
    }

    @Override
    public List<GroupName> getGroupNames(Collection<Student> students, int[] ids) {
        return getByIndices(students, ids, Student::groupName);
    }

    @Override
    public List<String> getFullNames(Collection<Student> students, int[] ids) {
        return getByIndices(students, ids, s -> s.firstName() + " " + s.lastName());
    }

    private <T> List<T> getByIndices(Collection<Student> students, int[] ids, Function<Student, T> extractor) {
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
