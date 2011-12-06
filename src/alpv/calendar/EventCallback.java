package alpv.calendar;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EventCallback extends Remote {
	
	/**
	 * the server calls this method to inform a client when an event starts for the initially provided user
	 * @param e the triggering event
	 * @throws RemoteException
	 */
	
	void call(Event e) throws RemoteException;

}
