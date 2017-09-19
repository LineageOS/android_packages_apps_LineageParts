package org.lineageos.lineageparts.profiles.actions.item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.support.v7.preference.R;

/**
 * Created by shade on 9/12/16.
 */

public abstract class BaseItem implements Item {

    protected abstract String getTitle();

    protected abstract String getSummary();

    private View mView;

    @Override
    public View getView(LayoutInflater inflater, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = inflater.inflate(R.layout.profile_action_item, parent, false);
            // Do some initialization
        } else {
            view = convertView;
        }

        mView = view;

        TextView text = (TextView) view.findViewById(android.R.id.title);
        String title = getTitle();
        if (title == null) {
            text.setVisibility(View.GONE);
        } else {
            text.setText(title);
        }

        TextView desc = (TextView) view.findViewById(android.R.id.summary);
        String summary = getSummary();
        if (summary == null) {
            desc.setVisibility(View.GONE);
        } else {
            desc.setText(summary);
        }
        
        return view;
    }

    protected Context getContext() {
        return mView.getContext();
    }

    protected String getString(int resId) {
        return getContext().getResources().getString(resId);
    }
}
