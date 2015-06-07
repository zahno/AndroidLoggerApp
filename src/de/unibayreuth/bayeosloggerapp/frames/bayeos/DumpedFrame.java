package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import android.app.ProgressDialog;
import android.util.Log;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame.Number;
import de.unibayreuth.bayeosloggerapp.tools.DateAdapter;

public class DumpedFrame {
	Date timestamp;
	byte length;
	Frame frame;

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
			byte[] subframe = new byte[length];

			if (length > rawDumpData.length - i - 4) {
				Log.i("DumpedFrame", "Wrong bulk length");
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
