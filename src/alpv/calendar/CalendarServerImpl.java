package alpv.calendar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class CalendarServerImpl extends UnicastRemoteObject implements
		CalendarServer {

	private static final long serialVersionUID = 65300214135859767L;
	private static final String FILE = "calendar.dat";

	private final HashMap<Long, Event> _events;
	private final HashMap<String, PriorityQueue<Event>> _userEvents;

	private final Registry _registry;

	public CalendarServerImpl(int port) throws RemoteException {

		// Load data from file or use new data
		HashMap<Long, Event> events = new HashMap<Long, Event>();
		HashMap<String, PriorityQueue<Event>> userEvents = new HashMap<String, PriorityQueue<Event>>();
		if ((new File(FILE)).exists()) {
			try {
				FileInputStream fin = new FileInputStream(FILE);
				ObjectInputStream ois = new ObjectInputStream(fin);
				CalendarServerData calendarData = (CalendarServerData) ois
						.readObject();
				ois.close();

				events = calendarData.getEvents();
				userEvents = calendarData.getUserEvents();

			} catch (Exception e) {
				System.err.println("Data file not usable.");
			}
		}
		_events = events;
		_userEvents = userEvents;

		// Create RMI
		_registry = LocateRegistry.createRegistry(port);
		_registry.rebind("calendarServer", this);

		try {
			String address = (InetAddress.getLocalHost()).toString();
			System.out.println("Server running @ " + address + ":" + port);
		} catch (UnknownHostException e) {
			System.err.println("Can't determine adress.");
		}

		// Create UI
		(new Thread(new CalendarServerUI(this))).run();
	}

	/**
	 * Adds an event
	 */
	public long addEvent(Event e) throws RemoteException {
		long id = e.getBegin().getTime();
		while (_events.containsValue(id)) {
			id++;
		}
		e.setId(id);

		// Store id -> event
		_events.put(id, e);

		// Store user -> events
		addToUsers(Arrays.asList(e.getUser()), e);

		return id;
	}

	/**
	 * Removes an event by id
	 */
	public boolean removeEvent(long id) throws RemoteException {

		// Remove from storage
		Event e = _events.remove(id);
		if (e == null) {
			return false;
		}

		// Remove from users
		removeFromUsers(Arrays.asList(e.getUser()), e);

		return true;
	}

	/**
	 * Updates an event
	 */
	public boolean updateEvent(long id, Event e) throws RemoteException {
		e.setId(id);
		Event oldEvent = _events.get(id);
		if (oldEvent == null) {
			return false;
		}

		oldEvent.setName(e.getName());

		// Queues sorts only on access
		boolean dateModified = false;
		if (!oldEvent.getBegin().equals(e.getBegin())) {
			dateModified = true;
			oldEvent.setBegin(e.getBegin());
		}

		// Update users
		// Check for added and deleted users
		List<String> oldUsers = Arrays.asList(oldEvent.getUser());

		ArrayList<String> removeUsers = new ArrayList<String>(oldUsers);
		ArrayList<String> modifiyUsers = new ArrayList<String>();
		ArrayList<String> addedUsers = new ArrayList<String>();

		for (String user : e.getUser()) {
			if (oldUsers.contains(user)) {
				modifiyUsers.add(user);
			} else {
				addedUsers.add(user);
			}
			removeUsers.remove(user);
		}

		// Remove deleted users

		removeFromUsers(removeUsers, e);
		// Add added users
		addToUsers(addedUsers, e);
		// Modifiy exisiting users
		if (dateModified) {
			modifiyForUsers(modifiyUsers, e);
		}

		return true;
	}

	/**
	 * Modifiy an event for a list of users
	 */
	private void modifiyForUsers(List<String> users, Event e) {
		for (String user : users) {
			PriorityQueue<Event> events = _userEvents.get(user);
			events.remove(e);
			events.add(e);
			_userEvents.put(user, events);
		}
	}

	/**
	 * Remove an event from a list of users
	 */
	private void removeFromUsers(List<String> users, Event e) {
		for (String user : users) {
			PriorityQueue<Event> events = _userEvents.get(user);

			events.remove(e);
			if (events.isEmpty()) {
				_userEvents.remove(e);
			} else {
				_userEvents.put(user, events);
			}
		}
	}

	/**
	 * Add an event to a list of users
	 */
	private void addToUsers(List<String> users, Event e) {
		for (String user : users) {
			PriorityQueue<Event> events = _userEvents.get(user);
			if (events == null) {
				events = new PriorityQueue<Event>();
			}
			events.add(e);
			_userEvents.put(user, events);
		}
	}

	public List<Event> listEvents(String user) throws RemoteException {

		PriorityQueue<Event> pq = _userEvents.get(user);
		if (pq == null) {
			return new ArrayList<Event>();
		}

		ArrayList<Event> events = new ArrayList<Event>();
		for (Event e : pq) {
			events.add(e);
		}
		return events;
	}

	public Event getNextEvent(String user) throws RemoteException {

		Calendar calendar = Calendar.getInstance();
		Date currentTime = calendar.getTime();

		Event event = null;
		do {
			event = _userEvents.get(user).peek();
		} while (event != null && !event.isAfter(currentTime));

		// TODO blocking

		return event;
	}

	@Override
	public void RegisterCallback(EventCallback ec, String user)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void UnregisterCallback(EventCallback ec) throws RemoteException {
		// TODO Auto-generated method stub

	}

	public void close() {
		// Close RMI
		try {
			_registry.unbind("calendarServer");
		} catch (AccessException e) {
			System.err.println("Can't close server.");
		} catch (RemoteException e) {
			System.err.println("Can't close server.");
		} catch (NotBoundException e) {
			System.err.println("Can't close server.");
		}

		// Store data
		try {
			FileOutputStream fout = new FileOutputStream(FILE);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(new CalendarServerData(_events, _userEvents));
			oos.close();
		} catch (Exception e) {
			System.err.println("Can't store data.");
		}
	}

	private class CalendarServerData implements Serializable {

		private static final long serialVersionUID = 995624507044214456L;
		private HashMap<Long, Event> _events;
		private HashMap<String, PriorityQueue<Event>> _userEvents;

		public CalendarServerData(HashMap<Long, Event> events,
				HashMap<String, PriorityQueue<Event>> userEvents) {
			_events = events;
			_userEvents = userEvents;
		}

		public HashMap<Long, Event> getEvents() {
			return _events;
		}

		public HashMap<String, PriorityQueue<Event>> getUserEvents() {
			return _userEvents;
		}

	}

	private class CalendarServerUI implements Runnable {

		private CalendarServerImpl _server;

		public CalendarServerUI(CalendarServerImpl server) {
			_server = server;
		}

		public void run() {

			System.out.print("Welcome to calendar server.\n"
					+ "Possible commands:\n"
					+ "quite - shutdown the server and save data\n");

			BufferedReader br = new BufferedReader(new InputStreamReader(
					System.in));
			try {
				while (true) {

					String line = br.readLine();

					// Shutdown the server
					if (line.equals("quite")) {
						_server.close();
						break;
					}
				}
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			System.out.println("Bye.");
			System.exit(0);
		}

	}
}
