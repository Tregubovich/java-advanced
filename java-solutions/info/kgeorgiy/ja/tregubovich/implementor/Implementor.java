package info.kgeorgiy.ja.tregubovich.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Implementor implements Impler {
    @Override
    public void implement(Class<?> aClass, Path path) throws ImplerException {
        if (aClass.isPrimitive() || aClass.equals(String.class) || aClass.equals(Enum.class)) {
            throw new ImplerException(aClass.getName() + " can't be implemented or extended");
        }
        Path classPath = path.resolve(Arrays.stream(aClass.getName().split("\\."))
                .collect(Collectors.joining(
                        FileSystems.getDefault().getSeparator()
                )) + "Impl.java");
        try {
            Files.createDirectories(classPath.getParent());
            Files.createFile(classPath);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (Writer writer = Files.newBufferedWriter(classPath)) {
            implClass(writer, aClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private void implClass(Writer writer, Class<?> aClass) throws IOException {
        write(writer, "package " + aClass.getPackageName() + ";");
        write(writer, "public class " + aClass.getSimpleName() + "Impl " + (aClass.isInterface() ? "implements " : "extends ") + aClass.getSimpleName() + " {");
        implCons(writer, aClass);
        implMethods(writer, aClass);
        write(writer, "}");
    }

    private void implCons(Writer writer, Class<?> aClass) throws IOException {
        for (Constructor<?> c : aClass.getConstructors()) {
            write(writer, "public " + aClass.getSimpleName() + "Impl(" + toArgs(c.getParameterTypes()) + ")" + " {");
            write(writer, "super(" + toArgs(c.getParameterTypes(), false) + ");");
            write(writer, "}");
        }
    }

    private void implMethods(Writer writer, Class<?> aClass) throws IOException {
        for (Method m : Stream.concat(Arrays.stream(aClass.getMethods()), Arrays.stream(aClass.getDeclaredMethods())).collect(Collectors.toSet())) {
            int mods = m.getModifiers();
            if (Modifier.isStatic(mods) || Modifier.isFinal(mods) || Modifier.isPrivate(mods)) {
                continue;
            }
            write(writer, "@Override");
            write(writer, "public " + getReturnType(m.getReturnType()) + " " + m.getName() + "(" + toArgs(m.getParameterTypes()) + ")" + " {");
            write(writer, "return " + getZeroType(m.getReturnType()) + ";");
            write(writer, "}");
        }
    }

    private String getZeroType(Class<?> returnType) {
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

    private String getReturnType(Class<?> returnType) {
        if (returnType.isArray()) {
            return getReturnType(returnType.componentType()) + "[]";
        } else {
            return returnType.getName();
        }
    }

    private String toArgs(Class<?>[] parameterTypes) {
        return toArgs(parameterTypes, true);
    }

    private String toArgs(Class<?>[] parameterTypes, boolean withTypes) {
        return IntStream.range(0, parameterTypes.length)
                .mapToObj(i -> (withTypes ? parameterTypes[i].getCanonicalName() + " " : "") + "arg" + i)
                .collect(Collectors.joining(", "));
    }

    private void write(Writer writer, String s) throws IOException {
        writer.write(s + System.lineSeparator());
        System.err.println(s);
    }
}
