package de.unibayreuth.bayeosloggerapp.android.main;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class AppFragmentPagerAdapter extends FragmentPagerAdapter {
	final int PAGE_COUNT = 3;
	private String tabTitles[] = new String[] { "Logger", "Dumps", "Live" };
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
				mainActivity
						.setLoggerFragment(new LoggerFragment(mainActivity));
			}
			return mainActivity.getLoggerFragment();
		case 1:
			if (mainActivity.getDumpsFragment() == null) {
				mainActivity.setDumpsFragment(new DumpsFragment(mainActivity));
			}
			return mainActivity.getDumpsFragment();
		case 2:
			if (mainActivity.getLiveFragment() == null) {
				mainActivity.setLiveFragment(new LiveFragment(mainActivity));
			}
			return mainActivity.getLiveFragment();
		}

		return null;
	}

	@Override
	public CharSequence getPageTitle(int position) {
		// Generate title based on item position
		return tabTitles[position];
	}
}