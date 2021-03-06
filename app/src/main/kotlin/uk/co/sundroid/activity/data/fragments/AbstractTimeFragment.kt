package uk.co.sundroid.activity.data.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TimePicker
import kotlinx.android.synthetic.main.inc_timebar.*
import uk.co.sundroid.R
import uk.co.sundroid.activity.Page
import uk.co.sundroid.domain.LocationDetails
import uk.co.sundroid.util.isNotEmpty
import uk.co.sundroid.util.log.e
import uk.co.sundroid.util.prefs.Prefs
import uk.co.sundroid.util.time.formatTimeStr
import uk.co.sundroid.util.view.ButtonDragGestureDetector
import uk.co.sundroid.util.view.ButtonDragGestureDetector.ButtonDragGestureDetectorListener
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar.DAY_OF_MONTH
import java.util.Calendar.DAY_OF_YEAR
import java.util.Calendar.MONTH
import java.util.Calendar.MINUTE
import java.util.Calendar.HOUR_OF_DAY
import java.util.Calendar.YEAR

abstract class AbstractTimeFragment : AbstractDataFragment(), OnSeekBarChangeListener {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val weekdayFormat = SimpleDateFormat("EEEE", Locale.US)

    protected abstract val layout: Int

    protected abstract fun initialise(location: LocationDetails, dateCalendar: Calendar, timeCalendar: Calendar, view: View)

    protected abstract fun update(location: LocationDetails, dateCalendar: Calendar, timeCalendar: Calendar, view: View, timeOnly: Boolean)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View? {
        if (container == null) {
            return null
        }
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        safeInitialise(view)
        safeUpdate(view, false)
    }

    override fun initialise() {
        if (view != null) {
            safeInitialise(view)
        }
    }

    override fun update() {
        if (view != null) {
            safeUpdate(view, false)
        }
    }

    private fun onDateSet(year: Int, month: Int, date: Int) {
        getDateCalendar().set(year, month, date)
        getTimeCalendar().set(year, month, date)
        update()
    }

    private fun onTimeSet(hour: Int, minute: Int) {
        getTimeCalendar().set(HOUR_OF_DAY, hour)
        getTimeCalendar().set(MINUTE, minute)
        update()
    }

    private fun safeInitialise(view: View?) {
        val location = getLocation()
        val dateCalendar = getDateCalendar()
        val timeCalendar = getTimeCalendar()
        try {
            if (view != null && !isDetached) {
                initGestures()
                updateDateAndTime(view, dateCalendar, timeCalendar)
                initialise(location, dateCalendar, timeCalendar, view)
            }
        } catch (e: Exception) {
            e(TAG, "Failed to init data view", e)
        }

    }

    private fun safeUpdate(view: View?, timeOnly: Boolean) {
        val location = getLocation()
        val dateCalendar = getDateCalendar()
        val timeCalendar = getTimeCalendar()
        try {
            if (view != null && !isDetached) {
                updateDateAndTime(view, dateCalendar, timeCalendar)
                update(location, dateCalendar, timeCalendar, view, timeOnly)
            }
        } catch (e: Exception) {
            e(TAG, "Failed to update data view", e)
        }

    }

    private fun initGestures() {
        val dateListener = object : ButtonDragGestureDetectorListener {
            override fun onButtonDragUp() = changeCalendars(MONTH, -1)
            override fun onButtonDragDown() = changeCalendars(MONTH, 1)
            override fun onButtonDragLeft() = changeCalendars(DAY_OF_MONTH, -1)
            override fun onButtonDragRight() = changeCalendars(DAY_OF_MONTH, 1)
        }
        val dateDetector = GestureDetector(requireContext(), ButtonDragGestureDetector(dateListener, requireContext()))
        zoneButton.setOnClickListener { setPage(Page.TIME_ZONE) }
        dateButton.setOnClickListener { showDatePicker() }
        dateButton.setOnTouchListener { _, e -> dateDetector.onTouchEvent(e) }

        val timeListener = object : ButtonDragGestureDetectorListener {
            override fun onButtonDragUp() = prevHour()
            override fun onButtonDragDown() = nextHour()
            override fun onButtonDragLeft() = prevMinute()
            override fun onButtonDragRight() = nextMinute()
        }
        val timeDetector = GestureDetector(requireContext(), ButtonDragGestureDetector(timeListener, requireContext()))
        timeButton.setOnClickListener { showTimePicker() }
        timeButton.setOnTouchListener { _, e -> timeDetector.onTouchEvent(e) }
        timeSeeker.setOnSeekBarChangeListener(this)
    }

    private fun updateDateAndTime(view: View?, dateCalendar: Calendar, timeCalendar: Calendar) {
        val location = getLocation()
        if (view == null) {
            return
        }


        if (Prefs.showTimeZone(requireContext())) {
            show(view, R.id.zoneButton)
            val zone = location.timeZone!!.zone
            val zoneDST = zone.inDaylightTime(Date(dateCalendar.timeInMillis + 12 * 60 * 60 * 1000))
            val zoneName = zone.getDisplayName(zoneDST, TimeZone.LONG)
            text(view, R.id.zoneName, zoneName)

            var zoneCities = location.timeZone!!.getOffset(dateCalendar.timeInMillis + 12 * 60 * 60 * 1000) // Get day's main offset.
            if (isNotEmpty(location.timeZone!!.cities)) {
                zoneCities += " " + location.timeZone!!.cities!!
            }
            text(view, R.id.zoneCities, zoneCities)
        } else {
            remove(view, R.id.zoneButton)
        }

        val time = formatTimeStr(requireContext(), timeCalendar, allowSeconds = false, allowRounding = false)
        show(view, R.id.timeHM, time)

        dateFormat.timeZone = dateCalendar.timeZone
        weekdayFormat.timeZone = dateCalendar.timeZone
        val date = dateFormat.format(Date(dateCalendar.timeInMillis))
        val weekday = weekdayFormat.format(Date(dateCalendar.timeInMillis))
        show(view, R.id.dateDMY, date)
        show(view, R.id.dateWeekday, weekday)

        val minutes = timeCalendar.get(HOUR_OF_DAY) * 60 + timeCalendar.get(MINUTE)
        (view.findViewById<View>(R.id.timeSeeker) as SeekBar).progress = minutes
    }

    private fun showDatePicker() {
        val calendar = getDateCalendar()
        val today = Calendar.getInstance(calendar.timeZone)
        val listener = { _: DatePicker, y: Int, m: Int, d: Int -> onDateSet(y, m, d) }
        val dialog = DatePickerDialog(requireContext(), listener, calendar.get(YEAR), calendar.get(MONTH), calendar.get(DAY_OF_MONTH))
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Today") { _, _ -> onDateSet(today.get(YEAR), today.get(MONTH), today.get(DAY_OF_MONTH))}
        dialog.show()
    }

    private fun showTimePicker() {
        val calendar = getTimeCalendar()
        val now = Calendar.getInstance(calendar.timeZone)
        val listener = { _: TimePicker, h: Int, m: Int -> onTimeSet(h, m) }
        val dialog = TimePickerDialog(activity, listener, calendar.get(HOUR_OF_DAY), calendar.get(MINUTE), prefs.clockType24())
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Now") { _, _ -> onTimeSet(now.get(HOUR_OF_DAY), now.get(MINUTE))}
        dialog.show()
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            val hours = progress / 60
            val minutes = progress - hours * 60
            getTimeCalendar().set(HOUR_OF_DAY, hours)
            getTimeCalendar().set(MINUTE, minutes)
            safeUpdate(view, false)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        // No action.
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        // No action.
    }

    private fun changeCalendars(field: Int, diff: Int) {
        arrayOf(getDateCalendar(), getTimeCalendar()).forEach { it.add(field, diff) }
        update()
    }

    private fun nextMinute() {
        if (view != null) {
            val dayOfYear = getTimeCalendar().get(DAY_OF_YEAR)
            getTimeCalendar().add(MINUTE, 1)
            val minutes = getTimeCalendar().get(HOUR_OF_DAY) * 60 + getTimeCalendar().get(MINUTE)
            (view!!.findViewById<View>(R.id.timeSeeker) as SeekBar).progress = minutes
            if (getTimeCalendar().get(DAY_OF_YEAR) != dayOfYear) {
                getDateCalendar().add(DAY_OF_MONTH, 1)
                safeUpdate(view, false)
            } else {
                safeUpdate(view, true)
            }
        }
    }

    private fun prevMinute() {
        if (view != null) {
            val dayOfYear = getTimeCalendar().get(DAY_OF_YEAR)
            getTimeCalendar().add(MINUTE, -1)
            val minutes = getTimeCalendar().get(HOUR_OF_DAY) * 60 + getTimeCalendar().get(MINUTE)
            (view!!.findViewById<View>(R.id.timeSeeker) as SeekBar).progress = minutes
            if (getTimeCalendar().get(DAY_OF_YEAR) != dayOfYear) {
                getDateCalendar().add(DAY_OF_MONTH, -1)
                safeUpdate(view, false)
            } else {
                safeUpdate(view, true)
            }
        }
    }

    private fun nextHour() {
        if (view != null) {
            val dayOfYear = getTimeCalendar().get(DAY_OF_YEAR)
            getTimeCalendar().add(HOUR_OF_DAY, 1)
            val minutes = getTimeCalendar().get(HOUR_OF_DAY) * 60 + getTimeCalendar().get(MINUTE)
            (view!!.findViewById<View>(R.id.timeSeeker) as SeekBar).progress = minutes
            if (getTimeCalendar().get(DAY_OF_YEAR) != dayOfYear) {
                getDateCalendar().add(DAY_OF_MONTH, 1)
                safeUpdate(view, false)
            } else {
                safeUpdate(view, true)
            }
        }
    }

    private fun prevHour() {
        if (view != null) {
            val dayOfYear = getTimeCalendar().get(DAY_OF_YEAR)
            getTimeCalendar().add(HOUR_OF_DAY, -1)
            val minutes = getTimeCalendar().get(HOUR_OF_DAY) * 60 + getTimeCalendar().get(MINUTE)
            (view!!.findViewById<View>(R.id.timeSeeker) as SeekBar).progress = minutes
            if (getTimeCalendar().get(DAY_OF_YEAR) != dayOfYear) {
                getDateCalendar().add(DAY_OF_MONTH, -1)
                safeUpdate(view, false)
            } else {
                safeUpdate(view, true)
            }
        }
    }

    companion object {

        private val TAG = AbstractTimeFragment::class.java.simpleName
    }

}
