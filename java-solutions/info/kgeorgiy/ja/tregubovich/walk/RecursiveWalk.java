package info.kgeorgiy.ja.tregubovich.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RecursiveWalk {

    static boolean FNV_32;
    static boolean RECURSIVE;

    public static void main(String[] args) {
        if (args == null || args.length < 2) {
            error("Usage: java RecursiveWalk <input> <output> [<hash-type>]");
            return;
        }
        if (args.length == 3 && args[2] != "fnv-32" && args[2] != "fnv-64") {
            error("Invalid hash type");
            return;
        }
        boolean fnv32 = args.length < 3 || args[2].equals("fnv-32");
        walk(args[0], args[1], fnv32, true);
    }

    protected static Path makeInput(String input) {
        Path inputPath;
        if ((inputPath = getPath(input, "input")) == null) {
            return null;
        }

        if (!inputPath.toFile().canRead()) {
            error("Input file isn't readable");
            return null;
        }
        return inputPath;
    }

    protected static Path makeOutput(String output) {
        Path outputPath;
        if ((outputPath = getPath(output, "output")) == null) {
            return null;
        }

        if (!outputPath.toFile().exists()) {
            try {
                Files.createDirectories(outputPath.getParent());
                Files.createFile(outputPath);
            } catch (Exception e) {
                error("Failed to create output file: " + e.getMessage());
                return null;
            }
        }
        if (!outputPath.toFile().canWrite()) {
            error("Output file isn't writable");
            return null;
        }
        return outputPath;
    }

    protected static Path getPath(String path, String fileType) {
        try {
            return FileSystems.getDefault().getPath(path);
        } catch (Exception e) {
            error("Invalid " + fileType + " path: " + e.getMessage());
            return null;
        }
    }

    public static void walk(String input, String output) {
        walk(input, output, true, true);
    }

    public static void walk(
        String input,
        String output,
        boolean fnv32,
        boolean recursive
    ) {
        FNV_32 = fnv32;
        RECURSIVE = recursive;
        Path inputPath;
        if ((inputPath = makeInput(input)) == null) {
            return;
        }
        Path outputPath;
        if ((outputPath = makeOutput(output)) == null) {
            return;
        }

        try (
            LineNumberReader lineReader = new LineNumberReader(
                Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)
            );
            Writer writer = Files.newBufferedWriter(
                outputPath,
                StandardCharsets.UTF_8
            );
        ) {
            String read;
            while ((read = lineReader.readLine()) != null) {
                try {
                    Path filePath;
                    if ((filePath = getPath(read, "file")) == null) {
                        invalidFile(read, writer, "Invalid file path");
                        continue;
                    }
                    if (!filePath.toFile().canRead()) {
                        invalidFile(
                            filePath.toFile().toString(),
                            writer,
                            "File isn't readable"
                        );
                        continue;
                    }
                    proceedLine(filePath, writer);
                } catch (IOException e) {
                    error("Error writing to output file: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            error("Error reading input file: " + e.getMessage());
        }
    }

    protected static void invalidFile(String path, Writer writer, String msg)
        throws IOException {
        error(msg);
        writer.write(
            (FNV_32 ? String.format("%08x", 0) : String.format("%016x", 0L)) +
                " " +
                path +
                "\n"
        );
    }

    protected static void proceedLine(Path path, Writer writer)
        throws IOException {
        if (path.toFile().isFile()) {
            long hash = hashOfFile(path);
            String res =
                (FNV_32
                    ? String.format("%08x", (int) hash)
                    : String.format("%016x", hash)) +
                " " +
                path.toFile().toString();
            writer.write(res + "\n");
        } else if (RECURSIVE && path.toFile().isDirectory()) {
            for (Path child : Files.list(path).collect(Collectors.toList())) {
                proceedLine(child, writer);
            }
        } else {
            error(path.toFile().toString() + " is not a file");
            writer.write(
                (FNV_32
                        ? String.format("%08x", 0)
                        : String.format("%016x", 0L)) +
                    " " +
                    path.toFile().toString() +
                    "\n"
            );
        }
    }

    protected static long hashOfFile(Path path) {
        char[] buffer = new char[2048];
        try (
            BufferedReader reader = Files.newBufferedReader(
                path,
                StandardCharsets.ISO_8859_1
            );
        ) {
            int read;
            long hval;
            if (FNV_32) {
                hval = 0x811c9dc5;
                while ((read = reader.read(buffer)) != -1) {
                    hval = fnv32((int) hval, buffer, read);
                }
            } else {
                hval = 0xcbf29ce484222325L;
                while ((read = reader.read(buffer)) != -1) {
                    hval = fnv64(hval, buffer, read);
                }
            }
            return hval;
        } catch (IOException e) {
            error("Error hashing file: " + e.getMessage());
            return 0;
        }
    }

    protected static int fnv32(int x0, char[] buf, int read) {
        final int fnv_prime = 0x01000193;
        int hval = x0;
        for (int i = 0; i < read; i++) {
            hval *= fnv_prime;
            hval ^= buf[i];
        }
        return hval;
    }

    protected static long fnv64(long x0, char[] buf, int read) {
        final long fnv_prime = 0x00000100000001b3L;
        long hval = x0;
        for (int i = 0; i < read; i++) {
            hval *= fnv_prime;
            hval ^= buf[i];
        }
        return hval;
    }

    protected static void error(String message) {
        System.err.println(message);
    }
}
