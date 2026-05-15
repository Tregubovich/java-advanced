package info.kgeorgiy.ja.tregubovich.bank;

import info.kgeorgiy.ja.tregubovich.bank.bank.Bank;
import info.kgeorgiy.ja.tregubovich.bank.bank.RemoteBank;
import info.kgeorgiy.ja.tregubovich.bank.people.InsufficientFundsException;
import info.kgeorgiy.ja.tregubovich.bank.people.Person;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Random;

public class BankTest {
    public static final int REGISTRY_PORT = 8880;
    public static final int BANK_PORT = 8881;

    private static Registry registry;
    private static Bank bank;

    @BeforeAll
    static void beforeAll() throws RemoteException {
        registry = LocateRegistry.createRegistry(REGISTRY_PORT);

        bank = new RemoteBank(BANK_PORT);
        UnicastRemoteObject.exportObject(bank, BANK_PORT);
        registry.rebind("//localhost/bank", bank);
    }

    @AfterAll
    static void afterAll() throws RemoteException, NotBoundException {
        UnicastRemoteObject.unexportObject(bank, true);
        registry.unbind("//localhost/bank");
    }

    @Test
    void createPerson() throws RemoteException {
        createRandomPerson();
    }

    @Test
    void getPerson()  throws RemoteException {
        final Person expectedPerson = createRandomPerson();
        final Person actualPerson = bank.getPerson(expectedPerson.getPassportId(), false);
        Assertions.assertEquals(expectedPerson, actualPerson);
    }

    @Test
    void deposit() throws RemoteException {
        final Person person = createRandomPerson();
        person.deposit("1", 100);
        Assertions.assertEquals(100, person.getBalance("1"));

        person.deposit("1", 500);
        Assertions.assertEquals(600, person.getBalance("1"));

        person.deposit("2", 300);
        Assertions.assertEquals(300, person.getBalance("2"));
        Assertions.assertEquals(600, person.getBalance("1"));

        person.deposit("2", -200);
        Assertions.assertEquals(100, person.getBalance("2"));

        try {
            person.deposit("2", -500);
            Assertions.fail("Exception expected");
        } catch (InsufficientFundsException _) {

        } catch (final Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    @Test
    void changeRemotePerson() throws RemoteException {
        final Person person = createRandomPerson();
        final Person anotherPerson = bank.getPerson(person.getPassportId(), false);

        person.deposit("1", 100);
        person.deposit("2", 300);
        Assertions.assertEquals(100, anotherPerson.getBalance("1"));
        Assertions.assertEquals(300, anotherPerson.getBalance("2"));

        anotherPerson.deposit("2", -200);
        Assertions.assertEquals(100, person.getBalance("2"));
    }

    @Test
    void changeLocalPerson() throws RemoteException {
        final Person remotePerson = createRandomPerson();
        final Person localPerson = bank.getPerson(remotePerson.getPassportId(), true);

        Assertions.assertEquals(remotePerson.getName(), localPerson.getName());
        Assertions.assertEquals(remotePerson.getSurname(), localPerson.getSurname());
        Assertions.assertEquals(remotePerson.getPassportId(), localPerson.getPassportId());
        Assertions.assertEquals(remotePerson.getAccounts(), localPerson.getAccounts());

        remotePerson.deposit("1", 100);
        Assertions.assertEquals(0, localPerson.getBalance("1"));

        final Person anotherPerson = bank.getPerson(remotePerson.getPassportId(), true);
        localPerson.deposit("2", 300);
        Assertions.assertEquals(300, localPerson.getBalance("2"));
        Assertions.assertEquals(100, remotePerson.getBalance("1"));
        Assertions.assertEquals(100, anotherPerson.getBalance("1"));
    }

    Person createRandomPerson() throws RemoteException {
        final String name = "Andrey";
        final String surname = "Tregubovich";
        final int passportId = Math.abs(new Random().nextInt() % 100000);

        final Person person = bank.createPerson(name, surname, passportId);
        Assertions.assertNotNull(person);

        Assertions.assertEquals(name, person.getName());
        Assertions.assertEquals(surname, person.getSurname());
        Assertions.assertEquals(passportId, person.getPassportId());

        return person;
    }
}
