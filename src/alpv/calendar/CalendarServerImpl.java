package alpv.calendar;

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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

public class CalendarServerImpl extends UnicastRemoteObject implements
		CalendarServer {

	private static final long serialVersionUID = 65300214135859767L;

	private final HashMap<Long, Event> _events;
	private final PriorityQueue<Event> _upcomingEvents;
	private final HashMap<String, ArrayList<Event>> _userEvents;
	private final HashMap<String, ArrayList<EventCallback>> _userCallbacks;

	private boolean _running;
	private final Registry _registry;

	public CalendarServerImpl(int port) throws RemoteException {

		// Load data from file or use new data
		CalendarServerData calendarData = CalendarServerData.load();
		if (calendarData == null) {
			_events = new HashMap<Long, Event>();
			_upcomingEvents = new PriorityQueue<Event>();
			_userEvents = new HashMap<String, ArrayList<Event>>();
		} else {
			_events = calendarData.getEvents();
			_upcomingEvents = calendarData.getUpcomingEvents();
			_userEvents = calendarData.getUserEvents();
		}

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

		// Set running
		_running = true;

		// Create UI
		(new Thread(new CalendarServerUI(this))).start();

		// Create Notificator
		(new Thread(new CalendarServerNotificator(this))).start();
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

		// Add to queue
		_upcomingEvents.add(e);

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

		// Remove from queue
		_upcomingEvents.remove(e);

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
		removeFromUsers(removeUsers, localEvent);
		// Add added users
		addToUsers(addedUsers, localEvent);
		// Update users in our event
		localEvent.setUser(remoteEvent.getUser());

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

	/**
	 * List events for a specific user
	 */
	public List<Event> listEvents(String user) throws RemoteException {
		ArrayList<Event> events = _userEvents.get(user);
		if (events == null) {
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
		if (callbacks == null) {
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
		for (ArrayList<EventCallback> list : values) {
			if (list.contains(ec)) {
				list.remove(ec);
			}
		}
	}

	/**
	 * Unregister from a single notification
	 */
	public void UnregisterCallback(EventCallback ec, String user)
			throws RemoteException {
		ArrayList<EventCallback> callbacks = _userCallbacks.get(user);
		if (callbacks != null) {
			callbacks.remove(ec);
			_userCallbacks.put(user, callbacks);
		}
	}

	/**
	 * Get list of upcoming events
	 */
	PriorityQueue<Event> getUpcomingEvents() {
		return _upcomingEvents;
	}

	/**
	 * Notifiy about an event
	 */
	public void notify(Event e) {
		System.out.println("Notifiy");

		for (String user : e.getUser()) {
			ArrayList<EventCallback> callbacks = _userCallbacks.get(user);
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

	}

	/**
	 * Close the server
	 */
	public void close() {

		_running = false;

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

		// Save data
		if (!CalendarServerData.save(_events, _upcomingEvents, _userEvents)) {
			System.err.println("Can't save data.");
		}
	}

	/**
	 * Server running?
	 */
	public boolean running() {
		return _running;
	}

	/**
	 * Removes all data
	 */
	public void flush() {
		_events.clear();
		_upcomingEvents.clear();
		_userEvents.clear();
		_userCallbacks.clear();
	}
}