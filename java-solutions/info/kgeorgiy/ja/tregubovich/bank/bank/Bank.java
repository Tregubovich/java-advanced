package info.kgeorgiy.ja.tregubovich.bank.bank;

import info.kgeorgiy.ja.tregubovich.bank.person.Person;
import info.kgeorgiy.ja.tregubovich.bank.person.RemotePerson;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates a new person with specified parameters if it does not already exist.
     *
     * @param name           person's name
     * @param surname        person's surname
     * @param passportID person's passport id
     * @return created or existing person.
     */
    RemotePerson createPerson(String name, String surname, int passportID) throws RemoteException, PersonAlreadyExistException;

    /**
     * Returns person by passport id.
     *
     * @param passportID person's passport id
     * @param isLocal        to look up for a local person or not
     * @return person with specified passport id or {@code null} if such account does not exist.
     */
    Person getPerson(int passportID, boolean isLocal) throws RemoteException;
}
