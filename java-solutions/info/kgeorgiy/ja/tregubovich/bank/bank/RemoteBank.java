package info.kgeorgiy.ja.tregubovich.bank.bank;

import info.kgeorgiy.ja.tregubovich.bank.person.LocalPerson;
import info.kgeorgiy.ja.tregubovich.bank.person.Person;
import info.kgeorgiy.ja.tregubovich.bank.person.RemotePerson;
import info.kgeorgiy.ja.tregubovich.bank.person.RemotePersonImpl;

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
    public Person createPerson(final String name, final String surname, final int passportId) throws RemoteException, PersonAlreadyExistException {
        final RemotePerson person = new RemotePersonImpl(name, surname, passportId, port);
        if (persons.putIfAbsent(passportId, (Person) person) == null) {
            System.out.println("Creating person: " + name +  " " + surname + ", " + passportId);
            UnicastRemoteObject.exportObject(person, port);
            return (Person) person;
        } else {
            System.out.println("Person already exists: " + name +  surname + ", " + passportId);
            final Person expectedPerson = persons.get(passportId);
            if (!expectedPerson.getName().equals(name) || !expectedPerson.getSurname().equals(surname)) {
                throw new PersonAlreadyExistException("Same ID for person: " + name +  surname + ", " + passportId);
            }
            return expectedPerson;
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
