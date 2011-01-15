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

    // Storage paths
    public static final String ODK_ROOT = Environment.getExternalStorageDirectory() + "/groupinform/";
    public static final String FORMS_PATH = ODK_ROOT + "forms/";
    public static final String INSTANCES_PATH = ODK_ROOT + "instances/";
    public static final String FORMS_X_MEDIA_DIRECTORY_SUFFIX = "-media/";
    public static final String DATABASE_PATH = ODK_ROOT + "metadata/";

    public static final String DEVICE_CACHE_FILE_NAME = "devices.json";
    public static final String FOLDER_CACHE_FILE_NAME = "folders.json";
    public static final String FORM_LOGO_FILE_NAME = "form_logo.png";
    public static final String SPLASH_FILE_NAME = "splash.png";
    public static final String CONFIG_PATH = ODK_ROOT + "config/";
    public static final String DEFAULT_CONFIG_PATH = CONFIG_PATH + "default/";
    public static final String SPLASH_SCREEN_FILE_PATH = DEFAULT_CONFIG_PATH + SPLASH_FILE_NAME;
    public static final String FORM_LOGO_FILE_PATH = DEFAULT_CONFIG_PATH + FORM_LOGO_FILE_NAME;
    public static final String FOLDER_CACHE_FILE_PATH = DATABASE_PATH + FOLDER_CACHE_FILE_NAME;
    public static final String DEVICE_CACHE_FILE_PATH = DATABASE_PATH + DEVICE_CACHE_FILE_NAME;
    public static final String XSL_EXTENSION_PATH = ODK_ROOT + "xsl/";
    public static final String CACHE_PATH = ODK_ROOT + "cache/";
    public static final String TMPFILE_PATH = CACHE_PATH + "tmp.jpg";
    
    public static final int IMAGE_WIDGET_MAX_WIDTH = 1024;
    public static final int IMAGE_WIDGET_MAX_HEIGHT = 768;
    public static final int IMAGE_WIDGET_QUALITY = 75;

    public static final int TIME_TWO_MINUTES = 120 * 1000;
    
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

    public static final boolean deleteFile(String path) {
        if (storageReady()) {
            File f = new File(path);
            return f.delete();
        } else {
            return false;
        }
    }

    public static void deleteInstanceCacheFiles(String instanceId) 
    {
        File cacheDir = new File(FileUtils.CACHE_PATH);
        String[] fileNames = cacheDir.list();
    
        for (String file : fileNames) {
            Log.v(Collect.LOGTAG, t + "evaluating " + file + " for removal");
    
            if (Pattern.matches("^" + instanceId + "[.].*", file)) {
                if (FileUtils.deleteFile(FileUtils.CACHE_PATH + file)) {
                    Log.d(Collect.LOGTAG, t + "removed file " + file);
                } else {
                    Log.e(Collect.LOGTAG, t + "unable to remove file " + file);
                }
            }
        }
    }

    /*
     * Used by ImageWidget to scale a captured bitmap to something that will display nicely on the screen
     */
    public static Bitmap getBitmapScaledToDisplay(File f, int screenHeight, int screenWidth)
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
        
        o = new BitmapFactory.Options();
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
    
        return Bitmap.createScaledBitmap(
                BitmapFactory.decodeFile(f.getAbsolutePath(), new Options()), 
                newWidth, 
                newHeight, 
                false);
    }

    public static final String getDatabasePath() {

    	File dir = new File(DATABASE_PATH);
        if (!storageReady()) {
            return null;
        }
        
        if (!dir.exists()) {
            if (!createFolder(DATABASE_PATH)) {
                return null;
            }
        }
        return DATABASE_PATH;
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


    public static final String getFormMediaPath(String formXml) {
    	int startIdx = formXml.lastIndexOf("/") + 1;
    	String mediaPath = FileUtils.FORMS_PATH +
         		formXml.substring(startIdx, formXml.lastIndexOf(".")) +
         		FileUtils.FORMS_X_MEDIA_DIRECTORY_SUFFIX;
    
    	 Log.i(t, "formXml: " + formXml + " mediaPath: " + mediaPath);
    	 return mediaPath;
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
            Log.e("No Cache File", e.getMessage());
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
