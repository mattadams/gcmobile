package com.radicaldynamic.groupinform.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.support.CouchDbRepositorySupport;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinitionDocument;
import com.radicaldynamic.groupinform.documents.FormInstanceDocument;

public class FormDefinitionRepository extends CouchDbRepositorySupport<FormDefinitionDocument>
{
    private final static String t = "FormDefinitionRepository: ";
    
    public FormDefinitionRepository(CouchDbConnector db) 
    {
        super(FormDefinitionDocument.class, db);
        initStandardDesignDocument();
    }

    public List<FormDefinitionDocument> getAllByKeys(Collection<Object> keys) 
    {
        List<FormDefinitionDocument> forms = db.queryView(createQuery("all").keys(keys).includeDocs(true), FormDefinitionDocument.class);
        return forms;
    }
    
    public HashMap<String, HashMap<String, String>> getFormsByInstanceStatus(FormInstanceDocument.Status status) 
    {
        HashMap<String, HashMap<String, String>> results = new HashMap<String, HashMap<String, String>>();
        ViewResult r = db.queryView(createQuery("by_instance_status").group(true));        
        List<Row> rows = r.getRows();
        
        for(Row record : rows) {
            try {
                JSONArray key = (JSONArray) new JSONTokener(record.getKey()).nextValue();

                /*
                 * Document ID:     key.getString(0)
                 * Status category: key.getString(1)
                 */                
                if (status.toString().equals(key.getString(1))) {
                    if (!results.containsKey(key.getString(0)))
                        results.put(key.getString(0), new HashMap<String, String>());
                    
                    results.get(key.getString(0)).put(key.getString(1), record.getValue());
                }
            } catch (JSONException e) {
                Log.e(Collect.LOGTAG, t + "failed to parse complex key in getFormsByInstanceStatus, key: " + record.getKey() + ", value: " + record.getValue());
                e.printStackTrace();
            }
        }
        
        return results;
    }
    
    public HashMap<String, HashMap<String, String>> getFormsWithInstanceCounts()
    {
        HashMap<String, HashMap<String, String>> results = new HashMap<String, HashMap<String, String>>();
        ViewResult r = db.queryView(createQuery("by_instance_status").group(true));        
        List<Row> rows = r.getRows();
        
        for(Row record : rows) {
            try {
                JSONArray key = (JSONArray) new JSONTokener(record.getKey()).nextValue();

                /*
                 * Document ID:     key.getString(0)
                 * Status category: key.getString(1)
                 */                
                if (!results.containsKey(key.getString(0)))
                    results.put(key.getString(0), new HashMap<String, String>());
                
                results.get(key.getString(0)).put(key.getString(1), record.getValue());
            } catch (JSONException e) {
                Log.e(Collect.LOGTAG, t + "failed to parse complex key in getFormsAsInstanceCount, key: " + record.getKey() + ", value: " + record.getValue());
                e.printStackTrace();
            }
        }
        
        return results;
    }
    
    public Map<String, List<String>> getFormsByAggregateReadiness() 
    {
        Map<String, List<String>> results = new HashMap<String, List<String>>();
        ViewResult r = db.queryView(createQuery("by_instance_aggregate_readiness"));
        List<Row> rows = r.getRows();
        
        for(Row record : rows) {
            List<String> values = new ArrayList<String>();
            
            if (results.containsKey(record.getKey()))
                values = results.get(record.getKey());                

            values.add(record.getValue());
            results.put(record.getKey(), values);
        }        
        
        return results;
    }
}