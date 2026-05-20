package info.kgeorgiy.ja.tregubovich.bank.account;

public class LocalAccount extends Account {
    public LocalAccount(final String id) {
        super(id);
    }

    public LocalAccount(final Account account) {
        super(account.getId(), account.getBalance());
    }
}
