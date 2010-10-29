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

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class WallpaperMain extends WallpaperService
	implements OnSharedPreferenceChangeListener {
	
	public static final String SHARED_PREFS_NAME = "wallpapersettings";
	private SharedPreferences mPrefs;
	private long duration;
	private String path;
	private boolean rotate;
	private boolean random;

	@Override
	public Engine onCreateEngine() {
		return new WallpaperEngine();
	}
	
	@Override
	public void onCreate() {
		mPrefs = this.getSharedPreferences(SHARED_PREFS_NAME, 0);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		path = mPrefs.getString("path", "/sdcard/DCIM/100MEDIA");
		duration = Long.parseLong(mPrefs.getString("duration", "60000"));
		rotate = mPrefs.getBoolean("rotate", false);
		random = mPrefs.getBoolean("rotate", false);
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		mPrefs.unregisterOnSharedPreferenceChangeListener(this);
		super.onDestroy();
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		path = mPrefs.getString("path", "/sdcard/DCIM/100MEDIA");
		duration = Long.parseLong(mPrefs.getString("duration", "60000"));
		rotate = mPrefs.getBoolean("rotate", false);
		random = mPrefs.getBoolean("random", false);
	}
	
	class WallpaperEngine extends Engine {

		private ServiceWorker worker;
		
		WallpaperEngine() {
			worker = new ServiceWorker(getSurfaceHolder());
		}
		
		@Override
		public void onCreate(SurfaceHolder holder) {
			super.onCreate(holder);
			setTouchEventsEnabled(true);
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
			worker.stopPainting();
		}
		
		@Override
		public void onVisibilityChanged(boolean visible) {
			if (visible) {
				worker.resumePainting();
			} else {
				worker.pausePainting();
			}
		}
		
		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				                     int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			worker.setSurfaceSize(width, height);
		}
		
		@Override
		public void onSurfaceCreated(SurfaceHolder holder) {
			super.onSurfaceCreated(holder);
			if (!worker.isAlive()) {
				worker.start();
			}
		}
		
		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			boolean retry = true;
			worker.stopPainting();
			while (retry) {
				try {
					worker.join();
					retry = false;
				} catch (InterruptedException e) {}
			}
		}
		
		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				                     float xStep, float yStep,
				                     int xPixels, int yPixels) {
		}
		
		@Override
		public void onTouchEvent(MotionEvent event) {
			super.onTouchEvent(event);
		}
		
		class ServiceWorker extends Thread
			implements OnSharedPreferenceChangeListener {
			
			private SurfaceHolder holder;
			private int width;
			private int height;
			private boolean run;
			private boolean wait;
			private boolean redraw;
			private long currentTime;
			private long lastTime;
			private int index;
			
			ServiceWorker(SurfaceHolder holder) {
				this.holder = holder;
				this.wait = true;
				this.lastTime = 0;
				this.redraw = true;
				this.index = -1;
				mPrefs.registerOnSharedPreferenceChangeListener(this);
			}
			
			private final FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					String ext = name.substring(name.lastIndexOf('.')+1, name.length());
					if (ext.equals("jpg") || ext.equals("png")) {
						return true;
					} else {
						return false;
					}
				}
			};
			
			@Override
			public void run() {
				this.run = true;
				Canvas c = null;
				Paint p = new Paint();
				while(this.run) {
					currentTime = System.currentTimeMillis();
					if (currentTime - lastTime > duration) {
						lastTime = currentTime;
						this.redraw = true;
					}
					
					if (redraw) {
						Bitmap bitmap;
						try {
							synchronized (this.holder) {
								File[] files = null;
								try {
									files = new File(path).listFiles(filter);
								} catch (Exception e) {}
								
								if (files == null || files.length == 0) {
									bitmap = BitmapFactory.decodeResource(
											getResources(), R.drawable.sdcard_error);
								} else {
									if (random) {
										int rIndex;
										do {
											rIndex = (int)(Math.random() * files.length);
										} while (rIndex == index);
										bitmap = Util.decodeFile(files[rIndex], width, height);
										index = rIndex;
									} else {
										if (++index >= files.length) index = 0;
										bitmap = Util.decodeFile(files[index], width, height);
									}
									
									
									int screenOrientation = getResources().getConfiguration().orientation;
									if (bitmap.getWidth() > bitmap.getHeight()
											&& screenOrientation == Configuration.ORIENTATION_PORTRAIT) {
										if (rotate) bitmap = Util.rotate(bitmap, 90);
									}
									else if (bitmap.getHeight() > bitmap.getWidth()
											&& screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
										if (rotate) bitmap = Util.rotate(bitmap, -90);
									}
									
									bitmap = Util.transform(new Matrix(),
											bitmap, width, height, true, true);

									for (int i = 0; i<=255; i++) {
										c = this.holder.lockCanvas(null);
										p.setAlpha(i);
										c.drawBitmap(bitmap, 0,	0, p);
										if (c != null) {
											this.holder.unlockCanvasAndPost(c);
										}
										i+=17;
										synchronized (this) {
											try {
												sleep(50);
											} catch (Exception e) {}
										}
									}

									c = this.holder.lockCanvas(null);
									c.drawColor(Color.BLACK);
									p.setAlpha(255);
									c.drawBitmap(bitmap,
											(width/2f) - (bitmap.getWidth()/2f),
										    (height/2f) - (bitmap.getHeight()/2f), p);
									if (c != null) {
										this.holder.unlockCanvasAndPost(c);
									}

									bitmap.recycle();
								}
							}
						} finally {
							redraw = false;
						}
					}
					
					synchronized (this) {
						if (wait) {
							try {
								wait();
							} catch (Exception e) {}
						}
					}
				}
			}

			public void pausePainting() {
				this.wait = true;
				synchronized (this) {
					this.notify();
				}
			}

			public void resumePainting() {
				this.wait = false;
				// this.redraw = true;
				synchronized (this) {
					this.notify();
				}
			}

			public void stopPainting() {
				this.run = false;
				synchronized (this) {
					this.notify();
				}
			}
			
			public void setSurfaceSize(int width, int height) {
				this.width = width;
				this.height = height;
				this.redraw = true;
				synchronized (this) {
					this.notify();
				}
			}

			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				Log.d("Wallpaper Slideshow", key + " changed");
				if (key.equals("path")) {
					this.index = -1;
					this.lastTime = 0;
					this.redraw = true;
				}
				else if (key.equals("rotate")) {
					this.redraw = true;
				}
				synchronized (this) {
					this.notify();
				}
			}
		}

	}
}