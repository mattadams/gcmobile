package com.radicaldynamic.groupinform.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.ektorp.ReplicationStatus;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.listeners.SynchronizeFoldersListener;
import com.radicaldynamic.groupinform.logic.AccountFolder;
import com.radicaldynamic.groupinform.services.DatabaseService;

//public class SynchronizeFoldersTask extends AsyncTask<Params, Progress, Result> {
public class SynchronizeFoldersTask extends AsyncTask<Void, Void, Void> 
{
    private static final String t = "SynchronizeFoldersTask: ";
    
    private SynchronizeFoldersListener mStateListener;
    
    private HashMap<String, HashMap<String, ReplicationStatus>> mSummary = new HashMap<String, HashMap<String, ReplicationStatus>>();
    private ArrayList<String> mFoldersToSynchronize = new ArrayList<String>();    
    
    private int mTransferMode = SynchronizeFoldersListener.MODE_UNDEFINED;
    private boolean mPostExecuteSwitch = false;
    
    private int mErrorCode = SynchronizeFoldersListener.OUTCOME_OKAY;
    
    final Handler progressHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mStateListener != null) {
                mStateListener.synchronizationHandler(msg);
            }
        }
    };

    @Override
    protected Void doInBackground(Void... nothing) 
    {
        if (Collect.getInstance().getIoService().isSignedIn() || Collect.getInstance().getIoService().goOnline()) {
            mSummary = synchronize();
        } else {
            mErrorCode = SynchronizeFoldersListener.OUTCOME_DISCONNECTED;
        }
        
        return null;
    }
    
    @Override
    protected void onPreExecute()
    {
        // Trigger display of progress dialog if so implemented
        synchronized (this) {
            if (mStateListener != null) {
                mStateListener.synchronizationHandler(null);
            }
        }
    }
    
    @Override 
    protected void onPostExecute(Void nothing)
    {
        synchronized (this) {
            if (mStateListener != null) {
                Bundle b = new Bundle();
                
                // Go through summary of replications and look for problems
                Set<String> folders = mSummary.keySet();
                Iterator<String> folderIds = folders.iterator();
                
                while (folderIds.hasNext()) {
                    String id = folderIds.next();                    
                    HashMap<String, ReplicationStatus> result = mSummary.get(id);
                    
                    if (result.containsKey(SynchronizeFoldersListener.PUSH_RESULT)) {
                        ReplicationStatus r = result.get(SynchronizeFoldersListener.PUSH_RESULT);
                        
                        if (r == null || (!r.isOk() && !r.isNoChanges())) {
                            mErrorCode = SynchronizeFoldersListener.OUTCOME_INTERRUPTED;
                        }
                    }
                    
                    if (result.containsKey(SynchronizeFoldersListener.PULL_RESULT)) {
                        ReplicationStatus r = result.get(SynchronizeFoldersListener.PULL_RESULT);
                        
                        if (r == null || (!r.isOk() && !r.isNoChanges())) {
                            mErrorCode = SynchronizeFoldersListener.OUTCOME_INTERRUPTED;
                        }
                    }
                }                
                
                if (mErrorCode > 0) {
                    b.putBoolean(SynchronizeFoldersListener.SUCCESSFUL, false);
                    b.putInt(SynchronizeFoldersListener.FAILURE, mErrorCode);
                } else {
                    b.putBoolean(SynchronizeFoldersListener.SUCCESSFUL, true);
                }
                
                // Pass along optional execution path
                b.putBoolean(SynchronizeFoldersListener.POS, mPostExecuteSwitch);
                
                mStateListener.synchronizationTaskFinished(b);
            }
        }        
    }
    
    // Activity handler; this will be used during pre-execute to set up activity UI
    public void setListener(SynchronizeFoldersListener sl) 
    {
        synchronized (this) {
            mStateListener = sl;
        }
    }
    
    // A specific set of folders to synchronize vs. everything
    public void setFolders(ArrayList<String> folders)
    {
        mFoldersToSynchronize = folders;
    }
    
    // Return with result; use for alternate workflow in activity post-task handling 
    public void setPostExecuteSwitch(boolean s)
    {
        mPostExecuteSwitch = s;
    }

    // Set transfer direction (up, down or both)
    public void setTransferMode(int mode)
    {
        mTransferMode = mode;
    }
    
    // Run synchronization process using task configuration 
    private HashMap<String, HashMap<String, ReplicationStatus>> synchronize()
    {
        HashMap<String, HashMap<String, ReplicationStatus>> status = new HashMap<String, HashMap<String, ReplicationStatus>>();
        
        Set<String> folderSet = Collect.getInstance().getInformOnlineState().getAccountFolders().keySet();
        Iterator<String> folderIds = folderSet.iterator();
        
        int progress = 0;
        int total = 0;
        
        // Figure out how many folders are marked for replication
        while (folderIds.hasNext()) {
            AccountFolder folder = Collect.getInstance().getInformOnlineState().getAccountFolders().get(folderIds.next());
            
            // If we were supplied with a specific list of folders to synchronize AND this folder isn't in the list
            if (!mFoldersToSynchronize.isEmpty() && !mFoldersToSynchronize.contains(folder.getId())) {
                if (Collect.Log.VERBOSE) Log.v(Collect.LOGTAG, t + folder.getId() + " is not among the list of folders to synchronize; removing it from the queue");
                folderIds.remove();
            } else if (folder.isReplicated()) {
                // Otherwise, keep it in the list and increment the total number of folders to process                
                total++;
            }            
        }
        
        // Abort (no folders to replicate)
        if (total == 0) {
            return status;
        }
        
        // Reset iterator
        folderIds = folderSet.iterator();    
            
        while (folderIds.hasNext()) {
            AccountFolder folder = Collect.getInstance().getInformOnlineState().getAccountFolders().get(folderIds.next());
            HashMap<String, ReplicationStatus> replicationResults = new HashMap<String, ReplicationStatus>();
            
            // Remember if a database existed prior to the PULL that follows this
            boolean dbMayHaveChanges = Collect.getInstance().getDbService().isDbLocal(folder.getId());
            
            if (folder.isReplicated()) {
                // Update progress dialog
                Message msg = progressHandler.obtainMessage();
                msg.arg1 = ++progress;
                msg.arg2 = total;
                progressHandler.sendMessage(msg);

                try {
                    if (mTransferMode == SynchronizeFoldersListener.MODE_PULL || mTransferMode == SynchronizeFoldersListener.MODE_SWAP) {
                        replicationResults.put(SynchronizeFoldersListener.PULL_RESULT, Collect.getInstance().getDbService().replicate(folder.getId(), DatabaseService.REPLICATE_PULL));
                    }
                } catch (Exception e) {
                    if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + "problem pulling " + folder.getId() + ": " + e.toString());
                    e.printStackTrace();

                    replicationResults.put(SynchronizeFoldersListener.PULL_RESULT, null);
                }

                // A push should only occur if the database exists locally and is likely to have changes
                if (dbMayHaveChanges) {
                    try {                        
                        if (mTransferMode == SynchronizeFoldersListener.MODE_PUSH || mTransferMode == SynchronizeFoldersListener.MODE_SWAP) {
                            replicationResults.put(SynchronizeFoldersListener.PUSH_RESULT, Collect.getInstance().getDbService().replicate(folder.getId(), DatabaseService.REPLICATE_PUSH));
                        }
                    } catch (Exception e) {
                        if (Collect.Log.WARN) Log.w(Collect.LOGTAG, t + "problem pushing " + folder.getId() + ": " + e.toString());
                        e.printStackTrace();

                        replicationResults.put(SynchronizeFoldersListener.PUSH_RESULT, null);
                    }
                }

                status.put(folder.getId(), replicationResults);
            }                
        }

        return status;
    }
}
