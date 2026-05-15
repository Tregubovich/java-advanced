package info.kgeorgiy.ja.tregubovich.bank;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.bank.Bank;
import info.kgeorgiy.ja.tregubovich.bank.people.Person;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public final class Client {
    /** Utility class. */
    private Client() {}

    public static void main(final String... args) throws RemoteException {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull) || args.length != 5) {
            System.err.println("Usage: Client <name> <surname> <passportId> <accountId> <amount>");
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
        final int passportId = Integer.parseInt(args[2]);
        final String accountId = args[3];
        final int amount = Integer.parseInt(args[4]);

        Person person = bank.getPerson(passportId, false);
        if (person == null) {
            System.out.println("Creating person");
            person = bank.createPerson(name, surname, passportId);
        } else {
            if (!person.getName().equals(name) || !person.getSurname().equals(surname)) {
                System.out.println("Account with passport id " + passportId + " does not match");
                System.out.println("Expected " + person.getName() + " " + person.getSurname() + ", got " + name + " " + surname);
                return;
            }
        }

        System.out.println("Balance: " + person.getBalance(accountId));
        System.out.println("Adding money");
        person.deposit(accountId, amount);
    }
}
