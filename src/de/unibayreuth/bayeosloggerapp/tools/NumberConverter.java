package de.unibayreuth.bayeosloggerapp.tools;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame;

public class NumberConverter {

	public static ByteOrder order = ByteOrder.LITTLE_ENDIAN;

	
	public static byte[] toByte(Number value, Frame.Number valueType) {
		switch (valueType) {
		case Float32:
			return toByteFloat32((Float) value);
		case UInt32:
			return toByteUInt32((Long) value);
		case Int16:
			return toByteInt16((Short) value);
		case Int32:
			return toByteInt32((Integer) value);
		case UInt8:
			byte[] b = new byte[1];
			b[0] = value.byteValue();
			return b;
		default:
			return null;
		}

	}

	// Double 64
	public static byte[] toByteDouble(Double value) {
		byte[] b = new byte[8];
		ByteBuffer bb = ByteBuffer.wrap(b);
		bb.order(order);
		bb.putDouble(value);
		return b;
	}

	public static Double fromByteDouble(byte[] value) {
		return fromByteDouble(value, 0);
	}

	public static Double fromByteDouble(byte[] value, int offset) {
		ByteBuffer bb = ByteBuffer.wrap(value, offset, 8);
		bb.order(order);
		return bb.getDouble();
	}

	// Float32
	public static byte[] toByteFloat32(Float value) {
		byte[] b = new byte[4];
		ByteBuffer bb = ByteBuffer.wrap(b);
		bb.order(order);
		bb.putFloat(value);
		return b;
	}

	public static Float fromByteFloat32(byte[] value) {
		return fromByteFloat32(value, 0);
	}

	public static Float fromByteFloat32(byte[] value, int offset) {
		ByteBuffer bb = ByteBuffer.wrap(value, offset, 4);
		bb.order(order);
		return bb.getFloat();
	}

	// Int32
	public static byte[] toByteInt32(Integer value) {
		byte[] b = new byte[4];
		ByteBuffer bb = ByteBuffer.wrap(b);
		bb.order(order);
		bb.putInt(value);
		return b;
	}

	public static Integer fromByteInt32(byte[] value) {
		return fromByteInt32(value, 0);
	}

	public static Integer fromByteInt32(byte[] value, int offset) {
		ByteBuffer bb = ByteBuffer.wrap(value, offset, 4);
		bb.order(order);
		return bb.getInt();
	}

	// UInt32
	public static byte[] toByteUInt32(Long value) {
		if (value < 0)
			throw new IllegalArgumentException("Value is out of valid range.");
		byte[] b = new byte[4];
		if (order == ByteOrder.BIG_ENDIAN) {
			b[0] = (byte) ((value >> 24) & 0xff);
			b[1] = (byte) ((value >> 16) & 0xff);
			b[2] = (byte) ((value >> 8) & 0xff);
			b[3] = (byte) (value & 0xff);
		} else {
			b[3] = (byte) ((value >> 24) & 0xff);
			b[2] = (byte) ((value >> 16) & 0xff);
			b[1] = (byte) ((value >> 8) & 0xff);
			b[0] = (byte) (value & 0xff);
		}
		return b;
	}

	public static Long fromByteUInt32(byte[] value) {
		return fromByteUInt32(value, 0);
	}

	public static Long fromByteUInt32(byte[] value, int offset) {
		ByteBuffer bb = ByteBuffer.wrap(value, offset, 4);
		bb.order(order);
		return fromByteUInt32(bb);
	}

	public static Long fromByteUInt32(ByteBuffer bb) {
		Long ret;
		if (order == ByteOrder.LITTLE_ENDIAN) {
			ret = (bb.get() & 0xffL);
			ret += (bb.get() & 0xffL) << 8;
			ret += (bb.get() & 0xffL) << 16;
			ret += (bb.get() & 0xffL) << 24;
		} else {
			ret = (bb.get() & 0xffL) << 24;
			ret += (bb.get() & 0xffL) << 16;
			ret += (bb.get() & 0xffL) << 8;
			ret += (bb.get() & 0xffL);
		}
		return ret;
	}

	// Int16
	public static byte[] toByteInt16(Short value) {
		byte[] b = new byte[2];
		ByteBuffer bb = ByteBuffer.wrap(b);
		bb.order(order);
		bb.putShort(value);
		return b;
	}

	public static Short fromByteInt16(byte[] value) {
		return fromByteInt16(value, 0);
	}

	public static Short fromByteInt16(byte[] value, int offset) {
		ByteBuffer bb = ByteBuffer.wrap(value, offset, 2);
		bb.order(order);
		return bb.getShort();
	}

	// UInt8
	public static byte toByteUInt8(int value) {
		return (byte) value;
	}

	public static int fromByteUInt8(byte value) {
		return 0xff & value;
	}

	public static Object getNumber(byte[] payload, Frame.Number valuetype) {
		switch (valuetype) {
		case Float32: // Float
			return fromByteFloat32(payload);
		case Int32: // Integer
			return fromByteInt32(payload);
		case Int16: // Short
			return fromByteInt16(payload);
		case UInt8: // UInt8
			return fromByteUInt8(payload[0]);
		case Long64:
			return null;
		case UInt32:
			return fromByteUInt32(payload);
		default:
			break;
		}
		return null;
	}

	public static Object getNumber(ByteBuffer bb, Frame.Number valuetype) {
		switch (valuetype) {
		case Float32: // Float
			return bb.getFloat();
		case Int32: // Integer
			return bb.getInt();
		case Int16: // Short
			return bb.getShort();
		case UInt8: // UInt8
			return 0xff & bb.get();
		case Long64:
			return null;
		case UInt32:
			return fromByteUInt32(bb);
		default:
			break;
		}
		return null;
	}

}
