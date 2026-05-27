package info.kgeorgiy.ja.tregubovich.hello;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPNonblockingServer extends AbstractHelloUDPServer {
    private static final int TIMEOUT = 5000;

    static void main(final String... args) {
        main(HelloUDPNonblockingServer.class, args);
    }

    private final Map<Integer, DatagramChannel> channels = new ConcurrentHashMap<>();

    private ExecutorService senders;
    private Selector selector;

    @Override
    public void start(final int threads, final Map<Integer, String> ports) {
        senders = Executors.newFixedThreadPool(threads);
        try {
            selector = Selector.open();
            for (final Map.Entry<Integer, String> entry : ports.entrySet()) {
                final DatagramChannel channel = selector.provider().openDatagramChannel();
                channels.put(entry.getKey(), channel);
                channel.configureBlocking(false);
                channel.bind(new InetSocketAddress(entry.getKey()));
                channel.register(selector, SelectionKey.OP_READ, new Context(channel.socket().getReceiveBufferSize(), entry.getKey()));
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                while (!Thread.interrupted()) {
                    try {
                        final int selected;
                        try {
                            selected = selector.select(TIMEOUT);
                        } catch (final ClosedSelectorException e) {
                            break;
                        }

                        if (selected == 0) continue;

                        final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                        while (iterator.hasNext()) {
                            final SelectionKey key = iterator.next();
                            iterator.remove();

                            final DatagramChannel channel = (DatagramChannel) key.channel();
                            final Context ctx = (Context) key.attachment();

                            if (key.isReadable()) {
                                final InetSocketAddress address = (InetSocketAddress) channel.receive(ctx.buffer.clear());
                                final String request = CHARSET.decode(ctx.buffer.flip()).toString();
                                senders.submit(() -> {
                                    ctx.responses.add(new Response(getResponse(ports.get(ctx.port), request), address));
                                    key.interestOpsOr(SelectionKey.OP_WRITE);
                                    selector.wakeup();
                                });
                            }
                            if (key.isWritable()) {
                                if (ctx.responses.isEmpty()) {
                                    key.interestOps(SelectionKey.OP_READ);
                                    continue;
                                }

                                final Response response = ctx.responses.remove();
                                channel.send(ByteBuffer.wrap(response.message), response.address);
                            }
                        }
                    } catch (final IOException e) {
                        System.err.println("Caught IOException: " + e.getMessage());
                        throw new UncheckedIOException(e);
                    }
                }
            });
        } catch (final IOException | UncheckedIOException e) {
            System.err.println("Can't open selector: " + e.getMessage());
        }
    }

    private record Response(byte[] message, SocketAddress address) {
    }

    private static final class Context {
        ByteBuffer buffer;
        Queue<Response> responses;
        int port;

        Context(final int buffSize, final int port) {
            this.buffer = ByteBuffer.allocateDirect(buffSize);
            this.responses = new ConcurrentLinkedQueue<>();
            this.port = port;
        }
    }

    @Override
    public void close() {
        senders.close();
        try {
            for (final DatagramChannel channel : channels.values()) {
                channel.close();
            }
            selector.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
