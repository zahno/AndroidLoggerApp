package de.unibayreuth.bayeosloggerapp.android.main;

import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import de.unibayreuth.bayeosloggerapp.android.tools.SelectableTableRow;
import de.unibayreuth.bayeosloggerapp.android.tools.TableCreator;
import de.unibayreuth.bayeosloggerapp.tools.StringTools;

public class Dumps_DeleteDialog extends DialogFragment {
	// private static final String TAG = "Delete Dialog";
	private static Logger LOG = LoggerFactory
			.getLogger(Dumps_DeleteDialog.class);

	private String[][] files;
	private Vector<SelectableTableRow> selectedrows;
	private View rawTableView;
	private MainActivity context;

	public Dumps_DeleteDialog(Vector<SelectableTableRow> selectedRows,
			MainActivity context) {

		this.context = context;
		this.selectedrows = selectedRows;

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

		this.rawTableView = TableCreator.createTable(files, context);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		View v = TableCreator.addMessage(rawTableView, getActivity(),
				"The following file(s) will be deleted.");
		LinearLayout v1 = new LinearLayout(context);

		int dps = TableCreator.getDps(context, 5);
		v1.setPadding(dps, dps / 2, dps / 2, dps / 2);

		final CheckBox v2 = new CheckBox(getActivity());
		v2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		v2.setText("Delete related CSV file");
		v2.setTextColor(Color.DKGRAY);
		v1.addView(v2);

		if (v2 != null) {
			v = TableCreator.combineViews(v, v1, getActivity());
		}

		return new AlertDialog.Builder(getActivity())
				.setTitle("Delete " + selectedrows.size() + " file(s)?")
				.setView(v)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								deleteFiles(selectedrows, v2.isChecked(),
										context);

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

	/**
	 * @param selectedrows2
	 */
	public static void deleteFiles(Vector<SelectableTableRow> rows,
			boolean deleteCSV, MainActivity context) {
		for (SelectableTableRow row : rows) {
			row.getRawFile().delete();
			if (deleteCSV && row.getParsedFile() != null)
				row.getParsedFile().delete();
		}
		if (context.loggingEnabled())
			LOG.info("Deleted files: {}", rows.toString());
		context.getDumpsFragment().refreshTable();
	}

	public View getTableView() {
		return rawTableView;
	}

}
