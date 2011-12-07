package alpv.calendar;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Main {
	private static final String	USAGE	= String.format("usage: java -jar UB%%X_%%NAMEN server PORT%n" +
														"         (to start a server)%n" +
														"or:    java -jar UB%%X_%%NAMEN client SERVERIPADDRESS SERVERPORT%n" +
														"         (to start a client)");

	/**
	 * Starts a server/client according to the given arguments. 
	 * @param args
	 */
	public static void main(String[] args) {
		
		try {
			int i = 0;

			if(args[i].equals("server")) {
				new CalendarServerImpl(new Integer(args[++i]).intValue());
			}
			else if(args[i].equals("client")) {
				(new CalendarClient(args[++i], new Integer(args[++i]).intValue())).run();
			}
			else
				throw new IllegalArgumentException();
		}
		catch(ArrayIndexOutOfBoundsException e) {
			System.err.println(USAGE);
		}
		catch(NumberFormatException e) {
			System.err.println(USAGE);
		}
		catch(IllegalArgumentException e) {
			System.err.println(USAGE);
		} catch (RemoteException e) {
			System.err.println("Can't connect. (RemoteException)");
		} catch (NotBoundException e) {
			System.err.println("Can't connect. (NotBoundException)");
		}
	}
}