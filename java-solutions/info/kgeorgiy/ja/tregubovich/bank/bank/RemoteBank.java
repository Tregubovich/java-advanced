package info.kgeorgiy.ja.tregubovich.bank.bank;

import info.kgeorgiy.ja.tregubovich.bank.account.Account;
import info.kgeorgiy.ja.tregubovich.bank.account.RemoteAccount;
import info.kgeorgiy.ja.tregubovich.bank.people.LocalPerson;
import info.kgeorgiy.ja.tregubovich.bank.people.Person;
import info.kgeorgiy.ja.tregubovich.bank.people.RemotePerson;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<Integer, Person> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Person createPerson(final String name, final String surname, final int passportId) throws RemoteException {
        final Person person = new RemotePerson(name, surname, passportId, port);
        if (persons.putIfAbsent(passportId, person) == null) {
            System.out.println("Creating person: " + name +  " " + surname + ", " + passportId);
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            System.out.println("Person already exists: " + name +  surname + ", " + passportId);
            return persons.get(passportId);
        }
    }

    @Override
    public Person getPerson(final int passportId, final boolean isLocal) throws RemoteException {
        System.out.println("Retrieving person: " + passportId);
        final Person person = persons.get(passportId);
        if (person != null && isLocal) {
            return new LocalPerson(person);
        }
        return person;
    }
}
