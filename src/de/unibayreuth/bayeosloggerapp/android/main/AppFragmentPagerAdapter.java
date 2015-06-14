package de.unibayreuth.bayeosloggerapp.android.main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class AppFragmentPagerAdapter extends FragmentPagerAdapter {
	final int PAGE_COUNT = 4;
	private String tabTitles[] = new String[] { "Logger", "Dumps", "Live",
			"Preferences" };
	private MainActivity mainActivity;

	public AppFragmentPagerAdapter(FragmentManager fm, MainActivity context) {
		super(fm);
		this.mainActivity = context;
	}

	@Override
	public int getCount() {
		return PAGE_COUNT;
	}

	@Override
	public Fragment getItem(int position) {

		switch (position) {
		case 0:
			if (mainActivity.getLoggerFragment() == null) {
				mainActivity.setLoggerFragment(new LoggerFragment());
			}
			return mainActivity.getLoggerFragment();
		case 1:
			if (mainActivity.getDumpsFragment() == null) {
				mainActivity.setDumpsFragment(new DumpsFragment());
			}
			return mainActivity.getDumpsFragment();
		case 2:
			if (mainActivity.getLiveFragment() == null) {
				mainActivity.setLiveFragment(new LiveFragment());
			}
			return mainActivity.getLiveFragment();

		case 3:
			if (mainActivity.getPreferenceFragment() == null) {
				mainActivity.setPreferenceFragment(new AppPreferences());
			}
			return mainActivity.getPreferenceFragment();
		}

		return null;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		// Generate title based on item position
		return tabTitles[position];
	}
}