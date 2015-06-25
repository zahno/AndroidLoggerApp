package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.util.Date;

import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame.Number;
import de.unibayreuth.bayeosloggerapp.tools.DateAdapter;
import de.unibayreuth.bayeosloggerapp.tools.NumberConverter;

public class TimestampFrame {

	private byte type;
	private Date date;
	private Frame frame;

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

	public TimestampFrame(DumpedFrame dumpedFr) {
		this.type = Frame.FRAMETYPE_TIMESTAMP_FRAME;
		this.date = dumpedFr.getTimestamp();
		this.frame = dumpedFr.getFrame();
	}
	
	public byte[] asByteArray(){
		byte[] b = new byte[1 + 4+ frame.getLength()];
		b[0] = this.type;
		
		long timestamp = DateAdapter.getSeconds(date);
		byte[] ts = NumberConverter.toByte(timestamp, Number.UInt32);
		
		for (int i = 0; i < ts.length; i++){
			b[i+1] = ts[i];
		}
		
		byte[] fr = frame.asByteArray();
		
		for (int i = 0; i < frame.getLength(); i++){
			b[i+5] = fr[i];
		}
		
		return b;
		
	}
	
	
}
