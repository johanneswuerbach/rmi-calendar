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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class CalendarServerImpl extends UnicastRemoteObject implements
		CalendarServer {

	private static final long serialVersionUID = 65300214135859767L;
	private static final String FILE = "calendar.dat";

	private final HashMap<Long, Event> _events;
	private final PriorityQueue<Event> _upcomingEvents;
	private final HashMap<String, ArrayList<Event>> _userEvents;
	private final HashMap<String, ArrayList<EventCallback>> _userCallbacks;

	private final Registry _registry;

	public CalendarServerImpl(int port) throws RemoteException {

		// Load data from file or use new data
		HashMap<Long, Event> events = new HashMap<Long, Event>();
		HashMap<String, ArrayList<Event>> userEvents = new HashMap<String, ArrayList<Event>>();
		PriorityQueue<Event> upcomingEvents = new PriorityQueue<Event>();

		if ((new File(FILE)).exists()) {
			try {
				FileInputStream fin = new FileInputStream(FILE);
				ObjectInputStream ois = new ObjectInputStream(fin);
				CalendarServerData calendarData = (CalendarServerData) ois
						.readObject();
				ois.close();

				events = calendarData.getEvents();
				upcomingEvents = calendarData.getUpcomingEvents();
				userEvents = calendarData.getUserEvents();

			} catch (Exception e) {
				System.err.println("Data file not usable.");
			}
		}
		_events = events;
		_upcomingEvents = upcomingEvents;
		_userEvents = userEvents;
		_userCallbacks = new HashMap<String, ArrayList<EventCallback>>();

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

		// Create Notificator
		(new Thread(new CalendarServerNotificator(this))).run();
	}

	/**
	 * Adds an event
	 */
	public synchronized long addEvent(Event e) throws RemoteException {
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
	public synchronized boolean removeEvent(long id) throws RemoteException {

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
	public synchronized boolean updateEvent(long id, Event remoteEvent)
			throws RemoteException {
		remoteEvent.setId(id);
		Event localEvent = _events.get(id);
		if (localEvent == null) {
			return false;
		}

		localEvent.setName(remoteEvent.getName());

		// If date changed, remove and readd to queue
		if (!localEvent.getBegin().equals(remoteEvent.getBegin())) {
			_upcomingEvents.remove(localEvent);
			localEvent.setBegin(remoteEvent.getBegin());
			_upcomingEvents.add(localEvent);
		}

		// Update users
		// Check for added and deleted users
		List<String> oldUsers = Arrays.asList(localEvent.getUser());

		ArrayList<String> removeUsers = new ArrayList<String>(oldUsers);
		ArrayList<String> addedUsers = new ArrayList<String>();

		for (String user : remoteEvent.getUser()) {
			if (!oldUsers.contains(user)) {
				addedUsers.add(user);
			}
			removeUsers.remove(user);
		}

		// Remove deleted users
		removeFromUsers(removeUsers, remoteEvent);
		// Add added users
		addToUsers(addedUsers, remoteEvent);

		return true;
	}

	/**
	 * Remove an event from a list of users
	 */
	private void removeFromUsers(List<String> users, Event e) {
		for (String user : users) {
			ArrayList<Event> events = _userEvents.get(user);

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
			ArrayList<Event> events = _userEvents.get(user);
			if (events == null) {
				events = new ArrayList<Event>();
			}
			events.add(e);
			_userEvents.put(user, events);
		}
	}

	public List<Event> listEvents(String user) throws RemoteException {
		ArrayList<Event> events = _userEvents.get(user);
		if(events == null) {
			events = new ArrayList<Event>();
		}
		return events;
	}

	public Event getNextEvent(String user) throws RemoteException {
		// TODO
		return null;
	}

	/**
	 * Register for event callbacks for a specific user
	 */
	public void RegisterCallback(EventCallback ec, String user)
			throws RemoteException {
		
		ArrayList<EventCallback> callbacks = _userCallbacks.get(user);
		if(callbacks == null) {
			callbacks = new ArrayList<EventCallback>();
		}
		callbacks.add(ec);
		
		_userCallbacks.put(user, callbacks);
		
	}
	
	/**
	 * Unregister from event all registered callbacks
	 */
	public void UnregisterCallback(EventCallback ec) throws RemoteException {
		Collection<ArrayList<EventCallback>> values = _userCallbacks.values();
		for(ArrayList<EventCallback> list : values) {
			if(list.contains(ec)) {
				list.remove(ec);
			}
		}
	}
	
	/**
	 * Unregister from a single notification
	 */
	public void UnregisterCallback(EventCallback ec, String user) throws RemoteException {
		ArrayList<EventCallback> callbacks = _userCallbacks.get(user);
		if(callbacks != null) {
			callbacks.remove(ec);
			_userCallbacks.put(user, callbacks);
		}
	}

	/**
	 * Get list of upcoming events
	 */
	private PriorityQueue<Event> getUpcomingEvents() {
		return _upcomingEvents;
	}

	/**
	 * Notifiy about an event
	 */
	public void nextEvent(Event e) {
		ArrayList<EventCallback> callbacks = _userCallbacks.get(e.getUser());
		if (callbacks != null) {
			for (EventCallback callback : callbacks) {
				try {
					callback.call(e);
				} catch (RemoteException e1) {
					System.err.println("Callback failed.");
				}
			}
		}

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
			oos.writeObject(new CalendarServerData(_events, _upcomingEvents,
					_userEvents));
			oos.close();
		} catch (Exception e) {
			System.err.println("Can't store data.");
		}
	}

	private class CalendarServerData implements Serializable {

		private static final long serialVersionUID = 995624507044214456L;
		private final HashMap<Long, Event> _events;
		private final PriorityQueue<Event> _upcomingEvents;
		private final HashMap<String, ArrayList<Event>> _userEvents;

		public CalendarServerData(HashMap<Long, Event> events,
				PriorityQueue<Event> upcomingEvents,
				HashMap<String, ArrayList<Event>> userEvents) {
			_events = events;
			_upcomingEvents = upcomingEvents;
			_userEvents = userEvents;
		}

		public PriorityQueue<Event> getUpcomingEvents() {
			return _upcomingEvents;
		}

		public HashMap<Long, Event> getEvents() {
			return _events;
		}

		public HashMap<String, ArrayList<Event>> getUserEvents() {
			return _userEvents;
		}

	}

	private class CalendarServerUI implements Runnable {

		private final CalendarServerImpl _server;

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
			// No other way to close RMI, ...
			System.exit(0);
		}
	}

	private class CalendarServerNotificator implements Runnable {

		private final CalendarServerImpl _server;
		private static final int SLEEP = 200;

		public CalendarServerNotificator(CalendarServerImpl server) {
			_server = server;
		}

		@Override
		public void run() {
			
			Calendar calendar = Calendar.getInstance();
			long currentTime = calendar.getTime().getTime();

			PriorityQueue<Event> events = _server.getUpcomingEvents();
			Event event = events.peek();
			if(event != null) {
				long abs = currentTime - event.getBegin().getTime();
				if(abs < SLEEP * (-1)) {
					// Rather old event
					events.poll();
				}
				else if(abs < SLEEP) {
					// Notifiy
					events.poll();
					_server.nextEvent(event);
				}
			}
			
			
			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}
	}
}
