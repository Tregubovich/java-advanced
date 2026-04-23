package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

public class HelloUDPClient implements HelloClient {
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
                    final int finalThread = thread;
                    executor.submit(() -> {
                        for (int request = 1; request <= requests; request++) {
                            final byte[] msg = (prefix + request + "_" + finalThread).getBytes();
                            DatagramPacket packet = new DatagramPacket(msg, msg.length, address, port);
                            try {
                                socket.send(packet);

                                packet = new DatagramPacket(new byte[msg.length], msg.length);
                                socket.receive(packet);
                            } catch (final IOException _) {
                                request--;
                            }
                        }
                        latch.countDown();
                    });
                }
                latch.await();
            } catch (final SocketException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
