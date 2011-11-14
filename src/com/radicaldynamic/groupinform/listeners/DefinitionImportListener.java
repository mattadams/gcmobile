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


public interface DefinitionImportListener 
{
    public static final String MESSAGE = "message";             // Key for any message returned to the listener
    public static final String SUCCESSFUL = "successful";       // Key for whether or not the import was successful
    public static final String FILENAME = "filename";           // Key for the name of file that was imported
    
    void importTaskFinished(Bundle data);
}