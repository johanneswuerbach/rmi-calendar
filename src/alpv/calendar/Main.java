package alpv.calendar;

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
		
		java.util.HashMap<Integer, String> map = new java.util.HashMap<Integer, String>();
		map.put(1, "1");
		map.put(5, "5");
		map.put(8, "8");
		map.put(20, "2");
		map.put(9, "9");
		map.put(3, "3");
		
		for(String test : map.values()) {
			System.out.println(test);
		}
		
		try {
			int i = 0;

			if(args[i].equals("server")) {
				// TODO
			}
			else if(args[i].equals("client")) {
				// TODO
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
		}
	}
}