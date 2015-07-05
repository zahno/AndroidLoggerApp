package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ProgressDialog;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame.Number;
import de.unibayreuth.bayeosloggerapp.tools.DateAdapter;

public class DumpedFrame {
	private Date timestamp;
	private byte length;
	private Frame frame;
	
	private static Logger LOG = LoggerFactory.getLogger(DumpedFrame.class);


	public DumpedFrame(byte[] data) {
		byte[] time = new byte[4];
		for (int i = 0; i < 4; i++) {
			time[i] = data[i];
		}
		// Integer
		this.timestamp = DateAdapter.getDate(((Integer[]) Frame.parsePayload(
				time, Number.Int32))[0]);

		length = data[4];

		byte[] byteFrame = new byte[data.length - 5];
		for (int i = 5; i < data.length; i++) {
			byteFrame[i - 5] = data[i];
		}

		frame = Frame.toBayEOSFrame(byteFrame);
	}

	public DumpedFrame(Date timestamp, byte length, Frame frame) {
		this.timestamp = timestamp;
		this.length = length;
		this.frame = frame;
	}

	public static Vector<DumpedFrame> parseDumpFile(byte[] rawDumpData) {
		return parseDumpFile(rawDumpData, null);
	}

	public String toString() {
		return "Dumped Frame: "
				+ new SimpleDateFormat("EEE',' dd.MM.yyyy 'at' HH:mm:ss z",
						Locale.US).format(timestamp) + "; Frame: "
				+ frame.toString();
	}

	public Frame getFrame() {
		return frame;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public byte getLength() {
		return length;
	}

	public void setLength(byte length) {
		this.length = length;
	}

	public static Vector<DumpedFrame> parseDumpFile(byte[] rawDumpData,
			ProgressDialog binaryDumpProgress) {
		Vector<DumpedFrame> frames = new Vector<DumpedFrame>();
		byte length;
		if (binaryDumpProgress != null) {
			binaryDumpProgress.setIndeterminate(false);
			binaryDumpProgress.setMax(frames.size());
		}
		for (int i = 0; i < rawDumpData.length
				&& rawDumpData.length - 1 - i >= 5; i += length + 5) {
			if (binaryDumpProgress != null) {
				binaryDumpProgress.setProgress(i);
			}
			// Timestamp

			byte[] time = new byte[4];
			for (int j = 0; j < 4; j++) {
				time[j] = rawDumpData[i + j];
			}
			Date timestamp = DateAdapter.getDate(((Integer[]) Frame
					.parsePayload(time, Number.Int32))[0]);

			length = rawDumpData[i + 4];
			
			//Something really bad happened. File must be broken! 
			if (length < 1){
				LOG.warn("Corrupt file? The length of a frame must be greater than 1! Abort parsing.");
				break;
				}
			
			byte[] subframe = new byte[length];

			if (length > rawDumpData.length - i - 4) {
				LOG.warn("Corrupt file? The length of the frame exceeds the length of the bulk! Abort parsing.");
				break;
			}

			for (int j = 5; j < length + 5; j++) {
				subframe[j - 5] = rawDumpData[i + j];
			}

			frames.add(new DumpedFrame(timestamp, length, Frame
					.toBayEOSFrame(subframe)));
		}
		return frames;
	}
}
