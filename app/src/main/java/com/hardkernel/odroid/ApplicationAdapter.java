package com.hardkernel.odroid;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class ApplicationAdapter extends ArrayAdapter {

    private List<ApplicationInfo> apps;
    private List<String> title;
    public ApplicationAdapter(Context context, int resource, List<String> title, List<ApplicationInfo> apps) {
        super(context, resource, title);
        this.apps = apps;
        this.title = title;
    }

    public View getViews(int position, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(this.getContext());
        View layout = inflater.inflate(R.layout.applist_dropdown_item_1line, parent, false);

        ImageView image = (ImageView) layout.findViewById(R.id.appIcon);
        TextView viewTitle = (TextView) layout.findViewById(R.id.appTitle);

        for(ApplicationInfo app: apps) {
            if (position == 0) {
                image.setImageResource(android.R.drawable.ic_delete);
                viewTitle.setText("No shortcut");

            } else if (title.get(position).equals("home")) {
                image.setImageResource(android.R.drawable.sym_def_app_icon);
                viewTitle.setText("Home");

            } else if (app.packageName.equals(title.get(position))) {
                PackageManager pm = this.getContext().getPackageManager();
                image.setImageDrawable(app.loadIcon(pm));
                viewTitle.setText(app.loadLabel(pm));
            }
        }

        return layout;
    }
    @Override
    public View getDropDownView(int position, View convertView,
                                ViewGroup parent) {
        return getViews(position, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getViews(position, parent);
    }
}
