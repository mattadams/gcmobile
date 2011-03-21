package com.couchone.couchdb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.radicaldynamic.groupinform.utilities.FileUtils;

public class CouchInstaller {
	
//	final static String baseUrl = "http://couchone-android.s3.amazonaws.com/";
//	final static String dataPath = "/data/data/com.couchone.couchdb";
    
    final static String baseUrl = "http://192.168.110.120/~matt/rdinc/projects/groupcomplete/couch/android-build/";
    final static String dataPath = "/data/data/com.radicaldynamic.groupinform";

	final static String TAG = "CouchDB";

	public static void doInstall(Handler handler) throws IOException {		
		// WARNING: This deleted any previously installed couchdb data 
		// and binaries stored on the sdcard to keep in line with usual 
		// android app behaviour. However there doesnt look to be a way to protect
		// ourselves from wiping the entire sdcard with a typo, so just be careful
		File couchDir = new File(FileUtils.EXTERNAL_COUCH);
		
		if (couchDir.exists()) {
			deleteDirectory(couchDir);
			deleteDirectory(new File(FileUtils.EXTERNAL_ERLANG));
		}
		
		for(String pkg : packageSet()) {
			if (!(new File(FileUtils.EXTERNAL_FILES + "/" + pkg + ".installedfiles")).exists()) {
				installPackage(pkg, handler);
			}	
		}

		Message done = Message.obtain();
		done.what = CouchInstallActivity.COMPLETE;
		handler.sendMessage(done);
	}

	/* 
	 * This fetches a given package from amazon and tarbombs it to the filsystem
	 */
	private static void installPackage(String pkg, Handler handler) throws IOException 
	{		
		Log.v(TAG, "Installing " + pkg);
		
		HttpClient pkgHttpClient = new DefaultHttpClient();
		HttpGet tgzrequest = new HttpGet(baseUrl + pkg + ".tgz");
		HttpResponse response = pkgHttpClient.execute(tgzrequest);
		StatusLine status = response.getStatusLine();
		
		ArrayList<String> installedfiles = new ArrayList<String>();
		
		// Used for later initialization of /data/data/...
		ArrayList<String> allInstalledFiles = new ArrayList<String>();		
		Map<String, Integer> allInstalledFileModes = new HashMap<String, Integer>();
		Map<String, String> allInstalledFileTypes = new HashMap<String, String>();
		Map<String, String> allInstalledLinks = new HashMap<String, String>();  
		
		Log.d(TAG, "Request returned status " + status);
		
		if (status.getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			InputStream instream = entity.getContent();
			TarArchiveInputStream tarstream = new TarArchiveInputStream(new GZIPInputStream(instream));			
			TarArchiveEntry e = null;
			
			float filesInArchive = 0;
			float filesUnpacked = 0;			
			
			while ((e = tarstream.getNextTarEntry()) != null) {
			    if (filesInArchive == 0 && e.getName().startsWith("filecount")) {
			        String [] count = e.getName().split("\\.");
			        filesInArchive = Integer.valueOf(count[1]);
			        continue;
			    }
			    
				if (e.isDirectory()) {
					File f = new File(e.getName());
					
					if (!f.exists() && !new File(e.getName()).mkdir()) { 
						throw new IOException("Unable to create directory: " + e.getName());
					}
					
					Log.v(TAG, "MKDIR: " + e.getName());
					
					allInstalledFiles.add(e.getName());
					allInstalledFileModes.put(e.getName(), e.getMode());
					allInstalledFileTypes.put(e.getName(), "d");
				} else if (!"".equals(e.getLinkName())) {
					Log.v(TAG, "LINK: " + e.getName() + " -> " + e.getLinkName());
					Runtime.getRuntime().exec(new String[] { "ln", "-s", e.getName(), e.getLinkName() });
					installedfiles.add(e.getName());
					
					allInstalledFiles.add(e.getName());
					allInstalledLinks.put(e.getName(), e.getLinkName());
                    allInstalledFileModes.put(e.getName(), e.getMode());
                    allInstalledFileTypes.put(e.getName(), "l");
				} else {
					File target = new File(e.getName());
					
					if (target.getParent() != null) {
						new File(target.getParent()).mkdirs();
					}
					
					Log.v(TAG, "Extracting " + e.getName());
					IOUtils.copy(tarstream, new FileOutputStream(target));
					installedfiles.add(e.getName());
					
					allInstalledFiles.add(e.getName());
                    allInstalledFileModes.put(e.getName(), e.getMode());
                    allInstalledFileTypes.put(e.getName(), "f");
				}

                // getMode: 420 (644), 493 (755), 509 (775), 511 (link 775)
				//Log.v(TAG, "File mode is " + e.getMode());
				
				//TODO: Set to actual tar perms.
				Runtime.getRuntime().exec("chmod 755 " + e.getName()); 
				
				// Update progress in UI
				Message progress = new Message();
				progress.arg1 = (int) Math.round(++filesUnpacked / filesInArchive * 100);
				progress.arg2 = 0;
				progress.what = CouchInstallActivity.PROGRESS;
				handler.sendMessage(progress);
			}

			tarstream.close();
			instream.close();
			
			FileWriter iLOWriter = new FileWriter(FileUtils.EXTERNAL_FILES + "/" + pkg + ".installedfiles");
			
			for (String file : installedfiles) {
				iLOWriter.write(file + "\n");
			}
			
			iLOWriter.close();
			
			for (String file : installedfiles) {
				if (file.endsWith(".postinst.sh")) {
					Runtime.getRuntime().exec("sh " + file);
				}
			}
			
    		// Write out full list of all installed files + file modes
    		iLOWriter = new FileWriter(FileUtils.EXTERNAL_FILES + "/installedfiles.index");
    		
    		for (String file : allInstalledFiles) {
    		    iLOWriter.write(
    		            allInstalledFileTypes.get(file).toString() + " " + 
    		            allInstalledFileModes.get(file).toString() + " " + 
    		            file + " " +
    		            allInstalledLinks.get(file) + "\n");
    		}
    		
    		iLOWriter.close();
		} else {
			throw new IOException();
		}
	}

	/*
	 * Verifies that CouchDB is installed by checking the package files we 
	 * write on installation + the data directory on the sd card
	 */
	public static boolean checkInstalled() 
	{				
		for (String pkg : packageSet()) {
			File file = new File(FileUtils.EXTERNAL_FILES + "/" + pkg + ".installedfiles");
			
			if (!file.exists()) {
				return false;
			}
		}
		
		return new File(FileUtils.EXTERNAL_COUCH).exists();
	}

	/*
	 * List of packages that need to be installed
	 */
	public static List<String> packageSet() 
	{
		ArrayList<String> packages = new ArrayList<String>();
	
		// TODO: Different CPU arch support.
		// TODO: Some kind of sane remote manifest for this (remote updater)
//		packages.add("couch-erl-1.0"); // CouchDB, Erlang, CouchJS
//		packages.add("fixup-1.0"); //Cleanup old mochi, retrigger DNS fix install.
//		packages.add("dns-fix"); //Add inet config to fallback on erlang resolver
//		if (android.os.Build.VERSION.SDK_INT == 7) {
//			packages.add("couch-icu-driver-eclair");
//		} else if (android.os.Build.VERSION.SDK_INT == 8) {
//			packages.add("couch-icu-driver-froyo");
//		} else if (android.os.Build.VERSION.SDK_INT == 9) {	
//			packages.add("couch-icu-driver-gingerbread");
//		} else {
//			throw new RuntimeException("Unsupported Platform");
//		}
		
		packages.add("release-" + Build.VERSION.SDK_INT + "-1.0");
		
		return packages;
	}
	
	/*
	 * Recursively delete directory
	 */
	public static Boolean deleteDirectory(File dir) 
	{
		if (dir.isDirectory()) {
			String[] children = dir.list();
			
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDirectory(new File(dir, children[i]));
				
				if (!success) {
					return false;
				}
			}
		}
		
		return dir.delete();
	}
}
