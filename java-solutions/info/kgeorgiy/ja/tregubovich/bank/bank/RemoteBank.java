package info.kgeorgiy.ja.tregubovich.bank.bank;

import info.kgeorgiy.ja.tregubovich.bank.person.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<Integer, RemotePerson> persons = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public RemotePerson createPerson(final String name, final String surname, final int passportID) throws RemoteException, PersonAlreadyExistException {
        final RemotePerson person = new RemotePersonImpl(name, surname, passportID, port);
        if (persons.putIfAbsent(passportID, person) == null) {
            System.out.println("Creating person: " + name +  " " + surname + ", " + passportID);
            UnicastRemoteObject.exportObject(person, port);
            return person;
        } else {
            System.out.println("Person already exists: " + name + " " + surname + ", " + passportID);
            final RemotePerson expectedPerson = persons.get(passportID);
            if (!expectedPerson.getName().equals(name) || !expectedPerson.getSurname().equals(surname)) {
                throw new PersonAlreadyExistException("Same ID for person: " + name +  surname + ", " + passportID);
            }
            return expectedPerson;
        }
    }

    @Override
    public Person getPerson(final int passportID, final boolean isLocal) throws RemoteException {
        System.out.println("Retrieving person: " + passportID);
        final Person person = (Person) persons.get(passportID);
        if (person != null && isLocal) {
            return new LocalPerson(person);
        }
        return person;
    }
}
