package info.kgeorgiy.ja.tregubovich.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;

    private static class HostSemaphore {
        final Semaphore semaphore;
        final Queue<Runnable> queue;

        HostSemaphore(final int perHost) {
            semaphore = new Semaphore(perHost);
            queue = new ArrayDeque<>();
        }
    }

    private final int perHost;
    private final Map<String, HostSemaphore> hostsPermits;

    private final ExecutorService downloadExecutor;
    private final ExecutorService extractorExecutor;

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;

        this.perHost = perHost;
        hostsPermits = new ConcurrentHashMap<>();

        downloadExecutor = Executors.newFixedThreadPool(downloaders);
        extractorExecutor = Executors.newFixedThreadPool(extractors);
    }

    static void main(final String... args) {
        if (args.length == 0) {
            System.err.println("Usage: WebCrawler url [depth [downloaders [extractors [perHost]]]]");
        }
        final String url = args[0];
        try {
            int depth = 1;
            int downloaders = 100;
            int extractors = 100;
            int perHost = Integer.MAX_VALUE;

            if (args.length > 1) {
                depth = Integer.parseInt(args[1]);
            }
            if (args.length > 2) {
                downloaders = Integer.parseInt(args[2]);
            }
            if (args.length > 3) {
                extractors = Integer.parseInt(args[3]);
            }
            if (args.length > 4) {
                perHost = Integer.parseInt(args[4]);
            }

            try (final Crawler crawler = new WebCrawler(new CachingDownloader(0), downloaders, extractors, perHost)) {
                final Result result = crawler.download(url, depth);
                System.out.println(result);
            }
        } catch (final IOException exception) {
            System.err.println("Can't create directory for downloading: " + exception.getMessage());
        }
    }

    @Override
    public Result download(final String url, final int depth) {
        return download(url, depth, _ -> true);
    }

    @Override
    public Result download(final String url, final int depth, final List<String> includes) {
        final Set<String> unique = new HashSet<>(includes);
        return download(url, depth, curUrl -> unique.stream().anyMatch(curUrl::contains));
    }

    @Override
    public Result advancedDownload(final String url, final int depth, final List<String> hosts) {
        final Set<String> unique = new HashSet<>();
        unique.addAll(hosts);
        return download(url, depth, curUrl -> {
            try {
                return unique.contains(URLUtils.getHost(curUrl));
            } catch (MalformedURLException _) {
                return false;
            }
        });
    }

    private Result download(final String url, int depth, final Predicate<String> urlFilter) {
        final Set<String> used = ConcurrentHashMap.newKeySet();
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();

        List<String> urls = List.of(url);
        while (depth-- > 0) {
            final Phaser phaser = new Phaser(1);
            final List<String> nextUrls = Collections.synchronizedList(new ArrayList<>(urls));

            for (final String url1 : urls) {
                if (!used.add(url1) || !urlFilter.test(url1)) {
                    continue;
                }

                final String host = getHost(url1);
                phaser.register();
                submitTask(host, getDownloadTask(depth, downloaded, errors, url1, phaser, nextUrls));
            }
            phaser.arriveAndAwaitAdvance();
            urls = nextUrls;
        }
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private String getHost(final String url) {
        final String host;
        try {
            host = URLUtils.getHost(url);
        } catch (final MalformedURLException exception) {
            throw new RuntimeException(exception);
        }
        return host;
    }

    private void submitTask(final String host, final Runnable task) {
        final HostSemaphore permits = hostsPermits.computeIfAbsent(host, _ -> new HostSemaphore(perHost));
        synchronized (permits) {
            if (permits.semaphore.tryAcquire()) {
                downloadExecutor.submit(() -> {
                    task.run();
                    releaseNext(permits);
                });
            } else {
                permits.queue.add(task);
            }
        }
    }

    private void releaseNext(final HostSemaphore permits) {
        final Runnable next;
        synchronized (permits) {
            next = permits.queue.poll();
            if (next == null) {
                permits.semaphore.release();
                return;
            }
        }
        downloadExecutor.submit(() -> {
            next.run();
            releaseNext(permits);
        });
    }

    private Runnable getDownloadTask(
            final int depth,
            final Set<String> downloaded,
            final Map<String, IOException> errors,
            final String url,
            final Phaser phaser,
            final List<String> nextUrls
    ) {
        return () -> {
            try {
                final Document document = downloader.download(url);

                downloaded.add(url);
                if (depth == 0) {
                    return;
                }
                phaser.register();
                extractorExecutor.submit(getExtractorTask(errors, url, phaser, nextUrls, document));
            } catch (final IOException exception) {
                errors.put(url, exception);
            } finally {
                phaser.arriveAndDeregister();
            }
        };
    }

    private static Runnable getExtractorTask(
            final Map<String, IOException> errors,
            final String url,
            final Phaser phaser,
            final List<String> nextUrls,
            final Document document
    ) {
        return () -> {
            try {
                final List<String> extracted = document.extractLinks();
                nextUrls.addAll(extracted);
            } catch (final IOException exception) {
                errors.put(url, exception);
            } finally {
                phaser.arriveAndDeregister();
            }
        };
    }

    @Override
    public void close() {
        downloadExecutor.close();
        extractorExecutor.close();
    }
}
