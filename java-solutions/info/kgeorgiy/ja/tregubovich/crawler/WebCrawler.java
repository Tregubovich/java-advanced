package info.kgeorgiy.ja.tregubovich.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

public class WebCrawler implements AdvancedCrawler {
    private final Downloader downloader;

    private final int perHost;
    private final Map<String, Semaphore> hostsPermits;

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
        final Set<String> unique = new HashSet<>(hosts); // :NOTE: OutOfMemoryError: Java heap space
        return download(url, depth, curUrl -> {
            try {
                return unique.contains(URLUtils.getHost(curUrl));
            } catch (MalformedURLException _) {
                return false;
            }
        });
    }

    private Result download(final String url, final int depth, final Predicate<String> urlFilter) {
        final Set<String> used = ConcurrentHashMap.newKeySet();
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();

        recursiveDownload(List.of(url), used, downloaded, errors, depth, urlFilter);
        return new Result(new ArrayList<>(downloaded), errors);
    }

    private void recursiveDownload(
            final List<String> urls,
            final Set<String> used,
            final Set<String> downloaded,
            final Map<String, IOException> errors,
            final int depth,
            final Predicate<String> urlFilter) {
        if (depth <= 0) {
            return;
        }

        final Phaser phaser = new Phaser(1);
        final Queue<String> nextUrls = new ConcurrentLinkedQueue<>();

        for (final String url : urls) {
            if (!used.add(url)) {
                continue;
            }
            if (!urlFilter.test(url)) {
                continue;
            }

            final String host = getHost(url);

            phaser.register();
            downloadExecutor.submit(getTask(downloaded, errors, url, host, phaser, nextUrls));
        }
        phaser.arriveAndAwaitAdvance();
        recursiveDownload(new ArrayList<>(nextUrls), used, downloaded, errors, depth - 1, urlFilter);
        // :NOTE: стек рекрсии
    }

    private String getHost(final String url) {
        final String host;
        try {
            host = URLUtils.getHost(url);
        } catch (final MalformedURLException exception) {
            throw new RuntimeException(exception);
        }
        hostsPermits.putIfAbsent(host, new Semaphore(perHost));
        hostsPermits.get(host).acquireUninterruptibly(); // :NOTE: блокируем рабочие потоки скачивания
        return host;
    }

    private Runnable getTask(
            final Set<String> downloaded,
            final Map<String, IOException> errors,
            final String url,
            final String host,
            final Phaser phaser,
            final Queue<String> nextUrls
    ) {
        return () -> {
            try {
                final Document document = downloader.download(url);
                hostsPermits.get(host).release();

                downloaded.add(url);
                phaser.register(); // :NOTE: на последнем уровне не надо извлекать
                // :NOTE: вынести
                extractorExecutor.submit(() -> {
                    try {
                        final List<String> extracted = document.extractLinks();
                        nextUrls.addAll(extracted);
                    } catch (final IOException exception) {
                        errors.put(url, exception);
                    } finally {
                        phaser.arriveAndDeregister();
                    }
                });
            } catch (final IOException exception) {
                errors.put(url, exception);
                hostsPermits.get(host).release();
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
