package de.unibayreuth.bayeosloggerapp.android.tools;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Vector;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;
import de.unibayreuth.bayeosloggerapp.android.main.MainActivity;
import de.unibayreuth.bayeosloggerapp.android.main.R;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DumpedFrame;

public class SelectableTableRow extends TableRow {

	private boolean isSelected = false;
	private File rawFile, excelFile;
	private String name;
	private Integer records = null;
	private String start, end;
	private TextView tv_name, tv_start, tv_end, tv_records;

	public SelectableTableRow(Context context, File rawFile) {
		super(context);
		this.rawFile = rawFile;
		this.setClickable(true);
		this.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!isSelected) {
					select();
				} else {
					deselect();
				}
			}
		});
		updateFileMetadata();
	}

	public void setTvName(TextView name) {
		this.tv_name = name;
	}

	public void setTvStart(TextView start) {
		this.tv_start = start;
	}

	public void setTvEnd(TextView end) {
		this.tv_end = end;
	}

	public void setTvRecords(TextView records) {
		this.tv_records = records;
	}

	public TextView getTvName() {
		return tv_name;
	}

	public TextView getTvStart() {
		return tv_start;
	}

	public TextView getTvEnd() {
		return tv_end;
	}

	public TextView getTvRecords() {
		return tv_records;
	}

	private void updateFileMetadata() {

		byte[] readFile = ReadWriteFile.readFile(this.getRawFile());

		Vector<DumpedFrame> dumpedFrames = DumpedFrame.parseDumpFile(readFile);

		setStart(dumpedFrames.firstElement().getTimestamp());
		setEnd(dumpedFrames.lastElement().getTimestamp());
		setRecords(dumpedFrames.size());

		// ReadWriteFile.readExcelFile(this);
	}

	public boolean isSelected() {
		return isSelected;
	}

	public void setRawFile(File file) {
		this.rawFile = file;
	}

	public File getRawFile() {
		return rawFile;
	}

	public void setParsedFile(File file) {
		this.excelFile = file;
	}

	public File getParsedFile() {
		return excelFile;
	}

	public String getName() {
		return name;
	}

	public String getStart() {
		return start;
	}

	public String getEnd() {
		return end;
	}

	public Integer getRecords() {
		return records;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setStart(Date start) {
		this.start = (new SimpleDateFormat("EEE',' dd.MM.yyyy 'at' HH:mm:ss z",
				Locale.US).format(start));
	}

	public void setStart(String start) {
		this.start = start;
	}

	public void setEnd(Date end) {
		this.end = (new SimpleDateFormat("EEE',' dd.MM.yyyy 'at' HH:mm:ss z",
				Locale.US).format(end));
	}

	public void setEnd(String end) {
		this.end = end;
	}

	public void setRecords(int records) {
		this.records = records;
	}

	private void highlight() {
		for (int i = 0; i < ((ViewGroup) this).getChildCount(); ++i) {
			View column = ((ViewGroup) this).getChildAt(i);
			column.setBackgroundResource(R.drawable.cell_shape_highlighted);
		}
	}

	private void unhighlight() {
		for (int i = 0; i < ((ViewGroup) this).getChildCount(); ++i) {
			View column = ((ViewGroup) this).getChildAt(i);
			column.setBackgroundResource(R.drawable.cell_shape);
		}

	}

	public void addParsedFileInformation() {
		File parsedFile = ReadWriteFile.containsFile(
				MainActivity.DirectoryNameParsed, rawFile.getName()
						.split("\\.")[0]);

		if (parsedFile != null) {
			this.setParsedFile(parsedFile);
		}
	}

	public void select() {
		highlight();
		isSelected = true;
	}

	public void deselect() {
		unhighlight();
		isSelected = false;
	}

}
