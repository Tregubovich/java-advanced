package info.kgeorgiy.ja.tregubovich.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class RecursiveWalk {

    public static void main(String[] args) {
        if (args == null || args.length != 2) {
            System.err.println("Usage: java RecursiveWalk <input> <output>");
            return;
        }
        walk(args[0], args[1]);
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
        writer.write(String.format("%08x", 0) + " " + path + "\n");
    }

    protected static void proceedLine(Path path, Writer writer)
        throws IOException {
        if (path.toFile().isFile()) {
            int hash = hashOfFile(path);
            String res =
                String.format("%08x", hash) + " " + path.toFile().toString();
            writer.write(res + "\n");
        } else if (path.toFile().isDirectory()) {
            for (Path child : Files.list(path).collect(Collectors.toList())) {
                proceedLine(child, writer);
            }
        }
    }

    protected static int hashOfFile(Path path) {
        char[] buffer = new char[2048];
        try (
            BufferedReader reader = Files.newBufferedReader(
                path,
                StandardCharsets.ISO_8859_1
            );
        ) {
            int read;
            int hval = 0x811c9dc5;
            while ((read = reader.read(buffer)) != -1) {
                hval = fnv(hval, buffer, read);
            }
            return hval;
        } catch (IOException e) {
            error("Error hashing file: " + e.getMessage());
            return 0;
        }
    }

    protected static int fnv(int x0, char[] buf, int read) {
        final int fnv_prime = 0x01000193;
        int hval = x0;
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
