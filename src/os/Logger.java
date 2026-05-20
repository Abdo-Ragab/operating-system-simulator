package os;

public class Logger {
	private static final StringBuilder buffer = new StringBuilder();

//	store
	public static void log(String msg) {
		System.out.println(msg);
		buffer.append(msg).append("\n");
	}

//	print
	public static String drain() {
		String s = buffer.toString();
		buffer.setLength(0);
		return s;
	}
}