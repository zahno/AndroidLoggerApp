package de.unibayreuth.bayeosloggerapp.android.tools;

import android.content.Context;
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
		
		final float scale = context.getResources().getDisplayMetrics().density;
		int dps = (int) (10 * scale + 0.5f);

		TableLayout A = new TableLayout(context);
		A.setOrientation(LinearLayout.VERTICAL);
		A.setPadding(dps, dps, dps, dps);

		l.addView(A);
		TableRow row;

		for (int j = 0; j < array[0].length; j++) {
			row = new TableRow(context);
			for (int i = 0; i < array.length; i++) {

				TextView tv = new TextView(context);
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
}
