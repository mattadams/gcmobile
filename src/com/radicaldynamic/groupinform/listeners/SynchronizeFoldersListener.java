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

package com.radicaldynamic.groupinform.listeners;

import android.os.Bundle;
import android.os.Message;


public interface SynchronizeFoldersListener 
{
    // Transfer modes (SWAP is push/pull)
    public static final int MODE_UNDEFINED = 0;
    public static final int MODE_PULL = 1;
    public static final int MODE_PUSH = 2;
    public static final int MODE_SWAP = 3;
    
    public static final String SUCCESSFUL = "successful";   // Key for whether or not the synchronization was successful
    public static final String FAILURE = "failure";         // Key for failure reason 
    public static final String POS = "post_execute_switch"; // Key for post execute switch option
    
    // Keys for accessing specific synchronization result objects
    public static final String PULL_RESULT = "pull_result";
    public static final String PUSH_RESULT = "push_result";

    // Various outcomes, errors, etc.
    public static final int OUTCOME_OKAY = 0;               // Looking good!
    public static final int OUTCOME_DISCONNECTED = 1;       // Could not go online for some reason
    public static final int OUTCOME_INTERRUPTED = 2;        // Requested synchronization could not be completed for some reason    
    
    void synchronizationTaskFinished(Bundle data);
    void synchronizationHandler(Message msg);
}