package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer {
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
