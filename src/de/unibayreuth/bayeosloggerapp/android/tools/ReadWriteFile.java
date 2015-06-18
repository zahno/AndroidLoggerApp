package de.unibayreuth.bayeosloggerapp.android.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import android.app.ProgressDialog;
import android.os.Environment;
import android.util.Log;

import com.opencsv.CSVWriter;

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

	public static boolean saveCSV(Vector<DumpedFrame> dumpedFrames,
			String fileName) {

		return saveCSV(dumpedFrames, fileName, null, null);
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

	public static boolean saveCSV(Vector<DumpedFrame> dumpedFrames,
			String fileName, MainActivity context) {
		return saveCSV(dumpedFrames, fileName, null, context);
	}

	public static boolean saveCSV(Vector<DumpedFrame> dumpedFrames,
			String fileName, final ProgressDialog binaryDumpProgress,
			MainActivity context) {
		boolean success = false;

		
		// check if available and not read only
		if (!isExternalStorageWritable()) {
			Log.w("FileUtils", "Storage not available or read only");
			return false;
		}

		File rootPath = new File(Environment.getExternalStorageDirectory(),
				MainActivity.DirectoryNameParsed);
		if (!rootPath.exists()) {
			rootPath.mkdirs();
		}

		if (binaryDumpProgress != null) {
			context.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					binaryDumpProgress.setMessage("Saving CSV file..");
				}
			});
			binaryDumpProgress.setIndeterminate(true);
		}

		File file = new File(rootPath, fileName + ".csv");

		if (file.exists()) {
			file.delete();
		}


		String csv = file.getAbsolutePath();
		CSVWriter writer;
		try {
			writer = new CSVWriter(new FileWriter(csv));

			DumpedFrame frame;
			String[] values = null;
			for (int i = 0; i < dumpedFrames.size(); i++) {
				if (binaryDumpProgress != null)
					binaryDumpProgress.setProgress(i);

				frame = dumpedFrames.get(i);

				if (frame.getFrame() instanceof DataFrame) {
					if (((DataFrame) frame.getFrame()).getValues() != null) {

						Set<Short> channels = ((DataFrame) frame.getFrame())
								.getValues().keySet();

						values = new String[channels.size() + 1];

						for (Short channel : channels) {
							values[channel] = String.valueOf(((DataFrame) frame
									.getFrame()).getValues().get(channel));
						}
					}
				}

				values[0] = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss",
						Locale.US).format(frame.getTimestamp());
				writer.writeNext(values);

			}

			writer.close();
			success = true;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return success;
	}

}
