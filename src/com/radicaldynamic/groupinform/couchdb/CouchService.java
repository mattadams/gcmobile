package com.radicaldynamic.groupinform.couchdb;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONException;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

import com.couchone.libcouch.AeSimpleSHA1;
import com.couchone.libcouch.AndCouch;
import com.couchone.libcouch.Base64Coder;
import com.radicaldynamic.groupinform.utilities.FileUtilsExtended;

public class CouchService extends Service 
{
	private CouchProcess couch = new CouchProcess();
	
	// A list of couchClients that awaiting notifications of couch starting
	private Map<String, InformCouchClient> couchClients = new HashMap<String, InformCouchClient>();

	// Contains a mapping of database names to their listeners
	public class dbListeners {
		private Map<String, CouchCtrlListener> databases = new HashMap<String, CouchCtrlListener>();
	}

	// Contains a mapping of package names to database listeners
	private Map<String, dbListeners> listeners = new HashMap<String, dbListeners>();

	@SuppressWarnings("unused")
    private NotificationManager mNM;

	/*
	 * This is called to start the service
	 */
	@Override
	public void onCreate() {
//		notifyStarting();
		couch.service = this;
		couch.start("/system/bin/sh", FileUtilsExtended.EXTERNAL_COUCH + "/bin/couchdb", "");
	}

	@Override
	public void onDestroy() {
		if (couch.started) { 
			couch.stop();
		}
		couchClients.clear();
//		mNM.cancelAll();
	}

	/*
	 * This is called called on the initial binding
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return new CouchServiceImpl();
	}


	/*
	 * implements the callbacks that clients can call into the couchdb service
	 */
	public class CouchServiceImpl extends InformCouchService.Stub {

		@Override
		public void initCouchDB(InformCouchClient cb) throws RemoteException 
		{
			String packageName = packageNameFromUid(Binder.getCallingUid());
			couchClients.put(packageName, cb);

			// Notify the client straight away that couch has started
			if (couch.started == true) {
				couchStarted();
			}
		}

		@Override
		public void initDatabase(InformCouchClient callback, String tag, String pass, boolean cmdDb) throws RemoteException 
		{			
			String packageName = packageNameFromUid(Binder.getCallingUid());
			String userName = packageName.replace(".", "_");
			String dbName = tag + "-" + userName;

			createIfNotExists(dbName, userName, pass);

			// clients can request a command database that proxies replication 
			// requests as they dont have admin permissions to post directly
			// Command databases are currently unused
			/*
			if (cmdDb) {

				createIfNotExists(dbName + "-ctrl", userName, pass);

				dbListeners dbs = listeners.containsKey(packageName) 
					? listeners.get(packageName) 
					: new dbListeners();

				final CouchCtrlListener listener = 
					getOrCreateListener(dbs, packageName, dbName);

				new Thread(new Runnable() {
					public void run() {
						listener.start();
					}
				}).start();
			}
			*/

			// Notify the client that their database is ready
			callback.databaseCreated(dbName, userName, pass, tag);
		}

		/*
		 * When a client quits we just cancel any control database listeners
		 * dont actually stop couch, that is done when the last client unbinds
		 */
		@Override
		public void quitCouchDB() throws RemoteException 
		{
			cancelListeners();
		}
	};

	/*
	 * Applications that restart may have existing listener objects
	 */
	@SuppressWarnings("unused")
    private CouchCtrlListener getOrCreateListener(dbListeners tmp, String packageName, String dbName) {
		if (tmp.databases.containsKey(dbName)) {
			return tmp.databases.get(dbName);
		} else {
			CouchCtrlListener temp = new CouchCtrlListener(couch.url(), dbName,
					CouchProcess.adminUser, CouchProcess.adminPass);
			tmp.databases.put(dbName, temp);
			listeners.put(packageName, tmp);
			return temp;
		}
	}

	/*
	 * Create a new CouchDB user
	 */
	private void createUser(String user, String pass) {
		try {
			String salt = CouchProcess.generatePassword(10);
			String hashed = AeSimpleSHA1.SHA1(pass + salt);
			String json = "{\"_id\":\"org.couchdb.user:" + user + "\","
					+ "\"type\":\"user\"," + "\"name\":\"" + user + "\","
					+ "\"roles\":[]," + "\"password_sha\":\"" + hashed + "\", "
					+ "\"salt\":\"" + salt + "\"}";
			AndCouch.post(couch.url() + "_users", json, adminHeaders());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
	};

	/*
	 * If the requests database does not exist, create it and 
	 * set the given user as an admin for it
	 */
	private void createIfNotExists(String dbName, String user, String pass) {
		try {
			String url = couch.url() + dbName;
			AndCouch res = AndCouch.get(couch.url() + dbName, adminHeaders());
			if (res.status == 404) {
				createUser(user, pass);
				AndCouch.put(url, null, adminHeaders());
				String sec = "{\"admins\":{\"names\":[\"" + user
						+ "\"],\"roles\":[]},\"readers\":{\"names\":[],\"roles\":[]}}";
				AndCouch.put(url + "/_security", sec, adminHeaders());
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	};

	/*
	 * once couch has started we need to notify all waiting clients
	 */
	void couchStarted() throws RemoteException {
		
//		notifyStarted();
		
		for (Entry<String, InformCouchClient> entry : couchClients.entrySet()) {
			InformCouchClient client = entry.getValue();
			client.couchStarted(couch.host, couch.port);
			couchClients.remove(entry.getKey());
		}
	}

	/*
	 * Control databases have a long polling listener, stop them when an application
	 * quits
	 */
	private void cancelListeners() { 
		String packageName = packageNameFromUid(Binder.getCallingUid());
		if (listeners.containsKey(packageName)) {
			dbListeners tmp = listeners.get(packageName);
			for (Map.Entry<String, CouchCtrlListener> temp : tmp.databases.entrySet()) {
				temp.getValue().cancel();
			}
		}
	}

	private String packageNameFromUid(int uid) {
		PackageManager pm = getPackageManager();
		String[] packages = pm.getPackagesForUid(Binder.getCallingUid());
		return packages[0];
	}

//	private void notifyStarting() {
//		mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//		int icon = R.drawable.ic_stat_group_complete;
//		CharSequence tickerText = "Inform Database Starting";
//		long when = System.currentTimeMillis();
//		Intent notificationIntent = new Intent(this, CouchFutonActivity.class);
//		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//		notification.setLatestEventInfo(
//		        getApplicationContext(),
//				"Group Inform Database Starting", "Please Wait...", 
//				contentIntent);
//		mNM.notify(1, notification);		
//	}
	
//	private void notifyStarted() { 
//		int icon = R.drawable.ic_stat_group_complete;
//		CharSequence tickerText = "Inform Database Running";
//		long when = System.currentTimeMillis();
//
//		Notification notification = new Notification(icon, tickerText, when);
//		notification.flags = Notification.FLAG_ONGOING_EVENT;
//		Intent i = new Intent(CouchService.this, LauncherActivity.class);
//		notification.setLatestEventInfo(
//				getApplicationContext(),
//				"Inform Database Running",
//				"", 
//				PendingIntent.getActivity(CouchService.this, 0, i, 0));
//		mNM.cancel(1);
//		mNM.notify(2, notification);
//		startForeground(2, notification);
//	}
	
	private String[][] adminHeaders() {
		String auth = Base64Coder.encodeString(CouchProcess.adminUser + ":" + CouchProcess.adminPass);
		String[][] headers = { { "Authorization", "Basic " + auth } };
		return headers;
	}
}
