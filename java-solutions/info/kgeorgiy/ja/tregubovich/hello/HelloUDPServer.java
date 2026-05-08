package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
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

    private ExecutorService listeners;
    private ExecutorService senders;

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        listeners = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
        senders = Executors.newFixedThreadPool(threads);
        for (final Map.Entry<Integer, String> entry : ports.entrySet()) {
            final int port = entry.getKey();
            final String format = entry.getValue();
            try {
                final DatagramSocket socket = new DatagramSocket(port);
                final int buffSize = socket.getReceiveBufferSize();

                sockets.put(port, socket);
                listeners.submit(getListenTask(socket, buffSize, format));
            } catch (final SocketException e) {
                throw new RuntimeException("Can't bind port: " + port, e);
            }
        }
    }

    private Runnable getListenTask(final DatagramSocket socket, final int buffSize, final String format) {
        return () -> {
            final DatagramPacket requestPacket = new DatagramPacket(new byte[buffSize], buffSize);
            while (!Thread.interrupted() && !socket.isClosed()) {
                try {
                    socket.receive(requestPacket);
                    // :NOTE: line length
                    final String requestMsg = new String(requestPacket.getData(), requestPacket.getOffset(), requestPacket.getLength(), StandardCharsets.UTF_8);
                    senders.submit(getSendTask(socket, format, requestMsg, requestPacket.getAddress(), requestPacket.getPort()));
                } catch (final IOException e) {
                    System.err.println("Can't receive request: " + e.getMessage());
                }
            }
        };
    }

    private Runnable getSendTask(final DatagramSocket socket, final String format, final String msg, final InetAddress address, final int port) {
        return () -> {
            final DatagramPacket responsePacket = getResponsePacket(format, msg, address, port);
            try {
                socket.send(responsePacket);
            } catch (final IOException e) {
                System.err.println("Can't send response: " + e.getMessage());
            }
        };
    }

    private static DatagramPacket getResponsePacket(final String format, final String msg, final InetAddress address, final int port) {
        final byte[] response = format.replace("%%", msg).getBytes();
        return new DatagramPacket(response, response.length, address, port);
    }

    @Override
    public void close() {
        sockets.values().forEach(DatagramSocket::close);
        listeners.shutdownNow();
        senders.shutdownNow(); // :NOTE: since 1.19 senders.close();
    }
}
