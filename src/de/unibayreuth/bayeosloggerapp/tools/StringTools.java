package de.unibayreuth.bayeosloggerapp.tools;

import java.net.HttpURLConnection;

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
	
	public static String byteCountConverter(long bytes) {
	    int unit = 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    char pre = ("kMGTPE").charAt(exp-1);
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
	
	public static String httpCodeToString(int responseCode) {
		String code;
		switch (responseCode) {
		case (HttpURLConnection.HTTP_OK):
			code = responseCode + " (OK)";
			break;
		case (HttpURLConnection.HTTP_UNAUTHORIZED):
			code = responseCode + " (Unautorized)";
			break;
		case (HttpURLConnection.HTTP_NOT_FOUND):
			code = responseCode + " (Not Found)";
			break;
		default:
			code = responseCode + "";
			break;
		}
		return code;
	}
}
