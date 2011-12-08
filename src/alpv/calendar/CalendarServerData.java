package alpv.calendar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Container for all server data
 * Provides methods to save to file and load from file
 */
public class CalendarServerData implements Serializable {

	private static final long serialVersionUID = 995624507044214456L;
	private static final String FILE = "calendar.dat";

	private final HashMap<Long, Event> _events;
	private final PriorityBlockingQueue<Event> _upcomingEvents;
	private final HashMap<String, ArrayList<Event>> _userEvents;

	public CalendarServerData(HashMap<Long, Event> events,
			PriorityBlockingQueue<Event> upcomingEvents,
			HashMap<String, ArrayList<Event>> userEvents) {
		_events = events;
		_upcomingEvents = upcomingEvents;
		_userEvents = userEvents;
	}

	public PriorityBlockingQueue<Event> getUpcomingEvents() {
		return _upcomingEvents;
	}

	public HashMap<Long, Event> getEvents() {
		return _events;
	}

	public HashMap<String, ArrayList<Event>> getUserEvents() {
		return _userEvents;
	}

	/**
	 * Load data from file
	 */
	public static CalendarServerData load() {
		if ((new File(FILE)).exists()) {
			try {
				FileInputStream fin = new FileInputStream(FILE);
				ObjectInputStream ois = new ObjectInputStream(fin);
				CalendarServerData calendarData = (CalendarServerData) ois
						.readObject();
				ois.close();

				return calendarData;

			} catch (Exception e) {
				System.err.println("Can't load data file.");
				// e.printStackTrace();
			}
		}
		
		return null;
	}

	/**
	 * Save data to a file
	 */
	public static boolean save(HashMap<Long, Event> events,
			PriorityBlockingQueue<Event> upcomingEvents,
			HashMap<String, ArrayList<Event>> userEvents) {

		try {
			FileOutputStream fout = new FileOutputStream(FILE);
			ObjectOutputStream oos = new ObjectOutputStream(fout);
			oos.writeObject(new CalendarServerData(events, upcomingEvents,
					userEvents));
			oos.close();

			return true;

		} catch (Exception e) {
			return false;
		}

	}
}
