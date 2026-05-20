package info.kgeorgiy.ja.tregubovich.bank.bank;

import info.kgeorgiy.ja.tregubovich.bank.person.Person;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Bank extends Remote {
    /**
     * Creates a new person with specified parameters if it does not already exist.
     *
     * @param name           person's name
     * @param surname        person's surname
     * @param passportId person's passport id
     * @return created or existing person.
     */
    Person createPerson(String name, String surname, int passportId) throws RemoteException, PersonAlreadyExistException;

    /**
     * Returns person by passport id.
     *
     * @param passportId person's passport id
     * @param isLocal        to look up for a local person or not
     * @return person with specified passport id or {@code null} if such account does not exist.
     */
    Person getPerson(int passportId, boolean isLocal) throws RemoteException;
}
