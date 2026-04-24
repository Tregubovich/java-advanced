package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
    static void main(final String... args) {
        if (args.length != 2) {
            System.err.println("Usage: HelloUDPServer <port> <threads>");
        }
        final int port = Integer.parseInt(args[0]);
        final int threads = Integer.parseInt(args[1]);
        try (final HelloServer server = new HelloUDPServer()) {
            server.start(port, threads);
        }
    }

    private DatagramSocket socket;
    private ExecutorService executor;

    @Override
    public void start(final int port, final int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            throw new RuntimeException(e);
        }
        executor = Executors.newThreadPerTaskExecutor(Thread.ofPlatform().factory());
        executor.submit(() -> {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            while (!socket.isClosed()) {
                try {
                    socket.receive(packet);

                    final InetAddress addressDestination = packet.getAddress();
                    final int portDestination = packet.getPort();

                    final byte[] msg = ("Hello, " + new String(packet.getData(), 0, packet.getLength())).getBytes();
                    packet = new DatagramPacket(msg, msg.length, addressDestination, portDestination);
                    socket.send(packet);
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @Override
    public void close() {
        socket.close();
        executor.close();
    }
}
