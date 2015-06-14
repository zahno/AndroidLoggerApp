package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.Vector;

import de.unibayreuth.bayeosloggerapp.frames.serial.SerialFrame;
import de.unibayreuth.bayeosloggerapp.tools.DateAdapter;
import de.unibayreuth.bayeosloggerapp.tools.NumberConverter;

public class Frame {

	public static final byte FRAMETYPE_DATAFRAME = 0x1;
	public static final byte FRAMETYPE_COMMAND = 0x2;
	public static final byte FRAMETYPE_COMMAND_RESPONSE = 0x3;
	public static final byte FRAMETYPE_MESSAGE = 0x4;
	public static final byte FRAMETYPE_ERROR_MESSAGE = 0x5;
	public static final byte FRAMETYPE_ROUTED_FRAME = 0x6;
	public static final byte FRAMETYPE_DELAYED_FRAME = 0x7;
	public static final byte FRAMETYPE_ROUTED_FRAME_RSSI = 0x8;
	public static final byte FRAMETYPE_TIMESTAMP_FRAME = 0x9;

	public static final short Dataframe_Mask = 0xf0;
	public static final byte Valuetype_Mask = 0x0f;

	public static final byte DATAFRAME = 0x20;
	public static final byte DATAFRAME_WITH_CHANNEL_OFFSET = 0x0;
	public static final byte DATAFRAME_WITH_CHANNEL_INDEX = 0x40;

	public static final byte BINARY_DUMP = 0x0A;

	private byte frameType;
	private byte[] data;
	private byte length;

	public static enum Number {
		Float32(0x01), Int32(0x02), Int16(0x03), UInt8(0x04), UInt32(5), Long64(
				6);

		private int byteRepresentation;

		public int getByteRepresentation() {
			return byteRepresentation;
		}

		private Number(int byteRepresentation) {
			this.byteRepresentation = byteRepresentation;
		}

		public int getByteLength() {
			switch (this) {
			case Long64:
				return 8;
			case UInt32:
			case Float32:
			case Int32:
				return 4;
			case Int16:
				return 2;
			case UInt8:
				return 1;
			default:
				return 0;
			}
		}

		public static Number getNumber(byte bitmask) {
			switch (bitmask) {
			case 0x01:
				return Float32;
			case 0x02:
				return Int32;
			case 0x03:
				return Int16;
			case 0x04:
				return UInt8;
			}
			return null;
		}

	};

	public Frame(byte[] payload) {
		length = (byte) payload.length;
		data = new byte[payload.length - 1];
		for (int i = 0; i < payload.length; i++) {
			if (i == 0)
				this.frameType = payload[i];
			else
				this.data[i - 1] = payload[i];
		}
	}

	public static Vector<Frame> toBayEOSFrames(Vector<SerialFrame> serialFrames) {

		Vector<Frame> bayEOSFrames = new Vector<>(serialFrames.size());

		for (SerialFrame serialFrame : serialFrames) {
			byte[] payload = serialFrame.getPayload();
			bayEOSFrames.add(toBayEOSFrame(payload));
		}

		return bayEOSFrames;

	}

	public static Frame toBayEOSFrame(byte[] payload) {
		if (payload.length == 0)
			return null;

		byte frameType = payload[0];
		switch (frameType) {
		case FRAMETYPE_DATAFRAME:
			return new DataFrame(payload);

			// Command and Response
		case FRAMETYPE_COMMAND:
		case FRAMETYPE_COMMAND_RESPONSE:
			return new CommandAndResponseFrame(payload);

			// Message and Error Message
		case FRAMETYPE_MESSAGE:
		case FRAMETYPE_ERROR_MESSAGE:
			return new MessageFrame(payload);

			// TODO not implemented yet
		case FRAMETYPE_ROUTED_FRAME:
			break;
		case FRAMETYPE_DELAYED_FRAME:
			break;
		case FRAMETYPE_ROUTED_FRAME_RSSI:
			break;
		case FRAMETYPE_TIMESTAMP_FRAME:
			break;

		default:
			break;
		}
		return null;
	}

	public static Object[] parsePayload(byte[] payload, Number valuetype) {

		Object[] values;
		int length = payload.length / valuetype.getByteLength();

		// get length of result array
		if (valuetype == Number.Float32)
			values = new Float[length];
		else if (valuetype == Number.Int32)
			values = new Integer[length];
		else if (valuetype == Number.UInt32)
			values = new Long[length];
		else if (valuetype == Number.Int16)
			values = new Short[length];
		else if (valuetype == Number.UInt8)
			values = new Integer[length];
		else
			return null;

		ByteBuffer bf_payload = ByteBuffer.wrap(payload);
		bf_payload.order(ByteOrder.LITTLE_ENDIAN);
		int i = -1;
		while (bf_payload.remaining() > 0) {
			i++;

			values[i] = NumberConverter.getNumber(bf_payload, valuetype);
		}
		return values;
	}

	public byte[] getData() {
		return data;
	}

	public byte getType() {
		return frameType;
	}

	public int getLength() {
		return length;
	}

	public byte[] asByteArray() {
		byte[] b = new byte[1 + data.length];
		b[0] = frameType;
		for (int i = 0; i < data.length; i++)
			b[i + 1] = data[i];
		return b;
	}

}
