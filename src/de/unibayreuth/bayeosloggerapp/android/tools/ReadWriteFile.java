package de.unibayreuth.bayeosloggerapp.android.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.ProgressDialog;
import android.os.Environment;

import com.opencsv.CSVWriter;

import de.unibayreuth.bayeosloggerapp.android.main.MainActivity;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DataFrame;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DumpedFrame;

public class ReadWriteFile {

	private static Logger LOG = LoggerFactory.getLogger(ReadWriteFile.class);

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

	public static byte[] readFile(File file) throws IOException {

		return readFile(file, null);
	}

	public static boolean saveCSV(Vector<DumpedFrame> dumpedFrames,
			String fileName) throws IOException {

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

	public static byte[] readFile(File file, ProgressDialog binaryDumpProgress)
			throws IOException {

		LOG.info("Reading file: " + file.getName());

		FileInputStream fin = null;
		byte[] fileContent = null;
		// create FileInputStream object
		fin = new FileInputStream(file);

		fileContent = new byte[(int) file.length()];

		if (binaryDumpProgress != null) {
			binaryDumpProgress.setIndeterminate(true);
		}
		// Reads up to certain bytes of data from this input stream into an
		// array of bytes.
		fin.read(fileContent);

		// close the streams using close method
		if (fin != null)
			fin.close();

		return fileContent;
	}

	public static boolean saveCSV(Vector<DumpedFrame> dumpedFrames,
			String fileName, MainActivity context) throws IOException {
		return saveCSV(dumpedFrames, fileName, null, context);
	}

	public static boolean saveCSV(Vector<DumpedFrame> dumpedFrames,
			String fileName, final ProgressDialog binaryDumpProgress,
			MainActivity context) throws IOException {
		boolean success = false;

		// check if available and not read only
		if (!isExternalStorageWritable()) {
			if (context.loggingEnabled())
				LOG.warn("FileUtils", "Storage not available or read only");
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
		writer = new CSVWriter(new FileWriter(csv));

		DumpedFrame frame;
		String[] values = null;
		for (int i = 0; i < dumpedFrames.size(); i++) {
			values = null;

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
			if (values == null)
				values = new String[1];
			values[0] = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US)
					.format(frame.getTimestamp());
			writer.writeNext(values);

		}

		writer.close();
		success = true;

		return success;
	}

}
