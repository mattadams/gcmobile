package com.radicaldynamic.turboform.repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ektorp.CouchDbConnector;
import org.ektorp.ViewResult;
import org.ektorp.ViewResult.Row;
import org.ektorp.support.CouchDbRepositorySupport;
import org.ektorp.support.View;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;

import android.util.Log;

import com.radicaldynamic.turboform.application.Collect;
import com.radicaldynamic.turboform.documents.FormDocument;
import com.radicaldynamic.turboform.documents.InstanceDocument;

@View(name = "all", map = "function(doc) { if (doc.type == 'form') emit (doc._id, doc._id) }")
public class FormRepository extends CouchDbRepositorySupport<FormDocument>
{
    public FormRepository(CouchDbConnector db) {
        super(FormDocument.class, db);
        initStandardDesignDocument();
    }

    public List<FormDocument> getAllByKeys(Collection<Object> keys) {
        List<FormDocument> forms = db.queryView(createQuery("all").keys(keys).includeDocs(true), FormDocument.class);
        return forms;
    }
    
    @View(name = "by_instance_status", map = "function(doc) { if (doc.type == 'instance') emit ([doc.form, doc.status], 1); }", reduce = "function(keys, values) { return sum(values); }")
    public Map<String, String> getFormsByInstanceStatus(InstanceDocument.Status status) {
        Map<String, String> results = new HashMap<String, String>();
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
                    results.put(key.getString(0), record.getValue());
                }
            } catch (JSONException e) {
                Log.e(Collect.LOGTAG, "Failed to parse complex key in getFormsByInstanceStatus, key: " + record.getKey() + ", value: " + record.getValue());
                e.printStackTrace();
            }
        }
        
        return results;
    }
}
