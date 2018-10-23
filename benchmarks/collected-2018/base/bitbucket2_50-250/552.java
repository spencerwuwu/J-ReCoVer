// https://searchcode.com/api/result/64433129/

package de.fau.fsahoy.android.api10;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import de.fau.fsahoy.android.api10.trip.LogbookRecord;

public class LogbookAdapter extends BaseAdapter {
	private List<LogbookRecord> logbook;
	private final Activity context;

	public LogbookAdapter(Activity context, List<LogbookRecord> logbook) {
		this.context = context;
		this.logbook = logbook;
		if(this.logbook == null)
			this.logbook = new ArrayList<LogbookRecord>();
	}

	public void setLogbook(List<LogbookRecord> logbook) {
		this.logbook = logbook;
	}
	
	// to optimize the drawing (reduce XML parsing)
	static class ViewHolder {
		protected TextView text;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.tripinfo_logbook_record, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.text = (TextView) rowView.findViewById(R.id.tripinfo_logbook_record);
			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		String text = logbook.get(position).getDeparturePlace() + " - " + logbook.get(position).getArrivalPlace();
		text += " (" + logbook.get(position).getNotes() + ")";
		holder.text.setText(text);

		return rowView;
	}

	public int getCount() {
		return logbook.size();
	}

	public Object getItem(int position) {
		return logbook.get(position);
	}

	public long getItemId(int position) {
		return logbook.get(position).getId();
	}
}

