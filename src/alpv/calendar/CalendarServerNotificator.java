package alpv.calendar;

import java.util.Calendar;
import java.util.PriorityQueue;

public class CalendarServerNotificator implements Runnable {

	private final CalendarServerImpl _server;
	private static final int SLEEP = 200;
	private long _lastRun;

	public CalendarServerNotificator(CalendarServerImpl server) {
		_server = server;
		_lastRun = currentTime();
	}

	@Override
	public void run() {

		while (_server.running()) {

			PriorityQueue<Event> events = _server.getUpcomingEvents();

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

			if (event != null) {
				// Check, whether notify now or later
				long abs = event.getBegin().getTime() - _lastRun;
				if (abs < SLEEP) {
					// Notifiy
					events.poll();
					_server.notify(event);
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

	public long currentTime() {
		Calendar calendar = Calendar.getInstance();
		return calendar.getTime().getTime();
	}
}