package alpv.calendar;

import java.util.Calendar;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Checks event queue for now starting events
 */
public class CalendarServerNotificator implements Runnable {

	private final CalendarServerImpl _server;
	private static final int SLEEP = 200;
	private long _lastRun;

	public CalendarServerNotificator(CalendarServerImpl server) {
		_server = server;
		_lastRun = currentTime();
	}

	/**
	 * Start checking
	 */
	public void run() {

		while (_server.running()) {

			PriorityBlockingQueue<Event> events = _server.getUpcomingEvents();

			// Remove all events before last run
			Event event = events.peek();
			boolean found = false;
			while (event != null && !found) {
				if (_lastRun > event.getBegin().getTime()) {
					events.poll();
					event = events.peek();
				} else {
					found = true;
				}
			}

			// Notifies server if events happen in current iteration
			boolean finished = false;
			while(event != null && !finished) {
				// Check, whether notify now or later
				long abs = event.getBegin().getTime() - _lastRun;
				if (abs < SLEEP) {
					// Notifiy
					events.poll();
					_server.notify(event);
					event = events.peek();
				}
				else {
					finished = true;
				}
			}

			_lastRun = currentTime();

			try {
				Thread.sleep(SLEEP);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * Returns current time in millis
	 */
	public long currentTime() {
		Calendar calendar = Calendar.getInstance();
		return calendar.getTime().getTime();
	}
}