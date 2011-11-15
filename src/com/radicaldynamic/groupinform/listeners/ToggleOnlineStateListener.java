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


public interface ToggleOnlineStateListener 
{
    public static final String OUTCOME = "outcome";         // Key for passing this task's outcome
    public static final String POS = "post_execute_switch"; // Key for post execute switch option
    
    public static final int SUCCESSFUL     = 0;             // Task completed without problem
    public static final int CANNOT_SIGNIN  = 1;             // Could not go online for some reason
    public static final int CANNOT_SIGNOUT = 2;             // Could not go offline for some reason
    
    void toggleOnlineStateTaskFinished(Bundle data);
    void toggleOnlineStateHandler();
}