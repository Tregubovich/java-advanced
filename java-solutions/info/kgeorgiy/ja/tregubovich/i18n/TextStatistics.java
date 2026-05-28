package info.kgeorgiy.ja.tregubovich.i18n;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.*;
import java.util.*;
import java.util.function.Function;

public class TextStatistics {
    public static void main(final String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull) || args.length != 4) {
            System.err.println("Usage: TextStatistics <input locale> <output locale> <input file> <output file>");
            return;
        }

        final Locale inputLocale = Locale.forLanguageTag(args[0]);
        final Locale outputLocale = Locale.forLanguageTag(args[1]);

        final Collator collator = Collator.getInstance(inputLocale);

        final Map<StatisticType, Summary<?>> statistic = Map.of(
                StatisticType.SENTENCES, new Summary<>(collator::compare, Comparator.comparingInt(String::length), s -> (double) s.length()),
                StatisticType.WORDS, new Summary<>(collator::compare, Comparator.comparingInt(String::length), s -> (double) s.length()),
                StatisticType.NUMBERS, new Summary<>(Comparator.comparingDouble(Number::doubleValue), null, Number::doubleValue),
                StatisticType.MONEY, new Summary<>(Comparator.comparingDouble(Number::doubleValue), null, Number::doubleValue),
                StatisticType.DATES, new Summary<Date>(Comparator.naturalOrder(), null, Date::getTime)
        );

        summarizeText(args[2], inputLocale, statistic);
        writeSummary(Paths.get(args[2]).getFileName().toString(), args[3], outputLocale, statistic);
    }

    @SuppressWarnings("unchecked")
    private static void summarizeText(final String fileName, final Locale inputLocale, final Map<StatisticType, Summary<?>> statistic) {
        try (final FileReader reader = new FileReader(fileName, StandardCharsets.UTF_8)) {
            final String inputText = reader.readAllAsString();

            summaryAsString(inputText, BreakIterator.getSentenceInstance(inputLocale), (Summary<String>) statistic.get(StatisticType.SENTENCES));
            summaryAsString(inputText, BreakIterator.getWordInstance(inputLocale), (Summary<String>) statistic.get(StatisticType.WORDS));

            summaryAsNumber(inputText, NumberFormat.getCurrencyInstance(inputLocale), (Summary<Number>) statistic.get(StatisticType.MONEY));
            summaryAsDate(inputText, List.of(
                    DateFormat.getDateInstance(DateFormat.SHORT, inputLocale),
                    DateFormat.getDateInstance(DateFormat.MEDIUM, inputLocale),
                    DateFormat.getDateInstance(DateFormat.LONG, inputLocale),
                    DateFormat.getDateInstance(DateFormat.FULL, inputLocale)
            ), (Summary<Date>) statistic.get(StatisticType.DATES));
            summaryAsNumber(inputText, NumberFormat.getNumberInstance(inputLocale), (Summary<Number>) statistic.get(StatisticType.NUMBERS));
        } catch (final IOException e) {
            System.err.println("Can't read from input file: " + e.getMessage());
        }
    }

    private static void summaryAsString(final String text, final BreakIterator iterator, final Summary<String> summary) {
        iterator.setText(text);
        int curBoundary = 0;
        while (true) {
            final int nextBoundary = iterator.next();
            if (nextBoundary == BreakIterator.DONE) {
                break;
            }

            final String token = text.substring(curBoundary, nextBoundary).trim().replaceAll("\\s+", " ");
            if (!token.isEmpty() && token.chars().anyMatch(Character::isLetter)) {
                summary.addValue(token);
            }
            curBoundary = nextBoundary;
        }
    }

    private static void summaryAsNumber(final String text, final NumberFormat formatter, final Summary<Number> summary) {
        final ParsePosition position = new ParsePosition(0);
        while (position.getIndex() < text.length()) {
            final int start = position.getIndex();
            final Number number = formatter.parse(text, position);
            if (number != null) {
                summary.addValue(number);
            }
            if (position.getIndex() == start) {
                position.setIndex(start + 1);
            }
        }
    }

    private static void summaryAsDate(final String text, final List<DateFormat> formatters, final Summary<Date> summary) {
        final ParsePosition position = new ParsePosition(0);
        while (position.getIndex() < text.length()) {
            final int start = position.getIndex();
            for (final DateFormat formatter : formatters) {
                final Date date = formatter.parse(text, position);
                if (date != null) {
                    summary.addValue(date);
                    break;
                }
            }
            if (position.getIndex() == start) {
                position.setIndex(start + 1);
            }
        }
    }

    private static void writeSummary(final String input, final String output, final Locale outputLocale, final Map<StatisticType, Summary<?>> summaries) {
        final ResourceBundle bundle = ResourceBundle.getBundle("info.kgeorgiy.ja.tregubovich.i18n.properties.Messages", outputLocale);
        try (final FileWriter writer = new FileWriter(output, StandardCharsets.UTF_8)) {
            writeln(writer,
                    MessageFormat.format(bundle.getString("analyzedFile"), input),
                    bundle.getString("summaryStatistics")
            );

            for (final StatisticType type : StatisticType.values()) {
                final Summary<?> summary = summaries.get(type);
                writeln(writer,
                        tab(MessageFormat.format(bundle.getString(type.withKey("summary")), summary.getAmount()))
                );
            }

            writeStatistic(writer,
                    bundle,
                    StatisticType.SENTENCES,
                    summaries.get(StatisticType.SENTENCES),
                    s -> (String) s,
                    s -> NumberFormat.getNumberInstance(outputLocale).format(s),
                    true);
            writeStatistic(writer,
                    bundle,
                    StatisticType.WORDS,
                    summaries.get(StatisticType.WORDS),
                    s -> (String) s,
                    s -> NumberFormat.getNumberInstance(outputLocale).format(s),
                    true);
            final NumberFormat numberFormat =  NumberFormat.getNumberInstance(outputLocale);
            numberFormat.setGroupingUsed(false);
            numberFormat.setMinimumFractionDigits(1);
            writeStatistic(writer,
                    bundle,
                    StatisticType.NUMBERS,
                    summaries.get(StatisticType.NUMBERS),
                    numberFormat::format
            );
            writeStatistic(writer,
                    bundle,
                    StatisticType.MONEY,
                    summaries.get(StatisticType.MONEY),
                    NumberFormat.getCurrencyInstance(outputLocale)::format
            );
            writeStatistic(writer,
                    bundle,
                    StatisticType.DATES,
                    summaries.get(StatisticType.DATES),
                    DateFormat.getDateInstance(DateFormat.DEFAULT, outputLocale)::format
            );
        } catch (final IOException e) {
            System.err.println("Can't write in output file: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void writeStatistic(
            final Writer writer,
            final ResourceBundle bundle,
            final StatisticType type,
            final Summary<T> summary,
            final Function<Object, String> formatter
    ) throws IOException {
        writeStatistic(writer, bundle, type, (Summary<T>) summary, (Function<T, String>) formatter, formatter, false);
    }

    private static <T> void writeStatistic(
            final Writer writer,
            final ResourceBundle bundle,
            final StatisticType type,
            final Summary<T> summary,
            final Function<T, String> formatter,
            final Function<? super Double, String> avgFormatter,
            final boolean hasLength
    ) throws IOException {
        writeln(writer,
                bundle.getString(type.withKey("statistics")),
                tab(MessageFormat.format(bundle.getString(type.withKey("count")), summary.getAmount(), summary.getDistinctAmount()))
        );

        final T min = summary.getMin();
        final T max = summary.getMax();
        if (min != null && max != null) {
            writeln(writer,
                    tab(MessageFormat.format(bundle.getString(type.withKey("min")), formatter.apply(min))),
                    tab(MessageFormat.format(bundle.getString(type.withKey("max")), formatter.apply(max)))
            );
        }
        if (hasLength) {
            final String minLength = (String) summary.getMinLength();
            final String maxLength = (String) summary.getMaxLength();
            if (minLength != null && maxLength != null) {
                writeln(writer,
                        tab(MessageFormat.format(bundle.getString(type.withKey("minLength")), minLength.length(), formatter.apply(summary.getMinLength()))),
                        tab(MessageFormat.format(bundle.getString(type.withKey("maxLength")), maxLength.length(), formatter.apply(summary.getMaxLength())))
                );
            }
        }
        final double avg = summary.getAverage();
        if (!Double.isNaN(avg)) {
            writeln(writer,
                    tab(MessageFormat.format(bundle.getString(type.withKey("average")), avgFormatter.apply(summary.getAverage())))
            );
        }
    }

    private static String tab(final String text) {
        return "\t" + text;
    }

    private static void writeln(final Writer writer, final String... strings) throws IOException {
        writer.write(String.join(System.lineSeparator(), strings));
        writer.write(System.lineSeparator());
    }
}
