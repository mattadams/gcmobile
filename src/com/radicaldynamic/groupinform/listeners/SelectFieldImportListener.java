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

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;


public interface SelectFieldImportListener 
{
    public static final int MODE_PREVIEW = 0;               // Read in a limited number of records
    public static final int MODE_IMPORT = 1;                // Perform the full import
    
    public static final String SUCCESSFUL = "successful";   // Key for whether the synchronization was successful 
    public static final String MODE = "mode";               // Key for returning input mode
    public static final String CLEAR_LIST = "clear_list";   // Key for whether to append to or replace the list 
       
    void importTaskFinished(Bundle data, ArrayList<List<String>> importedRecords);
}