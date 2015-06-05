package de.unibayreuth.bayeosloggerapp.tools;

public class StringTools {
	public static String asciiToString(byte[] ascii) {
		StringBuilder sb = new StringBuilder();
		for (byte b : ascii) {
			sb.append((char) b);
		}
		return sb.toString();
	}

	public static String arrayToHexString(Object[] values) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < values.length; i++) {
			if (i == 0)
				sb.append(String.format("%02X", values[0]));
			else
				sb.append(", " + String.format("%02X", values[i]));
		}
		return sb.toString();
	}

	public static String arrayToHexString(byte[] values) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < values.length; i++) {
			if (i == 0)
				sb.append(String.format("%02X", values[0]));
			else
				sb.append(", " + String.format("%02X", values[i]));
		}
		return sb.toString();
	}

	public static String arrayToString(Object[] values) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < values.length; i++) {
			if (i == 0)
				sb.append(values[0]);
			else
				sb.append(", " + values[i]);
		}
		return sb.toString();
	}
}
