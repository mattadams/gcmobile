package com.radicaldynamic.turboform.activities;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.radicaldynamic.turboform.R;
import com.radicaldynamic.turboform.views.TouchListView;

public class FormBuilderList extends ListActivity
{
    private static String[] items = { "lorem", "ipsum", "dolor", "sit", "amet",
            "consectetuer", "adipiscing", "elit", "morbi", "vel", "ligula",
            "vitae", "arcu", "aliquet", "mollis", "etiam", "vel", "erat",
            "placerat", "ante", "porttitor", "sodales", "pellentesque",
            "augue", "purus" };
    private IconicAdapter adapter = null;
    private ArrayList<String> array = new ArrayList<String>(Arrays
            .asList(items));

    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setContentView(R.layout.form_builder_main);

        adapter = new IconicAdapter();
        setListAdapter(adapter);

        TouchListView tlv = (TouchListView) getListView();

        tlv.setDropListener(onDrop);
        tlv.setRemoveListener(onRemove);
    }

    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to)
        {
            String item = adapter.getItem(from);

            adapter.remove(item);
            adapter.insert(item, to);
        }
    };

    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which)
        {
            adapter.remove(adapter.getItem(which));
        }
    };

    class IconicAdapter extends ArrayAdapter<String>
    {
        IconicAdapter() {
            super(FormBuilderList.this, R.layout.form_builder_row2, array);
        }

        public View getView(int position, View convertView, ViewGroup parent)
        {
            View row = convertView;

            if (row == null) {
                LayoutInflater inflater = getLayoutInflater();

                row = inflater.inflate(R.layout.form_builder_row2, parent,
                        false);
            }

            TextView label = (TextView) row.findViewById(R.id.label);

            label.setText(array.get(position));

            return (row);
        }
    }
}
