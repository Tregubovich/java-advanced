package info.kgeorgiy.ja.tregubovich.bank;

import info.kgeorgiy.ja.tregubovich.bank.account.LocalAccount;
import info.kgeorgiy.ja.tregubovich.bank.bank.Bank;
import info.kgeorgiy.ja.tregubovich.bank.bank.PersonAlreadyExistException;
import info.kgeorgiy.ja.tregubovich.bank.bank.RemoteBank;
import info.kgeorgiy.ja.tregubovich.bank.person.InsufficientFundsException;
import info.kgeorgiy.ja.tregubovich.bank.person.LocalPerson;
import info.kgeorgiy.ja.tregubovich.bank.person.Person;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BankTest {
    public static final List<String> NAMES = List.of("Andrey", "Ivan", "Nikita", "Aleksey");
    public static final List<String> SURNAMES = List.of("Tregubovich", "Solonenko", "Rzhevin");
    public static int REGISTRY_PORT;
    public static int BANK_PORT;

    private static Registry registry;
    private static Bank bank;

    @BeforeAll
    static void beforeAll() throws IOException {
        try (final ServerSocket socket = new ServerSocket(0)) {
            REGISTRY_PORT = socket.getLocalPort();
        }

        try (final ServerSocket socket = new ServerSocket(0)) {
            BANK_PORT = socket.getLocalPort();
        }

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
    void test10_checkTypes() {
        try {
            Remote _ = (Remote) new LocalAccount("1");
            Assertions.fail("Local account shouldn't implement Remote");
        } catch (ClassCastException _) {
        }

        try {
            Remote _ = (Remote) new LocalPerson("Andrey", "Tregubovich", 1);
            Assertions.fail("Local person shouldn't implement Remote");
        } catch (ClassCastException _) {
        }
    }

    @Test
    void test20_createPerson() throws RemoteException {
        createRandomPerson();
    }

    @Test
    void test21_createPersonWithSameName() throws RemoteException {
        createPerson("Andrey", "Tregubovich", 1);
        createPerson("Andrey", "Solonenko", 2);
        createPerson("Ivan", "Tregubovich", 3);
        createPerson("Andrey", "Tregubovich", 4);
    }

    @Test
    void test22_createPersonWithSameId() throws RemoteException {
        createPerson("Andrey", "Tregubovich", 1);
        createPerson("Andrey", "Tregubovich", 1);
        try {
            createPerson("Ivan", "Olkhov", 1);
            Assertions.fail("Creating person with same id isn't allowed");
        } catch (PersonAlreadyExistException _) {

        }
    }


    @Test
    void test30_getRemotePerson() throws RemoteException {
        final Person expectedPerson = createRandomPerson();
        final Person actualPerson = bank.getPerson(expectedPerson.getPassportId(), false);
        Assertions.assertEquals(expectedPerson, actualPerson);
    }

    @Test
    void test31_getLocalPerson() throws RemoteException {
        final Person expectedPerson = createRandomPerson();
        final Person actualPerson = bank.getPerson(expectedPerson.getPassportId(), true);
        Assertions.assertNotNull(actualPerson);

        try {
            LocalPerson _ = (LocalPerson) actualPerson;
        } catch (ClassCastException _) {
            Assertions.fail("Expected LocalPeson");
        }

        Assertions.assertEquals(expectedPerson.getName(), actualPerson.getName());
        Assertions.assertEquals(expectedPerson.getSurname(), actualPerson.getSurname());
        Assertions.assertEquals(expectedPerson.getPassportId(), actualPerson.getPassportId());
    }

    @Test
    void test40_deposit() throws RemoteException {
        final Person person = createRandomPerson();
        deposit(person, "1", 100);
        Assertions.assertEquals(100, person.getBalance("1"));

        deposit(person, "1", 500);
        Assertions.assertEquals(600, person.getBalance("1"));

        deposit(person, "2", 300);
        Assertions.assertEquals(300, person.getBalance("2"));
        Assertions.assertEquals(600, person.getBalance("1"));

        deposit(person, "2", -200);
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
    void test50_changeRemotePerson() throws RemoteException {
        final Person person = createRandomPerson();
        final Person anotherPerson = bank.getPerson(person.getPassportId(), false);

        deposit(person, "1", 100);
        deposit(person, "2", 300);
        Assertions.assertEquals(100, anotherPerson.getBalance("1"));
        Assertions.assertEquals(300, anotherPerson.getBalance("2"));

        deposit(anotherPerson, "2", -200);
        Assertions.assertEquals(100, person.getBalance("2"));
    }

    @Test
    void test51_changeLocalPerson() throws RemoteException {
        final Person remotePerson = createRandomPerson();
        final Person localPerson = bank.getPerson(remotePerson.getPassportId(), true);

        Assertions.assertEquals(remotePerson.getName(), localPerson.getName());
        Assertions.assertEquals(remotePerson.getSurname(), localPerson.getSurname());
        Assertions.assertEquals(remotePerson.getPassportId(), localPerson.getPassportId());
        Assertions.assertEquals(remotePerson.getAccounts(), localPerson.getAccounts());

        deposit(remotePerson, "1", 100);
        Assertions.assertEquals(0, localPerson.getBalance("1"));

        final Person anotherPerson = bank.getPerson(remotePerson.getPassportId(), true);
        deposit(localPerson, "2", 300);
        Assertions.assertEquals(300, localPerson.getBalance("2"));
        Assertions.assertEquals(100, remotePerson.getBalance("1"));
        Assertions.assertEquals(100, anotherPerson.getBalance("1"));
    }

    @Test
    void test60_parallelDepositDifferentAccounts() throws RemoteException {
        final int TESTS = 50;
        final int THREADS = 5;

        final Person person = createRandomPerson();
        try (final ExecutorService es = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 1; i <= TESTS; i++) {
                final int num = i;
                es.submit(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                        deposit(person, String.valueOf(num), num);
                    } catch (Exception _) {
                        Assertions.fail("Unexpected exception");
                    }
                });
            }
        }
        for (int i = 1; i <= TESTS; i++) {
            final int balance = person.getBalance(String.valueOf(i));
            Assertions.assertEquals(i, balance);
        }
    }

    @Test
    void test61_parallelDepositSameAccounts() throws RemoteException {
        final int TESTS = 50;
        final int THREADS = 5;

        final Person person = createRandomPerson();
        int total = 0;
        try (final ExecutorService es = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 1; i <= TESTS; i++) {
                final int num = i;
                total += num;
                es.submit(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                        deposit(person, "123", num);
                    } catch (Exception _) {
                        Assertions.fail("Unexpected exception");
                    }
                });
            }
        }
        final int balance = person.getBalance("123");
        Assertions.assertEquals(total, balance);
    }

    @Test
    void test62_parallelCreatingPerson() {
        final String NAME = "Andrey";
        final String SURNAME = "Tregubovich";
        final int PASSPORT_ID = 467749;

        final int TESTS = 50;
        final int THREADS = 5;

        try (final ExecutorService es = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 1; i <= TESTS; i++) {
                es.submit(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(50);
                        final Person person = bank.createPerson(NAME, SURNAME, PASSPORT_ID);
                        deposit(person, "1", 1);
                        deposit(person, "2", 2);
                        deposit(person, "3", 3);
                    } catch (Exception _) {
                        Assertions.fail("Unexpected exception");
                    }
                });
            }
        }
    }

    private static void deposit(final Person person, final String id, final int amount) {
        try {
            person.deposit(id, amount);
        } catch (final Exception e) {
            Assertions.fail("Unexpected exception: " + e.getMessage());
        }
    }

    Person createRandomPerson() throws RemoteException {
        final Random rnd = new Random();

        final String name = NAMES.get(rnd.nextInt(NAMES.size()));
        final String surname = SURNAMES.get(rnd.nextInt(SURNAMES.size()));
        final int passportId = Math.abs(rnd.nextInt() % 100000);

        return createPerson(name, surname, passportId);
    }

    private static Person createPerson(final String name, final String surname, final int passportId) throws RemoteException {
        final Person person = bank.createPerson(name, surname, passportId);
        Assertions.assertNotNull(person);

        Assertions.assertEquals(name, person.getName());
        Assertions.assertEquals(surname, person.getSurname());
        Assertions.assertEquals(passportId, person.getPassportId());

        return person;
    }
}
