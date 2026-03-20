package info.kgeorgiy.ja.tregubovich.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.tools.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Generates implementations for classes and interfaces and can pack them into a jar.
 */
public class Implementor implements Impler, JarImpler {
    /**
     * Default constructor.
     */
    public Implementor() {
    }

    /**
     * Program entry point.
     *
     * @param args arguments: class name or "-jar aClass file.jar"
     */
    public static void main(String[] args) {
        if (args.length != 1 && args.length != 3) {
            System.out.println("Usage: java Implementor <aClass>");
            System.out.println("or");
            System.out.println("Usage: java Implementor -jar <aClass> <file>.jar");
            return;
        }
        boolean isJar = (args[0].equals("-jar"));
        Class<?> aClass;
        try {
            aClass = ClassLoader.getSystemClassLoader().loadClass(args[!isJar ? 0 : 1]);
        } catch (ClassNotFoundException e) {
            System.err.println("Can't load class: " + e.getMessage());
            return;
        }
        try {
            if (isJar) {
                new Implementor().implement(aClass, Paths.get(""));
            } else {
                new Implementor().implementJar(aClass, Paths.get(args[2]));
            }
        } catch (ImplerException e) {
            System.err.println("Impossible to implement: " + e.getMessage());
        }
    }

    /**
     * Generates implementation source code.
     *
     * @param aClass class to implement
     * @param path   output directory
     * @throws ImplerException if implementation is impossible
     */
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
        Path classPath = getPath(aClass, path);
        try (OutputStream writer = Files.newOutputStream(classPath)) {
            implClass(writer, aClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates implementation, compiles it and writes to jar.
     *
     * @param aClass class to implement
     * @param path   jar file path
     * @throws ImplerException if error occurs
     */
    @Override
    public void implementJar(Class<?> aClass, Path path) throws ImplerException {
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("tmp");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        implement(aClass, tempDir);
        compile(List.of(getPath(aClass, tempDir)), List.of(aClass), Charset.defaultCharset());
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(path))) {
            String entryName = aClass.getPackageName().replace('.', '/') + "/"
                    + aClass.getSimpleName() + "Impl.class";
            jar.putNextEntry(new JarEntry(entryName));
            Files.copy(getClassFilePath(aClass, tempDir), jar);
            jar.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Compiles java files.
     *
     * @param files        files to compile
     * @param dependencies dependencies for classpath
     * @param charset      encoding
     * @throws ImplerException if compiler not found
     */
    private static void compile(final List<Path> files, final List<Class<?>> dependencies, final Charset charset) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Could not find java compiler, include tools.jar to classpath");
        }
        final String classpath = getClassPath(dependencies).stream()
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
        final String[] args = Stream.concat(
                Stream.of("-cp", classpath, "-encoding", charset.name()),
                files.stream().map(Path::toString)
        ).toArray(String[]::new);
        compiler.run(null, null, null, args);
    }

    /**
     * Builds classpath.
     *
     * @param dependencies classes
     * @return paths list
     */
    private static List<Path> getClassPath(final List<Class<?>> dependencies) {
        return dependencies.stream()
                .map(dependency -> {
                    try {
                        return Path.of(dependency.getProtectionDomain().getCodeSource().getLocation().toURI());
                    } catch (final URISyntaxException e) {
                        throw new AssertionError(e);
                    }
                })
                .toList();
    }

    /**
     * Builds path to .java file.
     *
     * @param aClass class
     * @param path   root directory
     * @return path to file
     * @throws ImplerException if directory creation fails
     */
    private static Path getPath(Class<?> aClass, Path path) throws ImplerException {
        Path classPath = path.resolve(
                String.join(File.separator, aClass.getPackageName().split("\\."))
                        + File.separator + aClass.getSimpleName()
                        + "Impl.java");
        try {
            Files.createDirectories(classPath.getParent());
        } catch (IOException e) {
            throw new ImplerException("Can't create parent directory: " + e.getMessage());
        }
        return classPath;
    }

    /**
     * Returns path to .class file.
     *
     * @param aClass class
     * @param path   root directory
     * @return path to compiled class
     */
    private Path getClassFilePath(Class<?> aClass, Path path) {
        return path.resolve(aClass.getPackageName().replace('.', File.separatorChar))
                .resolve(aClass.getSimpleName() + "Impl.class");
    }

    /**
     * Writes class implementation.
     *
     * @param writer output stream
     * @param aClass class
     * @throws IOException     if write fails
     * @throws ImplerException if error occurs
     */
    private static void implClass(OutputStream writer, Class<?> aClass) throws IOException, ImplerException {
        write(writer, "package " + aClass.getPackageName() + ";",
                "public class " + aClass.getSimpleName() + "Impl " +
                        (aClass.isInterface() ? "implements " : "extends ") +
                        (aClass.getDeclaringClass() == null ? "" : aClass.getDeclaringClass().getSimpleName() + ".") +
                        aClass.getSimpleName() + " {");
        implCons(writer, aClass);
        implMethods(writer, aClass);
        write(writer, "}");
    }

    /**
     * Generates constructors.
     *
     * @param writer output stream
     * @param aClass class
     * @throws IOException     if write fails
     * @throws ImplerException if error occurs
     */
    private static void implCons(OutputStream writer, Class<?> aClass) throws IOException, ImplerException {
        Set<Constructor<?>> cons = Stream.concat(Arrays.stream(aClass.getConstructors()),
                Arrays.stream(aClass.getDeclaredConstructors())).collect(Collectors.toSet());
        boolean hasCons = aClass.isInterface();
        for (Constructor<?> c : cons) {
            if (Modifier.isPrivate(c.getModifiers())) continue;
            try {
                write(writer,
                        "public " + aClass.getSimpleName() + "Impl(" + getArgs(c.getParameterTypes()) + ")" +
                                getExceptionTypes(c.getExceptionTypes()) + " {",
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

    /**
     * Generates methods.
     *
     * @param writer output stream
     * @param aClass class
     * @throws IOException     if write fails
     * @throws ImplerException if error occurs
     */
    private static void implMethods(OutputStream writer, Class<?> aClass) throws IOException, ImplerException {
        for (Method m : getAllMethods(aClass)) {
            if (m == null) continue;
            write(writer,
                    "public " + getReturnType(m.getReturnType()) + " " + m.getName() +
                            "(" + getArgs(m.getParameterTypes()) + ")" +
                            getExceptionTypes(m.getExceptionTypes()) + " {",
                    "return " + getZeroType(m.getReturnType()) + ";",
                    "}");
        }
    }

    /**
     * Collects abstract methods.
     *
     * @param aClass class
     * @return methods collection
     */
    private static Collection<Method> getAllMethods(Class<?> aClass) {
        Map<String, Method> methods = new HashMap<>();
        Set<String> notOverriden = new HashSet<>();
        while (aClass != null) {
            for (Method m : Stream.concat(Arrays.stream(aClass.getMethods()),
                    Arrays.stream(aClass.getDeclaredMethods())).toList()) {
                int mods = m.getModifiers();
                if (Modifier.isStatic(mods) || Modifier.isFinal(mods) || Modifier.isPrivate(mods)) {
                    notOverriden.add(m.getName() + Arrays.toString(m.getParameterTypes()));
                } else if (Modifier.isAbstract(mods)) {
                    methods.putIfAbsent(m.getName() + Arrays.toString(m.getParameterTypes()), m);
                }
            }
            aClass = aClass.getSuperclass();
        }
        return methods.entrySet().stream()
                .filter(m -> !notOverriden.contains(m.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    /**
     * Builds exception string.
     *
     * @param exceptionTypes exception types
     * @return string
     */
    private static String getExceptionTypes(Class<?>[] exceptionTypes) {
        return exceptionTypes.length == 0 ? " "
                : " throws " + Arrays.stream(exceptionTypes).map(Class::getName).collect(Collectors.joining(", "));
    }

    /**
     * Returns default value.
     *
     * @param returnType type
     * @return value string
     */
    private static String getZeroType(Class<?> returnType) {
        if (!returnType.isPrimitive()) return "null";
        if (returnType.equals(boolean.class)) return "false";
        if (returnType.equals(void.class)) return "";
        return "0";
    }

    /**
     * Returns type name.
     *
     * @param returnType type
     * @return type string
     * @throws ImplerException if type is private
     */
    private static String getReturnType(Class<?> returnType) throws ImplerException {
        if (Modifier.isPrivate(returnType.getModifiers())) {
            throw new ImplerException("Private return type in method: " + returnType.getSimpleName());
        }
        if (returnType.isArray()) {
            return getReturnType(returnType.componentType()) + "[]";
        }
        return returnType.getName();
    }

    /**
     * Builds argument list.
     *
     * @param parameterTypes parameters
     * @return string
     * @throws ImplerException if invalid type
     */
    private static String getArgs(Class<?>[] parameterTypes) throws ImplerException {
        return getArgs(parameterTypes, true);
    }

    /**
     * Builds argument list.
     *
     * @param parameterTypes parameters
     * @param withTypes      include types or not
     * @return string
     * @throws ImplerException if invalid type
     */
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

    /**
     * Writes strings to stream.
     *
     * @param writer output stream
     * @param s      strings
     * @throws IOException if write fails
     */
    private static void write(OutputStream writer, String... s) throws IOException {
        writer.write(String.join(System.lineSeparator(), s).getBytes());
    }
}