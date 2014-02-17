/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cliff.comichelper;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.cliff.comic.Comic;
import com.cliff.comic.Volume;
import com.cliff.comichelper.service.ComicService;
import com.cliff.comichelper.service.Command;
import com.cliff.comichelper.service.Constants;
import com.cliff.comichelper.service.Notification;

import de.greenrobot.event.EventBus;

public class MainActivity extends Activity {
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private ProgressDialog progressDialog = null;

	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
//	private int[] mBannerIds = new int[] { R.drawable.manhua8_logo,
//			R.drawable.manhua8_logo };
	private static String[] mWebsites = new String[] { "http://www.manhua8.net",
			"http://www.manhua8.com" };
	private String[] mWebsiteNames = new String[] { "漫画吧",
			"漫画吧" };
	private WebViewFragment currentFragment = null;
	private static SharedPreferences preferences = null;
	private final String TAG = "MainActivity";

	// private boolean isWaiting = false;

	protected EventBus eventBus = EventBus.getDefault();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		SettingsActivity.initSettings(preferences);

		mTitle = mDrawerTitle = getTitle();
		setTitle(mTitle.toString()+" "+getAppVersionName(this));
		mDrawerTitle = mTitle;
//		mTitle += getAppVersionName(this).toString();
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		// set a custom shadow that overlays the main content when the drawer
		// opens
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
				GravityCompat.START);
		// set up the drawer's list view with items and click listener
		mDrawerList.setAdapter(new ArrayAdapter<String>(this,
				R.layout.drawer_list_item, mWebsiteNames) {

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				if (null == convertView) {
					LayoutInflater vi = (LayoutInflater) getContext()
							.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					convertView = vi.inflate(R.layout.drawer_list_item, null);
				}
//				ImageView imageBanner = ((ImageView) convertView
//						.findViewById(R.id.imageBanner));
//				imageBanner.setImageResource(mBannerIds[position]);
				TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
				TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
				text1.setText(mWebsiteNames[position]);
				text2.setText(mWebsites[position]);
				return convertView;
			}

		});
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// enable ActionBar app icon to behave as action to toggle nav drawer
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);

		// ActionBarDrawerToggle ties together the the proper interactions
		// between the sliding drawer and the action bar app icon
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description for accessibility */
		R.string.drawer_close /* "close drawer" description for accessibility */
		) {
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}

			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to
											// onPrepareOptionsMenu()
			}
		};
		mDrawerLayout.setDrawerListener(mDrawerToggle);

		if (savedInstanceState == null) {
			selectItem(0, true);
		}

		eventBus.register(this);
		startService(new Intent(this, ComicService.class));
		mDrawerLayout.openDrawer(mDrawerList);
	}

	@Override
	protected void onDestroy() {
		eventBus.unregister(this);
		stopService(new Intent(this, ComicService.class));
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content
		// view
		// boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
		// menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item)) {
			return true;
		}
		// Handle action buttons
		switch (item.getItemId()) {
		case R.id.action_download:
			// item.setEnabled(false);
			if (/* isWaiting */null != progressDialog) {
				// Toast.makeText(this, "waiting background response",
				// Toast.LENGTH_SHORT).show();
				return true;
			}
			// isWaiting = true;
			startProgressDialog(R.string.title_parse_comic, R.string.message_wait, true);
			Command command = new Command(Constants.COMMAND_PARSE);
			command.addParam(Constants.PARAM_URL,
					currentFragment.getCurrentUrl());
			eventBus.post(command);
			// if (null != currentFragment) {
			// new ParseTask().execute(currentFragment.getCurrentUrl());
			// }
			// Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
			// intent.putExtra(SearchManager.QUERY, getActionBar().getTitle());
			// if (intent.resolveActivity(getPackageManager()) != null) {
			// startActivity(intent);
			// } else {
			// Toast.makeText(this, R.string.app_not_available,
			// Toast.LENGTH_LONG).show();
			// }
			return true;
		case R.id.action_settings:
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	/* The click listner for ListView in the navigation drawer */
	private class DrawerItemClickListener implements
			ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			selectItem(position, false);
		}
	}

	private void selectItem(int position, boolean blank) {
		// update the main content by replacing fragments
		Fragment fragment = new WebViewFragment();
		Bundle args = new Bundle();
		if (blank)
			args.putString(WebViewFragment.ARG_URL, "about:blank");
		else
			args.putString(WebViewFragment.ARG_URL, "http://www.manhua8.com");
		fragment.setArguments(args);

		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction()
				.replace(R.id.content_frame, fragment).commit();

		// update selected item and title, then close the drawer
		mDrawerList.setItemChecked(position, true);
//		setTitle(mWebsiteNames[position]);
		mDrawerLayout.closeDrawer(mDrawerList);

		currentFragment = (WebViewFragment) fragment;
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}

	/**
	 * When using the ActionBarDrawerToggle, you must call it during
	 * onPostCreate() and onConfigurationChanged()...
	 */

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggls
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	/**
	 * Fragment that appears in the "content_frame", shows a webview
	 */
	public static class WebViewFragment extends Fragment {
		public static final String ARG_URL = "url";
		protected boolean userSelection = false;

		public WebViewFragment() {
			// Empty constructor required for fragment subclasses
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_webview,
					container, false);
			String url = getArguments().getString(ARG_URL);
			Spinner spinner = (Spinner)rootView.findViewById(R.id.spinnerAddress);
			ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
			        android.R.layout.simple_spinner_dropdown_item,
			            new ArrayList<String>());
			spinner.setAdapter(spinnerArrayAdapter);
			spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parentView, View view,
						int position, long id) {
					if (userSelection) {
						WebViewFragment.this.getWebView().loadUrl(((CheckedTextView)view).getText().toString());
					}
					else {
						userSelection = true;
					}
				}

				@Override
				public void onNothingSelected(AdapterView<?> parentView) {
				}
			});

			WebView webView = ((WebView) rootView.findViewById(R.id.webview));
			// WebViewClient client = new WebViewClient();
			// client.shouldOverrideUrlLoading(webView, url)
			webView.setWebViewClient(new WebViewClient() {

				@Override
				public void onPageFinished(WebView view, String url) {
					super.onPageFinished(view, url);
				}

				@Override
				public void onPageStarted(WebView view, String url,
						Bitmap favicon) {
					if (null == url)
						return;
					
					view.getSettings().setJavaScriptEnabled(preferences.getBoolean(SettingsActivity.KEY_ENABLE_JAVASCRIPT, false));
					Spinner spinner = (Spinner)WebViewFragment.this.getView().findViewById(R.id.spinnerAddress);
					ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
					boolean urlExists = false;
					int iFlag = 0;
					for (; iFlag < adapter.getCount(); iFlag++) {
						if (url.equals(adapter.getItem(iFlag))) {
							urlExists = true;
							break;
						}
					}
					if (!urlExists) {
						adapter.add(url);
						adapter.notifyDataSetChanged();
//						iFlag = adapter.getCount() -1;
					}
					userSelection = false;
					spinner.setSelection(iFlag);
					super.onPageStarted(view, url, favicon);
				}
			});
			WebSettings webSettings = webView.getSettings();
			webSettings.setBuiltInZoomControls(true);
			webView.loadUrl(url);
//			getActivity().setTitle("WebView");
			return rootView;
		}

		public String getCurrentUrl() {
			WebView webView = ((WebView) getView().findViewById(R.id.webview));
			return webView.getUrl();
		}

		public WebView getWebView() {
			WebView webView = ((WebView) getView().findViewById(R.id.webview));
			return webView;
		}
	}

	public void onEventMainThread(Notification event) {
		Log.d(TAG, "I got a notification");

		// isWaiting = false;

		if (Constants.NOTIFICATION_PARSE.equals(event.notificationName)) {
			stopProgressDialog();
			Comic comic = (Comic) event.getParam(Constants.PARAM_COMIC);
			if (null != comic) {
				// Toast.makeText(this, "Comic name is " + comic.comicName,
				// Toast.LENGTH_SHORT).show();
				showSelectVolumesDialog(comic);
			} else {
				Integer lastError = (Integer) event
						.getParam(Constants.PARAM_ERROR);
				if (null == lastError)
					lastError = 0;
				// Toast.makeText(this, "lastError is " + lastError,
				// Toast.LENGTH_SHORT).show();
				String title = getText(R.string.title_result).toString();
				String message = String.format(getText(R.string.message_parse_failed).toString(), lastError);
				showAlertDialog(title, message);
			}
		} 
		else if (Constants.NOTIFICATION_DOWNLOAD.equals(event.notificationName)) {
			if (null != progressDialog) {
				Integer progress = (Integer) event.getParam(Constants.PARAM_PROGRESS);
				Integer max = (Integer) event.getParam(Constants.PARAM_MAX);
				if (progress.intValue() == max.intValue()) {
					stopProgressDialog();
					String title = getText(R.string.title_done).toString();
					String message = String.format(getText(R.string.message_added_queue).toString(), progress);
					showAlertDialog(title, message);					
				}
				else {
					progressDialog.setMax(max);
					progressDialog.setProgress(progress);
					String message = String.format(getText(R.string.message_adding_queue).toString(), progress, max);
					progressDialog.setMessage(message);
				}
			}
		}
		else if (Constants.NOTIFICATION_VOLUME.equals(event.notificationName)) {
			if (null != progressDialog) {
				Volume volume = (Volume) event.getParam(Constants.PARAM_VOLUME);
				Integer error = (Integer)event.getParam(Constants.PARAM_ERROR);
				if (null != error) {
					stopProgressDialog();
					if (null != volume) {
						String title = getText(R.string.title_error).toString();
						String message = String.format(getText(R.string.message_error_volume).toString(), volume.volumeName, error);
						showAlertDialog(title, message);
					}
					else {
						String title = getText(R.string.title_error).toString();
						String message = getText(R.string.message_error_parser).toString();
						showAlertDialog(title, message);
					}
				}
				else {
					Integer progress = (Integer) event.getParam(Constants.PARAM_PROGRESS);
					Integer max = (Integer) event.getParam(Constants.PARAM_MAX);
					progressDialog.setMax(max);
					progressDialog.setProgress(progress);
					String message = String.format(getText(R.string.message_parsing_volume).toString(), volume.volumeName, progress, max);
					progressDialog.setMessage(message);
				}
			}
		}
	}

	protected void showSelectVolumesDialog(final Comic comic) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.title_select_volumes);
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < comic.volumes.length; i++)
			list.add(comic.volumes[i].volumeName);

		builder.setMultiChoiceItems(
				list.toArray(new CharSequence[list.size()]), null,
				new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog,
							int indexSelected, boolean isChecked) {
					}
				})
				// Set the action buttons
				.setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						allowCloseDialog(dialog, true);
						final AlertDialog alert = (AlertDialog) dialog;
						final ListView list = alert.getListView();
						ArrayList<Integer> intList = new ArrayList<Integer>();
						for (int i = 0; i < list.getAdapter().getCount(); i++) {
							if (list.isItemChecked(i))
								intList.add(i);
						}
						int[] volumes = new int[intList.size()];
						for (int i = 0; i < volumes.length; i++)
							volumes[i] = intList.get(i);
						Command command = new Command(
								Constants.COMMAND_DOWNLOAD);
						command.addParam(Constants.PARAM_COMIC, comic);
						command.addParam(Constants.PARAM_VOLUMES, volumes);
						command.addParam(Constants.PARAM_ROOTDIR, preferences.getString(SettingsActivity.KEY_DOWNLOAD_LOCATION, SettingsActivity.DIR_DOWNLOAD));
						eventBus.post(command);
						startProgressDialog(R.string.title_download, R.string.message_starting, false);
					}
				})
				.setNegativeButton(R.string.button_cancel,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								allowCloseDialog(dialog, true);
								dialog.cancel();
							}
						})
				.setNeutralButton(R.string.button_selectall,
						new DialogInterface.OnClickListener() {
							private boolean enabled = false;

							@Override
							public void onClick(DialogInterface dialog, int id) {
								allowCloseDialog(dialog, false);

								final AlertDialog alert = (AlertDialog) dialog;
								final ListView list = alert.getListView();
								enabled = !enabled;
								for (int i = 0; i < list.getAdapter()
										.getCount(); i++) {
									list.setItemChecked(i, enabled);
								}
							}
						});

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	protected void allowCloseDialog(DialogInterface dialog, boolean allow) {
		try {
			Field field = dialog.getClass().getSuperclass()
					.getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, allow);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	protected void startProgressDialog(int title, int message, boolean indeterminate) {
		progressDialog = ProgressDialog.show(MainActivity.this,
				getText(title), getText(message), indeterminate, false);
	}

	protected void stopProgressDialog() {
		if (null != progressDialog) {
			progressDialog.dismiss();
			progressDialog = null;
		}
	}

	protected void showAlertDialog(String title, String message) {
		new AlertDialog.Builder(MainActivity.this).setTitle(title)
				.setMessage(message).setPositiveButton(R.string.button_ok, null).show();
	}

	@Override
	public void onBackPressed() {
		WebView webView = currentFragment.getWebView();
		if (webView.canGoBack())
			webView.goBack();
		else
			super.onBackPressed();
	}

	public static String getAppVersionName(Context context) {  
	    String versionName = "";  
	    try {  
	        // ---get the package info---  
	        PackageManager pm = context.getPackageManager();  
	        PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);  
	        versionName = pi.versionName;  
	        if (versionName == null || versionName.length() <= 0) {  
	            return "";  
	        }  
	    } catch (Exception e) {  
	        Log.e("VersionInfo", "Exception", e);  
	    }  
	    return versionName;  
	}  	
}