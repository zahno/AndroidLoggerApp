package de.unibayreuth.bayeosloggerapp.android.tools;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import de.unibayreuth.bayeosloggerapp.android.main.R;

public class TableCreator {

	public static View createTable(String[][] array, Context context) {

		LinearLayout l = new LinearLayout(context);
		l.setGravity(Gravity.CENTER);

		TableLayout A = new TableLayout(context);
		A.setOrientation(LinearLayout.VERTICAL);
		int dps = getDps(context, 10);
		A.setPadding(dps, dps, dps, dps);

		l.addView(A);
		TableRow row;

		for (int j = 0; j < array[0].length; j++) {
			row = new TableRow(context);
			for (int i = 0; i < array.length; i++) {

				TextView tv = new TextView(context);
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
				tv.setPadding(dps, 0, dps, 0);
				if (i == 0)
					tv.setGravity(Gravity.CENTER);
				if (j == 0) {
					tv.setBackgroundResource(R.drawable.cell_head_shape);
				} else
					tv.setBackgroundResource(R.drawable.cell_shape);
				tv.setText(array[i][j]);
				row.addView(tv);
			}
			A.addView(row);
		}
		return l;
	}

	public static View addMessage(View v, Context context, String title,
			String message) {
		LinearLayout l = new LinearLayout(context);
		l.setOrientation(LinearLayout.VERTICAL);

		TextView tv = new TextView(context);
		tv.setText(title + "\n\t" + message);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

		int dps = getDps(context, 10);
		tv.setPadding(dps, dps, dps, dps);

		l.addView(tv);
		l.addView(v);

		return l;
	}

	public static View addMessage(View v, Context context, String message) {
		LinearLayout l = new LinearLayout(context);
		l.setOrientation(LinearLayout.VERTICAL);

		TextView tv = new TextView(context);
		tv.setText(message);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);

		int dps = getDps(context, 10);
		tv.setPadding(dps, dps, dps, dps);

		l.addView(tv);
		l.addView(v);

		return l;
	}

	public static View combineViews(View upperView, View lowerView,
			Context context) {
		LinearLayout l = new LinearLayout(context);
		l.setOrientation(LinearLayout.VERTICAL);

		l.addView(upperView);
		l.addView(lowerView);

		return l;

	}

	public static int getDps(Context context, int width) {
		float scale = context.getResources().getDisplayMetrics().density;
		return (int) (width * scale + 0.5f);
	}
}
