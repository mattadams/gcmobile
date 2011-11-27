package com.radicaldynamic.groupinform.utilities;

import java.io.File;
import java.util.Calendar;
import java.util.regex.Pattern;

import com.radicaldynamic.groupinform.application.Collect;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Environment;
import android.util.Log;

public final class FileUtilsExtended 
{
    private static final String t = "FileUtilsExtended: ";
    
    // Storage paths with support for API level 7
    public static final String EXTERNAL_PATH   = Environment.getExternalStorageDirectory() + "/Android/data/com.radicaldynamic.groupinform";
    public static final String EXTERNAL_FILES  = EXTERNAL_PATH + "/files";    // API level 8 can use getExternalFilesDir()
    public static final String EXTERNAL_CACHE  = EXTERNAL_PATH + "/cache";    // API level 8 can use getExternalCacheDir()
    public static final String EXTERNAL_COUCH  = EXTERNAL_PATH + "/couchdb";
    public static final String EXTERNAL_ERLANG = EXTERNAL_PATH + "/erlang";
    public static final String EXTERNAL_DB     = EXTERNAL_PATH + "/db";
    
    // Temporary storage for forms downloaded from ODK Aggregate or about to be uploaded to same
    public static final String ODK_IMPORT_PATH = EXTERNAL_CACHE + "/odk/import";
    public static final String ODK_UPLOAD_PATH = EXTERNAL_CACHE + "/odk/upload";
    
    // Temporary instance storage
    public static final String FORMS_PATH = EXTERNAL_CACHE + "/forms";
    public static final String INSTANCES_PATH = EXTERNAL_CACHE + "/instances";
    public static final String MEDIA_DIR = "media";
    
    public static final String CAPTURED_IMAGE_FILE = "tmp.jpg";
    public static final String DEVICE_CACHE_FILE   = "devices.json";
    public static final String FOLDER_CACHE_FILE   = "folders.json";
    public static final String FORM_LOGO_FILE      = "form_logo.png";
    public static final String SESSION_CACHE_FILE  = "session.bin";
    public static final String SPLASH_SCREEN_FILE  = "splash.png";
    
    public static final int IMAGE_WIDGET_MAX_WIDTH  = 1024;
    public static final int IMAGE_WIDGET_MAX_HEIGHT = 768;
    public static final int IMAGE_WIDGET_QUALITY    = 75;

    // Minutes in second units
    public static final int TIME_TWO_MINUTES = 120;
    
    // 24 hours (represented as milliseconds)
    private static final long TIME_24_HOURS = 86400000;
    
    /*
     * Delete files from the app cache directory on the SD card according to
     * file names matching a specific CouchDB document ID.  This is usually
     * a form instance ID.
     */
    public static void deleteExternalInstanceCacheFiles(String id) 
    {
        final String tt = t + "deleteExternalInstanceCacheFiles(): ";
        
        File cacheDir = new File(EXTERNAL_CACHE);
        String[] fileNames = cacheDir.list();
    
        for (String f : fileNames) {
            if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "evaluating " + f + " for removal");
    
            if (Pattern.matches("^" + id + "[.].*", f)) {
                if (new File(EXTERNAL_CACHE, f).delete()) {
                    if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "removed " + f);
                } else {
                    if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, tt + "unable to remove " + f);
                }
            }
        }
    }
    
    /*
     * Removed from ODK FileUtils but we still use it
     */
    public static boolean deleteFolder(String path) {
        // not recursive
        if (path != null && storageReady()) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    if (!file.delete()) {
                        if (Collect.Log.ERROR) Log.e(t, "Failed to delete " + file);
                    }
                }
            }
            return dir.delete();
        } else {
            return false;
        }
    }    
    
    /*
     * Recursively go through the external cache directories & files and 
     * remove empty directories and files older than 72 hours.
     */
    public static void expireExternalCache(File f)
    {
        final String tt = t + "expireExternalCache(): ";
        
        if (!f.exists()) {
            if (Collect.Log.WARN) Log.w(Collect.LOGTAG, tt + f.getAbsolutePath() + " could not be found, aborting");
            return;
        }
        
        if (f.isDirectory()) {
            if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + "expiring old files in " + f.getAbsolutePath());

            for (File c : f.listFiles()) {
                expireExternalCache(c);
            }
        }
        
        if (f.isFile() && (System.currentTimeMillis() - f.lastModified() >= TIME_24_HOURS * 3) || f.isDirectory() && f.listFiles().length == 0) {
            if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, tt + f.getName() + " older than 72 hours or empty directory, removing");
            f.delete();
        }
    }
    
    /*
     * Used by ImageWidget to resize a captured bitmap to equal to or less than maxWidth x maxHeight
     */
    public static Bitmap getBitmapResizedToStore(File f, int maxWidth, int maxHeight)
    {
        // Determine image size of f
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    
        int newWidth;
        int newHeight;
    
        float scaleWidth;
        float scaleHeight;
        
        if (o.outWidth >= o.outHeight) {
            scaleWidth = (float) maxWidth / o.outWidth;
            scaleHeight = scaleWidth;
        } else {
            scaleHeight = (float) maxHeight / o.outHeight;
            scaleWidth = scaleHeight;
        }
        
        newWidth = Math.round(o.outWidth * scaleWidth);
        newHeight = Math.round(o.outHeight * scaleHeight);
    
        if (Collect.Log.DEBUG) Log.d(t, "Image should be " + maxWidth + "x" + maxHeight 
                + ".  Image has been scaled to "
                + newWidth + "x" + newHeight);
    
        return Bitmap.createScaledBitmap(
                BitmapFactory.decodeFile(f.getAbsolutePath(), new Options()), 
                newWidth, 
                newHeight, 
                true);
    }    
    
    /*
     * Find out if a given file is older than span (in seconds)
     */
    public static final boolean isFileOlderThan(String path, int span)
    {
        File f = new File(path);
        
        if (f.lastModified() < (Calendar.getInstance().getTimeInMillis() - span * 1000))
            return true;
        else 
            return false;            
    }
    
    /*
     * Removed from ODK FileUtils but we still use it
     */
    public static boolean storageReady() {
        String cardstatus = Environment.getExternalStorageState();
        if (cardstatus.equals(Environment.MEDIA_REMOVED)
                || cardstatus.equals(Environment.MEDIA_UNMOUNTABLE)
                || cardstatus.equals(Environment.MEDIA_UNMOUNTED)
                || cardstatus.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            return false;
        } else {
            return true;
        }
    }
}
