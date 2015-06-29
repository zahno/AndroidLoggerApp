package de.unibayreuth.bayeosloggerapp.tools;

import java.util.Date;

public class DateAdapter {

	static long millisUntilMillenium = 946684800000L;

	public static long fiveMinutesInSeconds = 5 * 60;

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

	/**
	 * Returns true, if the difference between two dates is smaller than a given
	 * number of seconds. False, if the difference is bigger.
	 * 
	 * @param dateA
	 * @param dateB
	 * @param differenceInSeconds
	 * @return
	 */
	public static boolean differenceSmallerThan(Date dateA, Date dateB,
			long differenceInSeconds) {

		if (Math.abs((getSeconds(dateA) - getSeconds(dateB))) < differenceInSeconds) {
			return true;
		}
		return false;
	}

}