package de.unibayreuth.bayeosloggerapp.tools;

import java.util.Date;

public class DateAdapter {

	static long millisUntilMillenium = 946684800000L;

	/*
	 * @param d Date
	 * 
	 * @return Seconds since millenium (01/01/2000)
	 */
	public static long getSeconds(Date d) {
		if (d.getTime() < millisUntilMillenium)
			throw new IllegalArgumentException("Date is before millenium");
		long millis = d.getTime() - millisUntilMillenium;
		return (millis - millis % 1000) / 1000;
	}

	/*
	 * @param seconds Seconds since millenium (01/01/2000)
	 * 
	 * @return Date
	 */
	public static Date getDate(long seconds) {
		return new Date(millisUntilMillenium + seconds * 1000);
	}

}