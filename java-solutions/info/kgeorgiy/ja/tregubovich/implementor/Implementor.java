package info.kgeorgiy.ja.tregubovich.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Implementor implements Impler {
    @Override
    public void implement(Class<?> aClass, Path path) throws ImplerException {
        if (aClass.isPrimitive()
                || aClass.isArray()
                || Modifier.isFinal(aClass.getModifiers())
                || Modifier.isPrivate(aClass.getModifiers())
                || aClass.isSealed()
                || aClass.equals(Enum.class)
                || aClass.equals(Record.class)) {
            throw new ImplerException(aClass.getName() + " can't be implemented or extended");
        }
        Path classPath = path.resolve(Arrays.stream(aClass.getPackageName().split("\\."))
                .collect(Collectors.joining(
                        FileSystems.getDefault().getSeparator()
                )) + FileSystems.getDefault().getSeparator() + aClass.getSimpleName() + "Impl.java");
        // not working on linux with File.separatorChar
        try {
            Files.createDirectories(classPath.getParent());
        } catch (IOException e) {
            throw new ImplerException("Can't create parent directory: " + e.getMessage());
        }
        try (Writer writer = Files.newBufferedWriter(classPath)) {
            implClass(writer, aClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void implClass(Writer writer, Class<?> aClass) throws IOException, ImplerException {
        write(writer, "package " + aClass.getPackageName() + ";",
                "public class " + aClass.getSimpleName() + "Impl " + (aClass.isInterface() ? "implements " : "extends ") +
                        (aClass.getDeclaringClass() == null ? "" : aClass.getDeclaringClass().getSimpleName() + ".") + aClass.getSimpleName() + " {");
        implCons(writer, aClass);
        implMethods(writer, aClass);
        write(writer, "}");
    }

    private static void implCons(Writer writer, Class<?> aClass) throws IOException, ImplerException {
        Set<Constructor<?>> cons = Stream.concat(Arrays.stream(aClass.getConstructors()), Arrays.stream(aClass.getDeclaredConstructors())).collect(Collectors.toSet());
        boolean hasCons = aClass.isInterface();
        for (Constructor<?> c : cons) {
            if (Modifier.isPrivate(c.getModifiers())) {
                continue;
            }
            try {
                write(writer, "public " +
                                aClass.getSimpleName() + "Impl("
                                + getArgs(c.getParameterTypes()) + ")"
                                + getExceptionTypes(c.getExceptionTypes()) + " {",
                        "super(" + getArgs(c.getParameterTypes(), false) + ");",
                        "}");
            } catch (ImplerException e) {
                continue;
            }
            hasCons = true;
        }
        if (!hasCons) {
            throw new ImplerException(aClass.getSimpleName() + "hasn't accessible constructor");
        }
    }

    private static void implMethods(Writer writer, Class<?> aClass) throws IOException, ImplerException {
        for (Method m : getAllMethods(aClass)) {
            if (m == null) {
                continue;
            }
            write(writer, "public " +
                            getReturnType(m.getReturnType()) + " " + m.getName()
                            + "(" + getArgs(m.getParameterTypes()) + ")"
                            + getExceptionTypes(m.getExceptionTypes()) + " {",
                    "return " + getZeroType(m.getReturnType()) + ";", "}");
        }
    }

    private static Collection<Method> getAllMethods(Class<?> aClass) {
        Map<String, Method> methods = new HashMap<>();
        Set<String> notOverriden = new HashSet<>();
        while (aClass != null) {
            for (Method m : Stream.concat(Arrays.stream(aClass.getMethods()), Arrays.stream(aClass.getDeclaredMethods())).toList()) {
                int mods = m.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods) || Modifier.isPrivate(mods)) {
                    notOverriden.add(m.getName() + Arrays.toString(m.getParameterTypes()));
                } else if (Modifier.isAbstract(mods)) {
                    methods.putIfAbsent(m.getName() + Arrays.toString(m.getParameterTypes()), m);
                }
            }
            aClass = aClass.getSuperclass();
        }
        return methods
                .entrySet()
                .stream()
                .filter(m -> !notOverriden.contains(m.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    private static String getExceptionTypes(Class<?>[] exceptionTypes) {
        return (exceptionTypes.length == 0 ? " " : " throws " + Arrays.stream(exceptionTypes).map(Class::getName).collect(Collectors.joining(", ")));
    }

    private static String getZeroType(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return "null";
        }
        if (returnType.equals(boolean.class)) {
            return "false";
        }
        if (returnType.equals(void.class)) {
            return "";
        }
        return "0";
    }

    private static String getReturnType(Class<?> returnType) throws ImplerException {
        if (Modifier.isPrivate(returnType.getModifiers())) {
            throw new ImplerException("Private returnType in method: " + returnType.getSimpleName());
        }
        if (returnType.isArray()) {
            return getReturnType(returnType.componentType()) + "[]";
        } else {
            return returnType.getName();
        }
    }

    private static String getArgs(Class<?>[] parameterTypes) throws ImplerException {
        return getArgs(parameterTypes, true);
    }

    private static String getArgs(Class<?>[] parameterTypes, boolean withTypes) throws ImplerException {
        for (Class<?> p : parameterTypes) {
            if (Modifier.isPrivate(p.getModifiers())) {
                throw new ImplerException("Private arg in method: " + p.getSimpleName());
            }
        }
        return IntStream.range(0, parameterTypes.length)
                .mapToObj(i -> (withTypes ? parameterTypes[i].getCanonicalName() + " " : "") + "arg" + i)
                .collect(Collectors.joining(", "));
    }

    private static void write(Writer writer, String... s) throws IOException {
        writer.write(String.join(System.lineSeparator(), s));
    }
}
