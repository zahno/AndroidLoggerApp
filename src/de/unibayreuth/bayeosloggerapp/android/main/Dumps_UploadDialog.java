package de.unibayreuth.bayeosloggerapp.android.main;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Vector;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.TypedValue;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import de.unibayreuth.bayeosloggerapp.android.tools.ReadWriteFile;
import de.unibayreuth.bayeosloggerapp.android.tools.SelectableTableRow;
import de.unibayreuth.bayeosloggerapp.android.tools.TableCreator;
import de.unibayreuth.bayeosloggerapp.android.tools.ViewWrapper;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DumpedFrame;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.TimestampFrame;
import de.unibayreuth.bayeosloggerapp.tools.StringTools;

public class Dumps_UploadDialog extends DialogFragment {
	// private static final String TAG = "Upload Dialog ";
	private Logger LOG = LoggerFactory.getLogger(Dumps_UploadDialog.class);

	private String user, password;
	private String host, path;
	private String[][] files;
	private Vector<SelectableTableRow> selectedrows;
	private SharedPreferences preferences;
	private View tableView;

	public Dumps_UploadDialog(Vector<SelectableTableRow> selectedRows,
			MainActivity context) {
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
		if (context.loggingEnabled())
			LOG.info("Uploading files: {}", selectedrows.toString());
		this.tableView = TableCreator.createTable(files, context);

	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		this.preferences = PreferenceManager
				.getDefaultSharedPreferences(getActivity());
		this.host = preferences.getString("host", null);
		this.path = preferences.getString("path", null);

		View v = TableCreator.addMessage(tableView, getActivity(), "Gateway:",
				host + path);
		return new AlertDialog.Builder(getActivity())
				.setTitle("Upload " + selectedrows.size() + " file(s)?")
				.setView(v)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								user = preferences.getString("username", null);
								password = preferences.getString("password",
										null);

								new UploadDataToGateway(
										(MainActivity) getActivity(),
										selectedrows, user, password) {

								}.execute(host + path);
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

	public String getUser() {
		return user;
	}

	public String getPassword() {
		return password;
	}

	public View getTableView() {
		return tableView;
	}
}

class UploadDataToGateway extends AsyncTask<String, String, Void> {

	// private static final String TAG = "UploadDataToGateway";
	private Logger LOG = LoggerFactory.getLogger(UploadDataToGateway.class);

	private Vector<SelectableTableRow> selectedRows;
	private String userName, passWord;
	private ProgressDialog progress, cancel;
	private String[][] results;
	private boolean cancelled;
	private MainActivity context;

	private final static int maxNoOfFrames = 10000;

	public UploadDataToGateway(MainActivity activity,
			Vector<SelectableTableRow> selectedrows, String userName,
			String passWord) {
		this.context = activity;
		this.userName = userName;
		this.passWord = passWord;
		this.selectedRows = selectedrows;
		this.progress = new ProgressDialog(context);
		this.results = new String[3][selectedrows.size() + 1];
		this.results[0][0] = "No.";
		this.results[1][0] = "Name";
		this.results[2][0] = "HTTP Status Code";

	}

	@Override
	protected void onPreExecute() {

		progress.setTitle("Uploading " + selectedRows.size()
				+ " file(s) to gateway...");
		this.progress.setMessage("");
		progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progress.setIndeterminate(true);
		progress.setCancelable(false);

		progress.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						cancel = new ProgressDialog(context);
						cancel.setTitle("Cancel upload");
						cancel.setMessage("Finishing upload of current file..");
						cancel.setIndeterminate(true);
						cancel.setCancelable(false);

						cancel.show();

						cancelled = true;

					}
				});

		this.progress.show();

	}

	@Override
	protected Void doInBackground(String... urls) {

		Thread.currentThread().setName("AsyncTask UploadDataToGateway");

		SSLContext ctx;
		try {
			ctx = SSLContext.getInstance("TLS");
			ctx.init(null, new TrustManager[] { new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain,
						String authType) {
				}

				public void checkServerTrusted(X509Certificate[] chain,
						String authType) {
				}

				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[] {};
				}
			} }, null);
			HttpsURLConnection.setDefaultSSLSocketFactory(ctx
					.getSocketFactory());

			HttpsURLConnection
					.setDefaultHostnameVerifier(new HostnameVerifier() {
						public boolean verify(String hostname,
								SSLSession session) {
							return true;
						}
					});

		} catch (NoSuchAlgorithmException | KeyManagementException e1) {
			if (context.loggingEnabled())
				LOG.warn(
						"An exception occurred when connecting to gateway: {}",
						e1.getMessage());
		}

		SelectableTableRow row;
		for (int i = 0; i < selectedRows.size(); i++) {
			if (cancelled) {
				break;
			}
			row = selectedRows.get(i);

			results[0][i + 1] = i + 1 + "";
			results[1][i + 1] = row.getRawFile().getName();

			byte[] frames = null;
			try {
				frames = ReadWriteFile.readFile(row.getRawFile());
			} catch (IOException e1) {
				if (context.loggingEnabled())
					LOG.warn(
							"An Exception occurred when reading a .db file: {}",
							e1.getMessage());
			}
			Vector<DumpedFrame> dumpedFrames = DumpedFrame
					.parseDumpFile(frames);

			if (frames == null) {
				break;
			}

			try {

				publishProgress("["
						+ (i + 1)
						+ "/"
						+ selectedRows.size()
						+ "]: "
						+ row.getRawFile().getName()
						+ (" ("
								+ StringTools.byteCountConverter(row
										.getRawFile().length()) + ")"));

				for (int j = 0; j < dumpedFrames.size(); j += maxNoOfFrames) {
					if (cancelled) {
						break;
					}

					URL url = new URL(urls[0]);

					HttpsURLConnection connection = (HttpsURLConnection) url
							.openConnection();

					String body = "sender="
							+ URLEncoder.encode(row.getName(), "UTF-8")
							+ getFramesAsByte64(dumpedFrames, j);

					((HttpURLConnection) connection).setRequestMethod("POST");
					connection.setDoInput(true);
					connection.setDoOutput(true);
					connection.setUseCaches(false);
					connection.setRequestProperty(
							"Authorization",
							"Basic "
									+ Base64.encodeToString(
											((userName + ":" + passWord)
													.getBytes()),
											Base64.DEFAULT));
					connection.setRequestProperty("Content-Type",
							"application/x-www-form-urlencoded");
					connection.setRequestProperty("Content-Length",
							String.valueOf(body.length()));

					OutputStreamWriter writer = new OutputStreamWriter(
							connection.getOutputStream());
					writer.write(body);
					writer.flush();

					results[2][i + 1] = StringTools.httpCodeToString(connection
							.getResponseCode());
				}
			} catch (MalformedURLException e) {
				if (context.loggingEnabled())
					LOG.warn("URL seems to be malformed: {}", e.getMessage());
			} catch (IOException e) {
				if (context.loggingEnabled())
					LOG.warn(
							"An exception occured when trying to write to gateway: {}",
							e.getMessage());
			}

		}

		return null;
	}

	private String getFramesAsByte64(Vector<DumpedFrame> dumpedFrames,
			int offset) {

		StringBuilder sb = new StringBuilder();

		for (int i = offset; i < offset + maxNoOfFrames
				&& i < dumpedFrames.size(); i++) {
			DumpedFrame frame = dumpedFrames.get(i);

			if (frame.getFrame() != null) {
				sb.append("&bayeosframes[]=");

				try {
					sb.append(URLEncoder.encode(Base64.encodeToString(
							(new TimestampFrame(frame)).asByteArray(),
							Base64.NO_WRAP), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					if (context.loggingEnabled())
						LOG.warn("URL Encoding exception: {}", e.getMessage());
				}
			}

		}

		String res = sb.toString();
		if (context.loggingEnabled())
			LOG.info("Converted frames to URL & Base64 encoded String: {}", res);

		return res;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		progress.setMessage(values[0]);
	}

	@Override
	protected void onPostExecute(Void result) {
		//check if there are any cells with no http status code (-> no connection)
		
		for (int i = 0; i < results[0].length; i++){
			if (results[2][i] == null || results[2][i].isEmpty())
				results[2][i] = "No Connection?";
		}
		
		
		if (progress.isShowing()) {
			progress.dismiss();
		}
		if (cancel != null && cancel.isShowing())
			cancel.dismiss();

		View table = TableCreator.createTable(results, context);

		LinearLayout v1 = new LinearLayout(context);
		final CheckBox deleteFiles = new CheckBox(context);
		deleteFiles.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		deleteFiles.setText("Delete file(s)");
		deleteFiles.setChecked(true);
		v1.addView(deleteFiles);

		int dps = TableCreator.getDps(context, 5);
		v1.setPadding(dps, dps / 2, dps / 2, dps / 2);

		final LinearLayout v2 = new LinearLayout(context);
		final CheckBox deleteCSV = new CheckBox(context);
		deleteCSV.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
		deleteCSV.setText("Delete related CSV file (if existing)");
		deleteCSV.setChecked(false);

		v2.addView(deleteCSV);
		MainActivity.enable(v2);

		v2.setPadding(dps * 5, 0,0,0);

		deleteFiles.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked)
					MainActivity.enable(v2);
				else
					MainActivity.disable(v2);
			}
		});

		if (deleteFiles != null && deleteCSV != null) {
			table = TableCreator.combineViews(table, v1, context);
			table = TableCreator.combineViews(table, v2, context);
		}

		new AlertDialog.Builder(context)
				.setTitle("Upload Report")
				.setView(table)
				.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
								if (deleteFiles.isChecked())
									Dumps_DeleteDialog.deleteFiles(
											selectedRows,
											deleteCSV.isChecked(), context);
							}
						})

				.setIcon(android.R.drawable.ic_dialog_info).show();
		ViewWrapper.forceWrapContent(table);

		super.onPostExecute(result);
	}

}
