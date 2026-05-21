package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class AbstractHelloUDPClient implements HelloClient {
    public static final java.nio.charset.Charset CHARSET = StandardCharsets.UTF_8;
    public static final int SO_TIMEOUT = 200;

    protected static void main(final Class<? extends AbstractHelloUDPClient> clazz, final String... args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull) || args.length != 5) {
            System.err.println("Usage: " + clazz.getSimpleName() + " <host> <port> <prefix> <requests> <threads>");
            return;
        }
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String prefix = args[2];
        final int requests = Integer.parseInt(args[3]);
        final int threads = Integer.parseInt(args[4]);
        try {
            final AbstractHelloUDPClient client = clazz.getDeclaredConstructor().newInstance();
            client.run(host, port, prefix, requests, threads);
        } catch (final Exception e) {
            System.err.println("Can't run client: " + e.getMessage());
        }
    }

    public abstract void run(final String host, final int port, final String prefix, final int requests, final int threads);

    protected static byte[] createRequest(final String prefix, final int threadNum, final int requestNum) {
        return (prefix + requestNum + "_" + threadNum).getBytes(CHARSET);
    }

    protected static boolean isValidResponse(String responseMsg, final int requestNum, final int threadNum) {
        responseMsg = reverse(responseMsg);
        final String num1 = extractNumFromSuffix(responseMsg.chars()
                .dropWhile(c -> !Character.isDigit(c)));
        if (!num1.equals(reverse(String.valueOf(threadNum)))) {
            return false;
        }

        final String num2 = extractNumFromSuffix(responseMsg.chars()
                .dropWhile(c -> !Character.isDigit(c))
                .dropWhile(Character::isDigit)
                .dropWhile(c -> !Character.isDigit(c)));
        return num2.equals(reverse(String.valueOf(requestNum)));
    }

    private static String extractNumFromSuffix(final IntStream stream) {
        return stream.takeWhile(Character::isDigit)
                .mapToObj(Character::getNumericValue)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    private static String reverse(final String str) {
        return new StringBuilder(str).reverse().toString();
    }
}
