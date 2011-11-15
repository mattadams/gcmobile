package com.radicaldynamic.groupinform.tasks;

import android.os.AsyncTask;
import android.os.Bundle;

import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.listeners.ToggleOnlineStateListener;

// Go online or offline accordingly; folders should be synchronized by SynchronizeFoldersTask
public class ToggleOnlineStateTask extends AsyncTask<Void, Void, Void> 
{
    private ToggleOnlineStateListener mStateListener;
    
    private int mOutcome = ToggleOnlineStateListener.SUCCESSFUL;
    private boolean mPostExecuteSwitch = false;
    
    @Override
    protected Void doInBackground(Void... nothing)
    {   
        if (Collect.getInstance().getIoService().isSignedIn()) {
            if (!Collect.getInstance().getIoService().goOffline())
                mOutcome = ToggleOnlineStateListener.CANNOT_SIGNOUT;
        } else {
            if (!Collect.getInstance().getIoService().goOnline())
                mOutcome = ToggleOnlineStateListener.CANNOT_SIGNIN;
        }

        return null;
    }

    @Override
    protected void onPreExecute()
    {
        // Trigger display of dialog and adjust other visual aspects
        synchronized (this) {
            if (mStateListener != null) {
                mStateListener.toggleOnlineStateHandler();
            }
        }
    }

    @Override
    protected void onPostExecute(Void nothing)
    {   
        synchronized (this) {
            if (mStateListener != null) {
                Bundle b = new Bundle();
                b.putInt(ToggleOnlineStateListener.OUTCOME, mOutcome);
                b.putBoolean(ToggleOnlineStateListener.POS, mPostExecuteSwitch);
                mStateListener.toggleOnlineStateTaskFinished(b);
            }
        }
    }    
    
    public void setListener(ToggleOnlineStateListener sl) 
    {
        synchronized (this) {
            mStateListener = sl;
        }
    }
    
    // Return with result; use for alternate workflow in activity post-task handling 
    public void setPostExecuteSwitch(boolean s)
    {
        mPostExecuteSwitch = s;
    }
}
