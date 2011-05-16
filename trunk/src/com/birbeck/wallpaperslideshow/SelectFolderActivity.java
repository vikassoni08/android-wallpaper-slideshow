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
import java.util.ArrayList;
import java.util.Collection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class SelectFolderActivity extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.select_folder_activity);
		setResult(Activity.RESULT_CANCELED);
		
		ListView listView = getListView();
		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				String folder = ((TextView)arg1.findViewById(R.id.text2))
						.getText().toString();
				Intent intent = new Intent().putExtra("folder", folder);
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		});

		new SearchFoldersTask(this).execute();
	}

	public static final FileFilter ImageFilter = new FileFilter() {
		public boolean accept(File dir) {
			if (dir.isDirectory()) {
				return false;
			}

			String name = dir.getName();
			String ext = BitmapUtil.getExtension(name);

			if (name.equalsIgnoreCase("albumart.jpg")) {
				return false;
			}

			if (ext != null) {
				if (ext.equals("jpg") || ext.equals("jpeg")
						|| ext.equals("png") || ext.equals("gif")) {
					return true;
				} else {
					return false;
				}
			}
			return false;
		}
	};

	private final FileFilter NoMediaFilter = new FileFilter() {
		public boolean accept(File dir) {
			if (dir.getName().equalsIgnoreCase(".nomedia")) {
				return true;
			}
			return false;
		}
	};

	private final FileFilter PhotoFolderFilter = new FileFilter() {
		public boolean accept(File dir) {
			if (!dir.isDirectory()) {
				return false;
			} else if (dir.getName().startsWith(".")) {
				return false;
			} else {
				File[] files = dir.listFiles(NoMediaFilter);
				if (files.length > 0) {
					return false;
				}
			}
			return true;
		}
	};

	class SearchFoldersTask extends AsyncTask<Void, Void, String[]> {
		Context mContext = null;
		ProgressDialog mProgressDialog = null;

		public SearchFoldersTask(Context context) {
			mContext = context;
		}

		@Override
		protected void onPreExecute() {
			mProgressDialog = ProgressDialog.show(mContext, "Please wait",
					"Searching folders, this can take some time to complete...", true);
			mProgressDialog.setCancelable(true);
		}

		@Override
		protected String[] doInBackground(Void... params) {
			ArrayList<File> folders = new ArrayList<File>();
			listDirectories(folders, Environment.getExternalStorageDirectory(),
					PhotoFolderFilter);

			ArrayList<String> temp = new ArrayList<String>();
			for (File f : folders) {
				if (f.listFiles(ImageFilter).length > 0) {
					temp.add(f.toString());
				}
			}

			String[] result = new String[temp.size()];
			for (int i = 0; i < temp.size(); i++) {
				result[i] = temp.get(i);
			}

			return result;
		}

		@Override
		protected void onPostExecute(String[] result) {
			if (mProgressDialog != null && mProgressDialog.isShowing()) {
				mProgressDialog.dismiss();
			}

			if (result != null && result.length > 0) {
				ListActivity activity = (ListActivity) mContext;
				activity.setListAdapter(new FolderArrayAdapter(mContext,
						R.layout.select_folder_list_item, result));
			} else {
				AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
						.setTitle("No photos")
						.setMessage(
								"There were no folders containing photos found.")
						.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										dialog.cancel();
										finish();
									}
								});
				builder.show();
			}
		}
	}

	public static void listDirectories(Collection<File> files, File directory,
			FileFilter filter) {
		File[] found = directory.listFiles((FileFilter) filter);
		if (found != null) {
			for (int i = 0; i < found.length; i++) {
				files.add(found[i]);
				if (found[i].isDirectory()) {
					listDirectories(files, found[i], filter);
				}
			}
		}
	}

}
