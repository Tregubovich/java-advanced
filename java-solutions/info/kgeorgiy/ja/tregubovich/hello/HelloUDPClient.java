package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    static void main(final String... args) {
// :NOTE:
        //        if (args != null || Arrays.stream(args).anyMatch(Objects::isNull)) {
//
//        }

        if (args.length != 5) {
            System.err.println("Usage: HelloUDPClient <host> <port> <prefix> <requests> <threads>");
        }
        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        final String prefix = args[2];
        final int requests = Integer.parseInt(args[3]);
        final int threads = Integer.parseInt(args[4]);
        new HelloUDPClient().run(host, port, prefix, requests, threads);
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int requests, final int threads) {
        try (final ExecutorService executor = Executors.newFixedThreadPool(threads)) {
            final InetAddress address;
            try {
                address = InetAddress.getByName(host);
            } catch (final UnknownHostException e) {
                throw new RuntimeException(e);
            }

            for (int thread = 1; thread <= threads; thread++) {
                executor.submit(getTask(port, prefix, requests, thread, address));
            }
        }
    }

    private static Runnable getTask(
            final int port,
            final String prefix,
            final int requests,
            final int threadNum,
            final InetAddress address
    ) {
        return () -> {
            for (int requestNum = 1; requestNum <= requests; requestNum++) {
                // :NOTE: отдельная функция createRequest
                // :NOTE: StandardCharsets.UTF_8 в static final DEFAULT_CHARSET
                final byte[] msg = (prefix + requestNum + "_" + threadNum).getBytes(StandardCharsets.UTF_8);
                try (final DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(200); // :NOTE: вынетси в константу DEFAULT_SOCKET_TIMEOUT
                    final int buffSize = socket.getReceiveBufferSize();

                    final DatagramPacket request = new DatagramPacket(msg, msg.length, address, port);
                    final DatagramPacket response = new DatagramPacket(new byte[buffSize], buffSize);

                    tryToSend(socket, request, response, threadNum, requestNum);
                } catch (final SocketException e) {
                    System.err.println("Can't create socket:" + e.getMessage());
                }
            }
        };
    }

    private static void tryToSend(
            final DatagramSocket socket,
            final DatagramPacket request,
            final DatagramPacket response,
            final int threadNum,
            final int requestNum
    ) {
        while (!Thread.interrupted() && !socket.isClosed()) {
            try {
                socket.send(request);
                socket.receive(response);

                final String responseMsg = new String(response.getData(), response.getOffset(), response.getLength(), StandardCharsets.UTF_8);
                if (validate(responseMsg, requestNum, threadNum)) {
                    break;
                }
            } catch (final IOException e) {
                System.err.println("Can't send request: " + e.getMessage());
            }
        }
    }

    // :NOTE: naming - isValidResponse
    private static boolean validate(String responseMsg, final int requestNum, final int threadNum) {
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
