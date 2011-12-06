package alpv.calendar;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CalendarServer extends Remote {

	/**
	 * add an event to the database of the server the field e.id has to be
	 * ignored - the server needs to keep track of id-numbers and assign a new
	 * id
	 * 
	 * @param e
	 *            event to add
	 * @return returns the id of the event
	 * @throws RemoteException
	 */
	long addEvent(Event e) throws RemoteException;

	/**
	 * remove an event from the database
	 * 
	 * @param id
	 *            the id of the event
	 * @return true if the event was found and removed
	 * @throws RemoteException
	 */
	boolean removeEvent(long id) throws RemoteException;

	/**
	 * sets every field of the event with the same id to the new values
	 * 
	 * @param id
	 * @return true if e.id exists and the server was able to update every field
	 * @throws RemoteException
	 */
	boolean updateEvent(long id) throws RemoteException;

	/**
	 * 
	 * @param user
	 * @return a list with all the events having user as their user-field
	 * @throws RemoteException
	 */
	List<Event> listEvents(String user) throws RemoteException;

	/**
	 * this method blocks until an event starts for the provided user and then returns the event
	 * @param user
	 * @return the triggering event
	 * @throws RemoteException
	 */
	Event getNextEvent(String user) throws RemoteException;

	/**
	 * used to register the callback and associate it with the user string.
	 * the server is supposed to call back when an event starts for the user.
	 * @param ec
	 * @param user
	 * @throws RemoteException
	 */
	void RegisterCallback(EventCallback ec, String user) throws RemoteException;

	/**
	 * is used to remove the callback from the callbacklist in the server
	 * @param ec
	 * @throws RemoteException
	 */
	void UnregisterCallback(EventCallback ec)
			throws RemoteException;

}
