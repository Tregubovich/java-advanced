package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements NewHelloServer {
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

    private final Map<Integer, DatagramSocket> sockets = new ConcurrentHashMap<>();
    private ExecutorService executor;

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        executor = Executors.newFixedThreadPool(threads);
        for (final Map.Entry<Integer, String> entry : ports.entrySet()) {
            final int port = entry.getKey();
            final String format = entry.getValue();
            try {
                final DatagramSocket socket = new DatagramSocket(port);
                sockets.put(port, socket);
            } catch (final SocketException e) {
                throw new RuntimeException("Can't start port: " + port, e);
            }
            executor.submit(getTask(port, format));
        }
    }

    private Runnable getTask(final int port, final String format) {
        return () -> {
            final DatagramSocket socket = sockets.get(port);
            final DatagramPacket requestPacket = new DatagramPacket(new byte[1024], 1024);

            while (!socket.isClosed()) {
                try {
                    socket.receive(requestPacket);
                    final String msg = new String(requestPacket.getData(), requestPacket.getOffset(), requestPacket.getLength());

                    final byte[] response = format.replace("%%", msg).getBytes();
                    final DatagramPacket responsePacket = new DatagramPacket(
                            response,
                            response.length,
                            requestPacket.getAddress(),
                            requestPacket.getPort());
                    socket.send(responsePacket);
                } catch (final IOException _) {
                    break;
                }
            }
        };
    }

    @Override
    public void close() {
        sockets.values().forEach(DatagramSocket::close);
        executor.shutdownNow();
    }
}
