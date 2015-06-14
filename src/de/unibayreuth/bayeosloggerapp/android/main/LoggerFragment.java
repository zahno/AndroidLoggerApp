package de.unibayreuth.bayeosloggerapp.android.main;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class LoggerFragment extends Fragment {

	private static final String TAG = "LoggerFragment";

	Date currentDate, dateOfNextFrame, noNewFramesDate = DateAdapter
			.getDate(NumberConverter.fromByteInt32(new byte[] { (byte) 0xFF,
					(byte) 0xFF, (byte) 0xFF, (byte) 0xFF }));
	Integer readPosition = null;

	protected Vector<Byte> binaryDump;
	private ProgressDialog binaryDumpProgress;
	private long binaryDump_NumberOfBytes;
	private boolean interruptDump = false;

	Switch sw_connection;
	Button btn_saveData, btn_eraseData, btn_setName, btn_setSamplingInterval,
			btn_syncTime;
	TableLayout tbl_Layout;
	LinearLayout lin_Layout;
	OnCheckedChangeListener switchListener;

	TextView eT_version, eT_currentDate, eT_dateofNextFrame,
			eT_estimatedNewFrames;
	EditText eT_name, eT_samplingInt;

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
		lin_Layout = (LinearLayout) view
				.findViewById(R.id.logger_linLayout_SaveErase);

		sw_connection = (Switch) view.findViewById(R.id.switch_logger_connect);
		sw_connection.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					if (((MainActivity) getActivity()).openDevice()) {
						// loggerFragment.get((MainActivity) getActivity())().appendDevOpenedText();
						enableContent();
						ToastMessage.toastConnectionSuccessful(((MainActivity) getActivity()));

					} else {
						disableContent();
						ToastMessage.toastConnectionFailed(((MainActivity) getActivity()));
					}

				} else {
					((MainActivity) getActivity()).closeDevice();
					disableContent();
					// ToastMessage.toastDisconnected(loggerFragment.get((MainActivity) getActivity())());
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
				((MainActivity) getActivity()).addToQueue(SerialFrame.getReadPosition);
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

	public void initializeLoggerData() {
		((MainActivity) getActivity()).addToQueue(SerialFrame.getVersion);
		((MainActivity) getActivity()).addToQueue(SerialFrame.getName);
		((MainActivity) getActivity()).addToQueue(SerialFrame.getSamplingInterval);
		updateTime();
	}

	public void handleBulk(Bulk bulkFrame) {
		Vector<Byte> bulk = bulkFrame.getValuesAsVector();

		binaryDump.addAll(bulk);

		binaryDumpProgress.incrementProgressBy(bulkFrame.getLength() - 5);

		if (binaryDumpProgress.getProgress() == binaryDump_NumberOfBytes) {
			Log.i(TAG, "Dump finished. No. Bytes: " + binaryDump.size()
					+ " (expected " + binaryDump_NumberOfBytes + " Bytes)");

			((MainActivity) getActivity())
					.setBufferCommand(CommandAndResponseFrame.BayEOS_BufferCommand_SetReadPointerToEndPositionOfBinaryDump);
			((MainActivity) getActivity())
					.addToQueue(SerialFrame.setReadPointerToEndPositionOfBinaryDump);

			binaryDumpProgress.dismiss();
			updateTime();
			saveBulk();
		}

	}

	private void saveBulk() {
		byte[] binaryDumpArray = new byte[binaryDump.size()];
		for (int i = 0; i < binaryDump.size(); i++) {
			binaryDumpArray[i] = (byte) (binaryDump.get(i) & 0xff);
		}

		new SaveRawDumpTask() {
			@Override
			protected void onPreExecute() {
				this.name = eT_name.getText().toString();
				this.directoryName = MainActivity.DirectoryNameRaw;
			};
		}.execute(binaryDumpArray);
	}

	protected void setEstimatedNewFrames() {
		if (dateOfNextFrame.equals(noNewFramesDate)) {
			eT_estimatedNewFrames.setText("0");
			return;
		}

		if (dateOfNextFrame != null && currentDate != null
				&& eT_samplingInt.getText().length() != 0) {

			long seconds = (currentDate.getTime() - dateOfNextFrame.getTime()) / 1000;
			String samIn = eT_samplingInt.getText().toString();
			int samplingInterval = (int) Float.parseFloat(samIn);
			long estimatedNewFrames = seconds / samplingInterval;
			eT_estimatedNewFrames.setText(String.valueOf(estimatedNewFrames));
		}
	}

	public void enableContent() {
		sw_connection.setChecked(true);
		MainActivity.enable(tbl_Layout);
		MainActivity.enable(lin_Layout);
	}

	public void disableContent() {
		sw_connection.setChecked(false);
		MainActivity.disable(tbl_Layout);
		MainActivity.disable(lin_Layout);
	}

	public boolean dumpInterrupted() {
		return interruptDump;
	}

	public void setDumpInterrupted(boolean interruptDump) {
		this.interruptDump = interruptDump;
	}

	public void handle_GetTime(CommandAndResponseFrame receivedFrame) {
		Integer[] time = (Integer[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int32); // Integer
		currentDate = DateAdapter.getDate(time[0]);
		eT_currentDate.setText(new SimpleDateFormat(
				"EEE',' dd.MM.yyyy 'at' HH:mm:ss z", Locale.US)
				.format(currentDate));
	}

	public void handle_GetName(CommandAndResponseFrame receivedFrame) {
		eT_name.setText(StringTools.asciiToString(receivedFrame.getValues()));
	}

	public void handle_SetName(CommandAndResponseFrame receivedFrame) {
		String name = StringTools.asciiToString(receivedFrame.getValues());
		ToastMessage
				.toastSuccess(((MainActivity) getActivity()), "Set name to \"" + name + "\".");
	}

	public void handle_GetVersion(CommandAndResponseFrame receivedFrame) {
		eT_version
				.setText(StringTools.asciiToString(receivedFrame.getValues()));
	}

	public void handle_GetSamplingInterval(CommandAndResponseFrame receivedFrame) {
		Short[] samplingInterval = (Short[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int16); // Integer

		eT_samplingInt.setText(StringTools.arrayToString(samplingInterval));
	}

	public void handle_SetSamplingInterval(CommandAndResponseFrame receivedFrame) {
		short setSamplingInt = ((Short[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int16))[0];
		ToastMessage.toastSuccess(((MainActivity) getActivity()), "Set Sampling Interval to "
				+ setSamplingInt + " seconds.");
		updateTime();
	}

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

	public void handle_StartBinaryDump(CommandAndResponseFrame receivedFrame) {
		binaryDump_NumberOfBytes = ((Long[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.UInt32))[0]; // Integer
		Log.i(TAG, "Number of Bytes: " + binaryDump_NumberOfBytes);

		binaryDump = new Vector<>((int) binaryDump_NumberOfBytes);

		// progress bar
		binaryDumpProgress = new ProgressDialog(((MainActivity) getActivity()));
		binaryDumpProgress.setMax((int) binaryDump_NumberOfBytes);

		binaryDumpProgress.setTitle("Downloading Data..");
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

	public void updateTime() {
		((MainActivity) getActivity()).addToQueue(SerialFrame.getTime);
		((MainActivity) getActivity()).addToQueue(SerialFrame.getTimeOfNextFrame);

	}

	public void handle_BuferCommand_GetReadPosition(
			CommandAndResponseFrame receivedFrame) {
		// Log.i(TAG, "getReadPosition");

		readPosition = ((Integer[]) Frame.parsePayload(
				receivedFrame.getValues(), Number.Int32))[0]; // Integer
		if (readPosition != null) {

			((MainActivity) getActivity()).addToQueue(SerialFrame
					.toSerialFrame(CommandAndResponseFrame
							.command_startBinaryDump(readPosition)));

		}
	}
}

class SaveRawDumpTask extends AsyncTask<byte[], String, String> {
	String directoryName;
	String name;

	@Override
	protected String doInBackground(byte[]... bulkData) {

		if (!isExternalStorageWritable()) {
			Log.e("SaveBulk", "Storage not writable!");
			return null;
		}

		File rootPath = new File(Environment.getExternalStorageDirectory(),
				directoryName);
		if (!rootPath.exists()) {
			rootPath.mkdirs();
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		Date date = new Date();

		File bulk = new File(rootPath, "Dump_" + name + "_"
				+ dateFormat.format(date) + ".db");

		if (bulk.exists()) {
			bulk.delete();
		}

		try {
			FileOutputStream fos = new FileOutputStream(bulk.getPath());

			fos.write(bulkData[0]);
			fos.close();

		} catch (java.io.IOException e) {
			Log.e("SaveBulkTask", "Exception in writing a file", e);
		}

		return (null);
	}

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}
}
