/*
 *    Copyright (C) 2010 Stewart Gateley <birbeck@gmail.com>
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.birbeck.wallpaperslideshow;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class WallpaperSettings extends PreferenceActivity {
	ProgressDialog mProgressDialog = null;
	
	ArrayList<String> dirs;
	FilenameFilter imageFilter = new FilenameFilter() {
		public boolean accept(File file, String string) {
			string = string.toLowerCase();
			if (string.endsWith(".jpg") || string.endsWith(".png")) {
				return true;
			}
			return false;
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(
				WallpaperMain.SHARED_PREFS_NAME);
		addPreferencesFromResource(R.xml.wallpaper_settings);
		
		mProgressDialog = ProgressDialog.show(WallpaperSettings.this, "", 
                "Loading. Please wait...", true);
		
		new Thread() {
			public void run() {
				dirs = new ArrayList<String>();
				traverse(new File("/sdcard"));
				String[] filesStr = new String[dirs.size()];
				for (int i = 0; i < dirs.size(); i++) {
					filesStr[i] = dirs.get(i).toString();
				}

				ListPreference path = (ListPreference) findPreference("path");
				path.setEntries(filesStr);
				path.setEntryValues(filesStr);
				mProgressDialog.dismiss();
			}
		}.start();		
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	private void traverse(File dir) {
		if (dir.isDirectory()) {
			String[] files = dir.list();
			for (String file : files) {
				if (file.startsWith(".")) {
					continue;
				}
				if (!dirs.contains(dir.toString())) {
					String[] images = dir.list(imageFilter);
					if (images.length > 0) {
						dirs.add(dir.toString());
					}
				}
				traverse(new File(dir, file));
			}
		}
	}

}
