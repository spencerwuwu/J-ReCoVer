// https://searchcode.com/api/result/64433068/

package de.fau.fsahoy.android.api10;

import java.util.List;

import de.fau.fsahoy.android.api10.trip.Trip;
import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class TripAdapter extends BaseAdapter {

	private List<Trip> trips;
	private final Activity context;

	public TripAdapter(Activity context, List<Trip> trips) {
		//super(context, R.layout.mytrips_trip_item, trips);
		this.context = context;
		this.trips = trips;
	}

	public void setTrips(List<Trip> trips) {
		this.trips = trips;
	}
	
	// to optimize the drawing (reduce XML parsing)
	static class ViewHolder {
		protected TextView text;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		
		View rowView = convertView;
		if (rowView == null) {
			LayoutInflater inflater = context.getLayoutInflater();
			rowView = inflater.inflate(R.layout.mytrips_trip_item, null);
			ViewHolder viewHolder = new ViewHolder();
			viewHolder.text = (TextView) rowView.findViewById(R.id.mytrips_item);
			rowView.setTag(viewHolder);
		}

		ViewHolder holder = (ViewHolder) rowView.getTag();
		String text = trips.get(position).getName() + " (" + trips.get(position).getDescription() + ")";
		holder.text.setText(text);

		return rowView;
	}

	public int getCount() {
		return trips.size();
	}
	
	public Object getItem(int position) {
		return trips.get(position);
	}

	public long getItemId(int position) {
		return trips.get(position).getId();
	}
	
}
