package de.unibayreuth.bayeosloggerapp.android.main;

import java.io.File;
import java.util.Vector;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.unibayreuth.bayeosloggerapp.android.tools.ReadWriteFile;
import de.unibayreuth.bayeosloggerapp.android.tools.SelectableTableRow;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DumpedFrame;

public class DumpsFragment extends Fragment {
	private static final String TAG = "DumpsFragment";

	MainActivity mainActivity;

	TableRow.LayoutParams params = new TableRow.LayoutParams(
			LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f);

	private TableLayout tbllay_dumpsdata;
	private TextView tv_name, tv_start, tv_end, tv_records;
	private Button btn_refresh, btn_parseData, btn_selectAll;

	private Vector<SelectableTableRow> rows;

	private boolean interrupted = false;

	public DumpsFragment(MainActivity context) {
		this.mainActivity = context;
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

		View view = inflater.inflate(R.layout.fragment_dump, container, false);

		rows = new Vector<SelectableTableRow>();

		btn_refresh = (Button) view.findViewById(R.id.dumps_btn_refresh);
		btn_refresh.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				refreshTable();
			}
		});

		btn_parseData = (Button) view.findViewById(R.id.dumps_btn_parse);
		btn_parseData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				for (SelectableTableRow row : rows) {
					if (row.isSelected() && row.getParsedFile() == null) {

						parseData(row);

					}
				}
			}
		});

		btn_selectAll = (Button) view.findViewById(R.id.dumps_selectAll);
		btn_selectAll.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (((Button) v)
						.getText()
						.equals(getString(de.unibayreuth.bayeosloggerapp.android.main.R.string.dumps_selectAll))) {
					for (SelectableTableRow row : rows) {
						row.select();
					}
					((Button) v)
							.setText(getString(de.unibayreuth.bayeosloggerapp.android.main.R.string.dumps_deselectAll));
				} else {
					for (SelectableTableRow row : rows) {
						row.deselect();
					}
					((Button) v)
							.setText(getString(de.unibayreuth.bayeosloggerapp.android.main.R.string.dumps_selectAll));
				}
			}
		});

		tbllay_dumpsdata = (TableLayout) view
				.findViewById(R.id.dumps_tbllayout_data);

		tv_name = (TextView) view.findViewById(R.id.dumps_tv_name);
		tv_start = (TextView) view.findViewById(R.id.dumps_tv_start);
		tv_end = (TextView) view.findViewById(R.id.dumps_tv_end);
		tv_records = (TextView) view.findViewById(R.id.dumps_tv_records);

		refreshTable();

		return view;
	}

	public void refreshTable() {

		// all files in the directory of raw dumps
		File[] files = ReadWriteFile.getFiles(MainActivity.DirectoryNameRaw);

		for (File file : files) {
			// check if file already belongs to a table row
			boolean contained = false;
			for (SelectableTableRow row : rows) {
				if (row.getRawFile().equals(file)) {
					contained = true;
					// update row content for
					if (row.getParsedFile() != null) {
						row.getTvName().setText(row.getName());
						row.getTvStart().setText(row.getStart());
						row.getTvEnd().setText(row.getEnd());
						row.getTvRecords().setText(
								String.valueOf(row.getRecords()));
						row.invalidate();
					}
					break;
				}
			}
			if (contained)
				continue;

			/* Create a new row to be added. */
			SelectableTableRow tr = new SelectableTableRow(mainActivity, file);
			tr.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.MATCH_PARENT));

			tr.setName(file.getName().split("\\.")[0]);
			tr.addParsedFileInformation();

			TextView name = new TextView(mainActivity);
			TextView start = new TextView(mainActivity);
			TextView end = new TextView(mainActivity);
			TextView records = new TextView(mainActivity);

			tr.setTvName(name);
			tr.setTvStart(start);
			tr.setTvEnd(end);
			tr.setTvRecords(records);

			name.setBackgroundResource(R.drawable.cell_shape);
			name.setGravity(Gravity.CENTER);
			name.setText(tr.getName());
			name.setWidth(tv_name.getWidth());
			name.setLayoutParams(params);

			start.setBackgroundResource(R.drawable.cell_shape);
			start.setGravity(Gravity.CENTER);
			start.setWidth(tv_start.getWidth());
			start.setLayoutParams(params);
			start.setText(tr.getStart());

			end.setBackgroundResource(R.drawable.cell_shape);
			end.setGravity(Gravity.CENTER);
			end.setWidth(tv_end.getWidth());
			end.setLayoutParams(params);
			end.setText(tr.getEnd());

			records.setBackgroundResource(R.drawable.cell_shape);
			records.setGravity(Gravity.CENTER);
			records.setWidth(tv_records.getWidth());
			records.setLayoutParams(params);
			if (tr.getRecords() != null)
				records.setText(String.valueOf(tr.getRecords()));

			tr.addView(name);
			tr.addView(start);
			tr.addView(end);
			tr.addView(records);

			rows.add(tr);
			tbllay_dumpsdata.addView(tr);
		}
	}

	public void updateParseButtonState() {
		for (SelectableTableRow row : rows) {
			if (row.isSelected() && row.getParsedFile() == null) {
				btn_parseData.setEnabled(true);
				break;
			}
		}
		btn_parseData.setEnabled(false);
	}

	private void parseData(final SelectableTableRow row) {

		ProgressDialog pg = new ProgressDialog(mainActivity);

		pg.setTitle("Convert file \"" + row.getName() + "\" to Excel File");
		pg.setMessage("Test");
		pg.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		pg.setIndeterminate(false);

		pg.setCancelable(false);
		// TODO implement cancel
		// pg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
		// new DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// dialog.dismiss();
		// setInterrupted(true);
		//
		// }
		// });

		pg.show();

		ParseThread parse = new ParseThread(row, mainActivity, pg);
		parse.start();
	}

	protected void setInterrupted(boolean interrupted) {
		this.interrupted = interrupted;
	}

	public boolean convertInterrupted() {
		return interrupted;
	}

	class ParseThread extends Thread {

		private SelectableTableRow row;
		private Activity context;
		private ProgressDialog pg;

		public ParseThread(SelectableTableRow row, Activity context,
				ProgressDialog pg) {
			this.row = row;
			this.context = context;
			this.pg = pg;
		}

		@Override
		public void run() {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					pg.setMessage("Reading Binary Dump..");
				}
			});

			byte[] readFile = ReadWriteFile.readFile(row.getRawFile(), pg);

			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					pg.setMessage("Parsing Binary Dump");
				}
			});
			Vector<DumpedFrame> dumpedFrames = DumpedFrame.parseDumpFile(
					readFile, pg);
			pg.setMax(dumpedFrames.size());

			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					pg.setMessage("Creating Excel File");
				}
			});

			ReadWriteFile.saveExcelFile(dumpedFrames, row.getRawFile()
					.getName().split("\\.")[0], pg, mainActivity);

			row.addParsedFileInformation();

			pg.dismiss();
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					refreshTable();
				}
			});
		}

	}
}
