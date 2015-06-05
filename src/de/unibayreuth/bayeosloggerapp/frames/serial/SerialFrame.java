package de.unibayreuth.bayeosloggerapp.frames.serial;

import java.util.Vector;

import de.unibayreuth.bayeosloggerapp.frames.bayeos.F_CommandAndResponse;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame;

public class SerialFrame {

	public static final byte[] setReadPointerToEndPositionOfBinaryDump = SerialFrame
			.toSerialFrame(F_CommandAndResponse
					.command_BufferCommand_SetReadPointerToEndPositionOfBinaryDump());

	public static final byte[] modeStop = SerialFrame
			.toSerialFrame(F_CommandAndResponse.command_modeStop());

	public static final byte[] getTime = SerialFrame
			.toSerialFrame(F_CommandAndResponse.command_getTime());

	public static final byte[] getTimeOfNextFrame = SerialFrame
			.toSerialFrame(F_CommandAndResponse.command_getTimeOfNextFrame());

	public static final byte[] getReadPosition = SerialFrame
			.toSerialFrame(F_CommandAndResponse
					.command_BufferCommand_getReadPosition());

	public static final byte[] getName = SerialFrame
			.toSerialFrame(F_CommandAndResponse.command_getName());

	public static final byte[] getVersion = SerialFrame
			.toSerialFrame(F_CommandAndResponse.command_getVersion());

	public static final byte[] getSamplingInterval = SerialFrame
			.toSerialFrame(F_CommandAndResponse.command_getSamplingInterval());

	public static final byte[] startLiveData = SerialFrame
			.toSerialFrame(F_CommandAndResponse.command_startLiveData());

	
	public static final byte FRAME_DELIMITER = 0x7E;
	public static final byte ESCAPE_SIGN = 0x7D;

	public static final byte API_DATA = 0x1;
	public static final byte API_ACK = 0x2;

	public static final byte[] ACK_FRAME = { FRAME_DELIMITER, 0x01, API_ACK,
			0x01, (byte) 0xFC };
	public static final byte[] NACK_FRAME = { FRAME_DELIMITER, 0x01, API_ACK,
			0x02, (byte) 0xFB };
	public static final byte[] BREAK_FRAME = { FRAME_DELIMITER, 0x01, API_ACK,
			0x03, (byte) 0xFA };

	public static final byte[] GET_VERSION = { FRAME_DELIMITER, 0x02, API_DATA,
			0x02, 0x0d, (byte) 0xef };

	private static int id_counter = 1;

	private int id;
	private byte length;
	private byte api_type;
	private byte[] payload;
	private byte checkSum;

	private Boolean isValid;

	public SerialFrame(Vector<Byte> s) {

		this.id = id_counter++;

		this.length = s.get(1);
		this.api_type = s.get(2);

		this.payload = new byte[s.size() - 4];
		for (int i = 3; i < s.size() - 1; i++) {
			this.payload[i - 3] = s.get(i);
		}

		this.checkSum = (byte) (s.lastElement() & 0xff);
	}

	public SerialFrame(byte length, byte apiType, byte[] payload, byte checksum) {
		this.length = length;
		this.api_type = apiType;
		this.payload = payload;
		this.checkSum = checksum;
	}

	public short getLength() {
		return length;
	}

	public short getApiType() {
		return api_type;
	}

	public short getCheckSum() {
		return checkSum;
	}

	public byte[] getPayload() {
		return payload;
	}

	public byte[] asByteArray() {
		byte[] s = new byte[length + 4];
		s[0] = FRAME_DELIMITER;
		s[1] = length;
		s[2] = api_type;
		for (int i = 0; i < length; i++) {
			s[i + 3] = payload[i];
		}
		s[s.length - 1] = checkSum;

		return s;
	}

	public Vector<Byte> asByteVector() {
		Vector<Byte> s = new Vector<>(length + 4);
		s.add(FRAME_DELIMITER);
		s.add(length);
		s.add(api_type);
		for (int i = 0; i < length; i++) {
			s.add(payload[i]);
		}
		s.add(checkSum);

		return s;
	}

	@Override
	public String toString() {
		return "ID: " + id;
	}

	/**
	 * Checks whether or not a data frame is valid by verifying the frame's
	 * checksum
	 * 
	 * @param currentFrame
	 * @return true if valid, false if invalid
	 */
	public boolean isValid() {

		if (isValid != null)
			return isValid;
		// (1) check if 0xFF - (sum of api-type and payload) == checksum

		short sum = (short) (api_type & 0xff);

		for (short pl : payload) {
			sum += pl & 0xff;
		}

		short lastTwoDigits = (short) (sum & 0xff);

		short checkSum_calculated = (short) (0xff - lastTwoDigits);

		isValid = checkSum_calculated == checkSum;
		if (!isValid)
			return isValid;

		// (2) check if sum of api-type, payload and checksum == 0xFF

		sum += checkSum;
		lastTwoDigits = (short) (sum & 0xff);

		isValid = lastTwoDigits == 0xff;

		return isValid;
	}

	public int getId() {
		return id;
	}

	/**
	 * Removes and returns invalid frames of a vector of frames
	 * 
	 * @param frames
	 *            Vector of frames
	 * @return A vector containing invalid data frames
	 */
	public static Vector<SerialFrame> removeInvalidFrames(
			Vector<SerialFrame> frames) {
		Vector<SerialFrame> invalidFrames = new Vector<>();

		for (SerialFrame f : frames) {
			if (f != null && !f.isValid()) {
				System.out.println("Found invalid frame! " + f.toString());
				invalidFrames.add(f);
			}
		}
		frames.removeAll(invalidFrames);
		return invalidFrames;
	}

	public static byte calculateChecksum(byte apiType, byte[] payload) {

		short sum = (short) (apiType & 0xff);

		for (short pl : payload) {
			sum += pl & 0xff;
		}

		short lastTwoDigits = (short) (sum & 0xff);

		return (byte) (0xff - lastTwoDigits);
	}

	/**
	 * 
	 * @param serialFrame
	 *            : [Frame_Delimiter, Length, API-Type, Payload] without
	 *            checksum!
	 * @return
	 */
	private static byte calculateChecksum(byte apiType, byte bayEOSType,
			byte[] bayEOSData) {
		short sum = (short) (apiType & 0xff);

		sum += bayEOSType & 0xff;

		for (byte pl : bayEOSData) {
			sum += pl & 0xff;
		}

		short lastTwoDigits = (short) (sum & 0xff);

		return (byte) (0xff - lastTwoDigits);
	}

	public static byte[] toSerialFrame(Frame bayEOSFrame) {
		Vector<Byte> serialFrame = new Vector<>();
		serialFrame.add(FRAME_DELIMITER);

		// escape bytes
		addEscapedSign(serialFrame, (byte) bayEOSFrame.getLength());
		addEscapedSign(serialFrame, API_DATA);
		addEscapedSign(serialFrame, bayEOSFrame.getType());

		for (byte dataByte : bayEOSFrame.getData()) {
			addEscapedSign(serialFrame, dataByte);
		}

		addEscapedSign(
				serialFrame,
				calculateChecksum(API_DATA, bayEOSFrame.getType(),
						bayEOSFrame.getData()));

		byte[] res = new byte[serialFrame.size()];
		for (int i = 0; i < serialFrame.size(); i++)
			res[i] = serialFrame.get(i);
		return res;
	}

	private static void addEscapedSign(Vector<Byte> serialFrame, byte nextByte) {
		if (nextByte == FRAME_DELIMITER || nextByte == 0x7d || nextByte == 0x11
				|| nextByte == 0x13) {
			serialFrame.add((byte) 0x7d);
			serialFrame.add((byte) (nextByte ^ 0x20));
		} else
			serialFrame.add(nextByte);
	}

	public static SerialFrame toSerialFrame(byte length, byte apiType,
			byte[] payload, byte checksum) {
		if (payload[0] == Bulk.StartByte) {
			return new Bulk(length, apiType, payload, checksum);
		} else
			return new SerialFrame(length, apiType, payload, checksum);

	}
}
