package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;

import de.unibayreuth.bayeosloggerapp.tools.NumberConverter;

public class F_Data extends Frame {

	byte dataFrameType;
	Frame.Number valueType;
	Hashtable<Short, Float> values;

	public F_Data(byte[] payload) {

		super(payload);

		ByteBuffer bf_payload = ByteBuffer.wrap(payload);
		bf_payload.order(ByteOrder.LITTLE_ENDIAN);

		// second byte of payload
		bf_payload.position(1);
		byte valueType = bf_payload.get();

		// check first digit of the value type for dataframe type
		byte dm = (byte) (valueType & Dataframe_Mask);

		if (!(dm == Frame.DATAFRAME || dm == Frame.DATAFRAME_WITH_CHANNEL_INDEX || dm == Frame.DATAFRAME_WITH_CHANNEL_OFFSET))
			System.out.println("Unknown Data Frame Type");

		// check second digit of the value type for the actual value type
		byte vm = (byte) (valueType & Valuetype_Mask);

		Frame.Number type = Number.getNumber(vm);

		if (!(vm == Number.Float32.getByteRepresentation()
				|| vm == Number.Int16.getByteRepresentation()
				|| vm == Number.Int32.getByteRepresentation() || vm == Number.UInt8
					.getByteRepresentation()))
			System.out.println("Unknown Data Frame Type");

		// TODO how big can channel be?
		short channel = 0;
		if (dm == DATAFRAME_WITH_CHANNEL_OFFSET) {
			// third byte of payload
			channel = (short) (bf_payload.get() & 0xff);
		}

		int valuesNumber = bf_payload.limit() / type.getByteLength();

		Hashtable<Short, Float> values = new Hashtable<Short, Float>(
				valuesNumber);
		while (bf_payload.remaining() > 0) {
			// Read Channel
			if (dm == DATAFRAME_WITH_CHANNEL_INDEX) {
				// third byte of payload
				channel = (short) (bf_payload.get() & 0xff);
			} else {
				channel++;
			}
			// Read Value

			values.put(channel,
					(Float) NumberConverter.getNumber(bf_payload, type));
			// switch (vm) {
			// case (VALUETYPE_FLOAT32): // Float
			// values.put(channel, bf_payload.getFloat());
			// break;
			// case (VALUETYPE_INT32): // Integer
			// values.put(channel, (float) bf_payload.getInt());
			// break;
			// case (VALUETYPE_INT16): // Short
			// values.put(channel, (float) bf_payload.getShort());
			// break;
			// case (VALUETYPE_UINT8): // UInt8
			// values.put(channel, (float) (bf_payload.get() & 0xff));
			// break;
			// }
		}
		this.values = values;
		this.valueType = type;
		this.dataFrameType = dm;
	}

	public String toString() {
		String valtype = "";
		switch (valueType) {
		case Float32:
			valtype = "Float32";
			break;
		case Int16:
			valtype = "Int16";
			break;
		case Int32:
			valtype = "Int32";
			break;
		case UInt8:
			valtype = "UInt8";
			break;
		case Long64:
			valtype = "Long64";
			break;
		case UInt32:
			valtype = "UInt32";
			break;
		default:
			break;
		}
		return "Type: Data Frame \tValue Type:\t" + valtype
				+ "\t\tChannel = Value: " + values;
	}

	public Hashtable<Short, Float> getValues() {
		return values;
	}

}
