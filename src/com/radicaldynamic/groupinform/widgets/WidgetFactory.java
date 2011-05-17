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

package com.radicaldynamic.groupinform.widgets;

import org.javarosa.core.model.Constants;
import org.javarosa.form.api.FormEntryPrompt;

import android.content.Context;
import android.util.Log;

/**
 * Convenience class that handles creation of widgets.
 * 
 * @author Carl Hartung (carlhartung@gmail.com)
 */
public class WidgetFactory {

    /**
     * Returns the appropriate QuestionWidget for the given FormEntryPrompt.
     * 
     * @param fep prompt element to be rendered
     * @param context Android context
     */
    static public QuestionWidget createWidgetFromPrompt(FormEntryPrompt fep, Context context) {

        QuestionWidget questionWidget = null;
        switch (fep.getControlType()) {
            case Constants.CONTROL_INPUT:
                switch (fep.getDataType()) {
                    case Constants.DATATYPE_DATE_TIME:
                        questionWidget = new DateTimeWidget(context, fep);
                        break;
                    case Constants.DATATYPE_DATE:
                        questionWidget = new DateWidget(context, fep);
                        break;
                    case Constants.DATATYPE_TIME:
                        questionWidget = new TimeWidget(context, fep);
                        break;
                    case Constants.DATATYPE_DECIMAL:
                        questionWidget = new DecimalWidget(context, fep);
                        break;
                    case Constants.DATATYPE_INTEGER:
                        questionWidget = new IntegerWidget(context, fep);
                        break;
                    case Constants.DATATYPE_GEOPOINT:
                        questionWidget = new GeoPointWidget(context, fep);
                        break;
                    case Constants.DATATYPE_BARCODE:
                        questionWidget = new BarcodeWidget(context, fep);
                        break;
                    default:
                        questionWidget = new StringWidget(context, fep);
                        break;
                }
                break;
            case Constants.CONTROL_IMAGE_CHOOSE:
                questionWidget = new ImageWidget(context, fep);
                break;
            case Constants.CONTROL_AUDIO_CAPTURE:
                questionWidget = new AudioWidget(context, fep);
                break;
            case Constants.CONTROL_VIDEO_CAPTURE:
                questionWidget = new VideoWidget(context, fep);
                break;
            case Constants.CONTROL_SELECT_ONE:
                String appearance = fep.getAppearanceHint();

                if (appearance != null && appearance.contains("compact")) {
                    int numColumns = -1;
                    try {
                        numColumns =
                            Integer.parseInt(appearance.substring(appearance.indexOf('-') + 1));
                    } catch (Exception e) {
                        // Do nothing, leave numColumns as -1
                        Log.e("WidgetFactory", "Exception parsing numColumns");
                    }

                    questionWidget = new GridWidget(context, fep, numColumns);
                } else if (appearance != null && appearance.equals("minimal")) {
                    questionWidget = new SpinnerWidget(context, fep);
                } else if (appearance != null && appearance.contains("autocomplete")) {
                    String filterType = null;
                    try {
                        filterType = appearance.substring(appearance.indexOf('-') + 1);
                    } catch (Exception e) {
                        // Do nothing, leave filerType null
                        Log.e("WidgetFactory", "Exception parsing numColumns");
                    }
                    questionWidget = new AutoCompleteWidget(context, fep, filterType);

                } else {
                    questionWidget = new SelectOneWidget(context, fep);
                }
                break;
            case Constants.CONTROL_SELECT_MULTI:
                appearance = fep.getAppearanceHint();

                if (appearance != null && appearance.contains("compact")) {
                    int numColumns = -1;
                    try {
                        numColumns =
                            Integer.parseInt(appearance.substring(appearance.indexOf('-') + 1));
                    } catch (Exception e) {
                        // Do nothing, leave numColumns as -1
                        Log.e("WidgetFactory", "Exception parsing numColumns");
                    }

                    questionWidget = new GridMultiWidget(context, fep, numColumns);
                } else if (appearance != null && appearance.equals("minimal")) {
                    questionWidget = new SpinnerMultiWidget(context, fep);
                } else {
                    questionWidget = new SelectMultiWidget(context, fep);
                }
                break;
            case Constants.CONTROL_TRIGGER:
                questionWidget = new TriggerWidget(context, fep);
                break;
            default:
                questionWidget = new StringWidget(context, fep);
                break;
        }
        return questionWidget;
    }

}
