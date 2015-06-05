package de.unibayreuth.bayeosloggerapp.android.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class DumpsFragment extends Fragment {
	private static final String TAG = "DumpsFragment";

	MainActivity mainActivity;

	public DumpsFragment(MainActivity context) {
		this.mainActivity = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// retain this fragment
		setRetainInstance(true);

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View view = inflater.inflate(R.layout.fragment_dump, container, false);

		return view;
	}

}