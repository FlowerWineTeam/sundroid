package uk.co.sundroid.activity.location

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

import uk.co.sundroid.AbstractActivity
import uk.co.sundroid.R
import uk.co.sundroid.domain.LocationDetails
import uk.co.sundroid.domain.TimeZoneDetail
import uk.co.sundroid.util.prefs.SharedPrefsHelper
import uk.co.sundroid.util.log.*
import uk.co.sundroid.util.view.MergeAdapter
import uk.co.sundroid.util.time.TimeZoneResolver

import java.util.ArrayList
import java.util.TreeSet

class TimeZonePickerActivity : AbstractActivity(), OnItemClickListener {

    private var location: LocationDetails? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        location = SharedPrefsHelper.getSelectedLocation(this)
        val mode = intent.getIntExtra(INTENT_MODE, MODE_SELECT)

        setContentView(R.layout.zone)
        if (mode == MODE_SELECT) {
            setActionBarTitle(location!!.displayName, "Select time zone")
        } else {
            setActionBarTitle(location!!.displayName, "Change time zone")
            setDisplayHomeAsUpEnabled()
        }

        val list = findViewById<ListView>(R.id.timeZoneList)
        val adapter = MergeAdapter()
        if (location!!.possibleTimeZones != null && location!!.possibleTimeZones!!.size > 0 && location!!.possibleTimeZones!!.size < 20) {
            adapter.addView(View.inflate(this, R.layout.zone_best_header, null))
            val sortedTimeZones = TreeSet(location!!.possibleTimeZones!!)
            adapter.addAdapter(TimeZoneAdapter(ArrayList(sortedTimeZones)))
            adapter.addView(View.inflate(this, R.layout.zone_all_header, null))
        }

        val sortedTimeZones = TreeSet(TimeZoneResolver.getAllTimeZones())
        adapter.addAdapter(TimeZoneAdapter(ArrayList(sortedTimeZones)))

        list.adapter = adapter
        list.onItemClickListener = this
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        d(TAG, "onItemClick($position, $id)")

        // Update the passed location and update the current location.
        val timeZone = parent.getItemAtPosition(position) as TimeZoneDetail
        location!!.timeZone = timeZone
        SharedPrefsHelper.saveSelectedLocation(this, location!!)
        setResult(RESULT_TIMEZONE_SELECTED)
        finish()
    }

    override fun onNavBackSelected() {
        setResult(RESULT_CANCELLED)
        finish()
    }

    private inner class TimeZoneAdapter constructor(list: ArrayList<TimeZoneDetail>) : ArrayAdapter<TimeZoneDetail>(this@TimeZonePickerActivity, R.layout.zone_row, list) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var row = convertView
            if (row == null) {
                val inflater = layoutInflater
                row = inflater.inflate(R.layout.zone_row, parent, false)
            }
            val offset = row!!.findViewById<TextView>(R.id.timeZoneRowOffset)
            offset.text = getItem(position)!!.getOffset(System.currentTimeMillis())
            val cities = row.findViewById<TextView>(R.id.timeZoneRowCities)
            cities.text = getItem(position)!!.cities
            return row
        }

    }

    companion object {

        private val TAG = TimeZonePickerActivity::class.java.simpleName

        val REQUEST_TIMEZONE = 1111
        val RESULT_TIMEZONE_SELECTED = 2221
        val RESULT_CANCELLED = 2222


        val INTENT_MODE = "mode"

        val MODE_SELECT = 1
        val MODE_CHANGE = 2
    }

}
