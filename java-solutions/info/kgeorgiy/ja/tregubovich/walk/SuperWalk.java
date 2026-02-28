package info.kgeorgiy.ja.tregubovich.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public abstract class SuperWalk {
    public static void run(final String[] args, final boolean recursive) {
        if (args == null || args.length < 2) {
            error("Usage: java " + (recursive ? "RecursiveWalk" : "Walk") + " <input> <output> [<hash-type>]");
            return;
        }
        if (args.length == 3 && !args[2].equals("fnv-32") && !args[2].equals("fnv-64")) {
            error("Invalid hash type: " + args[2]);
            return;
        }
        final boolean fnv32 = args.length < 3 || args[2].equals("fnv-32");
        walk(args[0], args[1], fnv32, recursive);
    }

    protected static Path getPath(final String path, final String fileInfo) {
        try {
            // :NOTE: FileSystems.getDefault()
            return FileSystems.getDefault().getPath(path);
            // :NOTE: catch (final Exception e)
        } catch (final Exception e) {
            // :NOTE: gg
            error("Invalid " + fileInfo + " path: " + e.getMessage());
            return null;
        }
    }

    public static void walk(final String input, final String output, final boolean fnv32, final boolean recursive)  {
        final Path inputPath, outputPath;
        if ((inputPath = getPath(input, "input")) == null || (outputPath = getPath(output, "output")) == null) {
            return;
        }

        if (outputPath.getParent() != null) {
            try {
                Files.createDirectories(outputPath.getParent());
            } catch (final IOException e) {
                error("Unable to create output file: " + e.getMessage());
            }
        }

        try (final LineNumberReader lineReader = new LineNumberReader(Files.newBufferedReader(inputPath))) {
            try (final BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                String line;
                while ((line = lineReader.readLine()) != null) {
                    final Path filePath;
                    if ((filePath = getPath(line, "file")) == null) {
                        invalidFile(line, writer, fnv32);
                    } else {
                        processLine(filePath, writer, fnv32, recursive);
                    }
                }
            } catch (final IOException e) {
                error("Error writing to output file: " + e.getMessage());
            }
        } catch (final IOException e) {
            error("Error reading input file: " + e.getMessage());
        }
    }

    protected static void invalidFile(final String path, final Writer writer, final boolean fnv32)
            throws IOException {
        error("Invalid file");
        write(path, writer, 0L, fnv32);

    }


    protected static void processLine(final Path path, final Writer writer, final boolean fnv32, final boolean recursive)
            throws IOException {
        if (!Files.exists(path) || !recursive && !Files.isRegularFile(path)) {
            error(path.toFile() + " is not a file");
            write(path.toString(), writer, 0L, fnv32);
        } else {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(final Path path, final BasicFileAttributes attributes) throws IOException {
                    final long hash = hashOfFile(path, fnv32);
                    write(path.toString(), writer, hash, fnv32);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
                    invalidFile(file.toString(), writer, fnv32);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }

    private static void write(final String path, final Writer writer, final long hash, final boolean fnv32) throws IOException {
        writer.write((fnv32
                ? String.format("%08x", (int) hash)
                : String.format("%016x", hash)) +
                " " +
                path + System.lineSeparator());
    }

    protected static long hashOfFile(final Path path, final boolean fnv32) {
        final char[] buffer = new char[2048];
        try (final BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.ISO_8859_1)) {
            int read;
            long hval;
            final long prime;
            if (fnv32) {
                hval = 0x811c9dc5L;
                prime = 0x01000193;
            } else {
                hval = 0xcbf29ce484222325L;
                prime = 0x00000100000001b3L;
            }
            while ((read = reader.read(buffer)) != -1) {
                hval = fnv(hval, prime, buffer, read);
            }
            return fnv32 ? (int) hval : hval;
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
