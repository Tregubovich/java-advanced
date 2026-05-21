package info.kgeorgiy.ja.tregubovich.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;
import info.kgeorgiy.java.advanced.hello.NewHelloServer;

import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;

public abstract class AbstractHelloUDPServer implements NewHelloServer {
    public static final java.nio.charset.Charset CHARSET = StandardCharsets.UTF_8;

    protected static void main(final Class<? extends AbstractHelloUDPServer> clazz, final String... args) {
        if (args.length != 2) {
            System.err.println("Usage: " + clazz.getSimpleName() + "  <port> <threads>");
        }
        final int port = Integer.parseInt(args[0]);
        final int threads = Integer.parseInt(args[1]);
        try (final HelloServer server = clazz.getDeclaredConstructor().newInstance()) {
            server.start(port, threads);
        } catch (final Exception e) {
            System.err.println("Can't start server: " + e.getMessage());
        }
    }

    protected static String getRequestMsg(final DatagramPacket requestPacket) {
        return new String(requestPacket.getData(), requestPacket.getOffset(), requestPacket.getLength(), CHARSET);
    }

    protected static byte[] getResponse(final String format, final String msg) {
        return format.replace("%%", msg).getBytes();
    }
}
