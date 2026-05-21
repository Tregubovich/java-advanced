package info.kgeorgiy.ja.tregubovich.hello;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPClient extends AbstractHelloUDPClient {
    static void main(final String... args) {
        main(HelloUDPClient.class, args);
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
                final byte[] msg = createRequest(prefix, threadNum, requestNum);
                try (final DatagramSocket socket = new DatagramSocket()) {
                    socket.setSoTimeout(SO_TIMEOUT);
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
                if (isValidResponse(responseMsg, requestNum, threadNum)) {
                    break;
                }
            } catch (final IOException e) {
                System.err.println("Can't send request: " + e.getMessage());
            }
        }
    }
}
