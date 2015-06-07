package de.unibayreuth.bayeosloggerapp.android.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;
import java.util.Vector;

import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Environment;
import android.util.Log;
import android.widget.TableLayout;
import de.unibayreuth.bayeosloggerapp.android.main.MainActivity;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DataFrame;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DumpedFrame;

public class ReadWriteFile {

	public static File[] getFiles(String directoryPath) {

		File rootPath = new File(Environment.getExternalStorageDirectory(),
				directoryPath);
		if (!rootPath.exists()) {
			rootPath.mkdirs();
		}

		rootPath.mkdirs();
		File[] files = rootPath.listFiles();
		return files;
	}

	public static File containsFile(String directoryPath, String fileName) {

		File[] files = getFiles(directoryPath);

		if (files == null)
			return null;
		for (File file : files) {
			if (file.getName().split("\\.")[0].equals(fileName)) {
				return file;
			}
		}
		return null;
	}

	public static ArrayList<String> getFileNames(File[] files) {
		ArrayList<String> arrayFiles = new ArrayList<String>();
		if (files.length == 0)
			return null;
		else {
			for (int i = 0; i < files.length; i++)
				arrayFiles.add(files[i].getName());
		}

		return arrayFiles;
	}

	public static byte[] readFile(File file) {

		return readFile(file, null);
	}

	public static boolean saveExcelFile(Vector<DumpedFrame> dumpedFrames,
			String fileName) {

		return saveExcelFile(dumpedFrames, fileName, null, null);
	}

	public static TableLayout readExcelFile(SelectableTableRow inputRow) {
		boolean success = false;
		if (inputRow.getParsedFile() == null)
			return null;

		TableLayout tblLayout = new TableLayout(inputRow.getContext());

		try {

			FileInputStream inputStream = new FileInputStream(
					inputRow.getParsedFile());

			// Get the workbook instance for XLS file
			HSSFWorkbook workbook = new HSSFWorkbook(inputStream);

			// Get first sheet from the workbook
			Sheet sheet = workbook.getSheetAt(0);

			// Iterate through each rows from first sheet
			// Iterator<Row> rowIterator = sheet.iterator();
			inputRow.setRecords(sheet.getPhysicalNumberOfRows());

			// cell 0 row 0
			Cell cell = sheet.getRow(sheet.getFirstRowNum()).getCell(0);
			if (HSSFDateUtil.isCellDateFormatted(cell)) {
				inputRow.setStart(cell.getDateCellValue());
			}

			// cell 0 last row
			cell = sheet.getRow(sheet.getLastRowNum()).getCell(0);
			if (HSSFDateUtil.isCellDateFormatted(cell)) {
				inputRow.setEnd(cell.getDateCellValue());
			}

			inputStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/* Checks if external storage is available for read and write */
	private static boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		}
		return false;
	}

	public static byte[] readFile(File file, ProgressDialog binaryDumpProgress) {
		FileInputStream fin = null;
		byte[] fileContent = null;
		try {
			// create FileInputStream object
			fin = new FileInputStream(file);

			fileContent = new byte[(int) file.length()];

			if (binaryDumpProgress != null) {
				binaryDumpProgress.setIndeterminate(true);
			}
			// Reads up to certain bytes of data from this input stream into an
			// array of bytes.
			fin.read(fileContent);

		} catch (FileNotFoundException e) {
			System.out.println("File not found" + e);
		} catch (IOException ioe) {
			System.out.println("Exception while reading file " + ioe);
		} finally {
			// close the streams using close method
			try {
				if (fin != null) {
					fin.close();
				}
			} catch (IOException ioe) {
				System.out.println("Error while closing stream: " + ioe);
			}
		}
		return fileContent;
	}

	public static boolean saveExcelFile(Vector<DumpedFrame> dumpedFrames,
			String fileName, final ProgressDialog binaryDumpProgress,
			MainActivity context) {
		// check if available and not read only
		if (!isExternalStorageWritable()) {
			Log.w("FileUtils", "Storage not available or read only");
			return false;
		}

		boolean success = false;

		// New Workbook
		Workbook wb = new HSSFWorkbook();

		// New Sheet
		Sheet sheet = wb.createSheet("fileName");

		CellStyle cellStyle = wb.createCellStyle();
		CreationHelper createHelper = wb.getCreationHelper();
		cellStyle.setDataFormat(createHelper.createDataFormat().getFormat(
				"dd.MM.yyyy HH:mm:ss"));

		if (binaryDumpProgress != null) {
			binaryDumpProgress.setMax(dumpedFrames.size());
		}
		for (int i = 0; i < dumpedFrames.size(); i++) {
			if (binaryDumpProgress != null)
				binaryDumpProgress.setProgress(i);

			DumpedFrame frame = dumpedFrames.get(i);

			Row row = sheet.createRow(i);

			Cell dateCell = row.createCell(0);
			dateCell.setCellValue(frame.getTimestamp());
			dateCell.setCellStyle(cellStyle);

			if (frame.getFrame() instanceof DataFrame) {
				if (((DataFrame) frame.getFrame()).getValues() != null) {
					Set<Short> channels = ((DataFrame) frame.getFrame())
							.getValues().keySet();
					for (Short channel : channels) {
						Cell cell = row.createCell(channel);
						cell.setCellValue(((DataFrame) frame.getFrame())
								.getValues().get(channel));
					}
				}
			}
		}

		sheet.setDefaultColumnWidth(20);

		// Create a path where we will place our List of objects on external
		// storage
		// File file = new File(context.getExternalFilesDir(null), fileName);
		File rootPath = new File(Environment.getExternalStorageDirectory(),
				MainActivity.DirectoryNameParsed);
		if (!rootPath.exists()) {
			rootPath.mkdirs();
		}

		if (binaryDumpProgress != null) {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					binaryDumpProgress.setMessage("Saving Excel File..");
				}
			});
			binaryDumpProgress.setIndeterminate(true);
		}

		File file = new File(rootPath, fileName + ".xls");

		if (file.exists()) {
			file.delete();
		}

		FileOutputStream os = null;

		try {
			os = new FileOutputStream(file);
			wb.write(os);
			Log.w("FileUtils", "Writing file" + file);
			success = true;
			
		} catch (IOException e) {
			Log.w("FileUtils", "Error writing " + file, e);
		} catch (Exception e) {
			Log.w("FileUtils", "Failed to save file", e);
		} finally {
			try {
				if (null != os)
					os.close();
			} catch (Exception ex) {
			}
		}

		return success;
	}

}
