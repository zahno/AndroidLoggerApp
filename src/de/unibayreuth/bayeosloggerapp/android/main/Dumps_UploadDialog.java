package de.unibayreuth.bayeosloggerapp.android.main;

import java.io.IOException;
import java.io.OutputStreamWriter;
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import de.unibayreuth.bayeosloggerapp.android.tools.ReadWriteFile;
import de.unibayreuth.bayeosloggerapp.android.tools.SelectableTableRow;
import de.unibayreuth.bayeosloggerapp.android.tools.TableCreator;
import de.unibayreuth.bayeosloggerapp.android.tools.ViewWrapper;
import de.unibayreuth.bayeosloggerapp.frames.bayeos.DumpedFrame;
import de.unibayreuth.bayeosloggerapp.tools.StringTools;

public class Dumps_UploadDialog extends DialogFragment {
	private static final String TAG = "Upload Dialog ";
	private String user, password;
	private String host, path;
	private String[][] files;
	private Vector<SelectableTableRow> selectedrows;
	private SharedPreferences preferences;
	private View tableView;

	public Dumps_UploadDialog(Vector<SelectableTableRow> selectedRows,
			Context context) {
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
				.setTitle(
						"Upload following " + selectedrows.size() + " file(s)?")
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

	private static final String TAG = "UploadDataToGateway";
	private Vector<SelectableTableRow> selectedRows;
	private String userName, passWord;
	private ProgressDialog progress, cancel;
	private String[][] results;
	private boolean cancelled;
	private MainActivity context;

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
		progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		this.progress.setMessage("");
		progress.setMax(selectedRows.size());
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

		// TODO insecure connection ok?
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
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		SelectableTableRow row;
		for (int i = 0; i < selectedRows.size(); i++) {
			if (cancelled) {
				break;
			}
			row = selectedRows.get(i);

			// stringBuilder.append("\nFile \"" + row.getName() + "\": ");
			results[0][i + 1] = i + 1 + "";
			results[1][i + 1] = row.getRawFile().getName();

			try {

				publishProgress(row.getRawFile().getName()
						+ (" ("
								+ StringTools.byteCountConverter(row
										.getRawFile().length()) + ")"));

				URL url = new URL(urls[0]);

				HttpsURLConnection connection = (HttpsURLConnection) url
						.openConnection();

				String body = "sender="
						+ URLEncoder.encode(getLoggerName(row), "UTF-8")
						+ getFramesAsByte64(row);

				((HttpURLConnection) connection).setRequestMethod("POST");
				connection.setDoInput(true);
				connection.setDoOutput(true);
				connection.setUseCaches(false);
				connection
						.setRequestProperty(
								"Authorization",
								"Basic "
										+ Base64.encodeToString(((userName
												+ ":" + passWord).getBytes()),
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

			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return null;
	}

	private String getFramesAsByte64(SelectableTableRow row) {
		StringBuilder sb = new StringBuilder();
		byte[] frames = ReadWriteFile.readFile(row.getRawFile());
		Vector<DumpedFrame> dumpedFrames = DumpedFrame.parseDumpFile(frames);

		for (DumpedFrame frame : dumpedFrames) {
			if (frame.getFrame() != null) {
				sb.append("&bayeosframes[]=");
				sb.append(Base64.encodeToString(frame.getFrame().asByteArray(),
						Base64.DEFAULT));
			}

		}

		String res = sb.toString();
		res = res.replace("\r\n", "").replace("\n", "");
		Log.i(TAG, res);
		return res;
	}

	private String getLoggerName(SelectableTableRow row) {
		row.getName().split("_");
		String dateFormat = "_yyyy_MM_dd_HH_mm_ss";
		return (String) row.getName().subSequence(5,
				row.getName().length() - dateFormat.length());
	}

	@Override
	protected void onProgressUpdate(String... values) {
		progress.setMessage(values[0]);
		progress.incrementProgressBy(1);
	}

	@Override
	protected void onPostExecute(Void result) {
		if (progress.isShowing()) {
			progress.dismiss();
		}
		if (cancel != null && cancel.isShowing())
			cancel.dismiss();

		View table = TableCreator.createTable(results, context);
		new AlertDialog.Builder(context)
				.setTitle("Upload Report")
				.setView(table)
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
