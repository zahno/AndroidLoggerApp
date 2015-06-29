package de.unibayreuth.bayeosloggerapp.android.main;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.joda.time.Duration;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TableLayout;
import android.widget.TextView;
import de.unibayreuth.bayeosloggerapp.android.tools.ToastMessage;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.CommandAndResponseFrame;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.Frame.Number;
import de.unibayreuth.bayeosloggerapp.frames.serial.Bulk;
import de.unibayreuth.bayeosloggerapp.frames.serial.SerialFrame;
import de.unibayreuth.bayeosloggerapp.tools.DateAdapter;
import de.unibayreuth.bayeosloggerapp.tools.NumberConverter;
import de.unibayreuth.bayeosloggerapp.tools.StringTools;
import de.unibayreuth.bayeosloggerapp.tools.Tuple;

public class LoggerFragment extends Fragment {

	// private static final String TAG = "LoggerFragment";

	private Logger LOG = LoggerFactory.getLogger(LoggerFragment.class);

	Date currentLoggerDate, dateOfNextFrame, noNewFramesDate = DateAdapter
			.getDate(NumberConverter.fromByteInt32(new byte[] { (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));
	Integer readPosition = null;

	protected byte[] binaryDump;
	private ProgressDialog binaryDumpProgress;
	private long binaryDump_NumberOfBytes;
	private long binaryDump_ReceivedBytes;

	private boolean interruptDump = false;

	private Switch sw_connection;
	private Button btn_saveData, btn_eraseData, btn_setName,
			btn_setSamplingInterval, btn_syncTime;
	private TableLayout tbl_Layout;
	private RelativeLayout lin_Layout;

	private TextView eT_version, eT_currentDate, eT_dateofNextFrame,
			eT_estimatedNewFrames;
	private EditText eT_name, eT_samplingInt;

	private Tuple<Long, Instant> bytesAtTime;

	public LoggerFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// retain this fragment
		setRetainInstance(true);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (this.getView() != null) {
			((ViewGroup) this.getView().getParent()).removeView(this.getView());
			return this.getView();
		}

		View view = inflater
				.inflate(R.layout.fragment_logger, container, false);

		tbl_Layout = (TableLayout) view
				.findViewById(R.id.logger_tblLayout_LoggerData);
		lin_Layout = (RelativeLayout) view
				.findViewById(R.id.logger_linLayout_SaveErase);

		sw_connection = (Switch) view.findViewById(R.id.switch_logger_connect);
		sw_connection.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					if (((MainActivity) getActivity()).openDevice()) {
						enableContent();
						ToastMessage
								.toastConnectionSuccessful(((MainActivity) getActivity()));

					} else {
						disableContent();
						ToastMessage
								.toastConnectionFailed(((MainActivity) getActivity()));
					}

				} else {
					((MainActivity) getActivity()).closeDevice();
					disableContent();
				}
			}
		});

		eT_version = (TextView) view.findViewById(R.id.eT_loggerVersion);
		eT_version.setKeyListener(null);

		eT_name = (EditText) view.findViewById(R.id.eT_loggerName);

		eT_samplingInt = (EditText) view
				.findViewById(R.id.eT_logger_samplingIntervall);

		eT_currentDate = (TextView) view
				.findViewById(R.id.eT_logger_currentDate);
		eT_currentDate.setKeyListener(null);

		eT_dateofNextFrame = (TextView) view
				.findViewById(R.id.eT_logger_dateOfNextFrame);
		eT_dateofNextFrame.setKeyListener(null);

		eT_estimatedNewFrames = (TextView) view
				.findViewById(R.id.eT_logger_estimatedNewFrames);
		eT_estimatedNewFrames.setKeyListener(null);

		btn_setName = (Button) view.findViewById(R.id.btn_logger_setName);
		btn_setName.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				((MainActivity) getActivity()).addToQueue(SerialFrame
						.toSerialFrame(CommandAndResponseFrame
								.command_setName(eT_name.getText())));

				InputMethodManager inputMethodManager = (InputMethodManager) ((MainActivity) getActivity())
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(
						btn_setName.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});

		btn_setSamplingInterval = (Button) view
				.findViewById(R.id.btn_logger_setSamplingInterval);
		btn_setSamplingInterval.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!eT_samplingInt.getText().toString().isEmpty())
					((MainActivity) getActivity()).addToQueue(SerialFrame
							.toSerialFrame(CommandAndResponseFrame
									.command_setSamplingInterval(eT_samplingInt
											.getText())));

				InputMethodManager inputMethodManager = (InputMethodManager) ((MainActivity) getActivity())
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.hideSoftInputFromWindow(
						btn_setSamplingInterval.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);

			}
		});

		btn_syncTime = (Button) view.findViewById(R.id.btn_syncTime);
		btn_syncTime.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				updateTime();
			}
		});

		btn_saveData = (Button) view.findViewById(R.id.btn_logger_saveData);
		btn_saveData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				((MainActivity) getActivity())
						.addToQueue(SerialFrame.getReadPosition);
				((MainActivity) getActivity())
						.setBufferCommand(CommandAndResponseFrame.BayEOS_BufferCommand_GetReadPosition);

			}
		});

		btn_eraseData = (Button) view.findViewById(R.id.btn_logger_eraseData);
		btn_eraseData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getActivity());
				// Add the buttons
				builder.setPositiveButton(
						R.string.logger_eraseDialogue_confirm,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {

								((MainActivity) getActivity()).addToQueue(SerialFrame
										.toSerialFrame(CommandAndResponseFrame
												.command_BufferCommand_erase()));
								((MainActivity) getActivity())
										.setBufferCommand(CommandAndResponseFrame.BayEOS_BufferCommand_Erase);
							}
						});
				builder.setNegativeButton(R.string.logger_eraseDialogue_cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								// User cancelled the dialog
								// nothing happens
							}
						});
				// Set other dialog properties
				builder.setMessage(R.string.logger_eraseDialogue_eraseFrames)
						.setTitle(
								R.string.logger_eraseDialogue_eraseFramesTitle);

				// Create the AlertDialog
				(builder.create()).show();

			}
		});

		disableContent();

		return view;

	}

	/**
	 * Initializes the logger data by putting some commands to the write queue.
	 */
	public void initializeLoggerData() {
		((MainActivity) getActivity()).addToQueue(SerialFrame.getVersion);
		((MainActivity) getActivity()).addToQueue(SerialFrame.getName);
		((MainActivity) getActivity())
				.addToQueue(SerialFrame.getSamplingInterval);
		updateTime();
	}

	/**
	 * Called when a frame in dump mode is received.
	 * 
	 * @param bulkFrame
	 */
	public void handleBulk(Bulk bulkFrame) {
		if (((MainActivity) getActivity()).loggingEnabled())
			LOG.info("Received Bulk");

		final byte[] bulk = bulkFrame.getValues();

		// add the recieved bulk values to the array binaryDump
		for (int i = 0; i < bulk.length; i++)
			binaryDump[(int) (binaryDump_ReceivedBytes + i)] = bulk[i];

		binaryDump_ReceivedBytes += bulk.length;

		// Update the binaryDumpProgress dialog :

		// determine the estimated remaining time to show the user in the
		// progress dialog binaryDumpProgress
		Instant timestamp = new Instant();
		Duration d = new Duration(bytesAtTime.getSecond(), timestamp);
		if (d.getMillis() >= 2000) {

			final long estimatedTime = (d.getMillis() * ((binaryDump_NumberOfBytes - binaryDump_ReceivedBytes) / (binaryDump_ReceivedBytes - bytesAtTime
					.getFirst())));

			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {

					binaryDumpProgress.setMessage("Estimated time: "
							+ String.format(
									"%d min, %d sec",
									TimeUnit.MILLISECONDS
											.toMinutes(estimatedTime),
									TimeUnit.MILLISECONDS
											.toSeconds(estimatedTime)
											- TimeUnit.MINUTES
													.toSeconds(TimeUnit.MILLISECONDS
															.toMinutes(estimatedTime))));
				}
			});

			bytesAtTime.setFirst(binaryDump_ReceivedBytes);
			bytesAtTime.setSecond(timestamp);
		}
		binaryDumpProgress.incrementProgressBy(bulk.length);

		// If the recieved number of bytes equals the expected number of bytes
		if (binaryDumpProgress.getProgress() == binaryDump_NumberOfBytes) {
			LOG.info("Dump finished. No. Bytes: {} (expected {} Bytes)",
					binaryDump.length, binaryDump_NumberOfBytes);

			((MainActivity) getActivity())
					.setBufferCommand(CommandAndResponseFrame.BayEOS_BufferCommand_SetReadPointerToEndPositionOfBinaryDump);
			((MainActivity) getActivity())
					.addToQueue(SerialFrame.setReadPointerToEndPositionOfBinaryDump);

			binaryDumpProgress.dismiss();
			updateTime();

			saveDump(binaryDump);
		}

	}

	/**
	 * Saves the recieved dump into a .db file
	 * 
	 * @param binaryDump
	 */
	private void saveDump(byte[] binaryDump) {

		new SaveRawDump((MainActivity) getActivity(), eT_name.getText()
				.toString(), MainActivity.DirectoryNameRaw).execute(binaryDump);
	}

	/**
	 * Updates the TextView "Estimated new frames"
	 */
	protected void setEstimatedNewFrames() {
		if (dateOfNextFrame.equals(noNewFramesDate)) {
			eT_estimatedNewFrames.setText("0");
			return;
		}

		if (dateOfNextFrame != null && currentLoggerDate != null
				&& eT_samplingInt.getText().length() != 0) {

			long seconds = (currentLoggerDate.getTime() - dateOfNextFrame
					.getTime()) / 1000;
			String samIn = eT_samplingInt.getText().toString();
			int samplingInterval = (int) Float.parseFloat(samIn);
			long estimatedNewFrames = seconds / samplingInterval;
			eT_estimatedNewFrames.setText(String.valueOf(estimatedNewFrames));
		}
	}

	/**
	 * Enables the content
	 */
	public void enableContent() {
		sw_connection.setChecked(true);
		MainActivity.enable(tbl_Layout);
		MainActivity.enable(lin_Layout);
	}

	/**
	 * Disables the content
	 */
	public void disableContent() {
		sw_connection.setChecked(false);
		MainActivity.disable(tbl_Layout);
		MainActivity.disable(lin_Layout);
	}

	/**
	 * Returns if the Dump was interrupted or not.
	 * 
	 * @return Returns true only if there is a dump running and the user pressed
	 *         the cancel button. False otherwise.
	 * @author Christiane Goehring
	 */
	public boolean dumpInterrupted() {
		return interruptDump;
	}

	public void setDumpInterrupted(boolean interruptDump) {
		this.interruptDump = interruptDump;
	}

	public void handle_GetTime(CommandAndResponseFrame receivedFrame) {
		Integer[] time = (Integer[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int32); // Integer
		currentLoggerDate = DateAdapter.getDate(time[0]);
		eT_currentDate.setText(new SimpleDateFormat(
				"EEE',' dd.MM.yyyy 'at' HH:mm:ss z", Locale.US)
				.format(currentLoggerDate));

		Date currentSystemDate = new Date();
		if (!DateAdapter.differenceSmallerThan(currentSystemDate,
				currentLoggerDate, DateAdapter.fiveMinutesInSeconds)) {

			DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case DialogInterface.BUTTON_POSITIVE:
						((MainActivity) getActivity()).addToQueue(SerialFrame
								.toSerialFrame(CommandAndResponseFrame
										.command_setTime()));
						break;

					case DialogInterface.BUTTON_NEGATIVE:
						// No button clicked
						break;
					}
				}
			};

			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle("Logger time seems to be wrong!")
					.setMessage(
							"The time of the logger is off by more than 5 minutes compared to the time of your device.\n\nLogger Time:\n\t"
									+ new SimpleDateFormat(
											"EEE',' dd.MM.yyyy 'at' HH:mm:ss z",
											Locale.US)
											.format(currentLoggerDate)
									+ "\n\nSystem Time:\n\t"
									+ new SimpleDateFormat(
											"EEE',' dd.MM.yyyy 'at' HH:mm:ss z",
											Locale.US)
											.format(currentSystemDate)
									+ "\n\nDo you want to reset the logger date?")
					.setPositiveButton(android.R.string.ok, dialogClickListener)
					.setNegativeButton("Cancel", dialogClickListener).show();

		}

	}

	/**
	 * Handles the response to the command "Get Name"
	 * 
	 * @param receivedFrame
	 */
	public void handle_GetName(CommandAndResponseFrame receivedFrame) {
		eT_name.setText(StringTools.asciiToString(receivedFrame.getValues()));
	}

	/**
	 * Handles the response to the command "Set Name"
	 * 
	 * @param receivedFrame
	 */
	public void handle_SetName(CommandAndResponseFrame receivedFrame) {
		String name = StringTools.asciiToString(receivedFrame.getValues());
		eT_name.setText(name);
		ToastMessage.toastMessageBottom(((MainActivity) getActivity()),
				"Name set to \"" + name + "\".");
	}

	/**
	 * Handles the response to the command "Get Version"
	 * 
	 * @param receivedFrame
	 */
	public void handle_GetVersion(CommandAndResponseFrame receivedFrame) {
		eT_version
				.setText(StringTools.asciiToString(receivedFrame.getValues()));
	}

	/**
	 * Handles the response to the command "Get Sampling Interval"
	 * 
	 * @param receivedFrame
	 */
	public void handle_GetSamplingInterval(CommandAndResponseFrame receivedFrame) {
		Short[] samplingInterval = (Short[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int16); // Integer

		eT_samplingInt.setText(StringTools.arrayToString(samplingInterval));
	}

	/**
	 * Handles the response to the command "Set Sampling Interval"
	 * 
	 * @param receivedFrame
	 */
	public void handle_SetSamplingInterval(CommandAndResponseFrame receivedFrame) {
		short setSamplingInt = ((Short[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int16))[0];
		ToastMessage.toastMessageBottom(((MainActivity) getActivity()),
				"Sampling Interval set to " + setSamplingInt + " seconds.");
		eT_samplingInt.setText(""+setSamplingInt);
		updateTime();
	}

	/**
	 * Handles the response to the command "Get Time of Next Frame"
	 * 
	 * @param receivedFrame
	 */
	public void handle_TimeOfNextFrame(CommandAndResponseFrame receivedFrame) {
		Integer[] timeOfNextFrame = (Integer[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int32); // Integer
		dateOfNextFrame = DateAdapter.getDate(timeOfNextFrame[0]);

		if (!dateOfNextFrame.equals(noNewFramesDate))
			eT_dateofNextFrame.setText(new SimpleDateFormat(
					"EEE',' dd.MM.yyyy 'at' HH:mm:ss z", Locale.US)
					.format(dateOfNextFrame));
		else
			eT_dateofNextFrame.setText("No new frames found!");
		setEstimatedNewFrames();
	}

	/**
	 * Handles the response to the command "Start Binary Dump"
	 * 
	 * @param receivedFrame
	 */
	public void handle_StartBinaryDump(CommandAndResponseFrame receivedFrame) {

		bytesAtTime = new Tuple<Long, Instant>(Long.valueOf(0), new Instant());

		binaryDump_NumberOfBytes = ((Long[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.UInt32))[0]; // Integer
		LOG.info("Starting binary dump. Expecting {} bytes.",
				binaryDump_NumberOfBytes);
		if (binaryDump_NumberOfBytes == 0) {
			return;
		}

		if (binaryDump_NumberOfBytes <= Integer.MAX_VALUE)
			binaryDump = new byte[(int) binaryDump_NumberOfBytes];

		binaryDump_ReceivedBytes = 0;

		// progress bar
		binaryDumpProgress = new ProgressDialog(getActivity());
		binaryDumpProgress.setMax((int) binaryDump_NumberOfBytes);

		binaryDumpProgress.setTitle("Downloading Data..");
		binaryDumpProgress.setMessage("Estimated time: -");
		binaryDumpProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		binaryDumpProgress.setIndeterminate(false);

		binaryDumpProgress.setCancelable(false);
		binaryDumpProgress.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						setDumpInterrupted(true);
					}
				});

		binaryDumpProgress.show();
	}

	/**
	 * Puts the commands "Get Time" and "Get Time of Next Frame" into the write
	 * Queue and therefore updates the time
	 */
	public void updateTime() {
		((MainActivity) getActivity()).addToQueue(SerialFrame.getTime);
		((MainActivity) getActivity())
				.addToQueue(SerialFrame.getTimeOfNextFrame);

	}

	/**
	 * Handles the response to the command "Get Read Position"
	 * 
	 * @param receivedFrame
	 */

	public void handle_BufferCommand_GetReadPosition(
			CommandAndResponseFrame receivedFrame) {
		readPosition = ((Integer[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int32))[0]; // Integer
		if (readPosition != null) {

			((MainActivity) getActivity()).addToQueue(SerialFrame
					.toSerialFrame(CommandAndResponseFrame
							.command_startBinaryDump(readPosition)));

		}
	}

	/**
	 * Deletes all values from the Logger Data View
	 */
	public void resetView() {
		eT_name.setText("");
		eT_version.setText("");
		eT_samplingInt.setText("");
		eT_currentDate.setText("");
		eT_dateofNextFrame.setText("");
		eT_estimatedNewFrames.setText("");
	}
}

class SaveRawDump extends AsyncTask<byte[], String, String> {

	private Logger LOG = LoggerFactory.getLogger(SaveRawDump.class);

	private String directoryName;
	private ProgressDialog progress;
	private MainActivity context;
	private String filename;

	public SaveRawDump(MainActivity context, String loggerName, String path) {
		this.context = context;
		this.directoryName = path;

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
		Date date = new Date();

		this.filename = "Dump_" + loggerName + "_" + dateFormat.format(date)
				+ ".db";
	}

	@Override
	protected void onPreExecute() {
		context.runOnUiThread(new Runnable() {
			public void run() {
				progress = new ProgressDialog(context);

				progress.setTitle("Saving Dump");
				progress.setMessage("Saving \n\t" + filename + "...");
				progress.setIndeterminate(true);

				progress.setCancelable(false);

				progress.show();
			}
		});

	}

	@Override
	protected String doInBackground(byte[]... bulkData) {
		Thread.currentThread().setName("AsyncTask SaveRawDump");

		if (!isExternalStorageWritable()) {
			LOG.error("External storage not writable!");
			return null;
		}

		File rootPath = new File(Environment.getExternalStorageDirectory(),
				directoryName);
		if (!rootPath.exists()) {
			rootPath.mkdirs();
		}

		File bulk = new File(rootPath, filename);

		if (bulk.exists()) {
			bulk.delete();
		}
		if (context.loggingEnabled())
			LOG.info("Saving [{}] to {} ..",
					StringTools.arrayToHexString(bulkData[0]),
					bulk.getAbsolutePath());

		try {
			FileOutputStream fos = new FileOutputStream(bulk.getPath());

			fos.write(bulkData[0]);
			fos.close();
			LOG.info("..wrote {} successfully.", bulk.getAbsolutePath());

		} catch (java.io.IOException e) {
			if (context.loggingEnabled())
				LOG.warn("Exception in writing a file: {}", e.getMessage());
		}

		return null;
	}

	/**
	 * Checks if external storage is available for read and write
	 */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	@Override
	protected void onPostExecute(String result) {
		if (progress.isShowing()) {
			progress.dismiss();
		}

		new AlertDialog.Builder(context)
				.setTitle("Download Report")
				.setMessage(
						"Saved \n\t"
								+ filename
								+ "\nto \n\t"
								+ new File(Environment
										.getExternalStorageDirectory(),
										MainActivity.DirectoryNameRaw)
										.getAbsolutePath())
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
							}
						})

				.setIcon(android.R.drawable.ic_dialog_info).show();

		super.onPostExecute(result);
	}
}
