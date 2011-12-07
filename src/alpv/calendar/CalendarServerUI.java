package alpv.calendar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class CalendarServerUI implements Runnable {
	private final CalendarServerImpl _server;

	public CalendarServerUI(CalendarServerImpl server) {
		_server = server;
	}

	public void run() {

		System.out.print("Welcome to calendar server.\n"
				+ "Possible commands:\n"
				+ "quite - shutdown the server and save data\n");

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
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
