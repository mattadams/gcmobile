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

package com.radicaldynamic.turboform.tasks;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.ektorp.Attachment;
import org.ektorp.AttachmentInputStream;

import android.os.AsyncTask;
import android.util.Log;

import com.radicaldynamic.turboform.application.Collect;
import com.radicaldynamic.turboform.documents.GenericDocument;
import com.radicaldynamic.turboform.documents.InstanceDocument;
import com.radicaldynamic.turboform.listeners.InstanceUploaderListener;
import com.radicaldynamic.turboform.utilities.FileUtils;

/**
 * Background task for uploading completed forms.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class InstanceUploaderTask extends AsyncTask<String, Integer, ArrayList<String>> {
    private static final String t = "InstanceUploaderTask: ";
    
    //private static long MAX_BYTES = 1048576 - 1024;         // 1MB less 1KB overhead
    private static final int CONNECTION_TIMEOUT = 30000;
    
    InstanceUploaderListener mStateListener;
    String mUrl;   

    public void setUploadServer(String newServer) {
        mUrl = newServer;
    }

    @Override
    protected ArrayList<String> doInBackground(String... values) {
        ArrayList<String> uploadedIntances = new ArrayList<String>();
        int instanceCount = values.length;

        for (int i = 0; i < instanceCount; i++) {
            publishProgress(i + 1, instanceCount);

            // Configure connection
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
            HttpConnectionParams.setSoTimeout(params, CONNECTION_TIMEOUT);
            HttpClientParams.setRedirecting(params, false);

            // Setup client
            DefaultHttpClient httpClient = new DefaultHttpClient(params);
            HttpPost httpPost = new HttpPost(mUrl);

            // MIME POST
            MultipartEntity entity = new MultipartEntity();
            
            InstanceDocument instance = Collect.mDb.getDb().get(InstanceDocument.class, values[i]);
            
            if (instance.getDateAggregated() != null && instance.getDateUpdatedAsCalendar().after(instance.getDateAggregatedAsCalendar())) {
                Log.w(Collect.LOGTAG, t + values[i] + " cannot be uploaded to ODK Aggregate: dateUpdated is newer than dateAggregated");
                cancel(true);
            }
            
            HashMap<String, Attachment> attachments = (HashMap<String, Attachment>) instance.getAttachments();
            
            for (Entry<String, Attachment> entry : attachments.entrySet()) {
                String key = entry.getKey();
                String contentType = entry.getValue().getContentType();

                AttachmentInputStream ais = Collect.mDb.getDb().getAttachment(values[i], key);  
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int bytesRead = 0;

                FileOutputStream file;

                try {
                    if (key.equals("xml")) 
                        key = values[i] + ".xml";
                    
                    file = new FileOutputStream(new File(FileUtils.CACHE_PATH + key));
                    
                    while ((bytesRead = ais.read(buffer)) != -1) {
                        file.write(buffer, 0, bytesRead);
                    }
                    
                    ais.close();
                    output.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                File f = new File(FileUtils.CACHE_PATH + key);
                FileBody fb = new FileBody(f, contentType);
                
                if (contentType.equals("text/xml"))
                    entity.addPart("xml_submission_file", fb);
                else 
                    entity.addPart(f.getName(), fb);
                
//                if (f.delete())
//                    Log.d(Collect.LOGTAG, t + "removed " + f.getName());
//                else
//                    Log.e(Collect.LOGTAG, t + "unable to remove " + f.getName());                
                
                Log.i(Collect.LOGTAG, t + "added " + contentType + " file named " + f.getName() + " prior to httpPost");                
            }
            
            httpPost.setEntity(entity);

            // Prepare response and return uploaded
            HttpResponse response = null;
            
            try {
                response = httpClient.execute(httpPost);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                return uploadedIntances;
            } catch (IOException e) {
                e.printStackTrace();
                return uploadedIntances;
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return uploadedIntances;
            }

            // Check response
            // TODO: This isn't handled correctly
            String serverLocation = null;
            Header[] h = response.getHeaders("Location");
            
            if (h != null && h.length > 0) {
                serverLocation = h[0].getValue();
            } else {
                // Something should be done here...
                Log.e(Collect.LOGTAG, t + "location header was absent");
            }
            
            int responseCode = response.getStatusLine().getStatusCode();
            Log.e(Collect.LOGTAG, t + "received response code " + responseCode);

            // Verify that your response came from a known server
            if (serverLocation != null && mUrl.contains(serverLocation) && responseCode == 201) {
                uploadedIntances.add(values[i]);
                
                instance.setDateAggregated(GenericDocument.generateTimestamp());
                Collect.mDb.getDb().update(instance);
            }
            
            // Remove cache files pertaining to this upload
            Log.d(Collect.LOGTAG, t + "purging uploaded files");
            FileUtils.deleteInstanceCacheFiles(values[i]);
        }

        return uploadedIntances;
    }

    @Override
    protected void onPostExecute(ArrayList<String> value) {
        synchronized (this) {
            if (mStateListener != null) {
                mStateListener.uploadingComplete(value);
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        synchronized (this) {
            if (mStateListener != null) {
                // update progress and total
                mStateListener.progressUpdate(values[0].intValue(), values[1].intValue());
            }
        }
    }

    public void setUploaderListener(InstanceUploaderListener sl) {
        synchronized (this) {
            mStateListener = sl;
        }
    }
}
