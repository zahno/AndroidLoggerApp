package de.unibayreuth.bayeosloggerapp.tools;

import de.unibayreuth.bayeosloggerapp.android.main.MainActivity;
import android.view.Gravity;
import android.widget.Toast;

public class ToastMessage {

	public static void toastConnectionFailed(MainActivity mainActivity) {
		CharSequence text = "Connection failed!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(mainActivity, text, duration);
		toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 100);
		toast.show();
	}

	public static void toastConnectionSuccessful(MainActivity mainActivity) {
		CharSequence text = "Connection successful!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(mainActivity, text, duration);
		toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 100);
		toast.show();
	}

	public static void toastDisconnected(MainActivity mainActivity) {
		CharSequence text = "Disconnected!";
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(mainActivity, text, duration);
		toast.setGravity(Gravity.TOP | Gravity.CENTER, 0, 100);
		toast.show();
	}

	public static void toastSuccess(MainActivity mainActivity, String string) {
		CharSequence text = "Success! " + string;
		int duration = Toast.LENGTH_SHORT;
		Toast toast = Toast.makeText(mainActivity, text, duration);
		toast.setGravity(Gravity.BOTTOM | Gravity.CENTER, 0, 100);
		toast.show();
	}
}
