package alpv.calendar;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CalendarServerImpl implements CalendarServer {

	private HashMap<Long, Event> _events;
	private HashMap<String, ArrayList<Event>> _userEvents;
	private PriorityQueue<Event> _nextEvents;

	public CalendarServerImpl() {
		_events = new HashMap<Long, Event>();
		_nextEvents = new PriorityQueue<Event>();
	}

	/**
	 * Adds an element
	 */
	public long addEvent(Event e) throws RemoteException {
		long id = e.getBegin().getTime();
		while(_events.containsValue(id)) {
			id++;
		}
		e.setId(id);
		_events.put(id, e);
		_nextEvents.add(e);
		
		for(String user : e.getUser()) {
			ArrayList<Event> events = _userEvents.get(user);
			if(events == null) {
				events = new ArrayList<Event>();
			}
			events.add(e);
			_userEvents.put(user, events);
		}
		
		return id;
	}

	@Override
	public boolean removeEvent(long id) throws RemoteException {
		Event e = _events.remove(id);
		
		if(e == null) {
			return false;
		}
		
		_nextEvents.remove(e);
		return true;
	}
	
	@Override
	public boolean updateEvent(long id, Event e) throws RemoteException {
		Event oldEvent = _events.get(id);
		if(oldEvent == null) {
			return false;
		}
		
		if(oldEvent.getBegin().compareTo(e.getBegin()) != 0) {
			
		}
		
		oldEvent.setBegin(e.getBegin());
		oldEvent.setName(e.getName());
		oldEvent.setUser(e.getUser());
		return true;
	}
	
	public List<Event> listEvents(String user) throws RemoteException {
		return _userEvents.get(user);
	}

	@Override
	public Event getNextEvent(String user) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
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

}
