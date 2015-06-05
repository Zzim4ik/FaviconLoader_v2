package com.stickypassword.android.misc.faviconloader_v2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Patterns;

import com.stickypassword.android.StickyPasswordApp;
import com.stickypassword.android.StickyPasswordConstants;
import com.stickypassword.android.misc.MiscMethods;
import com.stickypassword.android.model.SPItem;

@SuppressLint("NewApi")
public class FaviconHelper implements StickyPasswordConstants {
	static FaviconHelper helper;
	boolean skip = false;

	public static FaviconHelper getInstance() throws Exception {
		if (helper == null)
			helper = new FaviconHelper();
		return helper;
	}

	Context context;
	

	private FaviconHelper() {
		context = StickyPasswordApp.getAppContext();
		
	}

	public void getFavIconInCache(final String url, final int preferedSize,
			final SPItem item, final int resID) {

		if (skip)
			return;

		try {
			String u = url;
			if (TextUtils.isEmpty(u))
				return;
			if (!u.matches("\\w*://.*"))
				u = "http://" + u;
			if (!Patterns.WEB_URL.matcher(u).matches())
				return;
			FaviconLoader faviconloader = new FaviconLoader(context);
			byte[] data = faviconloader.getIcon(context, url,
					(preferedSize <= 0 ? MiscMethods.iconSize() : preferedSize));
			faviconloader = null;
			if (data != null)
				item.setIconData(data);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	public void getFavIconInSerialPool(final String url,
			final int preferedSize, final SPItem item, final int resID) {

		if (skip)
			return;

		try {
			String u = url;
			if (TextUtils.isEmpty(u))
				return;

			if (!u.matches("\\w*://.*"))
				u = "http://" + u;

			if (!Patterns.WEB_URL.matcher(u).matches())
				return;
			FaviconLoader faviconloader = new FaviconLoader(context);
			byte[] data = faviconloader.getIcon(context, url,
					(preferedSize <= 0 ? MiscMethods.iconSize() : preferedSize));
			if (data != null) {
				item.setIconData(data);
				return;

			}
			faviconloader = null;
			
			boolean useNetworkDownloading = MiscMethods.isConnection(context);
			SharedPreferences settings = context.getSharedPreferences(
					PREFERENCES_NAME, Context.MODE_PRIVATE);
			final boolean wiFiOnly = settings.getBoolean(
					WIFI_ONLY_PREFERENCES_TAG, WIFI_ONLY_DEFAULT_VALUE);
			if (wiFiOnly && !MiscMethods.isWiFi(context)) {
				useNetworkDownloading = false;
			}

			if (!useNetworkDownloading)
				return;

			AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					try {
						FaviconLoader faviconloader = new FaviconLoader(context);
						byte[] data = faviconloader.getIcon(context, url,
								(preferedSize <= 0 ? MiscMethods.iconSize()
										: preferedSize));

						if (data != null) {
							item.setIconData(data);
							return null;
						}

						faviconloader
								.setCallback(new FaviconLoader.SaveCallback() {
									@Override
									public void saveData(String parentUrl,
											byte[] blob) {
										item.setIconData(blob);

									}
								});

						faviconloader.startNewTask(url,
								(preferedSize <= 0 ? MiscMethods.iconSize()
										: preferedSize));
						faviconloader = null;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					System.gc();
					return null;
				}
			};

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				asyncTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR,
						(Void[]) null);
			else
				asyncTask.execute((Void[]) null);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void getFavIconInThreadPool(final String url,
			final int preferedSize, final SPItem item, final int resID) {

		if (skip)
			return;

		try {
			String u = url;
			if (TextUtils.isEmpty(u))
				return;

			if (!u.matches("\\w*://.*"))
				u = "http://" + u;

			if (!Patterns.WEB_URL.matcher(u).matches())
				return;

			FaviconLoader faviconloader = new FaviconLoader(context);
			byte[] data = faviconloader.getIcon(context, url,
					(preferedSize <= 0 ? MiscMethods.iconSize() : preferedSize));
			if (data != null) {
				item.setIconData(data);
				return;

			}

			boolean useNetworkDownloading = MiscMethods.isConnection(context);
			SharedPreferences settings = context.getSharedPreferences(
					PREFERENCES_NAME, Context.MODE_PRIVATE);
			final boolean wiFiOnly = settings.getBoolean(
					WIFI_ONLY_PREFERENCES_TAG, WIFI_ONLY_DEFAULT_VALUE);
			if (wiFiOnly && !MiscMethods.isWiFi(context)) {
				useNetworkDownloading = false;
			}

			if (!useNetworkDownloading)
				return;

			AsyncTask<Void, Void, Void> asyncTask = new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					try {
						FaviconLoader faviconloader = new FaviconLoader(context);
						byte[] data = faviconloader.getIcon(context, url,
								(preferedSize <= 0 ? MiscMethods.iconSize()
										: preferedSize));

						if (data != null) {
							item.setIconData(data);
							return null;
						}
						faviconloader
								.setCallback(new FaviconLoader.SaveCallback() {
									@Override
									public void saveData(String parentUrl,
											byte[] blob) {
										item.setIconData(blob);
									}
								});

						faviconloader.startNewTask(url,
								(preferedSize <= 0 ? MiscMethods.iconSize()
										: preferedSize));
						faviconloader = null;
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					System.gc();
					return null;
				}
			};

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				asyncTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
						(Void[]) null);
			else
				asyncTask.execute((Void[]) null);

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
