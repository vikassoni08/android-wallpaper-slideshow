/*
**    Copyright (C) 2010 Stewart Gateley <birbeck@gmail.com>
**
**    This program is free software: you can redistribute it and/or modify
**    it under the terms of the GNU General Public License as published by
**    the Free Software Foundation, either version 3 of the License, or
**    (at your option) any later version.
**
**    This program is distributed in the hope that it will be useful,
**    but WITHOUT ANY WARRANTY; without even the implied warranty of
**    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
**    GNU General Public License for more details.
**
**    You should have received a copy of the GNU General Public License
**    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.birbeck.wallpaperslideshow;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

public class SelectFolderActivity extends Activity {

	private SharedPreferences mPrefs;
	private final int SELECT_FOLDER = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = this.getSharedPreferences(WallpaperMain.SHARED_PREFS_NAME, 0);

		setResult(RESULT_CANCELED);

		Intent intent = new Intent(Intent.ACTION_PICK,
				android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		intent.setType("image/*");
		startActivityForResult(Intent.createChooser(intent, "Select folder"), 1);
	}

	protected final void onActivityResult(final int requestCode,
			final int resultCode, final Intent i) {
		super.onActivityResult(requestCode, resultCode, i);

		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case SELECT_FOLDER:
				Editor editor = mPrefs.edit();
				editor.putString("path", getPath(i.getData()));
				editor.commit();
				setResult(RESULT_OK);
				break;
			}
		}

		finish();
	}

	private String getPath(final Uri uri) {
		Cursor cursor = managedQuery(uri,
				new String[] { MediaStore.Images.Media.DATA }, null, null, null);

		cursor.moveToFirst();
		return new File(cursor.getString(0)).getParent();
	}

}