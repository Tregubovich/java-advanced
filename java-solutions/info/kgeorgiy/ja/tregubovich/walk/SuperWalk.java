package info.kgeorgiy.ja.tregubovich.walk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public abstract class SuperWalk {
    static boolean FNV_32;
    static boolean RECURSIVE;

    public static void run(final String[] args, final boolean recursive) {
        if (args == null || args.length < 2) {
            error("Usage: java " + (recursive ? "RecursiveWalk" : "Walk") + " <input> <output> [<hash-type>]");
            return;
        }
        if (args.length == 3 && !args[2].equals("fnv-32") && !args[2].equals("fnv-64")) {
            error("Invalid hash type: " + args[2]);
            return;
        }
        FNV_32 = args.length < 3 || args[2].equals("fnv-32");
        RECURSIVE = recursive;
        walk(args[0], args[1]);
    }

    protected static Path makeInput(final String input) {
        final Path inputPath;
        if ((inputPath = getPath(input, "input")) == null) {
            return null;
        }

        if (!inputPath.toFile().canRead()) {
            error("Input file isn't readable");
            return null;
        }
        return inputPath;
    }

    protected static Path makeOutput(final String output) {
        final Path outputPath;
        if ((outputPath = getPath(output, "output")) == null) {
            return null;
        }

        if (!outputPath.toFile().exists()) {
            try {
                Files.createDirectories(outputPath.getParent());
                Files.createFile(outputPath);
            } catch (final Exception e) {
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

    protected static Path getPath(final String path, final String fileInfo) {
        try {
            return FileSystems.getDefault().getPath(path);
        } catch (final Exception e) {
            error("Invalid " + fileInfo + " path: " + e.getMessage());
            return null;
        }
    }

    public static void walk(
            final String input,
            final String output
    ) {
        final Path inputPath;
        if ((inputPath = makeInput(input)) == null) {
            return;
        }
        final Path outputPath;
        if ((outputPath = makeOutput(output)) == null) {
            return;
        }

        try (
                final LineNumberReader lineReader = new LineNumberReader(
                        Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)
                );
                final Writer writer = Files.newBufferedWriter(
                        outputPath,
                        StandardCharsets.UTF_8
                )
        ) {
            String read;
            while ((read = lineReader.readLine()) != null) {
                try {
                    final Path filePath;
                    if ((filePath = getPath(read, "file")) == null) {
                        invalidFile(read, writer, "Invalid file path");
                        continue;
                    }
                    if (!filePath.toFile().canRead()) {
                        invalidFile(
                                filePath.toString(),
                                writer,
                                "File " + filePath + " isn't readable"
                        );
                        continue;
                    }
                    proceedLine(filePath, writer);
                } catch (final IOException e) {
                    error("Error writing to output file: " + e.getMessage());
                }
            }
        } catch (final IOException e) {
            error("Error reading input file: " + e.getMessage());
        }
    }

    protected static void invalidFile(final String path, final Writer writer, final String msg)
            throws IOException {
        error(msg);
        write(path, writer, 0L);

    }

    protected static void proceedLine(final Path path, final Writer writer)
            throws IOException {
        if (path.toFile().isFile()) {
            final long hash = hashOfFile(path);
            write(path.toFile().toString(), writer, hash);
        } else if (RECURSIVE && path.toFile().isDirectory()) {
            try (final Stream<Path> fileList = Files.list(path)) {
                for (final Path child : fileList.toList()) {
                    proceedLine(child, writer);
                }
            } catch (final IOException e) {
                invalidFile(path.toString(), writer, "Error writing to output file: " + e.getMessage());
            }
        } else {
            error(path.toFile() + " is not a file");
            write(path.toFile().toString(), writer, 0L);
        }
    }

    private static void write(final String path, final Writer writer, final long hash) throws IOException {
        writer.write((FNV_32
                ? String.format("%08x", (int) hash)
                : String.format("%016x", hash)) +
                " " +
                path + System.lineSeparator());
    }

    protected static long hashOfFile(final Path path) {
        final char[] buffer = new char[2048];
        try (
                final BufferedReader reader = Files.newBufferedReader(
                        path,
                        StandardCharsets.ISO_8859_1
                )
        ) {
            int read;
            long hval;
            final long prime;
            if (FNV_32) {
                hval = 0x811c9dc5L;
                prime = 0x01000193;
            } else {
                hval = 0xcbf29ce484222325L;
                prime = 0x00000100000001b3L;
            }
            while ((read = reader.read(buffer)) != -1) {
                hval = fnv(hval, prime, buffer, read);
            }
            return FNV_32 ? (int) hval : hval;
        } catch (final IOException e) {
            error("Error hashing file: " + e.getMessage());
            return 0;
        }
    }

    protected static long fnv(final long x0, final long prime, final char[] buf, final int read) {
        long hval = x0;
        for (int i = 0; i < read; i++) {
            hval *= prime;
            hval ^= buf[i];
        }
        return hval;
    }

    protected static void error(final String message) {
        System.err.println(message);
    }
}
