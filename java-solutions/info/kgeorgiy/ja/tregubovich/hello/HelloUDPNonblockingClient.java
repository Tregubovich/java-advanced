package info.kgeorgiy.ja.tregubovich.hello;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HelloUDPNonblockingClient extends AbstractHelloUDPClient {
    static void main(final String... args) {
        main(HelloUDPNonblockingClient.class, args);
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int requests, final int threads) {
        final SocketAddress address;
        try {
            address = new InetSocketAddress(InetAddress.getByName(host), port);
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }

        final List<DatagramChannel> channels = new ArrayList<>();
        try (final Selector selector = Selector.open()) {
            for (int threadNum = 1; threadNum <= threads; threadNum++) {
                final DatagramChannel channel = selector.provider().openDatagramChannel();
                channels.add(channel);
                channel.configureBlocking(false);
                channel.connect(address);
                final int receiveBufferSize = channel.socket().getReceiveBufferSize();
                channel.register(selector, SelectionKey.OP_WRITE, new Context(threadNum, receiveBufferSize));
            }

            while (!Thread.interrupted() && !selector.keys().isEmpty()) {
                final int selected = selector.select(SO_TIMEOUT);

                if (selected == 0) {
                    selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
                }

                final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    final SelectionKey key = iterator.next();
                    iterator.remove();

                    final DatagramChannel channel = (DatagramChannel) key.channel();
                    final Context ctx = (Context) key.attachment();

                    if (key.isReadable()) {
                        channel.receive(ctx.buffer.clear());
                        final String response = CHARSET.decode(ctx.buffer.flip()).toString();
                        if (isValidResponse(response, ctx.requestNum, ctx.threadNum)) {
                            ctx.requestNum++;
                        }
                        if (ctx.requestNum > requests) {
                            channel.close();
                        } else {
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    } else if (key.isWritable()) {
                        channel.send(ByteBuffer.wrap(createRequest(prefix, ctx.threadNum, ctx.requestNum)), address);
                        key.interestOps(SelectionKey.OP_READ);
                    }
                }
            }

            for (final DatagramChannel channel : channels) {
                try {
                    channel.close();
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        } catch (final IOException e) {
            System.err.println("Can't open selector: " + e.getMessage());
        }
    }

    private static final class Context {
        int threadNum;
        int requestNum;
        ByteBuffer buffer;

        Context(final int threadNum, final int buffSize) {
            this.threadNum = threadNum;
            this.requestNum = 1;
            buffer = ByteBuffer.allocateDirect(buffSize);
        }
    }
}
