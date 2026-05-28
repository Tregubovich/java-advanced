package info.kgeorgiy.ja.tregubovich.i18n;

public enum StatisticType {
    SENTENCES("sentence", "sentences"),
    WORDS("word", "words"),
    NUMBERS("number", "numbers"),
    MONEY("amount", "money"),
    DATES("date", "dates");

    private final String singular;
    private final String plural;

    StatisticType(final String singular, final String plural) {
        this.singular = singular;
        this.plural = plural;
    }

    public String getBundleKeySingular() {
        return singular;
    }

    public String getBundleKeyPlural() {
        return plural;
    }

    public String withKey(final String key) {
        return key + "." + plural;
    }
}
