package info.kgeorgiy.ja.tregubovich.bank;

import info.kgeorgiy.ja.tregubovich.bank.bank.Bank;
import info.kgeorgiy.ja.tregubovich.bank.bank.PersonAlreadyExistException;
import info.kgeorgiy.ja.tregubovich.bank.person.InsufficientFundsException;
import info.kgeorgiy.ja.tregubovich.bank.person.RemotePerson;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public final class Client {
    /**
     * Utility class.
     */
    private Client() {
    }

    public static void main(final String... args) throws RemoteException {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull) || args.length != 5) {
            System.err.println("Usage: Client <name> <surname> <passportID> <accountID> <amount>");
            return;
        }

        final Bank bank;
        try {
            bank = (Bank) Naming.lookup("//localhost/bank");
        } catch (final NotBoundException e) {
            System.out.println("Bank is not bound");
            return;
        } catch (final MalformedURLException e) {
            System.out.println("Bank URL is invalid");
            return;
        }

        final String name = args[0];
        final String surname = args[1];
        final int passportID = Integer.parseInt(args[2]);
        final String accountID = args[3];
        final int amount = Integer.parseInt(args[4]);

        final RemotePerson person;
        try {
            person = bank.createPerson(name, surname, passportID);
        } catch (PersonAlreadyExistException _) {
            System.out.println("Person already exist");
            return;
        }

        try {
            person.deposit(accountID, amount);
        } catch (final InsufficientFundsException _) {
            System.out.println("Insufficient funds");
        }
    }
}
