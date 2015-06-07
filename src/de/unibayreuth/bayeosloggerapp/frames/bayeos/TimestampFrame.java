package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.util.Date;

import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame.Number;
import de.unibayreuth.bayeosloggerapp.tools.DateAdapter;

public class TimestampFrame {

	byte type;
	Date date;
	Frame frame;

	public TimestampFrame(byte[] data) {
		this.type = Frame.FRAMETYPE_TIMESTAMP_FRAME;
		byte[] time = new byte[4];
		for (int i = 1; i < 5; i++) {
			time[i] = data[i];
		}
		// Integer
		this.date = DateAdapter.getDate(((Integer[]) Frame.parsePayload(time,
				Number.Int32))[0]);

		byte[] byteFrame = new byte[data.length - 5];
		for (int i = 5; i < data.length; i++) {
			byteFrame[i - 5] = data[i];
		}

		frame = Frame.toBayEOSFrame(byteFrame);
	}
}
