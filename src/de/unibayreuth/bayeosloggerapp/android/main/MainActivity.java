package de.unibayreuth.bayeosloggerapp.android.main;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import de.unibayreuth.bayeosloggerapp.android.slidingtabs.SlidingTabLayout;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.F_CommandAndResponse;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.F_Data;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame;
import de.unibayreuth.bayeosloggerapp.frames.serial.Bulk;
import de.unibayreuth.bayeosloggerapp.frames.serial.SerialFrame;
import de.unibayreuth.bayeosloggerapp.tools.ToastMessage;
import de.unibayreuth.bayeosloggerapp.tools.Tuple;

public class MainActivity extends FragmentActivity {

	private static final String TAG = "Main Activity";
	private static D2xxManager ftD2xx = null;
	private FT_Device ftDev;
	private boolean deviceConnected = false;

	private LoggerFragment loggerFragment;
	private DumpsFragment dumpsFragment;
	private LiveFragment liveFragment;

	static final int READBUF_SIZE = 256;
	byte[] rbuf = new byte[READBUF_SIZE];

	private BlockingQueue<byte[]> writeQueue = new ArrayBlockingQueue<byte[]>(
			100);
	private boolean messageReceived, liveDataStarted = false;
	private Byte bufferCommand = null;

	Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		try {
			ftD2xx = D2xxManager.getInstance(this);
		} catch (D2xxManager.D2xxException ex) {
			Log.e(TAG, ex.toString());
		}

		// add filters for usb attached and detached
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		// Get the ViewPager and set it's PagerAdapter so that it can display
		// items
		ViewPager viewPager = (ViewPager) findViewById(R.id.viewpager);
		viewPager.setOffscreenPageLimit(3);
		viewPager.setAdapter(new AppFragmentPagerAdapter(
				getSupportFragmentManager(), MainActivity.this));

		// Give the SlidingTabLayout the ViewPager
		SlidingTabLayout slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
		// Center the tabs in the layout
		slidingTabLayout.setDistributeEvenly(true);
		slidingTabLayout.setViewPager(viewPager);

	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		closeDevice();
		super.onDestroy();
	};

	boolean openDevice() {
		// device was already opened once
		if (ftDev != null) {
			if (ftDev.isOpen()) {
				if (!deviceConnected()) {
					// mainActivity.updateView(true);
					setDeviceConfig();
					setDeviceConnected(true);
					loggerFragment.initializeLoggerData();
					new Thread(readLoop).start();
					new Thread(writeLoop).start();

					liveFragment.enableContent();
					return true;
				}
				// already open
				return true;
			}
		}

		int devCount = 0;
		devCount = ftD2xx.createDeviceInfoList(this);

		// Log.d(TAG, "Device Number: " + Integer.toString(devCount));

		D2xxManager.FtDeviceInfoListNode[] deviceList = new D2xxManager.FtDeviceInfoListNode[devCount];
		ftD2xx.getDeviceInfoList(devCount, deviceList);

		if (devCount <= 0) {
			return false;
		}

		if (ftDev == null) {
			setDevice(ftD2xx.openByIndex(this, 0));
		} else {
			synchronized (ftDev) {
				setDevice(ftD2xx.openByIndex(this, 0));
			}
		}

		if (ftDev.isOpen()) {
			if (!deviceConnected()) {
				// mainActivity.updateView(true);
				setDeviceConfig();
				setDeviceConnected(true);
				loggerFragment.initializeLoggerData();
				new Thread(readLoop).start();
				new Thread(writeLoop).start();

				liveFragment.enableContent();
				return true;
			}
		}
		return false;
	}

	private void setDeviceConfig() {
		if (ftDev.isOpen() == false) {
			return;
		}

		ftDev.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
		ftDev.setBaudRate(38400);
		ftDev.setDataCharacteristics(D2xxManager.FT_DATA_BITS_8,
				D2xxManager.FT_STOP_BITS_1, D2xxManager.FT_PARITY_NONE);
		ftDev.setFlowControl(D2xxManager.FT_FLOW_NONE, (byte) 0x0b, (byte) 0x0d);
		new Thread() {
			@Override
			public void run() {
				ftDev.purge((byte) (D2xxManager.FT_PURGE_TX | D2xxManager.FT_PURGE_RX));
			}
		}.start();
		ftDev.restartInTask();

	}

	public void closeDevice() {
		deviceConnected = false;
		liveFragment.disableContent();

		if (ftDev != null) {
			ftDev.close();
			ToastMessage.toastDisconnected(this);
		}
	}

	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				// TODO make this work
				// if(loggerFragment.openDevice()){
				// loggerFragment.enableContent();
				// }
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				// ftDev.recycle();
				closeDevice();
				loggerFragment.disableContent();

			}
		}
	};

	protected void addToQueue(byte[] serialFrame) {
		try {
			writeQueue.put(serialFrame);
		} catch (InterruptedException e) {
			Log.e(TAG, "Could not add new frame to queue: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private Runnable readLoop = new Runnable() {

		@Override
		public void run() {
			boolean escaped = true;

			while (true) {

				if (!deviceConnected()) {
					break;
				}

				synchronized (ftDev) {
					if (loggerFragment.dumpInterrupted()) {
						Log.e(TAG, "Dump interrupted");
						ftDev.write(SerialFrame.modeStop);
						loggerFragment.setDumpInterrupted(false);
						continue;
					}

					// Frame Delimiter
					Tuple<Integer, Byte> delimiter;
					do {
						delimiter = readByte(!escaped);
						if (delimiter.getFirst() == 0)
							break;
					} while (delimiter.getSecond() != 0x7e);

					// case: no new message received (count received bytes = 0)
					if (delimiter.getFirst() == 0) {
						messageReceived = false;
						// Log.i(TAG,
						// "Readthread: No new message received! Notify writeThread");
						try {
							ftDev.notify();
							ftDev.wait();
						} catch (InterruptedException e) {
							Log.e(TAG,
									"Wait in readLoop failed! "
											+ e.getMessage());
							e.printStackTrace();
						}
						continue;
					}

					messageReceived = true;

					// Length
					byte length = readByte(escaped).getSecond();
					// Log.i(TAG, "Length: " + length);

					// API Type
					byte apiType = readByte(escaped).getSecond();
					// Log.i(TAG, "API Type: " + apiType);

					// Payload
					byte[] payload = new byte[length];
					for (int i = 0; i < length; i++) {
						payload[i] = readByte(escaped).getSecond();
					}

					// StringBuilder sb = new StringBuilder();
					// sb.append("Payload: [");
					// sb.append(String.format("%02X", payload[0]));
					// for (int i = 1; i < length; i++) {
					// sb.append(", ");
					// sb.append(String.format("%02X", payload[i]));
					// }
					// sb.append("]");
					// Log.i(TAG, sb.toString());

					byte checksum = readByte(escaped).getSecond();

					if (checksum == SerialFrame.calculateChecksum(apiType,
							payload)) {

						if (payload.length == 1 && payload[0] == 0x1) {
							// Log.i(TAG, "Ack Frame received");
						} else {
							// Log.i(TAG, "New frame received!");

							ftDev.write(SerialFrame.ACK_FRAME);
							handleSerialFrame(SerialFrame.toSerialFrame(length,
									apiType, payload, checksum));
						}

					} else {
						ftDev.write(SerialFrame.NACK_FRAME);

					}
				}
			}
		}

		private Tuple<Integer, Byte> readByte(boolean byteEscaped) {

			int bytesToRead = 1;
			int receivedBytes = ftDev.read(rbuf, bytesToRead, 50);
			if (receivedBytes != 0) {

				if (!byteEscaped || rbuf[0] != 0x7d)
					return new Tuple<Integer, Byte>(receivedBytes, rbuf[0]);

				else {
					ftDev.read(rbuf, bytesToRead);
					return new Tuple<Integer, Byte>(receivedBytes,
							(byte) (rbuf[0] ^ 0x20));
				}
			}
			return new Tuple<Integer, Byte>(receivedBytes, (byte) 0);
		}
	};

	protected void handleSerialFrame(SerialFrame serialFrame) {

		if (serialFrame instanceof Bulk) {
			loggerFragment.handleBulk((Bulk) serialFrame);
			Log.i(TAG, serialFrame.toString());
		} else
			handlePayload(serialFrame.getPayload());
	}

	protected void handlePayload(byte[] payload) {
		final Frame receivedFrame = Frame.toBayEOSFrame(payload);
		if (receivedFrame != null)
			Log.i(TAG, receivedFrame.toString());
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				if (receivedFrame instanceof F_Data) {
					liveFragment.handle_DataFrame((F_Data) receivedFrame);
				}
				if (receivedFrame instanceof F_CommandAndResponse) {
					switch (((F_CommandAndResponse) receivedFrame)
							.getCommandType()) {
					case (F_CommandAndResponse.BayEOS_SetCannelAddress):
						break;
					case (F_CommandAndResponse.BayEOS_GetCannelAddress):
						break;
					case (F_CommandAndResponse.BayEOS_SetAutoSearch):
						break;
					case (F_CommandAndResponse.BayEOS_GetAutoSearch):
						break;
					case (F_CommandAndResponse.BayEOS_SetPin):
						break;
					case (F_CommandAndResponse.BayEOS_GetPin):
						break;
					case (F_CommandAndResponse.BayEOS_GetTime):
						loggerFragment
								.handle_GetTime((F_CommandAndResponse) receivedFrame);
						break;
					case (F_CommandAndResponse.BayEOS_SetTime):
						break;
					case (F_CommandAndResponse.BayEOS_GetName):
						loggerFragment
								.handle_GetName((F_CommandAndResponse) receivedFrame);

						break;
					case (F_CommandAndResponse.BayEOS_SetName):
						loggerFragment
								.handle_SetName((F_CommandAndResponse) receivedFrame);
						break;
					case (F_CommandAndResponse.BayEOS_StartData):
						break;
					case (F_CommandAndResponse.BayEOS_StopData):
						break;
					case (F_CommandAndResponse.BayEOS_GetVersion):
						loggerFragment
								.handle_GetVersion((F_CommandAndResponse) receivedFrame);

						break;
					case (F_CommandAndResponse.BayEOS_GetSamplingInt):
						loggerFragment
								.handle_GetSamplingInterval((F_CommandAndResponse) receivedFrame);

						break;
					case (F_CommandAndResponse.BayEOS_SetSamplingInt):
						loggerFragment
								.handle_SetSamplingInterval((F_CommandAndResponse) receivedFrame);

						break;
					case (F_CommandAndResponse.BayEOS_TimeOfNextFrame):
						loggerFragment
								.handle_TimeOfNextFrame((F_CommandAndResponse) receivedFrame);

						break;
					case (F_CommandAndResponse.BayEOS_StartLiveData):
						liveDataStarted = true;
						break;
					case (F_CommandAndResponse.BayEOS_ModeStop):
						liveDataStarted = false;
						break;
					case (F_CommandAndResponse.BayEOS_Seek):
						break;
					case (F_CommandAndResponse.BayEOS_StartBinaryDump):
						loggerFragment
								.handle_StartBinaryDump((F_CommandAndResponse) receivedFrame);

						break;
					case (F_CommandAndResponse.BayEOS_BufferCommand):
						switch (bufferCommand) {
						case (F_CommandAndResponse.BayEOS_BufferCommand_Erase):
							loggerFragment.updateTime();
							break;
						case (F_CommandAndResponse.BayEOS_BufferCommand_GetReadPosition):
							loggerFragment
									.handle_BuferCommand_GetReadPosition((F_CommandAndResponse) receivedFrame);

							break;
						case (F_CommandAndResponse.BayEOS_BufferCommand_GetWritePosition):
							break;
						case (F_CommandAndResponse.BayEOS_BufferCommand_SaveCurrentReadPointerToEEPROM):
							break;
						case (F_CommandAndResponse.BayEOS_BufferCommand_SetReadPointerToEndPositionOfBinaryDump):
							break;
						case (F_CommandAndResponse.BayEOS_BufferCommand_SetReadPointerToWritePointer):
							break;
						case (F_CommandAndResponse.BayEOS_BufferCommand_SetReadPointerToLastEEPROMPosition):
							break;
						default:
							Log.e(TAG,
									"Received Buffer Command Answer but no command was sent");
						}
						bufferCommand = null;

						break;
					default:
						break;
					}
				}
			}
		});

	}

	private Runnable writeLoop = new Runnable() {

		@Override
		public void run() {

			while (true) {

				if (!deviceConnected()) {
					break;
				}

				synchronized (getDevice()) {

					if (messageReceived) {
						try {
							// Log.i(TAG,
							// "Writethread: received Message true, so notify and wait");
							ftDev.notify();
							ftDev.wait();
						} catch (InterruptedException e) {
							Log.e(TAG,
									"Wait in writeLoop failed! "
											+ e.getMessage());
							e.printStackTrace();
						}
					}

					if (!writeQueue.isEmpty()) {
						messageReceived = true;

						try {
							byte[] current = writeQueue.take();
							ftDev.write(current);
							// Log.i(TAG,
							// "Writethread: no new message, so send message from queue");
							ftDev.notify();
							ftDev.wait();
						} catch (InterruptedException e) {
							Log.e(TAG,
									"Wait in writeLoop failed! "
											+ e.getMessage());
							e.printStackTrace();
						}
					} else {
						try {
							ftDev.notify();
							ftDev.wait();
						} catch (InterruptedException e) {
							Log.e(TAG, writeLoop + e.getMessage());
							e.printStackTrace();
						}
					}
				}

			}
		}
	};

	public static void enable(ViewGroup layout) {
		enOrDisable(layout, true);
	}

	public static void disable(ViewGroup layout) {
		enOrDisable(layout, false);
	}

	private static void enOrDisable(ViewGroup layout, boolean enable) {
		layout.setEnabled(enable);
		for (int i = 0; i < layout.getChildCount(); i++) {
			View child = layout.getChildAt(i);
			if (child instanceof ViewGroup) {
				enOrDisable((ViewGroup) child, enable);
			} else {
				child.setEnabled(enable);
			}
		}
	}

	protected void removeFromQueue(byte[] serialFrame) {
		writeQueue.remove(serialFrame);
	}

	public FT_Device getDevice() {
		return ftDev;
	}

	public void setDevice(FT_Device dev) {
		this.ftDev = dev;
	}

	public D2xxManager getD2xxManager() {
		return ftD2xx;
	}

	public boolean deviceConnected() {
		return deviceConnected;
	}

	public void setDeviceConnected(boolean b) {
		deviceConnected = b;
	}

	public LoggerFragment getLoggerFragment() {
		return loggerFragment;
	}

	public void setLoggerFragment(LoggerFragment loggerFragment) {
		this.loggerFragment = loggerFragment;
	}

	public DumpsFragment getDumpsFragment() {
		return dumpsFragment;
	}

	public void setDumpsFragment(DumpsFragment dumpsFragment) {
		this.dumpsFragment = dumpsFragment;
	}

	public LiveFragment getLiveFragment() {
		return liveFragment;
	}

	public void setLiveFragment(LiveFragment liveFragment) {
		this.liveFragment = liveFragment;
	}

	public void setBufferCommand(byte expectedBufferCommand) {
		this.bufferCommand = expectedBufferCommand;
	}

}