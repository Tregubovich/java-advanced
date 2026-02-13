package info.kgeorgiy.ja.tregubovich.walk;

public class Walk {

    public static void main(String[] args) {
        // :NOTE: copy-paste
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
