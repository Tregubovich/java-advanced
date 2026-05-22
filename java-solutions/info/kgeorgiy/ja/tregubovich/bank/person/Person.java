package info.kgeorgiy.ja.tregubovich.bank.person;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;

import java.util.Map;

public interface Person {
    /**
     * Returns person's name
     */
    String getName();


    /**
     * Returns person's surname
     */
    String getSurname();


    /**
     * Returns person's passport id
     */
    int getPassportID();

    /**
     * Returns person's accounts
     */
    Map<String, Account> getAccounts();

    /**
     * Adds or removes money from account with specified identifier.
     * If account doesn't exist creates new with empty balance.
     *
     * @param amount amount of money
     * @param id     account id
     * @throws InsufficientFundsException if there are not enough funds in the account
     */
    void deposit(String id, int amount) throws InsufficientFundsException;

    /**
     * Returns balance of the account with specified identifier.
     * If account doesn't exist creates new with empty balance.
     *
     * @return amount of funds on account
     */
    int getBalance(String id);
}
