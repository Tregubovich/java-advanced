package info.kgeorgiy.ja.tregubovich.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Walk {

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
        RecursiveWalk.walk(args[0], args[1], fnv32, false);
    }

    private static void error(String message) {
        System.err.println(message);
    }
}
