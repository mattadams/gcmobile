package com.radicaldynamic.groupinform.repositories;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.ektorp.ComplexKey;
import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.support.CouchDbRepositorySupport;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.util.Log;

import com.radicaldynamic.groupinform.application.Collect;
import com.radicaldynamic.groupinform.documents.FormInstance;

public class FormInstanceRepo extends CouchDbRepositorySupport<FormInstance>
{
    private final static String t = "FormInstanceRepo: ";
    
    public FormInstanceRepo(CouchDbConnector db) 
    {
        super(FormInstance.class, db, "GCMobileR1");
        initStandardDesignDocument();
    }
        
    @Override
    public List<FormInstance> getAll()
    {
        return queryView("allInstances");
    }
    
    public HashMap<String, JSONObject> getAllPlaceholders()
    {
        HashMap<String, JSONObject> results = new HashMap<String, JSONObject>();
        ViewResult r = db.queryView(createQuery("placeholderInstances"));
        
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
    
    public List<FormInstance> findByFormId(String formId) 
    {
        return queryView("instancesByDefinitionId", formId);
    }
    
    public List<FormInstance> findByFilterIndex(List<String> assignedTo, FormInstance.Status status)
    {
        if (status == FormInstance.Status.any && assignedTo.isEmpty()) {
            return db.queryView(createQuery("activeInstances").includeDocs(true), FormInstance.class);
        }
        
        if (!status.equals(FormInstance.Status.any) && assignedTo.isEmpty()) {
            ComplexKey startKey = ComplexKey.of(status.toString());
            ComplexKey endKey = ComplexKey.of(status.toString(), ComplexKey.emptyObject());
            return db.queryView(createQuery("instanceFilterIndex").startKey(startKey).endKey(endKey).includeDocs(true), FormInstance.class);
        }
        
        if (!status.equals(FormInstance.Status.any) && assignedTo.size() == 1) {
            ComplexKey startKey = ComplexKey.of(status.toString(), assignedTo.get(0));
            ComplexKey endKey = ComplexKey.of(status.toString(), assignedTo.get(0), ComplexKey.emptyObject());
            return db.queryView(createQuery("instanceFilterIndex").startKey(startKey).endKey(endKey).includeDocs(true), FormInstance.class);
        }
        
        if (assignedTo.size() > 0) {
            List<FormInstance> r = db.queryView(createQuery("instanceFilterIndex").keys(assignedTo).includeDocs(true), FormInstance.class);
            
            // Remove any instances that do not match the specified status
            if (!status.equals(FormInstance.Status.any)) {
                Iterator<FormInstance> instanceIterator = r.iterator();
                
                if (instanceIterator.hasNext()) {
                    FormInstance instance = instanceIterator.next();
                    
                    if (!status.equals(instance.getStatus())) {
                        instanceIterator.remove();
                    }
                }
            }
            
            return r;
        }

        return new ArrayList<FormInstance>();
    }

    /*
     * Given a formId and an InstanceDocument status, return a list of instance IDs 
     * belonging to the form in question and having the desired status.
     */
    public ArrayList<String> findByFormAndStatus(String formId, FormInstance.Status status) 
    {
        List<FormInstance> instancesByForm = findByFormId(formId);
        
        ArrayList<String> instanceIds = new ArrayList<String>();
        String state = status.toString();
        
        for (FormInstance doc : instancesByForm) {            
            if (doc.getStatus().toString().equals(state)) {            
                instanceIds.add(doc.getId());
            }
        }
        
        return instanceIds;
    }
}