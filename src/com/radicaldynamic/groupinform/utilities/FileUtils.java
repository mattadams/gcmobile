/*
 * Copyright (C) 2009 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.radicaldynamic.groupinform.utilities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Pattern;

import com.radicaldynamic.groupinform.application.Collect;

/**
 * Static methods used for common file operations.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public final class FileUtils {
    private final static String t = "FileUtils: ";

    // Used to validate and display valid form names.
    public static final String VALID_FILENAME = "[ _\\-A-Za-z0-9]*.x[ht]*ml";

    // Storage paths with support for API level 7
    public static final String EXTERNAL_PATH   = Environment.getExternalStorageDirectory() + "/Android/data/com.radicaldynamic.groupinform";
    public static final String EXTERNAL_FILES  = EXTERNAL_PATH + "/files";    // API level 8 can use getExternalFilesDir()
    public static final String EXTERNAL_CACHE  = EXTERNAL_PATH + "/cache";    // API level 8 can use getExternalCacheDir()
    public static final String EXTERNAL_COUCH  = EXTERNAL_PATH + "/couchdb";
    public static final String EXTERNAL_ERLANG = EXTERNAL_PATH + "/erlang";
    
    // Temporary storage for forms downloaded from ODK Aggregate
    public static final String ODK_DOWNLOAD_PATH = EXTERNAL_CACHE + "/download";
    public static final String ODK_UPLOAD_PATH = EXTERNAL_CACHE + "/upload";
    
    public static final String CAPTURED_IMAGE_FILE = "tmp.jpg";
    public static final String DEVICE_CACHE_FILE   = "devices.json";
    public static final String FOLDER_CACHE_FILE   = "folders.json";
    public static final String FORM_LOGO_FILE      = "form_logo.png";
    public static final String SESSION_CACHE_FILE  = "session.bin";
    public static final String SPLASH_SCREEN_FILE  = "splash.png";
    
    public static final int IMAGE_WIDGET_MAX_WIDTH  = 1024;
    public static final int IMAGE_WIDGET_MAX_HEIGHT = 768;
    public static final int IMAGE_WIDGET_QUALITY    = 75;

    public static final int TIME_TWO_MINUTES = 120;
    
    public static final boolean createFolder(String path) {
        if (storageReady()) {
            boolean made = true;
            File dir = new File(path);
            if (!dir.exists()) {
                made = dir.mkdirs();
            }
            return made;
        } else {
            return false;
        }
    }

    /*
     * Delete files from the app cache directory on the SD card according to
     * file names matching a specific CouchDB document ID.  This is usually
     * a form instance ID.
     */
    public static void deleteExternalInstanceCacheFiles(String id) 
    {
        final String tt = t + "deleteExternalInstanceCacheFiles(): ";
        
        File cacheDir = new File(FileUtils.EXTERNAL_CACHE);
        String[] fileNames = cacheDir.list();
    
        for (String f : fileNames) {
            Log.v(Collect.LOGTAG, tt + "evaluating " + f + " for removal");
    
            if (Pattern.matches("^" + id + "[.].*", f)) {
                if (new File(FileUtils.EXTERNAL_CACHE, f).delete()) {
                    Log.d(Collect.LOGTAG, tt + "removed " + f);
                } else {
                    Log.e(Collect.LOGTAG, tt + "unable to remove " + f);
                }
            }
        }
    }

    public static final boolean deleteFolder(String path) {
        // not recursive
        if (path != null && storageReady()) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    if (!file.delete()) {
                        Log.i(t, "Failed to delete " + file);
                    }
                }
            }
            return dir.delete();
        } else {
            return false;
        }
    }
    
    /*
     * Used by ImageWidget to scale a captured bitmap to something that will display nicely on the screen
     */
    public static final Bitmap getBitmapScaledToDisplay(File f, int screenHeight, int screenWidth)
    {
        // Determine image size of f
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    
        int heightScale = o.outHeight / screenHeight;
        int widthScale = o.outWidth / screenWidth;
    
        // Powers of 2 work faster, sometimes, according to the doc.
        // We're just doing closest size that still fills the screen.
        int scale = Math.max(widthScale, heightScale);
    
        // get bitmap with scale ( < 1 is the same as 1)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = scale;
        Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
        
        Log.i(t, "Screen is " + screenHeight + "x" + screenWidth
                + ".  Image has been scaled down by " + scale + " to "
                + b.getHeight() + "x" + b.getWidth());
    
        return b;
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
    
        Log.i(t, "Image should be " + maxWidth + "x" + maxHeight 
                + ".  Image has been scaled to "
                + newWidth + "x" + newHeight);
    
        return Bitmap.createScaledBitmap(
                BitmapFactory.decodeFile(f.getAbsolutePath(), new Options()), 
                newWidth, 
                newHeight, 
                false);
    }

    public static final byte[] getFileAsBytes(File file) {
        byte[] bytes = null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
    
            // Get the size of the file
            long length = file.length();
            if (length > Integer.MAX_VALUE) {
                Log.e(t, "File " + file.getName() + "is too large");
                return null;
            }
    
            // Create the byte array to hold the data
            bytes = new byte[(int) length];
    
            // Read in the bytes
            int offset = 0;
            int read = 0;
            try {
                while (offset < bytes.length && read >= 0) {
                    read = is.read(bytes, offset, bytes.length - offset);
                    offset += read;
                }
            } catch (IOException e) {
                Log.e(t, "Cannot read " + file.getName());
                e.printStackTrace();
                return null;
            }
    
            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                try {
                    throw new IOException("Could not completely read file " + file.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            }
    
            return bytes;
    
        } catch (FileNotFoundException e) {
            Log.e(t, "Cannot find " + file.getName());
            e.printStackTrace();
            return null;
    
        } finally {
            // Close the input stream
            try {
                is.close();
            } catch (IOException e) {
                Log.e(t, "Cannot close input stream for " + file.getName());
                e.printStackTrace();
                return null;
            }
        }
    }

    public static final ArrayList<String> getFoldersAsArrayList(String path) {
        ArrayList<String> mFolderList = new ArrayList<String>();
        File root = new File(path);

        if (!storageReady()) {
            return null;
        }
        if (!root.exists()) {
            if (!createFolder(path)) {
                return null;
            }
        }
        if (root.isDirectory()) {
            File[] children = root.listFiles();
            for (File child : children) {
                boolean directory = child.isDirectory();
                if (directory) {
                    mFolderList.add(child.getAbsolutePath());
                }
            }
        }
        return mFolderList;
    }    
    
    public static final String getInstanceDirPath(String instanceFilePath) {
        File instance = new File(instanceFilePath);
        File instanceDir = instance.getParentFile();
        if ( !instance.getName().equals(instanceDir.getName() + ".xml")) {
            return null;
        }
        return instanceDir.getAbsolutePath();
    }

    public static final String getInstanceFilePath(String instanceDirPath) {
        File instanceDir = new File(instanceDirPath);
        File instance = new File(instanceDir, instanceDir.getName() + ".xml");
        return instance.getAbsolutePath();
    }

    public static final String getSubmissionBlobPath(String instanceDirPath) {
        File instanceDir = new File(instanceDirPath);
        File submissionBlob = new File(instanceDir, instanceDir.getName() + ".xml.submit");
        return submissionBlob.getAbsolutePath();
    }

    public static final String getMd5Hash(File file) {
        try {
            // CTS (6/15/2010) : stream file through digest instead of handing it the byte[]
            MessageDigest md = MessageDigest.getInstance("MD5");
            int chunkSize = 256;
    
            byte[] chunk = new byte[chunkSize];
    
            // Get the size of the file
            long lLength = file.length();
    
            if (lLength > Integer.MAX_VALUE) {
                Log.e(t, "File " + file.getName() + "is too large");
                return null;
            }
    
            int length = (int) lLength;
    
            InputStream is = null;
            is = new FileInputStream(file);
    
            int l = 0;
            for (l = 0; l + chunkSize < length; l += chunkSize) {
                is.read(chunk, 0, chunkSize);
                md.update(chunk, 0, chunkSize);
            }
    
            int remaining = length - l;
            if (remaining > 0) {
                is.read(chunk, 0, remaining);
                md.update(chunk, 0, remaining);
            }
            byte[] messageDigest = md.digest();
    
            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);
            while (md5.length() < 32)
                md5 = "0" + md5;
            is.close();
            return md5;
    
        } catch (NoSuchAlgorithmException e) {
            Log.e("MD5", e.getMessage());
            return null;
    
        } catch (FileNotFoundException e) {
            Log.e("No Xml File", e.getMessage());
            return null;
        } catch (IOException e) {
            Log.e("Problem reading from file", e.getMessage());
            return null;
        } 
    }

    public static final ArrayList<String> getValidFormsAsArrayList(String path) {
        ArrayList<String> formPaths = new ArrayList<String>();
    
        File dir = new File(path);
    
        if (!storageReady()) {
            return null;
        }
        if (!dir.exists()) {
            if (!createFolder(path)) {
                return null;
            }
        }
        File[] dirs = dir.listFiles();
        if (dirs != null) {
            for (int i = 0; i < dirs.length; i++) {
                // skip all the -media directories and "invisible" files that start with "."
                if (dirs[i].isDirectory() || dirs[i].getName().startsWith("."))
                    continue;
        	
                String formName = dirs[i].getName();
                Log.i(t, "Found formname: " + formName);
                
                formPaths.add(dirs[i].getAbsolutePath());
            }
        }
        return formPaths;
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

    public static final boolean storageReady() {
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
