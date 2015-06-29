package de.unibayreuth.bayeosloggerapp.frames.bayeos;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

import de.unibayreuth.bayeosloggerapp.tools.DateAdapter;
import de.unibayreuth.bayeosloggerapp.tools.NumberConverter;
import de.unibayreuth.bayeosloggerapp.tools.StringTools;

public class CommandAndResponseFrame extends Frame {

	// Frame Type
	public static final byte frameType_Command = 0x2;
	public static final byte frameType_Response = 0x3;

	// BayEOS Commands
	public static final byte BayEOS_SetCannelAddress = 0x1;
	public static final byte BayEOS_GetCannelAddress = 0x2;
	public static final byte BayEOS_SetAutoSearch = 0x3;
	public static final byte BayEOS_GetAutoSearch = 0x4;
	public static final byte BayEOS_SetPin = 0x5;
	public static final byte BayEOS_GetPin = 0x6;
	public static final byte BayEOS_GetTime = 0x7;
	public static final byte BayEOS_SetTime = 0x8;
	public static final byte BayEOS_GetName = 0x9;
	public static final byte BayEOS_SetName = 0xa;
	public static final byte BayEOS_StartData = 0xb;
	public static final byte BayEOS_StopData = 0xc;
	/**
	 * Data 1 = reset buffer, 2 = reset read pointer , 3 = set read pointer to
	 * write pointer, Still working but depreciated!!
	 */
	public static final byte BayEOS_GetVersion = 0xd;
	public static final byte BayEOS_GetSamplingInt = 0xe;
	public static final byte BayEOS_SetSamplingInt = 0xf;
	public static final byte BayEOS_TimeOfNextFrame = 0x10;
	public static final byte BayEOS_StartLiveData = 0x11;
	public static final byte BayEOS_ModeStop = 0x12; /*
													 * renamed from StopLiveData
													 * - Should be used to stop
													 * DUMP/LIVE/SEND-Mode
													 */
	public static final byte BayEOS_Seek = 0x13;
	public static final byte BayEOS_StartBinaryDump = 0x14;
	/** [unsigned long start_pos - optional][unsigned long end_pos - optional] */
	public static final byte BayEOS_BufferCommand = 0x15;

	/**
	 * 0: save current read pointer to EEPROM, 1: erase, 2: set read pointer to
	 * last EEPROM pos 3 = set read pointer to write pointer,4 = set read
	 * pointer to end pos of binary dump, 5 = get read pos, 6 get write pos
	 */

	public static final byte BayEOS_BufferCommand_SaveCurrentReadPointerToEEPROM = 0x0;
	public static final byte BayEOS_BufferCommand_Erase = 0x1;
	public static final byte BayEOS_BufferCommand_SetReadPointerToLastEEPROMPosition = 0x2;
	public static final byte BayEOS_BufferCommand_SetReadPointerToWritePointer = 0x3;
	public static final byte BayEOS_BufferCommand_SetReadPointerToEndPositionOfBinaryDump = 0x4;
	public static final byte BayEOS_BufferCommand_GetReadPosition = 0x5;
	public static final byte BayEOS_BufferCommand_GetWritePosition = 0x6;

	byte frameType;
	byte commandType;
	byte[] values;

	public CommandAndResponseFrame(byte[] payload) {
		super(payload);
		this.frameType = payload[0];
		this.commandType = payload[1];
		this.values = new byte[payload.length - 2];
		for (int i = 0; i < payload.length - 2; i++) {
			values[i] = payload[i + 2];
		}
	}

	public static CommandAndResponseFrame command_getVersion() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_GetVersion });
	}

	public static Frame command_getName() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_GetName });
	}

	public static Frame command_getSamplingInterval() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_GetSamplingInt });
	}

	public static Frame command_getTime() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_GetTime });
	}

	public static Frame command_getTimeOfNextFrame() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_TimeOfNextFrame });
	}

	public static Frame command_setName(CharSequence name) {
		byte[] arg = new byte[name.length() + 2];
		arg[0] = frameType_Command;
		arg[1] = BayEOS_SetName;
		for (int i = 0; i < name.length(); i++) {
			arg[i + 2] = (byte) name.charAt(i);
		}
		return new CommandAndResponseFrame(arg);
	}

	public static Frame command_setTime() {
		byte[] arg = new byte[6];
		arg[0] = frameType_Command;
		arg[1] = BayEOS_SetTime;
		long currentTimeInSeconds = DateAdapter.getSeconds(new Date());
		byte[] ts = NumberConverter.toByte(currentTimeInSeconds, Number.UInt32);

		for (int i = 0; i < ts.length; i++) {
			arg[i + 2] = ts[i];
		}
		return new CommandAndResponseFrame(arg);
	}

	public static Frame command_setSamplingInterval(
			CharSequence samplingInterval) {

		short samplingInt = Short.parseShort(samplingInterval.toString());

		byte[] bytes = new byte[2];
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
				.put(samplingInt);

		byte[] arg = new byte[2 + samplingInterval.length() * 2];

		arg[0] = frameType_Command;
		arg[1] = BayEOS_SetSamplingInt;
		for (int i = 0; i < bytes.length; i++) {
			arg[i + 2] = bytes[i];
		}
		return new CommandAndResponseFrame(arg);
	}

	public static Frame command_BufferCommand_saveCurrentReadPointerPositionToEEPROM() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_BufferCommand,
				BayEOS_BufferCommand_SaveCurrentReadPointerToEEPROM });
	}

	public static Frame command_BufferCommand_erase() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_BufferCommand, BayEOS_BufferCommand_Erase });
	}

	public static Frame command_BufferCommand_setReadPointerToLastEEPROMPosition() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_BufferCommand,
				BayEOS_BufferCommand_SetReadPointerToLastEEPROMPosition });
	}

	public static Frame command_BufferCommand_SetReadPointerToWritePointer() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_BufferCommand,
				BayEOS_BufferCommand_SetReadPointerToWritePointer });
	}

	public static Frame command_BufferCommand_SetReadPointerToEndPositionOfBinaryDump() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_BufferCommand,
				BayEOS_BufferCommand_SetReadPointerToEndPositionOfBinaryDump });
	}

	public static Frame command_BufferCommand_getReadPosition() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_BufferCommand, BayEOS_BufferCommand_GetReadPosition });
	}

	public static Frame command_BufferCommand_getWritePosition() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_BufferCommand, BayEOS_BufferCommand_GetWritePosition });
	}

	public static Frame command_modeStop() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_ModeStop });
	}

	public static Frame command_startBinaryDump(Integer readPosition) {
		byte[] bytes = new byte[4];
		ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
				.put(readPosition);

		byte[] arg = new byte[6];

		arg[0] = frameType_Command;
		arg[1] = BayEOS_StartBinaryDump;
		for (int i = 0; i < bytes.length; i++) {
			arg[i + 2] = bytes[i];
		}

		return new CommandAndResponseFrame(arg);
	}

	public static Frame command_startLiveData() {
		return new CommandAndResponseFrame(new byte[] { frameType_Command,
				BayEOS_StartLiveData });

	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		switch (frameType) {
		case frameType_Command:
			sb.append("Command: ");
			break;
		case frameType_Response:
			sb.append("Response to: ");
			break;
		default:
			break;
		}
		switch (commandType) {
		case (BayEOS_SetCannelAddress):
			sb.append("Set Channel Adress. ");
			break;
		case (BayEOS_GetCannelAddress):
			sb.append("Get Channel Adress. ");
			break;
		case (BayEOS_SetAutoSearch):
			sb.append("Set Auto Search. ");
			break;
		case (BayEOS_GetAutoSearch):
			sb.append("Get Auto Search. ");
			break;
		case (BayEOS_SetPin):
			sb.append("Set Pin. ");
			break;
		case (BayEOS_GetPin):
			sb.append("Get Pin. ");
			break;
		case (BayEOS_GetTime):
			sb.append("Get Time. ");
			sb.append("Value: ");
			Integer[] time = (Integer[]) parsePayload(values, Number.Int32); // Integer
			sb.append(DateAdapter.getDate(time[0]));
			break;
		case (BayEOS_SetTime):
			sb.append("Set Time. ");
			break;
		case (BayEOS_GetName):
			sb.append("Get Name. ");
			sb.append("Value: ");
			sb.append(StringTools.asciiToString(values));
			break;
		case (BayEOS_SetName):
			sb.append("Set Name. ");
			break;
		case (BayEOS_StartData):
			sb.append("Start Data. ");
			break;
		case (BayEOS_StopData):
			sb.append("Stop Data. ");
			break;
		case (BayEOS_GetVersion):
			sb.append("Get Version. ");
			sb.append("Value: ");
			sb.append(StringTools.asciiToString(values));
			break;
		case (BayEOS_GetSamplingInt):
			sb.append("Get Sampling Interval. ");
			sb.append("Value: ");
			Short[] interval = (Short[]) parsePayload(values, Number.Int16); // Short
			sb.append(interval[0]);
			break;
		case (BayEOS_SetSamplingInt):
			sb.append("Set Sampling Interval. ");
			break;
		case (BayEOS_TimeOfNextFrame):
			sb.append("Time of Next Frame. ");
			Integer[] nextFrameTime = (Integer[]) parsePayload(values,
					Number.Int32); // Integer
			sb.append(DateAdapter.getDate(nextFrameTime[0]));
			break;
		case (BayEOS_StartLiveData):
			sb.append("Start Live Data. ");
			break;
		case (BayEOS_ModeStop):
			sb.append("Mode Stop. ");
			break;
		case (BayEOS_Seek):
			sb.append("Seek. ");
			break;
		case (BayEOS_StartBinaryDump):
			sb.append("Start Binary Dump. ");
			break;
		case (BayEOS_BufferCommand):
			sb.append("Buffer Command. ");
			break;
		default:
			break;
		}

		return sb.toString();

	}

	public byte getCommandType() {
		return commandType;
	}

	public byte[] getValues() {
		return values;
	}

}
