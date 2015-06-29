package de.unibayreuth.bayeosloggerapp.android.main;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;

import de.unibayreuth.bayeosloggerapp.android.slidingtabs.SlidingTabLayout;
import de.unibayreuth.bayeosloggerapp.android.tools.ToastMessage;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.CommandAndResponseFrame;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DataFrame;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame;
import de.unibayreuth.bayeosloggerapp.frames.serial.Bulk;
import de.unibayreuth.bayeosloggerapp.frames.serial.SerialFrame;
import de.unibayreuth.bayeosloggerapp.tools.StringTools;
import de.unibayreuth.bayeosloggerapp.tools.Tuple;

public class MainActivity extends AppCompatActivity {

	// private static final String TAG = "Main Activity";

	private Logger LOG = LoggerFactory.getLogger(MainActivity.class);
	private boolean loggingenabled = false;

	private static D2xxManager ftD2xx = null;
	private FT_Device ftDev;
	private boolean deviceConnected = false;

	private LoggerFragment loggerFragment;
	private DumpsFragment dumpsFragment;
	private LiveFragment liveFragment;
	private AppPreferences appPreferences;

	static final int READBUF_SIZE = 100;
	byte[] rbuf = new byte[READBUF_SIZE];

	private BlockingQueue<byte[]> writeQueue = new ArrayBlockingQueue<byte[]>(
			100);
	private boolean messageReceived, liveDataStarted = false;
	private Byte bufferCommand = null;

	Handler mHandler = new Handler();

	public static final String DirectoryNameRaw = "BayEOS_Logger//Raw_Dumps";
	public static final String DirectoryNameParsed = "BayEOS_Logger//Parsed_Dumps";

	/**
	 * This method is called on create of the application.
	 * 
	 * @author Christiane Goehring
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setLoggingPreference();

		if (loggingenabled)
			LOG.info("======== BayEOS Logger App started ========");

		try {
			ftD2xx = D2xxManager.getInstance(this);
		} catch (D2xxManager.D2xxException ex) {
			if (loggingenabled)
				LOG.error(ex.getMessage());
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

		slidingTabLayout
				.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
					@Override
					public void onPageScrolled(int position,
							float positionOffset, int positionOffsetPixels) {
						// Do nothing
					}

					@Override
					public void onPageSelected(int position) {
						if (position == 0) {
							if (ftDev != null && ftDev.isOpen())
								loggerFragment.updateTime();
						} else if (position == 1) {
							dumpsFragment.refreshTable();
						} else if (position == 2) {
							// Whenever third fragment is visible, do something
						}
					}

					@Override
					public void onPageScrollStateChanged(int state) {
						// Do nothing
					}
				});

		getSupportActionBar().setIcon(R.drawable.ic_launcher);
		getSupportActionBar().setTitle(
				"  " + getResources().getString(R.string.app_name));
		getSupportActionBar().setDisplayUseLogoEnabled(true);
		getSupportActionBar().setDisplayShowHomeEnabled(true);

	}

	/**
	 * This method creates the "..." menu in the upper right corner of the
	 * application
	 * 
	 * @author Christiane Goehring
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * This method is calles when the user taps on an item of the "..." menu in
	 * the upper right
	 * 
	 * @author Christiane Goehring
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {

		case R.id.action_info:
			openInfo();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Called when the info-dialog is opened
	 * 
	 * @author Christiane Goehring
	 */
	private void openInfo() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.info).setTitle("About");

		AlertDialog alert = builder.create();
		alert.show();

	}

	/**
	 * Called when the application is being destroyed. Unregisters the Broadcast
	 * Receiver and closes the connection to the logger-device.
	 * 
	 * @author Christiane Goehring
	 */
	@Override
	protected void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		closeDevice();
		super.onDestroy();
	};

	/**
	 * Opens the connection to the logger-device
	 * 
	 * @return True, if connected successfully. False otherwise.
	 * @author Christiane Goehring
	 */
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

					loggerFragment.resetView();

					liveFragment.resetView();
					liveFragment.enableContent();

					return true;
				}
				// already open
				return true;
			}
		}

		int devCount = 0;
		devCount = ftD2xx.createDeviceInfoList(this);

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

				loggerFragment.resetView();

				liveFragment.enableContent();
				liveFragment.resetView();

				return true;
			}
		}
		return false;
	}

	/**
	 * Sets the configuration for the logger-device
	 * 
	 * @author Christiane Goehring
	 */
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

	/**
	 * Closes the connection to the logger-device
	 * 
	 * @author Christiane Goehring
	 */
	public void closeDevice() {
		deviceConnected = false;
		triggerModeStop();
		liveFragment.disableContent();

		if (ftDev != null) {
			ftDev.close();
			ToastMessage.toastDisconnected(this);
		}

	}

	/**
	 * This is the Broadcast Receiver that makes it possible to detect when a
	 * USB device is attached and ask whether this app should open or not. Also,
	 * when the USB is detached, the connection closes.
	 * 
	 * @author Christiane Goehring
	 */
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

	/**
	 * Adds a new frame to the queue of messages to be sent to the logger
	 * 
	 * @author Christiane Goehring
	 */
	protected void addToQueue(byte[] serialFrame) {

		try {
			if (liveDataStarted) {
				writeQueue.put(SerialFrame.modeStop);
			}
			writeQueue.put(serialFrame);
		} catch (InterruptedException e) {
			if (loggingenabled)
				LOG.warn("Failed to add SerialFrame to Writing-Queue: {}",
						e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * This is the Read Thread. As long as a device is connected this thread
	 * reads bytes sent by the logger and builds frames out of them. If there
	 * are no bytes in the reading-buffer then the Write Loop is notified.
	 * 
	 * 
	 * @author Christiane Goehring
	 */
	private Runnable readLoop = new Runnable() {

		@Override
		public void run() {
			boolean escaped = true;
			Thread.currentThread().setName("Read Thread");

			while (true) {

				if (!deviceConnected()) {
					break;
				}

				synchronized (ftDev) {
					// check if in dump mode and dump was interrupted
					if (loggerFragment.dumpInterrupted()) {
						int writtenBytes = ftDev.write(SerialFrame.modeStop);
						if (loggingenabled)
							LOG.info("Wrote 'modeStop' to device.");
						if (writtenBytes != SerialFrame.modeStop.length)
							while (writtenBytes != SerialFrame.modeStop.length) {
								writtenBytes = ftDev
										.write(SerialFrame.modeStop);
								if (loggingenabled)
									LOG.info("Wrote 'modeStop' again (Couldn't write frame to device)");
							}
					}

					// Search for the Frame Delimiter
					Tuple<Integer, Byte> delimiter;
					do {
						delimiter = readByte(!escaped);
						if (delimiter.getFirst() == 0)
							break;
					} while (delimiter.getSecond() != 0x7e);

					if (delimiter.getFirst() == 0) {
						messageReceived = false;
						try {
							ftDev.notify();
							ftDev.wait();
						} catch (InterruptedException e) {
							if (loggingenabled)
								LOG.warn("Wait in readLoop failed: {}"
										+ e.getMessage());
							e.printStackTrace();
						}
						continue;
					}

					// Found the start of a new frame

					messageReceived = true;

					StringBuilder sb = new StringBuilder();
					sb.append("Received new frame! ");
					// Length
					byte length = readByte(escaped).getSecond();
					sb.append("[Length: " + length + "] ");

					// API Type
					byte apiType = readByte(escaped).getSecond();

					// Payload
					byte[] payload = new byte[length];
					for (int i = 0; i < length; i++) {
						payload[i] = readByte(escaped).getSecond();
					}

					byte checksum = readByte(escaped).getSecond();

					// Check if checksum correct
					if (checksum == SerialFrame.calculateChecksum(apiType,
							payload)) {

						// Is the received frame an ACK frame?
						if (payload.length == 1 && payload[0] == 0x1) {
							if (loggingenabled)
								LOG.info("Received ACK");

						}
						// otherwise
						else {
							if (loggingenabled)
								LOG.info(sb.toString());

							int writtenBytes = ftDev
									.write(SerialFrame.ACK_FRAME);

							if (loggingenabled)
								LOG.info("Wrote ACK");

							if (writtenBytes != SerialFrame.ACK_FRAME.length)
								while (writtenBytes != SerialFrame.ACK_FRAME.length) {
									writtenBytes = ftDev
											.write(SerialFrame.ACK_FRAME);
									LOG.info("Wrote ACK again (Couldn't write frame to device)");
								}
							// ftDev.write(SerialFrame.ACK_FRAME);
							handleSerialFrame(SerialFrame.toSerialFrame(length,
									apiType, payload, checksum));
						}

					} else {
						ftDev.write(SerialFrame.NACK_FRAME);

					}
				}
			}
		}

		/**
		 * Reads a single byte from the read buffer
		 * 
		 * @param byteEscaped
		 *            Is the byte possible escaped?
		 * @return The next byte of the read buffer
		 * 
		 * @author Christiane Goehring
		 */
		private Tuple<Integer, Byte> readByte(boolean byteEscaped) {

			int bytesToRead = 1;
			int receivedBytes = ftDev.read(rbuf, bytesToRead, 200);

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

	/**
	 * Handles Serial Frames
	 * 
	 * @param serialFrame
	 *            A Serial Frame sent by the logger
	 * 
	 * @author Christiane Goehring
	 */
	protected void handleSerialFrame(SerialFrame serialFrame) {

		// Is the frame a Bulk frame (a frame that is sent when dump mode is
		// activated)
		if (serialFrame instanceof Bulk) {
			loggerFragment.handleBulk((Bulk) serialFrame);

		}
		// otherwise
		else
			handlePayload(serialFrame.getPayload());
	}

	/**
	 * This method handles the payload of a Serial Frame.
	 * 
	 * @param payload
	 * 
	 * @author Christiane Goehring
	 */
	protected void handlePayload(byte[] payload) {
		final Frame receivedFrame = Frame.toBayEOSFrame(payload);
		if (receivedFrame != null)
			if (loggingenabled)
				LOG.info("Received Frame contains: [{}]",
						receivedFrame.toString());
		mHandler.post(new Runnable() {

			@Override
			public void run() {
				if (receivedFrame instanceof DataFrame) {
					liveFragment.handle_DataFrame((DataFrame) receivedFrame);
				}
				if (receivedFrame instanceof CommandAndResponseFrame) {
					switch (((CommandAndResponseFrame) receivedFrame)
							.getCommandType()) {
					case (CommandAndResponseFrame.BayEOS_SetCannelAddress):
						break;
					case (CommandAndResponseFrame.BayEOS_GetCannelAddress):
						break;
					case (CommandAndResponseFrame.BayEOS_SetAutoSearch):
						break;
					case (CommandAndResponseFrame.BayEOS_GetAutoSearch):
						break;
					case (CommandAndResponseFrame.BayEOS_SetPin):
						break;
					case (CommandAndResponseFrame.BayEOS_GetPin):
						break;
					case (CommandAndResponseFrame.BayEOS_GetTime):
						loggerFragment
								.handle_GetTime((CommandAndResponseFrame) receivedFrame);
						break;
					case (CommandAndResponseFrame.BayEOS_SetTime):
						break;
					case (CommandAndResponseFrame.BayEOS_GetName):
						loggerFragment
								.handle_GetName((CommandAndResponseFrame) receivedFrame);

						break;
					case (CommandAndResponseFrame.BayEOS_SetName):
						loggerFragment
								.handle_SetName((CommandAndResponseFrame) receivedFrame);
						break;
					case (CommandAndResponseFrame.BayEOS_StartData):
						break;
					case (CommandAndResponseFrame.BayEOS_StopData):
						break;
					case (CommandAndResponseFrame.BayEOS_GetVersion):
						loggerFragment
								.handle_GetVersion((CommandAndResponseFrame) receivedFrame);

						break;
					case (CommandAndResponseFrame.BayEOS_GetSamplingInt):
						loggerFragment
								.handle_GetSamplingInterval((CommandAndResponseFrame) receivedFrame);

						break;
					case (CommandAndResponseFrame.BayEOS_SetSamplingInt):
						loggerFragment
								.handle_SetSamplingInterval((CommandAndResponseFrame) receivedFrame);

						break;
					case (CommandAndResponseFrame.BayEOS_TimeOfNextFrame):
						loggerFragment
								.handle_TimeOfNextFrame((CommandAndResponseFrame) receivedFrame);

						break;
					case (CommandAndResponseFrame.BayEOS_StartLiveData):
						liveDataStarted = true;
						break;
					case (CommandAndResponseFrame.BayEOS_ModeStop):
						triggerModeStop();
						break;
					case (CommandAndResponseFrame.BayEOS_Seek):
						break;
					case (CommandAndResponseFrame.BayEOS_StartBinaryDump):
						loggerFragment
								.handle_StartBinaryDump((CommandAndResponseFrame) receivedFrame);

						break;
					case (CommandAndResponseFrame.BayEOS_BufferCommand):
						switch (bufferCommand) {
						case (CommandAndResponseFrame.BayEOS_BufferCommand_Erase):
							loggerFragment.updateTime();
							break;
						case (CommandAndResponseFrame.BayEOS_BufferCommand_GetReadPosition):
							loggerFragment
									.handle_BufferCommand_GetReadPosition((CommandAndResponseFrame) receivedFrame);

							break;
						case (CommandAndResponseFrame.BayEOS_BufferCommand_GetWritePosition):
							break;
						case (CommandAndResponseFrame.BayEOS_BufferCommand_SaveCurrentReadPointerToEEPROM):
							break;
						case (CommandAndResponseFrame.BayEOS_BufferCommand_SetReadPointerToEndPositionOfBinaryDump):
							break;
						case (CommandAndResponseFrame.BayEOS_BufferCommand_SetReadPointerToWritePointer):
							break;
						case (CommandAndResponseFrame.BayEOS_BufferCommand_SetReadPointerToLastEEPROMPosition):
							break;
						default:
							if (loggingenabled)
								LOG.info("Received Buffer Command Answer but no command was sent");
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

	/**
	 * 
	 * Resets the values related to the "mode stop" command
	 * 
	 * @author Christiane Goehring
	 */
	protected void triggerModeStop() {
		if (liveDataStarted) {
			ToastMessage.toastMessageBottom(this, "Live Mode stopped");

			liveFragment.getToggleButton().setChecked(false);
			liveDataStarted = false;
		} else if (loggerFragment.dumpInterrupted()) {
			loggerFragment.setDumpInterrupted(false);
			ToastMessage.toastMessageBottom(this, "Dump stopped");
		}
	}

	/**
	 * This is the Write Thread. As long as a device is connected this thread
	 * sends bytes from the writeQueue to the logger. If there are no elements
	 * in the writeQueue the Read Thread is notified.
	 * 
	 */
	private Runnable writeLoop = new Runnable() {

		@Override
		public void run() {

			Thread.currentThread().setName("Write Thread");

			while (true) {

				if (!deviceConnected()) {
					break;
				}

				synchronized (getDevice()) {

					if (messageReceived) {
						try {
							ftDev.notify();
							ftDev.wait();
						} catch (InterruptedException e) {
							if (loggingenabled)
								LOG.warn("Wait in writeLoop failed! {} ",
										e.getMessage());
						}
					}

					if (!writeQueue.isEmpty()) {
						messageReceived = true;

						try {
							byte[] current = writeQueue.take();
							if (loggingenabled)
								LOG.info("Wrote [{}] to device.",
										StringTools.arrayToHexString(current));
							ftDev.write(current);
							ftDev.notify();
							ftDev.wait();
						} catch (InterruptedException e) {
							if (loggingenabled)
								LOG.warn("Wait in writeLoop failed! {} ",
										e.getMessage());
						}
					} else {
						try {
							ftDev.notify();
							ftDev.wait();
						} catch (InterruptedException e) {
							if (loggingenabled)
								LOG.warn("Wait in writeLoop failed! {} ",
										e.getMessage());
						}
					}
				}

			}
		}
	};

	/**
	 * Enables the contents of a ViewGroup
	 * 
	 * @param layout
	 */
	public static void enable(ViewGroup layout) {
		enOrDisable(layout, true);
	}

	/**
	 * Disables the contents of a ViewGroup
	 * 
	 * @param layout
	 */
	public static void disable(ViewGroup layout) {
		enOrDisable(layout, false);
	}

	/**
	 * Dis- or enables the contents of a ViewGroup.
	 * 
	 * @param layout
	 * @param enable
	 */
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

	/**
	 * Removes an item from the writeQueue
	 * 
	 * @param serialFrame
	 */
	protected boolean removeFromQueue(byte[] serialFrame) {
		return writeQueue.remove(serialFrame);
	}

	/**
	 * @return Returns the logger-device
	 */
	public FT_Device getDevice() {
		return ftDev;
	}

	/**
	 * Sets the logger-device
	 * 
	 * @param dev
	 *            Logger
	 */
	public void setDevice(FT_Device dev) {
		this.ftDev = dev;
	}

	/**
	 * 
	 * @return returns the device manager
	 */
	public D2xxManager getD2xxManager() {
		return ftD2xx;
	}

	/**
	 * 
	 * @return True, if a device is connected. False otherwise.
	 */

	public boolean deviceConnected() {
		return deviceConnected;
	}

	/**
	 * Sets if a device is connected
	 * 
	 * @param b
	 */
	public void setDeviceConnected(boolean b) {
		deviceConnected = b;
	}

	/**
	 * 
	 * @return The Logger Fragment
	 */
	public LoggerFragment getLoggerFragment() {
		return loggerFragment;
	}

	/**
	 * Sets the Logger Fragment
	 * 
	 * @param loggerFragment
	 */
	public void setLoggerFragment(LoggerFragment loggerFragment) {
		this.loggerFragment = loggerFragment;
	}

	/**
	 * 
	 * @return The Dump Fragment
	 */
	public DumpsFragment getDumpsFragment() {
		return dumpsFragment;
	}

	/**
	 * Sets the Dump Fragment
	 * 
	 * @param dumpsFragment
	 */
	public void setDumpsFragment(DumpsFragment dumpsFragment) {
		this.dumpsFragment = dumpsFragment;
	}

	/**
	 * 
	 * @return The Live Fragment
	 */
	public LiveFragment getLiveFragment() {
		return liveFragment;
	}

	/**
	 * Sets the Live Fragment
	 * 
	 * @param liveFragment
	 */
	public void setLiveFragment(LiveFragment liveFragment) {
		this.liveFragment = liveFragment;
	}

	/**
	 * Sets the current Buffer Command so we can track what Buffer Command is
	 * expected
	 * 
	 * @param expectedBufferCommand
	 */
	public void setBufferCommand(byte expectedBufferCommand) {
		this.bufferCommand = expectedBufferCommand;
	}

	/**
	 * Sets the Preference Fragment
	 * 
	 * @param appPreferences
	 */
	public void setPreferenceFragment(AppPreferences appPreferences) {
		this.appPreferences = appPreferences;
	}

	/**
	 * 
	 * @return The Preference Fragment
	 */
	public AppPreferences getPreferenceFragment() {
		return appPreferences;
	}

	/**
	 * Sets whether or not the user allowed to write application output into a
	 * log file by checking the set preference
	 */
	private void setLoggingPreference() {
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.loggingenabled = p.getBoolean("log", false);
	}

	/**
	 * 
	 * @return Whether the user enabled logging in the preferences
	 */
	public boolean loggingEnabled() {
		return this.loggingenabled;
	}

}
