// https://searchcode.com/api/result/12734516/

// Copyright (c) 2010-2011, Younghong "Hong" Cho <hongcho@sori.org>.
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
//   1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//   2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//   3. Neither the name of the organization nor the names of its contributors
// may be used to endorse or promote products derived from this software
// without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
// THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
package org.sori.kidsbbs.service;

import java.util.ArrayList;

import org.sori.kidsbbs.R;
import org.sori.kidsbbs.KidsBbs.NotificationType;
import org.sori.kidsbbs.KidsBbs.PackageBase;
import org.sori.kidsbbs.KidsBbs.ParamName;
import org.sori.kidsbbs.KidsBbs.Settings;
import org.sori.kidsbbs.data.ArticleInfo;
import org.sori.kidsbbs.data.BoardInfo;
import org.sori.kidsbbs.io.HttpXml;
import org.sori.kidsbbs.provider.ArticleDatabase;
import org.sori.kidsbbs.provider.ArticleDatabase.ArticleColumn;
import org.sori.kidsbbs.provider.ArticleDatabase.BoardColumn;
import org.sori.kidsbbs.provider.ArticleDatabase.BoardState;
import org.sori.kidsbbs.provider.ArticleProvider.ContentUri;
import org.sori.kidsbbs.provider.ArticleProvider.ContentUriString;
import org.sori.kidsbbs.provider.ArticleProvider.OrderBy;
import org.sori.kidsbbs.provider.ArticleProvider.Selection;
import org.sori.kidsbbs.ui.BoardListActivity;
import org.sori.kidsbbs.ui.preference.MainSettings;
import org.sori.kidsbbs.ui.preference.MainSettings.PrefKey;
import org.sori.kidsbbs.util.BroadcastUtils;
import org.sori.kidsbbs.util.DBUtils;
import org.sori.kidsbbs.util.DateUtils;
import org.sori.kidsbbs.util.BroadcastUtils.BroadcastType;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

public class UpdateService extends Service
		implements OnSharedPreferenceChangeListener {
	private static final String TAG = "UpdateService";

	private int mUpdateFreq;
	private UpdateTask mLastUpdate = null;

	ConnectivityManager mConnectivities;
	AlarmManager mAlarms;
	PendingIntent mAlarmIntent;

	private Integer mIsPausedSync = 0;
	private boolean mIsPaused = false;
	private boolean mBgDataEnabled = true;
	private boolean mNoConnectivity = false;

	private ContentResolver mResolver;
	private NotificationManager mNotificationManager;
	private Notification mNewArticlesNotification;
	private boolean mNotificationOn = true;
	private int mNotificationDefaults = Notification.DEFAULT_LIGHTS
			| Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE;
	private String mNotificationTitleString;
	private String mNotificationMessage;

	// Update to onStartCommand when min SDK becomes >= 5...
	@Override
	public void onStart(Intent _intent, int _startId) {
		setupAlarm(mUpdateFreq,
				_intent.getStringExtra(
						PackageBase.PARAM + ParamName.TABNAME));
	}

	public void onSharedPreferenceChanged(SharedPreferences _prefs, String _key) {
		if (_key.equals(MainSettings.PrefKey.UPDATE_FREQ)) {
			final int updateFreqNew = Integer.parseInt(_prefs.getString(_key,
					MainSettings.getDefaultUpdateFreq(this)));
			if (updateFreqNew != mUpdateFreq) {
				mUpdateFreq = updateFreqNew;
				setupAlarm(mUpdateFreq, null);
			}
		} else if (_key.equals(PrefKey.NOTIFICATION)) {
			mNotificationOn = _prefs.getBoolean(_key, true);
		} else if (_key.equals(PrefKey.NOTIFICATION_LIGHTS)) {
			if (_prefs.getBoolean(_key, true)) {
				mNotificationDefaults |= Notification.DEFAULT_LIGHTS;
			} else {
				mNotificationDefaults &= ~Notification.DEFAULT_LIGHTS;
			}
		} else if (_key.equals(PrefKey.NOTIFICATION_SOUND)) {
			if (_prefs.getBoolean(_key, true)) {
				mNotificationDefaults |= Notification.DEFAULT_SOUND;
			} else {
				mNotificationDefaults &= ~Notification.DEFAULT_SOUND;
			}
		} else if (_key.equals(PrefKey.NOTIFICATION_VIBRATE)) {
			if (_prefs.getBoolean(_key, true)) {
				mNotificationDefaults |= Notification.DEFAULT_VIBRATE;
			} else {
				mNotificationDefaults &= ~Notification.DEFAULT_VIBRATE;
			}
		}
	}

	private void setupAlarm(final long _period, final String _tabname) {
		if (_period > 0) {
			final long msPeriod = _period * 60 * 1000;
			mAlarms.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime() + msPeriod, msPeriod,
					mAlarmIntent);
		} else {
			mAlarms.cancel(mAlarmIntent);
		}
		refreshArticles(_tabname);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		final Resources resources = getResources();
		mNotificationTitleString =
			resources.getString(R.string.notification_title_text);
		mNotificationMessage =
			resources.getString(R.string.notification_message);

		mResolver = getContentResolver();

		mConnectivities = (ConnectivityManager) getSystemService(
				Context.CONNECTIVITY_SERVICE);

		mNotificationManager = (NotificationManager) getSystemService(
				Context.NOTIFICATION_SERVICE);
		mNewArticlesNotification = new Notification(R.drawable.icon,
				mNotificationTitleString, System.currentTimeMillis());
		mNewArticlesNotification.flags |= Notification.FLAG_AUTO_CANCEL;
		mNewArticlesNotification.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
		mNotificationDefaults |= mNewArticlesNotification.defaults;

		mAlarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		mAlarmIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				AlarmReceiver.UPDATE_BOARDS_ALARM), 0);

		final SharedPreferences prefs =
			PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		mUpdateFreq = Integer.parseInt(prefs.getString(PrefKey.UPDATE_FREQ,
				MainSettings.getDefaultUpdateFreq(this)));
		mNotificationOn = prefs.getBoolean(PrefKey.NOTIFICATION, true);
		mNotificationDefaults = 0;
		if (prefs.getBoolean(PrefKey.NOTIFICATION_LIGHTS, true)) {
			mNotificationDefaults |= Notification.DEFAULT_LIGHTS;
		}
		if (prefs.getBoolean(PrefKey.NOTIFICATION_SOUND, true)) {
			mNotificationDefaults |= Notification.DEFAULT_SOUND;
		}
		if (prefs.getBoolean(PrefKey.NOTIFICATION_VIBRATE, true)) {
			mNotificationDefaults |= Notification.DEFAULT_VIBRATE;
		}
		prefs.registerOnSharedPreferenceChangeListener(this);

		registerReceivers();
	}

	@Override
	public void onDestroy() {
		unregisterReceivers();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent _intent) {
		return null;
	}

	private class UpdateTask extends AsyncTask<String, Void, Integer> {
		@Override
		protected Integer doInBackground(String... _args) {
			return refreshTables(_args[0]);
		}

		@Override
		protected void onPostExecute(Integer _result) {
			stopSelf();
		}

		private int getTableState(final String _tabname) {
			final String[] PROJECTIONS = {
					BoardColumn.STATE,
			};
			int result = BoardState.PAUSED;
			final Cursor c = mResolver.query(ContentUri.BOARDS, PROJECTIONS,
					Selection.TABNAME, new String[] { _tabname }, null);
			if (c != null) {
				if (c.getCount() > 0) {
					c.moveToFirst();
					result = c.getInt(
							c.getColumnIndex(BoardColumn.STATE));
				}
				c.close();
			}
			return result;
		}

		private boolean setTableState(final String _tabname, final int _state) {
			final ContentValues values = new ContentValues();
			values.put(BoardColumn.STATE, _state);
			final int count = mResolver.update(ContentUri.BOARDS, values,
					Selection.TABNAME, new String[] { _tabname });
			return count > 0;
		}

		private synchronized int refreshTable(final String _tabname,
				final boolean _postNotification) {
			final String[] PROJECTION = {
					ArticleColumn.SEQ,
					ArticleColumn.USER,
					ArticleColumn.DATE,
					ArticleColumn.TITLE,
					ArticleColumn.READ,
			};

			final int tabState = getTableState(_tabname);
			if (tabState == BoardState.PAUSED) {
				return 0;
			}

			int error = 0;
			int count = 0;
			final String[] parsed = BoardInfo.parseTabname(_tabname);
			final String board = parsed[1];
			final int type = Integer.parseInt(parsed[0]);
			final Uri uri = Uri.parse(ContentUriString.LIST + _tabname);

			// Where to begin
			final int latest = HttpXml.getArticlesLastSeq(board, type);
			final int start_first = latest - Settings.MAX_FIRST_ARTICLES;
			final int start_max = latest - Settings.MAX_ARTICLES;
			int start = DBUtils.getBoardLastSeq(mResolver, _tabname) - 10;
			if (start <= 0) {
				start = start_first;
			} else if (start < start_max) {
				start = start_max;
			}
			if (start < 0) {
				start = 0;
			}
			Log.d(TAG, _tabname + ": (" + tabState + ") updating from "
					+ start);

			boolean fDone = false;
			while (!fDone) {
				ArrayList<ArticleInfo> articles;
				try {
					articles = HttpXml.getArticles(board, type, start);
				} catch (Exception e) {
					Log.e(TAG, _tabname + ": article retrieval failed", e);
					fDone = true;
					++error;
					break;
				}
				if (articles.isEmpty()) {
					Log.d(TAG, _tabname + ": no more articles");
					fDone = true;
					break;
				}
				int size = articles.size();
				for (int i = 0; !fDone && i < size; ++i) {
					final ArticleInfo info = articles.get(i);
					if (!DateUtils.isRecent(info.getDateString())) {
						continue;
					}
					final String[] args = new String[] { Integer.toString(
							info.getSeq()) };
					ArticleInfo old = null;
					final Cursor c = mResolver.query(uri, PROJECTION,
							Selection.SEQ, args, null);
					if (c != null) {
						if (c.getCount() > 0) {
							c.moveToFirst();
							// Cache the old entry.
							final int seq = c.getInt(c.getColumnIndex(
									ArticleColumn.SEQ));
							final String user = c.getString(c.getColumnIndex(
									ArticleColumn.USER));
							final String date = c.getString(c.getColumnIndex(
									ArticleColumn.DATE));
							final String title = c.getString(c.getColumnIndex(
									ArticleColumn.TITLE));
							final boolean read = c.getInt(c.getColumnIndex(
									ArticleColumn.READ)) != 0;
							old = new ArticleInfo(_tabname, seq, user, null,
									date, title, null, null, 1, read);
						}
						c.close();
					} else {
						// Unexpected...
						Log.e(TAG, _tabname + ": query failed: " + info.getSeq());
						fDone = true;
						++error;
						break;
					}

					final ContentValues values = new ContentValues();
					values.put(ArticleColumn.SEQ, info.getSeq());
					values.put(ArticleColumn.USER, info.getUser());
					values.put(ArticleColumn.AUTHOR, info.getAuthor());
					values.put(ArticleColumn.DATE, info.getDateString());
					values.put(ArticleColumn.TITLE, info.getTitle());
					values.put(ArticleColumn.THREAD, info.getThread());
					values.put(ArticleColumn.BODY, info.getBody());
					boolean read = info.getRead();
					if (old != null && old.getRead()) {
						read = true;
					}
					values.put(ArticleColumn.READ, read ? 1 : 0);

					boolean result = true;
					if (old == null) {
						// Not there...
						try {
							mResolver.insert(uri, values);
						} catch (SQLException e) {
							result = false;
						}
					} else {
						// Hmm... already there...
						if (info.getUser().equals(old.getUser())
								&& info.getDateString().equals(
										old.getDateString())
								&& info.getTitle().equals(old.getTitle())) {
							result = false;
						} else {
							try {
								mResolver.update(uri, values, Selection.SEQ,
										args);
							} catch (SQLException e) {
								result = false;
							}
						}
					}
					if (result) {
						++count;
						BroadcastUtils.announceBoardUpdated(UpdateService.this,
								_tabname);
					}
				}
				start = ((ArticleInfo) articles.get(articles.size() - 1))
						.getSeq() + 1;
				Log.d(TAG, _tabname + ": next from " + start);
			}
			final int trimmed = trimBoardTable(_tabname);
			Log.d(TAG, _tabname + ": trimed " + trimmed + " articles");
			if (_postNotification && count > 0) {
				notifyNewArticles(_tabname, count);
			}
			BroadcastUtils.announceBoardUpdated(UpdateService.this, _tabname);
			if (error > 0) {
				Log.e(TAG, _tabname + ": error after updating " + count
						+ " articles");
				BroadcastUtils.announceUpdateError(UpdateService.this);
			}
			setTableState(_tabname, ArticleDatabase.BoardState.SELECTED);
			return count;
		}

		private void notifyNewArticles(final String _tabname, final int _count) {
			if (!mNotificationOn) {
				return;
			}

			// Prepare pending intent for notification
			final String title = DBUtils.getBoardTitle(mResolver, _tabname);
			final PendingIntent pendingIntent = PendingIntent.getActivity(
					UpdateService.this, 0, new Intent(UpdateService.this,
							BoardListActivity.class), 0);

			// Notify new articles.
			mNewArticlesNotification.tickerText = title + " (" + _count + ")";
			mNewArticlesNotification.when = System.currentTimeMillis();
			mNewArticlesNotification.defaults = mNotificationDefaults;
			if ((mNotificationDefaults & Notification.DEFAULT_LIGHTS) != 0) {
				mNewArticlesNotification.flags |= Notification.FLAG_SHOW_LIGHTS;
			} else {
				mNewArticlesNotification.flags &= ~Notification.FLAG_SHOW_LIGHTS;
			}
			//mNewArticlesNotification.number =
			//	DBUtils.getTotalUnreadCount(mResolver);
			mNewArticlesNotification.setLatestEventInfo(UpdateService.this,
					mNotificationTitleString, mNotificationMessage,
					pendingIntent);

			mNotificationManager.notify(NotificationType.NEW_ARTICLE,
					mNewArticlesNotification);
		}

		private int refreshTables(final String _tabname) {
			final String[] PROJECTION = {
					BoardColumn.TABNAME
			};
			final String ORDERBY = OrderBy.STATE_ASC + "," + OrderBy._ID;

			boolean postNotification = true;
			final ArrayList<String> tabnames = new ArrayList<String>();
			if (TextUtils.isEmpty(_tabname)) {
				// Get all the boards...
				final Cursor c = mResolver.query(ContentUri.BOARDS, PROJECTION,
						Selection.STATE_ACTIVE, null, ORDERBY);
				if (c != null) {
					if (c.getCount() > 0) {
						c.moveToFirst();
						do {
							tabnames.add(c.getString(c.getColumnIndex(
									BoardColumn.TABNAME)));
						} while (c.moveToNext());
					}
					c.close();
				}
			} else {
				postNotification = false;
				tabnames.add(_tabname);
			}

			// Update each board in the list.
			int total_count = 0;
			int i = 0;
			int nTries = 0;
			while (i < tabnames.size()) {
				synchronized (mIsPausedSync) {
					while (mIsPaused) {
						try {
							mIsPausedSync.wait();
						} catch (Exception e) {
						}
					}
				}
				final String tabname = tabnames.get(i);
				try {
					final int count = refreshTable(tabname, postNotification);
					Log.d(TAG, tabname + ": updated " + count + " articles");
					total_count += count;
					++i;
					nTries = 0;
				} catch (Exception e) {
					Log.d(TAG, tabname + ": exception while updating (#"
							+ nTries + ")");
					if (++nTries > 2) {
						break;
					}
				}
			}
			return total_count;
		}
	}

	private void refreshArticles(final String _tabname) {
		if (mLastUpdate == null
				|| mLastUpdate.getStatus().equals(AsyncTask.Status.FINISHED)) {
			mLastUpdate = new UpdateTask();
			mLastUpdate.execute(_tabname);
		}
	}

	private int deleteArticles(final Uri _uri, final Cursor _c, int _max) {
		final int col_index = _c.getColumnIndex(ArticleColumn.SEQ);
		int count = 0;
		_c.moveToFirst();
		do {
			final int seq = _c.getInt(col_index);
			if (seq > 0) {
				count += mResolver.delete(_uri, Selection.SEQ,
						new String[] { Integer.toString(seq) });
			}
		} while (--_max > 0 && _c.moveToNext());
		return count;
	}

	private int trimBoardTable(final String _tabname) {
		final String[] PROJECTION = {
				ArticleColumn.SEQ,
		};
		final String WHERE = "DATE(" + ArticleColumn.DATE
				+ ")!='' AND JULIANDAY(" + ArticleColumn.DATE
				+ ")<=JULIANDAY('now'," + Settings.KST_DIFF + ","
				+ Settings.MAX_TIME + ")";

		// At least 15...
		int size = DBUtils.getBoardTableSize(mResolver, _tabname);
		if (size <= Settings.MIN_ARTICLES) {
			return 0;
		}

		final Uri uri = Uri.parse(ContentUriString.LIST + _tabname);

		// Find the trim point.
		int seq = 0;
		Cursor c = mResolver.query(uri, PROJECTION, WHERE, null,
				OrderBy.SEQ_DESC);
		if (c != null) {
			if (c.getCount() > 0) {
				c.moveToFirst();
				seq = c.getInt(0);
			}
			c.close();
		}

		// Now delete old stuff...
		int count = 0;
		if (seq > 0) {
			c = mResolver.query(uri, PROJECTION,
					ArticleColumn.SEQ + "<=" + seq, null, OrderBy.SEQ_ASC);
			if (c != null) {
				count += deleteArticles(uri, c, size - Settings.MIN_ARTICLES);
				c.close();
			}
		}

		// Reduce the size to a manageable size.
		size = DBUtils.getBoardTableSize(mResolver, _tabname);
		if (size > Settings.MAX_ARTICLES) {
			c = mResolver.query(uri, PROJECTION, null, null, OrderBy.SEQ_ASC);
			if (c != null) {
				count += deleteArticles(uri, c, size - Settings.MAX_ARTICLES);
				c.close();
			}
		}

		return count;
	}

	private class ArticleUpdatedReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context _context, Intent _intent) {
			final String tabname = _intent.getStringExtra(
					PackageBase.PARAM + BoardColumn.TABNAME);
			DBUtils.updateBoardCount(mResolver, tabname);
		}
	}

	private ArticleUpdatedReceiver mUpdateReceiver;

	private class ConnectivityReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context _context, Intent _intent) {
			final String action = _intent.getAction();
			if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				mNoConnectivity = _intent.getBooleanExtra(
						ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
			} else if (action.equals(
					ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED)) {
				mBgDataEnabled = mConnectivities.getBackgroundDataSetting();
			} else {
				return;
			}
			final boolean isPaused = mNoConnectivity || !mBgDataEnabled;
			synchronized (mIsPausedSync) {
				if (isPaused == mIsPaused) {
					return;
				}
				mIsPaused = isPaused;
				Log.d(TAG, mIsPaused ? "Update DISABLED" : "Update ENABLED");
				if (!mIsPaused) {
					mIsPausedSync.notify();
				}
			}
		}
	}

	private ConnectivityReceiver mConnReceiver;

	private void registerReceivers() {
		IntentFilter filter;
		mConnReceiver = new ConnectivityReceiver();
		filter = new IntentFilter(
				ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED);
		filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(mConnReceiver, filter);
		mUpdateReceiver = new ArticleUpdatedReceiver();
		filter = new IntentFilter(BroadcastType.ARTICLE_UPDATED);
		registerReceiver(mUpdateReceiver, filter);
	}

	private void unregisterReceivers() {
		unregisterReceiver(mConnReceiver);
		unregisterReceiver(mUpdateReceiver);
	}
}

