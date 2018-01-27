package com.aefyr.apheleia.adapters;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aefyr.apheleia.MainActivity;
import com.aefyr.apheleia.R;
import com.aefyr.apheleia.utility.Utility;

/**
 * Created by Aefyr on 27.01.2018.
 */

public class LandscapeModeSideMenuListViewAdapter extends BaseAdapter {
    private Context c;
    private ListView listView;
    private LayoutInflater inflater;
    private OnSideMenuInteractionListener listener;
    private String[] fragments = {MainActivity.FRAGMENT_DIARY, MainActivity.FRAGMENT_MARKS, MainActivity.FRAGMENT_FINALS, MainActivity.FRAGMENT_SCHEDULE, MainActivity.FRAGMENT_MESSAGES};
    private Pair<Integer, String>[] items;

    private View currentlySelected;

    public interface OnSideMenuInteractionListener {
        void onApheleiaFragmentSelected(String fragment);
        void onSettingsClick();
        void onLogoutClick();
    }

    public LandscapeModeSideMenuListViewAdapter(ListView listView, OnSideMenuInteractionListener listener){
        this.listView = listView;
        this.listener = listener;
        this.c = listView.getContext();
        inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        setupItems();
    }

    private void setupItems(){
        items = new Pair[7];
        items[0] = new Pair<>(R.drawable.ic_chrome_reader_mode_black_24dp, c.getString(R.string.diary));
        items[1] = new Pair<>(R.drawable.ic_apps_black_24dp, c.getString(R.string.marks));
        items[2] = new Pair<>(R.drawable.ic_flag_black_24dp, c.getString(R.string.finals));
        items[3] = new Pair<>(R.drawable.ic_schedule_black_24dp, c.getString(R.string.schedule));
        items[4] = new Pair<>(R.drawable.ic_email_black_24dp, c.getString(R.string.messages));
        items[5] = new Pair<>(R.drawable.ic_build_black_24dp, c.getString(R.string.action_settings));
        items[6] = new Pair<>(R.drawable.ic_clear_black_24dp, c.getString(R.string.logout));
    }

    @Override
    public int getCount() {
        return items==null?0:items.length;
    }

    @Override
    public Object getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return items[position].first;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View v;
        if(convertView!=null)
            v = convertView;
        else
            v = inflater.inflate(R.layout.landscape_side_menu_item, parent, false);

        ((TextView)v.findViewById(R.id.title)).setText(items[position].second);
        ((ImageView)v.findViewById(R.id.icon)).setImageResource(items[position].first);
        v.findViewById(R.id.layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(position<5) {
                    select(v);
                    listener.onApheleiaFragmentSelected(fragments[position]);
                }else if(position==5)
                    listener.onSettingsClick();
                else
                    listener.onLogoutClick();

            }
        });

        return v;
    }

    public void lidlSelect(String apheleiaFragment){
        final int viewIndex = Utility.indexOfStringInArray(apheleiaFragment, fragments);
        Log.d("LMSMLVA", "Selecting "+apheleiaFragment+ " with index "+viewIndex +" out of childs count: "+listView.getChildCount());
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                select(getViewByPosition(viewIndex+1, listView));

            }
        }, 500);
    }

    private void select(View v){
        if(currentlySelected!=null){
            ((TextView)currentlySelected.findViewById(R.id.title)).setTextColor(c.getResources().getColor(R.color.colorLSMText));
            ((ImageView)currentlySelected.findViewById(R.id.icon)).setColorFilter(c.getResources().getColor(R.color.colorLSMTint));

            TypedValue outValue = new TypedValue();
            c.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            currentlySelected.findViewById(R.id.layout).setBackgroundResource(outValue.resourceId);
        }
        currentlySelected = v;

        ((TextView)currentlySelected.findViewById(R.id.title)).setTextColor(c.getResources().getColor(R.color.colorPrimary));
        ((ImageView)currentlySelected.findViewById(R.id.icon)).setColorFilter(c.getResources().getColor(R.color.colorPrimary));
        currentlySelected.findViewById(R.id.layout).setBackgroundResource(R.drawable.landcscape_side_menu_bg_selected);

    }

    public View getViewByPosition(int pos, ListView listView) {
        final int firstListItemPosition = listView.getFirstVisiblePosition();
        final int lastListItemPosition = firstListItemPosition + listView.getChildCount() - 1;

        if (pos < firstListItemPosition || pos > lastListItemPosition ) {
            return listView.getAdapter().getView(pos, null, listView);
        } else {
            final int childIndex = pos - firstListItemPosition;
            return listView.getChildAt(childIndex);
        }
    }

}
