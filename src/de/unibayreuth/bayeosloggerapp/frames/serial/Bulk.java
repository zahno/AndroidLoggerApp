package de.unibayreuth.bayeosloggerapp.frames.serial;

import java.util.Vector;

import de.unibayreuth.bayeosloggerapp.tools.NumberConverter;
import de.unibayreuth.bayeosloggerapp.tools.StringTools;

public class Bulk extends SerialFrame {

	public static byte StartByte = 0x0a;
	long offset;
	byte[] values;

	public Bulk(byte length, byte apiType, byte[] payload, byte checksum) {
		super(length, apiType, payload, checksum);

		byte[] offset = new byte[4];
		for (int i = 0; i < 4; i++) {
			offset[i] = payload[i + 1];
		}
		this.offset = NumberConverter.fromByteUInt32(offset);

		values = new byte[payload.length - 5];
		for (int i = 0; i < payload.length - 5; i++) {
			values[i] = payload[i + 5];
		}
	}
	
	public byte[] getValues(){
		return values;
	}

	public String toString(){
		return "*Bulk* \tOffset: "+offset+", Payload: "+StringTools.arrayToHexString(values);
	}

	public Vector<Byte> getValuesAsVector() {
		Vector<Byte> v =  new Vector<Byte>(this.values.length);
		for (byte b: values){
			v.add(b);
		}
		return v;
	}
}
