package com.rea.learn;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;
import java.util.Vector;

/**
 * Created by ericbhatti on 12/30/15.
 * <p>
 * <p>
 * <p/> Class Description:
 *
 * @author Eric Bhatti
 *         <p>
 *         Company Name: Arpatech (http://arpatech.com/)
 *         <p>
 *         Jira Ticket: NULL
 * @since 30 December, 2015
 */
public class ScheduleAdapter extends ArrayAdapter<Schedule> {

    static int count =0;
    Context context;
    List<Schedule> schedules;

    /**
     * Constructor
     *
     * @param context   The current context.
     * @param resource  The resource ID for a layout file containing a TextView to use when instantiating views.
     * @param schedules The objects to represent in the ListView.
     */
    public ScheduleAdapter(Context context, int resource, List<Schedule> schedules) {
        super(context, resource, schedules);
        this.context = context;
        this.schedules = schedules;
    }

    /**
     * {@inheritDoc}
     *
     * @param position
     * @param convertView
     * @param parent
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.augment_list_item, parent, false);
            holder = new ViewHolder(view, getContext());
            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }
        Schedule schedule = schedules.get(position);
        if (schedule.getIsCurrent().booleanValue()) {
            holder.textViewClassName.setTextColor(Color.WHITE);
            holder.textViewClassStartTime.setTextColor(Color.WHITE);
            holder.textViewClassEndTime.setTextColor(Color.WHITE);
            holder.textViewClassName.setBackgroundColor(Color.parseColor("#1976D2"));
            holder.textViewClassStartTime.setBackgroundColor(Color.parseColor("#1976D2"));
            holder.textViewClassEndTime.setBackgroundColor(Color.parseColor("#1976D2"));
            view.setBackgroundColor(Color.BLACK);
            view.bringToFront();
        }
        else {
           if(count %2 == 0)
           {
            holder.textViewClassName.setTextColor(Color.BLACK);
            holder.textViewClassStartTime.setTextColor(Color.BLACK);
            holder.textViewClassEndTime.setTextColor(Color.BLACK);
            holder.textViewClassName.setBackgroundColor(Color.parseColor("#4DB6AC"));
            holder.textViewClassStartTime.setBackgroundColor(Color.parseColor("#4DB6AC"));
            holder.textViewClassEndTime.setBackgroundColor(Color.parseColor("#4DB6AC"));
            view.setBackgroundColor(Color.BLACK);
               view.setAlpha(0.5f);
            }
            else
           {
               holder.textViewClassName.setTextColor(Color.WHITE);
               holder.textViewClassStartTime.setTextColor(Color.WHITE);
               holder.textViewClassEndTime.setTextColor(Color.WHITE);
               holder.textViewClassName.setBackgroundColor(Color.parseColor("#004D40"));
               holder.textViewClassStartTime.setBackgroundColor(Color.parseColor("#004D40"));
               holder.textViewClassEndTime.setBackgroundColor(Color.parseColor("#004D40"));
               view.setBackgroundColor(Color.BLACK);
               view.setAlpha(0.5f);
           }
            count++;
        }

        holder.textViewClassName.setText(schedule.getClassName());
        holder.textViewClassStartTime.setText(schedule.getStartTime());
        holder.textViewClassEndTime.setText(schedule.getEndTime());
        return view;
    }

    public static class ViewHolder {

        TextView textViewClassName, textViewClassStartTime, textViewClassEndTime;

        public ViewHolder(View view, Context context) {
            textViewClassName = (TextView) view.findViewById(R.id.textViewClassName);
            textViewClassStartTime = (TextView) view.findViewById(R.id.textViewClassStartTime);
            textViewClassEndTime = (TextView) view.findViewById(R.id.textViewClassEndTime);
        }

    }
}
