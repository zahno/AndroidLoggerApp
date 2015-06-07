package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;

import android.util.Log;
import de.unibayreuth.bayeosloggerapp.tools.NumberConverter;

public class DataFrame extends Frame {

	public static final String TAG = "DataFrame";
	
	private byte dataFrameType;
	private Frame.Number valueType;
	private Hashtable<Short, Float> values;

	public DataFrame(byte[] payload) {

		super(payload);
		try{
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

			values.put(channel,
					(Float) NumberConverter.getNumber(bf_payload, type));
		}
		this.values = values;
		this.valueType = type;
		this.dataFrameType = dm;}
		catch(BufferUnderflowException e){
			Log.e(TAG, "Buffer Underflow Exception");
		}
	}

	@Override
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
