package com.radicaldynamic.groupinform.repositories;

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
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormDefinition;
import com.radicaldynamic.groupinform.documents.FormInstance;

public class FormDefinitionRepo extends CouchDbRepositorySupport<FormDefinition>
{
    private final static String t = "FormDefinitionRepo: ";
    
    public FormDefinitionRepo(CouchDbConnector db) 
    {
        super(FormDefinition.class, db, "FormDefinitionRepoR2");
        initStandardDesignDocument();
    }
    
    // For deuplication (when copying)
    public List<FormDefinition> findByName(String name) 
    {
        return queryView("byName", name);
    }
    
    // For deduplication (when importing from ODK Aggregate)
    public List<FormDefinition> findByXmlHash(String xmlHash) 
    {
        return queryView("byXmlHash", xmlHash);
    }

    public List<FormDefinition> getAllActive()
    {        
        return db.queryView(createQuery("allActive").includeDocs(true), FormDefinition.class);
    }
    
    public List<FormDefinition> getAllActiveByKeys(Collection<Object> keys) 
    {
        return db.queryView(createQuery("allActive").keys(keys).includeDocs(true), FormDefinition.class);
    }
    
    public HashMap<String, JSONObject> getAllPlaceholders()
    {
        HashMap<String, JSONObject> results = new HashMap<String, JSONObject>();
        ViewResult r = db.queryView(createQuery("allPlaceholders"));
        
        for (Row record : r.getRows()) {
            try {
                results.put(record.getKey(), (JSONObject) new JSONTokener(record.getValue()).nextValue());                
            } catch (JSONException e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "failed to parse complex value in getAllPlaceholders, key: " + record.getKey() + ", value: " + record.getValue());
                e.printStackTrace();
            }
        }
        
        return results;
    }
    
    public HashMap<String, HashMap<String, String>> getFormsByInstanceStatus(FormInstance.Status status) 
    {
        HashMap<String, HashMap<String, String>> results = new HashMap<String, HashMap<String, String>>();
        ViewResult r = db.queryView(createQuery("byInstanceStatus").group(true));        
        List<Row> rows = r.getRows();
        
        for(Row record : rows) {
            try {
                JSONArray key = (JSONArray) new JSONTokener(record.getKey()).nextValue();

                /*
                 * Document ID:     key.getString(0)
                 * Status category: key.getString(1)
                 */                
                if (status.equals(FormInstance.Status.any) || status.toString().equals(key.getString(1))) {
                    if (!results.containsKey(key.getString(0))) {
                        results.put(key.getString(0), new HashMap<String, String>());
                    }
                    
                    results.get(key.getString(0)).put(key.getString(1), record.getValue());
                }
            } catch (JSONException e) {
                if (Collect.Log.ERROR) Log.e(Collect.LOGTAG, t + "failed to parse complex key in getFormsByInstanceStatus, key: " + record.getKey() + ", value: " + record.getValue());
                e.printStackTrace();
            }
        }
        
        return results;
    }
    
    public Map<String, List<String>> getByAggregateReadiness() 
    {
        Map<String, List<String>> results = new HashMap<String, List<String>>();
        
        ViewResult r = db.queryView(createQuery("byAggregateReadiness"));
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