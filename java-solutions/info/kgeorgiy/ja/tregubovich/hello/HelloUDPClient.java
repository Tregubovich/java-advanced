package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {
    static void main(final String... args) {
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
        try (final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofPlatform().factory())) {
            final InetAddress address;
            try {
                address = InetAddress.getByName(host);
            } catch (final UnknownHostException e) {
                throw new RuntimeException(e);
            }

            try (final DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(100);
                final CountDownLatch latch = new CountDownLatch(threads);
                for (int thread = 1; thread <= threads; thread++) {
                    executor.submit(getTask(port, prefix, requests, thread, address, socket, latch));
                }
                latch.await();
            } catch (final SocketException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static Runnable getTask(
            final int port,
            final String prefix,
            final int requests,
            final int finalThread,
            final InetAddress address,
            final DatagramSocket socket,
            final CountDownLatch latch
    ) {
        return () -> {
            for (int requestNum = 1; requestNum <= requests; requestNum++) {
                final byte[] msg = (prefix + requestNum + "_" + finalThread).getBytes();
                final DatagramPacket request = new DatagramPacket(msg, msg.length, address, port);
                while (true) {
                    try {
                        socket.send(request);

                        final DatagramPacket response = new DatagramPacket(new byte[msg.length], msg.length);
                        socket.receive(response);
                        break;
                    } catch (final IOException _) {
                    }
                }
            }
            latch.countDown();
        };
    }
}
