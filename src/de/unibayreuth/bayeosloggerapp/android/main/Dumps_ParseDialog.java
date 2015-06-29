package de.unibayreuth.bayeosloggerapp.android.main;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import de.unibayreuth.bayeosloggerapp.android.tools.ReadWriteFile;
import de.unibayreuth.bayeosloggerapp.android.tools.SelectableTableRow;
import de.unibayreuth.bayeosloggerapp.android.tools.TableCreator;
import de.unibayreuth.bayeosloggerapp.android.tools.ViewWrapper;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DumpedFrame;
import de.unibayreuth.bayeosloggerapp.tools.StringTools;

public class Dumps_ParseDialog extends DialogFragment {
	// private static final String TAG = "Parse Dialog";
	// private Logger LOG = LoggerFactory.getLogger(Dumps_ParseDialog.class);

	private String[][] files;
	private String[][] alreadParsedFiles;
	private Vector<SelectableTableRow> selectedrows;
	private Vector<SelectableTableRow> alreadParsedRows;
	private View rawTableView, parsedTableView;

	public Dumps_ParseDialog(Vector<SelectableTableRow> selectedRows,
			Vector<SelectableTableRow> alreadyParsedRows, Context context) {

		this.selectedrows = selectedRows;

		this.alreadParsedRows = alreadyParsedRows;

		this.files = new String[3][selectedrows.size() + 1];
		this.files[0][0] = "No.";
		this.files[1][0] = "File";
		this.files[2][0] = "Size";

		for (int i = 0; i < selectedrows.size(); i++) {
			this.files[0][i + 1] = "" + (i + 1);
			this.files[1][i + 1] = selectedrows.get(i).getRawFile().getName();
			this.files[2][i + 1] = StringTools.byteCountConverter(selectedrows
					.get(i).getRawFile().length());
		}

		if (!alreadyParsedRows.isEmpty()) {
			alreadParsedFiles = new String[2][alreadyParsedRows.size() + 1];
			alreadParsedFiles[0][0] = "No.";
			alreadParsedFiles[1][0] = "File";

			for (int i = 0; i < alreadParsedRows.size(); i++) {
				alreadParsedFiles[0][i + 1] = "" + (i + 1);
				alreadParsedFiles[1][i + 1] = alreadParsedRows.get(i)
						.getParsedFile().getName();
			}
			this.parsedTableView = TableCreator.createTable(alreadParsedFiles,
					context);
		}

		this.rawTableView = TableCreator.createTable(files, context);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		final StringBuilder sb = new StringBuilder();
		sb.append("Path: \n" + MainActivity.DirectoryNameParsed);
		sb.append("\n\nFile(s):");

		View v = TableCreator.addMessage(rawTableView, getActivity(),
				"The selected files are going to be converted and saved at: ",
				new File(Environment.getExternalStorageDirectory(),
						MainActivity.DirectoryNameParsed).getAbsolutePath());
		View v2 = null;
		if (parsedTableView != null) {
			v2 = TableCreator
					.addMessage(
							parsedTableView,
							getActivity(),
							"The following selected file(s) are already converted to CSV. Path:  ",
							new File(Environment.getExternalStorageDirectory(),
									MainActivity.DirectoryNameParsed)
									.getAbsolutePath());

		}

		if (v2 != null) {
			v = TableCreator.combineViews(v, v2, getActivity());
		}

		return new AlertDialog.Builder(getActivity())
				.setTitle("Convert " + selectedrows.size() + " file(s)?")
				.setView(v)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {

								new ConvertToCSV(selectedrows,
										(MainActivity) getActivity()) {
								}.execute();
							}
						})
				.setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								dialog.cancel();
							}
						})

				.setIcon(R.drawable.ic_launcher).create();

	}

	public View getTableView() {
		return rawTableView;
	}
}

class ConvertToCSV extends AsyncTask<Void, String, Void> {

	// private static final String TAG = "ConvertToCSV";
	private Logger LOG = LoggerFactory.getLogger(ConvertToCSV.class);

	private ProgressDialog progress, cancel;
	private MainActivity context;
	private Vector<SelectableTableRow> selectedRows;
	private boolean cancelled;
	private String[][] results;

	public ConvertToCSV(Vector<SelectableTableRow> selectedrows,
			MainActivity context) {
		this.progress = new ProgressDialog(context);
		this.context = context;
		this.selectedRows = selectedrows;
		this.results = new String[3][selectedrows.size() + 1];
		this.results[0][0] = "No.";
		this.results[1][0] = "Name";
		this.results[2][0] = "Status";

		if (context.loggingEnabled())
			LOG.info("Converting files to CSV: {}", selectedrows.toString());

	}

	@Override
	protected void onPreExecute() {

		progress.setTitle("Converting " + selectedRows.size()
				+ " file(s) to CSV...");
		this.progress.setMessage("");
		progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progress.setIndeterminate(true);

		progress.setCancelable(false);

		progress.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancel = new ProgressDialog(context);
						cancel.setTitle("Cancel conversion");
						cancel.setMessage("Finishing conversion of current file..");
						cancel.setIndeterminate(true);
						cancel.setCancelable(false);

						cancel.show();

						cancelled = true;

					}
				});

		this.progress.show();

	}

	@Override
	protected Void doInBackground(Void... params) {

		Thread.currentThread().setName("AsyncTask ConvertToCSV");

		SelectableTableRow row;
		for (int i = 0; i < selectedRows.size(); i++) {
			if (cancelled) {
				break;
			}

			row = selectedRows.get(i);

			publishProgress("["
					+ (i + 1)
					+ "/"
					+ selectedRows.size()
					+ "]: "
					+ row.getRawFile().getName()
					+ (" ("
							+ StringTools.byteCountConverter(row.getRawFile()
									.length()) + ")"));

			byte[] readFile;
			try {
				readFile = ReadWriteFile.readFile(row.getRawFile());

				Vector<DumpedFrame> dumpedFrames = DumpedFrame
						.parseDumpFile(readFile);

				if (ReadWriteFile.saveCSV(dumpedFrames, row.getRawFile()
						.getName().split("\\.")[0], context)) {
					results[2][i + 1] = "Success";
				} else
					results[2][i + 1] = "Error";
				row.addParsedFileInformation();

				results[0][i + 1] = i + 1 + "";
				results[1][i + 1] = row.getParsedFile().getName();
			} catch (IOException e) {
				if (context.loggingEnabled())
					LOG.warn("An exception occured when converting to CSV: {}",
							e.getMessage());
			}
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		progress.setMessage(values[0]);
	}

	@Override
	protected void onPostExecute(Void result) {
		if (progress.isShowing()) {
			progress.dismiss();
		}
		if (cancel != null && cancel.isShowing())
			cancel.dismiss();

		View table = TableCreator.createTable(results, context);
		View v = TableCreator.addMessage(table, context,
				"The following files have been saved to:",
				new File(Environment.getExternalStorageDirectory(),
						MainActivity.DirectoryNameParsed).getAbsolutePath());

		new AlertDialog.Builder(context)
				.setTitle("Conversion Report")
				.setView(v)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
							}
						})

				.setIcon(android.R.drawable.ic_dialog_info).show();

		ViewWrapper.forceWrapContent(table);

		super.onPostExecute(result);
	}

}
