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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.util.Log;

public class WallpaperSettings extends PreferenceActivity {
	ProgressDialog mProgressDialog = null;
	private static final String TAG = "Wallpaper Slideshow";
	
	ArrayList<String> dirs;
	FilenameFilter imageFilter = new FilenameFilter() {
		public boolean accept(File file, String string) {
			string = string.toLowerCase();
			if (string.endsWith(".jpg") || string.endsWith(".jpeg") || string.endsWith(".png")) {
				return true;
			}
			return false;
		}
	};

	FileFilter noMediaFilter = new FileFilter() {
		public boolean accept(final File file) {
			if (file.isDirectory()) {
				// We can't access this directory
				if (!file.canRead()) {
					Log.d(TAG, file + " is not accessible");
					return false;
				}

				// We don't want hidden directories
				if (file.getName().startsWith(".")) {
					Log.d(TAG, file + " is hidden");
					return false;
				}
				
				// We don't want directories containing .nomedia
				final String[] files = file.list();
				for (final String f : files) {
					if (f.equalsIgnoreCase(".nomedia")) {
						Log.d(TAG, file + " contains .nomedia");
						return false;
					}
				}
				
				// We don't want directories containing only AlbumArt.jpg
				final String[] images = file.list(imageFilter);
				if (images.length == 1 &&
						images[0].equalsIgnoreCase("AlbumArt.jpg")) {
					Log.d(TAG, file + " contains only AlbumArt.jpg");
					return false;
				}

				// Valid directory
				return true;
			}

			// Not a directory
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
				if (mProgressDialog != null && mProgressDialog.isShowing()) {
					mProgressDialog.dismiss();
				}
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
			final File[] files = dir.listFiles(noMediaFilter);
			for (final File file : files) {
				final String strFile = file.toString();
				if (!dirs.contains(strFile)) {
					if (file.list(imageFilter).length > 0) {
						dirs.add(strFile);
					}
				}
				traverse(file);
			}
		}
	}

}
