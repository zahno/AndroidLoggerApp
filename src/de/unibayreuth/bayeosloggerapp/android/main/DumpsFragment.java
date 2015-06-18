package de.unibayreuth.bayeosloggerapp.android.main;

import java.io.File;
import java.util.Vector;

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
import de.unibayreuth.bayeosloggerapp.android.tools.TableCreator;
import de.unibayreuth.bayeosloggerapp.android.tools.ToastMessage;
import de.unibayreuth.bayeosloggerapp.android.tools.ViewWrapper;

public class DumpsFragment extends Fragment {
	private static final String TAG = "DumpsFragment";

	TableRow.LayoutParams params = new TableRow.LayoutParams(
			LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, 1f);

	private TableLayout tbllay_dumpsdata;
	private TextView tv_name, tv_start, tv_end, tv_records;
	private Button btn_refresh, btn_parseData, btn_selectAll, btn_upload;

	private Vector<SelectableTableRow> rows;

	public DumpsFragment() {
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
				ToastMessage.toastMessage((MainActivity) getActivity(),
						"Table refreshed.");
				return;
			}
		});

		btn_parseData = (Button) view.findViewById(R.id.dumps_btn_parse);
		btn_parseData.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				Vector<SelectableTableRow> selectedrows = new Vector<SelectableTableRow>();
				Vector<SelectableTableRow> alreadParsedRows = new Vector<SelectableTableRow>();

				for (SelectableTableRow row : rows) {
					if (row.isSelected() && row.getParsedFile() != null) {
						alreadParsedRows.add(row);
					}
					if (row.isSelected() && row.getParsedFile() == null) {
						selectedrows.add(row);
					}
				}
				if (selectedrows.size() == 0 && !alreadParsedRows.isEmpty()) {
					ToastMessage
							.toastMessage((MainActivity) getActivity(),
									"The file(s) you selected is (are) already converted.");
					return;
				} else if (selectedrows.size() == 0) {
					ToastMessage.toastMessage((MainActivity) getActivity(),
							"Select at least one file you wish to convert.");
					return;
				}
				Dumps_ParseDialog dialog = new Dumps_ParseDialog(selectedrows,
						alreadParsedRows, getActivity());
				dialog.show(
						((MainActivity) getActivity()).getFragmentManager(),
						"ParseDialog");
				ViewWrapper.forceWrapContent(dialog.getTableView());

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

		btn_upload = (Button) view.findViewById(R.id.dumps_btn_upload);
		btn_upload.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				if (getSelectedRows().size() == 0) {
					ToastMessage.toastMessage((MainActivity) getActivity(),
							"Select at least one file you wish to upload.");
					return;
				}
				Dumps_UploadDialog dialog = new Dumps_UploadDialog(
						getSelectedRows(), getActivity());
				dialog.show(
						((MainActivity) getActivity()).getFragmentManager(),
						"UploadDialog");
				ViewWrapper.forceWrapContent(dialog.getTableView());

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

	protected Vector<SelectableTableRow> getSelectedRows() {
		Vector<SelectableTableRow> res = new Vector<SelectableTableRow>();
		for (SelectableTableRow row : rows) {
			if (row.isSelected())
				res.add(row);
		}
		return res;
	}

	public void refreshTable() {

		// all files in the directory of raw dumps
		File[] files = ReadWriteFile.getFiles(MainActivity.DirectoryNameRaw);

		boolean contained;
		for (SelectableTableRow row : rows) {
			contained = false;
			for (File file : files) {
				if (row.getRawFile().equals(file)) {
					contained = true;
					break;
				}
			}
			if (!contained) {
				tbllay_dumpsdata.removeView(row);
				rows.remove(row);
			}
		}

		for (File file : files) {
			// check if file already belongs to a table row
			contained = false;
			for (SelectableTableRow row : rows) {
				if (row.getRawFile().equals(file)) {
					contained = true;
					break;
				}
			}
			if (contained)
				continue;

			int dps = TableCreator.getDps(getActivity(), 10);

			/* Create a new row to be added. */
			SelectableTableRow tr = new SelectableTableRow(
					((MainActivity) getActivity()), file);
			tr.setLayoutParams(new TableRow.LayoutParams(
					TableRow.LayoutParams.MATCH_PARENT,
					TableRow.LayoutParams.MATCH_PARENT));

			tr.setName(file.getName().split("\\.")[0]);
			tr.addParsedFileInformation();

			TextView name = new TextView(((MainActivity) getActivity()));
			TextView start = new TextView(((MainActivity) getActivity()));
			TextView end = new TextView(((MainActivity) getActivity()));
			TextView records = new TextView(((MainActivity) getActivity()));

			tr.setTvName(name);
			tr.setTvStart(start);
			tr.setTvEnd(end);
			tr.setTvRecords(records);

			name.setBackgroundResource(R.drawable.cell_shape);
			name.setGravity(Gravity.CENTER);
			name.setText(tr.getName());
			name.setWidth(tv_name.getWidth());
			name.setLayoutParams(params);
			name.setPadding(dps, 0, dps, 0);

			start.setBackgroundResource(R.drawable.cell_shape);
			start.setGravity(Gravity.CENTER);
			start.setWidth(tv_start.getWidth());
			start.setLayoutParams(params);
			start.setText(tr.getStart());
			start.setPadding(dps, 0, dps, 0);

			end.setBackgroundResource(R.drawable.cell_shape);
			end.setGravity(Gravity.CENTER);
			end.setWidth(tv_end.getWidth());
			end.setLayoutParams(params);
			end.setText(tr.getEnd());
			end.setPadding(dps, 0, dps, 0);

			records.setBackgroundResource(R.drawable.cell_shape);
			records.setGravity(Gravity.CENTER);
			records.setWidth(tv_records.getWidth());
			records.setLayoutParams(params);
			if (tr.getRecords() != null)
				records.setText(String.valueOf(tr.getRecords()));
			records.setPadding(dps, 0, dps, 0);

			tr.addView(name);
			tr.addView(start);
			tr.addView(end);
			tr.addView(records);

			rows.add(tr);
			tbllay_dumpsdata.addView(tr);
		}
		tbllay_dumpsdata.invalidate();
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

}
