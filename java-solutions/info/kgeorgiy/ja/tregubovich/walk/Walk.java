package info.kgeorgiy.ja.tregubovich.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Walk {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java Walk <input> <output>");
            return;
        }

        RecursiveWalk.walk(args[0], args[1]);
    }
}
