package com.indieteam.mytask.views

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.core.content.ContextCompat
import android.text.style.LineBackgroundSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.swipe.library.rx2.Swipe
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.indieteam.mytask.R
import com.indieteam.mytask.views.adapters.ScheduleAdapter
import com.indieteam.mytask.models.StudentCalendarCollection
import com.indieteam.mytask.models.TestScheduleCollection
import com.indieteam.mytask.models.TimeScheduleDetails
import com.indieteam.mytask.datas.InternetState
import com.indieteam.mytask.datas.datasources.domHTML.DomUpdateSchedule
import com.indieteam.mytask.datas.GoogleCalendar
import com.indieteam.mytask.datas.GoogleSignOut
import com.indieteam.mytask.datas.datasources.parseData.ParseScheduleJson
import com.indieteam.mytask.datas.notifications.AppNotification
import com.indieteam.mytask.datas.datasources.domHTML.DomSemesterSchedule
import com.indieteam.mytask.datas.services.AppService
import com.indieteam.mytask.datas.SqLite
import com.indieteam.mytask.views.fragments.*
import com.indieteam.mytask.views.interfaces.OnDomTestScheduleListener
import com.indieteam.mytask.views.interfaces.OnLoginListener
import com.indieteam.mytask.views.interfaces.OnSemesterScheduleListener
import com.indieteam.mytask.views.interfaces.OnSyncListener
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView.SELECTION_MODE_SINGLE
import kotlinx.android.synthetic.main.activity_week.*
import kotlinx.android.synthetic.main.fragment_process_bar.*
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION")
class WeekActivity : AppCompatActivity() {

    // Google oauth2
    lateinit var credential: GoogleAccountCredential
    var scope = Scope("https://www.googleapis.com/auth/calendar")
    var scope2 = Scope("https://www.googleapis.com/auth/calendar.events")
    var RC_SIGN_IN = 2
    val httpTransport = AndroidHttp.newCompatibleTransport()
    var jsonFactory = GsonFactory.getDefaultInstance()
    lateinit var service: com.google.api.services.calendar.Calendar
    var appName = "mystask-calendar"
    lateinit var gso: GoogleSignInOptions
    lateinit var mGoogleSignInClient: GoogleSignInClient
    lateinit var signInIntent: Intent
    lateinit var modifyDialog: ModifyDialog
    private val REQUEST_ACCOUNT = 1
    private lateinit var sqLite: SqLite
    private var scheduleJson: JSONObject? = null
    var parseScheduleJson: ParseScheduleJson? = null
    var dots = mutableMapOf<CalendarDay, Int>()
    var studentScheduleObjArr = ArrayList<StudentCalendarCollection>()
    private val dateStart = CalendarDay.from(Calendar.getInstance().get(Calendar.YEAR) - 1, 0, 1)
    private val dateEnd = CalendarDay.from(Calendar.getInstance().get(Calendar.YEAR) + 1, 11, 31)
    private lateinit var scheduleAdapter: ScheduleAdapter
    private var isAccountPermission = 0
    private val swipe = Swipe()
    private lateinit var customSwipe: CustomSwipe
    private var screenHeight = 0
    private var calendarHeight = 0
    private var statusBarHeight = 0
    private var navigationBarHeight = 0
    private var layoutCalendarMode = 0
    lateinit var sharedPref: SharedPreferences
    private val timeDetails = TimeScheduleDetails()
    private lateinit var internetState: InternetState
    private lateinit var appNotification: AppNotification
    private lateinit var addScheduleFragment: AddScheduleFragment
    private val testScheduleCollection = ArrayList<TestScheduleCollection>()
    private val monthHeightRatio = 35f
    private val weekHeightRatio = 12f
    private lateinit var googleCalendar: GoogleCalendar

    private val onLoginListener = object : OnLoginListener {
        override fun onLogin() {
        }

        override fun onSuccess(username: String, password: String, cookie: String, sessionUrl: String) {
            try {
                sqLite.updateInfo(username, password, cookie)
            } catch (e: SQLiteConstraintException) {
                e.printStackTrace()
            }
            DomSemesterSchedule(this@WeekActivity, sessionUrl, cookie, onSemesterScheduleListener).start()
        }

        override fun onFail() {
        }

        override fun onThrow(t: String) {
            runOnUiThread {
                supportFragmentManager.findFragmentByTag("processBarUpdate")?.let {
                    supportFragmentManager.beginTransaction().remove(it)
                            .commit()
                }
                visible()
                Toast.makeText(this@WeekActivity, t, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val onSemesterScheduleListener = object : OnSemesterScheduleListener {
        override fun onSuccess(semester: String, sessionUrl: String, signIn: String) {
            val bundle = Bundle()
            bundle.putString("semester", semester)
            bundle.putString("sessionUrl", sessionUrl)
            bundle.putString("signIn", signIn)

            val selectSemesterFragment = SelectSemesterScheduleFragment()
            selectSemesterFragment.arguments = bundle

            supportFragmentManager.findFragmentByTag("processBarUpdate")?.let {
                supportFragmentManager.beginTransaction().remove(it)
                        .commit()
            }

            if (supportFragmentManager.findFragmentByTag("selectSemesterFragment") == null) {
                supportFragmentManager.beginTransaction().add(R.id.calendar_root_view, selectSemesterFragment, "selectSemesterFragment")
                        .commit()
            }
        }

        override fun onSemesterSchedule() {
            runOnUiThread {
                supportFragmentManager.findFragmentByTag("processBarLogin")?.let {
                    runOnUiThread {
                        it.process.text = "Tải học kỳ..."
                    }
                }
            }
        }

        override fun onThrow(t: String) {
            runOnUiThread {
                supportFragmentManager.findFragmentByTag("processBarUpdate")?.let {
                    supportFragmentManager.beginTransaction().remove(it)
                            .commit()
                }
                visible()
                Toast.makeText(this@WeekActivity, t, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onDomTestSchedule = object : OnDomTestScheduleListener {
        override fun onDone(testScheduleCollection: ArrayList<TestScheduleCollection>) {
            this@WeekActivity.testScheduleCollection.clear()
            this@WeekActivity.testScheduleCollection.addAll(testScheduleCollection)
            runOnUiThread {
                if (supportFragmentManager.findFragmentByTag("selectTestScheduleFragment") != null) {
                    supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentByTag("selectTestScheduleFragment")!!)
                            .commit()
                }
                if (supportFragmentManager.findFragmentByTag("testScheduleFragment") == null) {
                    supportFragmentManager.beginTransaction().add(R.id.calendar_root_view, TestScheduleFragment(), "testScheduleFragment")
                            .commit()
                }
            }
        }

        override fun onFail(t: String) {
        }

        override fun onThrow(t: String) {
        }
    }

    private val onSyncListener = object : OnSyncListener {
        override fun onDone(m: String) {
            runOnUiThread {
                Toast.makeText(this@WeekActivity, m, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onState(s: String) {
            runOnUiThread {
                Toast.makeText(this@WeekActivity, s, Toast.LENGTH_SHORT).show()
            }
        }

        override fun onFail(t: String, m: String) {
            runOnUiThread {
                Toast.makeText(this@WeekActivity, m, Toast.LENGTH_SHORT).show()
            }
        }
    }

    inner class OnSwipeListener : com.github.pwittchen.swipe.library.rx2.SwipeListener {
        private var startTouchY = 0f

        override fun onSwipedUp(event: MotionEvent?): Boolean {
            return true
        }

        override fun onSwipedDown(event: MotionEvent?): Boolean {
            return true
        }

        override fun onSwipingUp(event: MotionEvent?) {
        }

        override fun onSwipedRight(event: MotionEvent?): Boolean {
            if (startTouchY > content_layout.y && event!!.y > content_layout.y) {
                customSwipe.right()
            }
            countSwipeRight = 0
            return true
        }

        private var countSwipeLeft = 0
        override fun onSwipingLeft(event: MotionEvent?) {
            if (countSwipeLeft == 0) {
                event?.let {
                    startTouchY = event.y
                }
            }
            countSwipeLeft++
        }

        private var countSwipeRight = 0
        override fun onSwipingRight(event: MotionEvent?) {
            if (countSwipeRight == 0) {
                event?.let {
                    startTouchY = event.y
                }
            }
            countSwipeRight++
        }

        override fun onSwipingDown(event: MotionEvent?) {
        }

        override fun onSwipedLeft(event: MotionEvent?): Boolean {
            if (startTouchY > content_layout.y && event!!.y > content_layout.y) {
                customSwipe.left()
            }
            countSwipeLeft = 0
            return true
        }

    }

    inner class DrawDots(private val colors: List<Int>, private val length: Int) : LineBackgroundSpan {

        override fun drawBackground(c: Canvas, p: Paint,
                                    left: Int, right: Int, top: Int,
                                    baseline: Int, bottom: Int,
                                    charSequence: CharSequence,
                                    start: Int, end: Int, lineNum: Int) {
            val lastColor = p.color
            var newLength = 0
            if (length == 1) {
                p.color = colors[0]
                newLength = length
            }
            if (length == 2) {
                p.color = colors[1]
                newLength = length
            }
            if (length in 3..5) {
                p.color = colors[2]
                newLength = length
            }
            if (length > 5) {
                p.color = colors[2]
                newLength = 5
            }
            var totalWidth = 0f
            for (i in 0 until newLength)
                if (i != 0)
                    totalWidth += (right.toFloat() / 100f) * 7.5f // 7.5 is space (percent) margin left of dots
            var cX = right / 2f - totalWidth / 2
            for (i in 0 until newLength) {
                c.drawCircle(cX, bottom.toFloat() + (bottom.toFloat() / 100f) * 10f, (right.toFloat() / 100f) * 2.1f, p)
                cX += (right.toFloat() / 100f) * 7.5f
            }
            p.color = lastColor
        }
    }

    inner class EventDecorator(private val mode: String, private val colors: List<Int>, val date: CalendarDay, private val dot: Int) : DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean {
            return date == day
        }

        override fun decorate(view: DayViewFacade) {
            if (mode == "Dots")
                view.addSpan(DrawDots(colors, dot))
            if (mode == "ToDay") {
                view.apply {
                    setBackgroundDrawable(resources.getDrawable(R.drawable.shape_bg_cal_today))
                }
            }
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        try {
            swipe.dispatchTouchEvent(event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_ACCOUNT) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isAccountPermission = 1
                googleCalendar = GoogleCalendar(applicationContext, this, onSyncListener)
                googleCalendar.start()
            } else {
                Toast.makeText(this@WeekActivity, "Permissions is not granted", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setCalendarDots() {
        for (i in dots) {
            calendarView.addDecorator(
                    EventDecorator("Dots",
                            listOf(resources.getColor(R.color.colorBlue), resources.getColor(R.color.colorBlueDark), resources.getColor(R.color.colorOrange)),
                            i.key, i.value))
        }
    }

    private fun drawBackgroundToday() {
        calendarView.addDecorator(EventDecorator("ToDay", listOf(resources.getColor(R.color.colorGrayDark)), CalendarDay.today(), -1))
    }


    private fun checkAccountPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.GET_ACCOUNTS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.GET_ACCOUNTS), REQUEST_ACCOUNT)
            } else {
                isAccountPermission = 1
            }
        } else {
            isAccountPermission = 1
        }
    }

    private fun init() {
        sqLite = SqLite(this)
        customSwipe = CustomSwipe(this)
        calenderEvents()
        title = ""
        val point = Point()
        windowManager.defaultDisplay.getSize(point)
        screenHeight = point.y
        val resourcesId = resources.getIdentifier("status_bar_height", "dimen", "android")
        statusBarHeight = resources.getDimensionPixelSize(resourcesId)
        val resourcesId2 = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        navigationBarHeight = resources.getDimensionPixelSize(resourcesId2)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        layoutCalendarMode = sharedPref.getInt("CalendarMode", 0)
        scheduleAdapter = ScheduleAdapter(this@WeekActivity, studentScheduleObjArr)
        calender_list_view.adapter = scheduleAdapter
        internetState = InternetState(this)
        appNotification = AppNotification(this)
        modifyDialog = ModifyDialog(this)

        googleCalendar = GoogleCalendar(applicationContext, this, onSyncListener)
    }

    fun preDate() {
        val calendarSelected = Calendar.getInstance()
        calendarSelected.set(calendarView.selectedDate.year, calendarView.selectedDate.month, calendarView.selectedDate.day)
        calendarSelected.add(Calendar.DAY_OF_MONTH, -1)
        val newCalendarDate = CalendarDay.from(calendarSelected)
        calendarView.currentDate = newCalendarDate
        calendarView.selectedDate = newCalendarDate

        val date = "${newCalendarDate.day}/${newCalendarDate.month + 1}/${newCalendarDate.year}"
        updateListView(date)
    }

    fun nextDate() {
        val calendarSelected = Calendar.getInstance()
        calendarSelected.set(calendarView.selectedDate.year, calendarView.selectedDate.month, calendarView.selectedDate.day)
        calendarSelected.add(Calendar.DAY_OF_MONTH, 1)
        val newCalendarDate = CalendarDay.from(calendarSelected)
        calendarView.currentDate = newCalendarDate
        calendarView.selectedDate = newCalendarDate

        val date = "${newCalendarDate.day}/${newCalendarDate.month + 1}/${newCalendarDate.year}"
        updateListView(date)
    }

    private fun calendarSetting() {
        calendarView.topbarVisible = false

        calendarView.state().edit().setMinimumDate(dateStart)
                .setMaximumDate(dateEnd)
                .commit()

        calendar_root_view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                calendar_root_view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                if (layoutCalendarMode == 0) {
                    setCalendarWeekHeight()
                    calendarView.state().edit()
                            .setCalendarDisplayMode(CalendarMode.WEEKS)
                            .commit()
                } else {
                    setCalendarMonthHeight()
                    calendarView.state().edit()
                            .setCalendarDisplayMode(CalendarMode.MONTHS)
                            .commit()
                }
            }
        })

        calendarView.currentDate = CalendarDay.today()
        calendarView.selectedDate = CalendarDay.today()
        calendarView.selectionMode = SELECTION_MODE_SINGLE
    }

    private fun setCalendarWeekHeight() {
        calendarHeight = ((screenHeight / 100) * weekHeightRatio).toInt();
        calendarView.layoutParams.height = calendarHeight;
        content_layout.layoutParams.height = screenHeight - calendarHeight - statusBarHeight
    }

    private fun setCalendarMonthHeight() {
        calendarHeight = ((screenHeight / 100) * monthHeightRatio).toInt();
        calendarView.layoutParams.height = calendarHeight
        content_layout.layoutParams.height = screenHeight - calendarHeight - statusBarHeight
    }

    private fun calenderEvents() {
        calendarView.setOnDateChangedListener { _, calendarDay, b ->
            val date = "${calendarDay.day}/${calendarDay.month + 1}/${calendarDay.year}"
            updateListView(date)
        }
        calendarView.setOnDateLongClickListener { _, calendarDay ->
            gone()
            val date = "${calendarDay.day}/${calendarDay.month + 1}/${calendarDay.year}"
            val bundle = Bundle()
            bundle.putString("date", date)
            addScheduleFragment = AddScheduleFragment()
            addScheduleFragment.arguments = bundle
            supportFragmentManager.beginTransaction().add(R.id.calendar_root_view, addScheduleFragment, "addScheduleFragment")
                    .commit()
        }
        calendarView.setOnMonthChangedListener { _, calendarDay ->
            calendarView.setTitleFormatter { "Tháng ${calendarDay.month + 1} Năm ${calendarDay.year}" }
        }
    }

    private fun updateListView(date: String) {
        val day = date.substring(0, date.indexOf("/")).toInt()
        val month = date.substring(date.indexOf("/") + 1, date.lastIndexOf("/")).toInt()
        //val year = date.substring(date.lastIndexOf("/") + 1, date.length).toInt()

        if (parseScheduleJson != null) {
            studentScheduleObjArr.removeAll(studentScheduleObjArr)
            parseScheduleJson!!.apply {
                getSubject(date)
                if (!subjectName.isEmpty() &&
                        !subjectTime.isEmpty() &&
                        !subjectPlace.isEmpty() &&
                        !teacher.isEmpty()) {
                    if (subjectName.size == subjectTime.size &&
                            subjectName.size == subjectPlace.size &&
                            subjectName.size == teacher.size) {
                        for (j in 0 until subjectName.size) {
                            var firstTime: Int
                            var endTime: Int
                            if (subjectTime[j].indexOf(",") > -1) {
                                firstTime = subjectTime[j].substring(0, subjectTime[j].indexOf(",")).toInt() - 1
                                endTime = subjectTime[j].substring(subjectTime[j].lastIndexOf(",") + 1, subjectTime[j].length).toInt() - 1
                                if (CalendarDay.from(2020, month, day).date >= CalendarDay.from(2020, 4, 15).date &&
                                        CalendarDay.from(2020, month, day).date < CalendarDay.from(2020, 10, 15).date)
                                    studentScheduleObjArr.add(StudentCalendarCollection(subjectName[j], /*subjectDate[j]*/"", subjectTime[j] + " (${timeDetails.timeSummerArr[firstTime].timeIn} -> ${timeDetails.timeSummerArr[endTime].timeOut})", subjectPlace[j], teacher[j]))
                                else
                                    studentScheduleObjArr.add(StudentCalendarCollection(subjectName[j], /*subjectDate[j]*/"", subjectTime[j] + " (${timeDetails.timeWinterArr[firstTime].timeIn} -> ${timeDetails.timeWinterArr[endTime].timeOut})", subjectPlace[j], teacher[j]))
                            } else {
                                firstTime = subjectTime[j].toInt() - 1
                                if (CalendarDay.from(2020, month, day).date >= CalendarDay.from(2020, 4, 15).date &&
                                        CalendarDay.from(2020, month, day).date < CalendarDay.from(2020, 10, 15).date)
                                    studentScheduleObjArr.add(StudentCalendarCollection(subjectName[j], /*subjectDate[j]*/"", subjectTime[j] + " (${timeDetails.timeSummerArr[firstTime].timeIn} -> ${timeDetails.timeSummerArr[firstTime].timeOut})", subjectPlace[j], teacher[j]))
                                else
                                    studentScheduleObjArr.add(StudentCalendarCollection(subjectName[j], /*subjectDate[j]*/"", subjectTime[j] + " (${timeDetails.timeWinterArr[firstTime].timeIn} -> ${timeDetails.timeWinterArr[firstTime].timeOut})", subjectPlace[j], teacher[j]))
                            }
                        }
                        scheduleAdapter.notifyDataSetChanged()
                    }
                } else {
                    // Nghi
                    scheduleAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status))
                googleApiAvailability.getErrorDialog(this, status, 2404).show()

            return false
        }
        return true
    }

    private fun initFloatButton() {
        val listItem =
                listOf(/*SpeedDialActionItem.Builder(R.id.fab_donate, R.drawable.ic_info)
                        .setLabel("G.thiệu")
                        .setLabelColor(resources.getColor(R.color.colorBlue))
                        .setLabelBackgroundColor(resources.getColor(R.color.colorWhite))
                        .setFabBackgroundColor(resources.getColor(R.color.colorWhite))
                        .create(),*/
                        SpeedDialActionItem.Builder(R.id.fab_calendar_mode, R.drawable.ic_switch)
                                .setLabel("Tuần/Tháng")
                                .setLabelColor(resources.getColor(R.color.colorBlue))
                                .setLabelBackgroundColor(resources.getColor(R.color.colorWhite))
                                .setFabBackgroundColor(resources.getColor(R.color.colorWhite))
                                .create(),
                        SpeedDialActionItem.Builder(R.id.fab_sync_google, R.drawable.ic_cloud_upload_24dp)
                                .setLabel("Tải lên Google Calendar")
                                .setLabelColor(resources.getColor(R.color.colorBlue))
                                .setLabelBackgroundColor(resources.getColor(R.color.colorWhite))
                                .setFabBackgroundColor(resources.getColor(R.color.colorWhite))
                                .create(),
                        SpeedDialActionItem.Builder(R.id.fab_update, R.drawable.ic_update)
                                .setLabel("C.nhật lịch học")
                                .setLabelColor(resources.getColor(R.color.colorBlue))
                                .setLabelBackgroundColor(resources.getColor(R.color.colorWhite))
                                .setFabBackgroundColor(resources.getColor(R.color.colorWhite))
                                .create(),
//                        SpeedDialActionItem.Builder(R.id.fab_test, R.drawable.ic_schedule_24dp)
//                                .setLabel("Lịch thi (Beta)")
//                                .setLabelColor(resources.getColor(R.color.colorPurpleDark))
//                                .setLabelBackgroundColor(resources.getColor(R.color.colorWhite))
//                                .setFabBackgroundColor(resources.getColor(R.color.colorWhite))
//                                .create(),
                        SpeedDialActionItem.Builder(R.id.fab_info, R.drawable.ic_profile)
                                .setLabel("C.nhân/QR")
                                .setLabelColor(resources.getColor(R.color.colorBlue))
                                .setLabelBackgroundColor(resources.getColor(R.color.colorWhite))
                                .setFabBackgroundColor(resources.getColor(R.color.colorWhite))
                                .create()

                )
        float_button.addAllActionItems(listItem)

        float_button.setOnActionSelectedListener { it ->
            when (it.id) {
                R.id.fab_calendar_mode -> {
                    if (layoutCalendarMode == 1) {
                        setCalendarWeekHeight()
                        calendarView.state().edit()
                                .setCalendarDisplayMode(CalendarMode.WEEKS)
                                .commit()
                        sharedPref.apply {
                            with(edit()) {
                                putInt("CalendarMode", 0)
                                apply()
                            }
                        }
                        layoutCalendarMode = sharedPref.getInt("CalendarMode", 0)
                    } else {
                        setCalendarMonthHeight()
                        calendarView.state().edit()
                                .setCalendarDisplayMode(CalendarMode.MONTHS)
                                .commit()
                        sharedPref.apply {
                            with(edit()) {
                                putInt("CalendarMode", 1)
                                apply()
                            }
                        }
                        layoutCalendarMode = sharedPref.getInt("CalendarMode", 0)
                    }
                }
                R.id.fab_info -> {
                    val intent = Intent(this@WeekActivity, StudentInfoActivity::class.java)
                    startActivity(intent)
                }
                R.id.fab_sync_google -> {

                    if (internetState.state()) {
                        if (isGooglePlayServicesAvailable()) {
                            checkAccountPermission()
                            if (isAccountPermission == 1) {
                                val email = sqLite.readEmail()
                                if (email.isNotBlank()) {
                                    appNotification.syncStart()
                                }
                                googleCalendar = GoogleCalendar(applicationContext, this, onSyncListener)
                                googleCalendar.start()
                            }
                        }
                    } else
                        Toast.makeText(this, "Mất kết nối", Toast.LENGTH_SHORT).show()
                }
                R.id.fab_test -> {
                    if (internetState.state()) {
                        gone()
                        if (supportFragmentManager.findFragmentByTag("selectSemesterFragment") == null) {
                            supportFragmentManager.beginTransaction().add(R.id.calendar_root_view, SelectSemesterTestFragment(), "selectTestScheduleFragment")
                                    .commit()
                            supportFragmentManager.executePendingTransactions()
                        }

                        if (supportFragmentManager.findFragmentByTag("processBarUpdate") == null) {
                            supportFragmentManager.beginTransaction().add(R.id.calendar_root_view, ProcessBarFragment(), "processBarUpdate")
                                    .commit()
                            supportFragmentManager.executePendingTransactions()
                        }

                        supportFragmentManager.findFragmentByTag("processBarUpdate")?.apply {
                            process?.text = "Tải lịch thi..."
                        }
                    } else
                        Toast.makeText(this, "Mất kết nối", Toast.LENGTH_SHORT).show()
                }
                R.id.fab_update -> {
                    if (internetState.state()) {
                        gone()
                        if (supportFragmentManager.findFragmentByTag("processBarUpdate") == null) {
                            supportFragmentManager.beginTransaction().add(R.id.calendar_root_view, ProcessBarFragment(), "processBarUpdate")
                                    .commit()
                            supportFragmentManager.executePendingTransactions()
                        }

                        supportFragmentManager.findFragmentByTag("processBarUpdate")?.apply {
                            process?.text = "Tải học kỳ..."
                        }
                        try {
                            DomUpdateSchedule(this, sqLite.readCookie(), onLoginListener).start()
                        } catch (e: Exception) {
                            visible()
                            supportFragmentManager.findFragmentByTag("processBarUpdate")?.let {
                                supportFragmentManager.beginTransaction().remove(it).commit()
                            }
                            Toast.makeText(this, "Err update", Toast.LENGTH_SHORT).show()
                            e.printStackTrace()
                        }
                    } else
                        Toast.makeText(this, "Mất kết nối", Toast.LENGTH_SHORT).show()
                }
              /*  R.id.fab_donate -> {
                    val intent = Intent(this, AboutActivity::class.java)
                    startActivity(intent)
                }*/
            }
            false //false to close float button
        }
    }

    fun gone() {
        calendarView.visibility = GONE
        content_layout.visibility = GONE
        float_button.visibility = GONE
    }

    fun visible() {
        calendarView.visibility = VISIBLE
        content_layout.visibility = VISIBLE
        float_button.visibility = VISIBLE
    }

    private fun moveToLogin() {
        val intent = Intent(this@WeekActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun toDay() {
        calendarView.currentDate = CalendarDay.today()
        calendarView.selectedDate = CalendarDay.today()
        val date = "${CalendarDay.today().day}/${CalendarDay.today().month + 1}/${CalendarDay.today().year}"
        updateListView(date)
    }

    private fun selectedDate(date: String) {
        val day = date.substring(0, date.indexOf("/")).toInt()
        val month = date.substring(date.indexOf("/") + 1, date.lastIndexOf("/")).toInt()
        val year = date.substring(date.lastIndexOf("/") + 1, date.length).toInt()
        calendarView.currentDate = CalendarDay.today()
        calendarView.selectedDate = CalendarDay.from(year, month - 1, day)
        updateListView("$day/$month/$year")
    }

    private fun startService() {
        val intent = Intent(this, AppService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(this, intent)
        else
            startService(intent)
    }

    private fun checkServiceRunning(): Boolean {
        try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (services in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (AppService::class.java.name == services.service.className) {
                    Log.d("service", "running")
                    Log.d("service_name", services.service.className.toString())
                    return true
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Log.d("service", "not running")
        return false
    }

    private fun checkCompatible(): Boolean {
        val valueDb = sqLite.readSchedule()
        val studentCalendar = JSONObject(valueDb)
        val jsonArray = studentCalendar.getJSONArray("calendar")

        for (i in 0 until jsonArray.length()) {
            try {
                jsonArray.getJSONObject(i).getString("subjectId")
                sqLite.readEmail()
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return false
    }

    private fun run() {
        //changeBackground()
        var readDb: Int
        var valueDb = ""

        try {
            valueDb = sqLite.readSchedule()
            readDb = 1
            Log.d("readdb", "readSchedule db done")
        } catch (e: Exception) {
            readDb = 0
            Log.d("readdb", "db is not exits, cannot readSchedule")
            Log.d("err", e.toString())
        }

        if (readDb == 0) {
            moveToLogin()
        } else {
            if (checkCompatible()) {
                scheduleJson = JSONObject(valueDb)
                parseScheduleJson = ParseScheduleJson(scheduleJson!!)

                initFloatButton()

                if (intent.getStringExtra("date") != null)
                    selectedDate(intent.getStringExtra("date"))
                else
                    toDay()

                drawBackgroundToday()
                dots = parseScheduleJson!!.initDots()
                setCalendarDots()
                swipe.setListener(OnSwipeListener())
                Log.d("service", checkServiceRunning().toString())
                if (!checkServiceRunning())
                    startService()
            } else {
                try {
                    sqLite.dropAll()
                    if (checkServiceRunning())
                        stopService(Intent(this, AppService::class.java))

                    GoogleSignOut(applicationContext).signOut()
                    moveToLogin()
                } catch (e: Exception) {
                    Log.d("Err", e.toString())
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("ResultCode", "$resultCode")
        Log.d("ResultOk", "${Activity.RESULT_OK}")
        when (requestCode) {
            RC_SIGN_IN -> {
                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(this@WeekActivity, "Chưa chọn tài khoản để đồng bộ", Toast.LENGTH_LONG).show()
                } else {
                    val email = GoogleSignIn.getClient(applicationContext, gso).silentSignIn().result?.email

                    email?.let {
                        credential.selectedAccountName = it
                        //Toast.makeText(this, GoogleSignIn.getClient(this@WeekActivity, gso).silentSignIn().result?.email, Toast.LENGTH_LONG).show()

                        sqLite.updateEmail(it)
                        appNotification.syncStart()

                        googleCalendar = GoogleCalendar(applicationContext, this, onSyncListener)
                        googleCalendar.start()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_week)
        init()
        calendarSetting()
        run()
    }

    override fun onBackPressed() {
        var quit = true
        if (supportFragmentManager.findFragmentByTag("processBarUpdate") == null) {
            if (supportFragmentManager.findFragmentByTag("addScheduleFragment") != null) {
                supportFragmentManager.beginTransaction().remove(addScheduleFragment)
                        .commit()
                visible()
                quit = false
            }
            if (supportFragmentManager.findFragmentByTag("updateScheduleFragment") != null) {
                val fragment = supportFragmentManager.findFragmentByTag("updateScheduleFragment")
                supportFragmentManager.beginTransaction().remove(fragment!!)
                        .commit()
                visible()
                quit = false
            }
            if (supportFragmentManager.findFragmentByTag("selectSemesterFragment") != null) {
                supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentByTag("selectSemesterFragment")!!)
                        .commit()
                visible()
                quit = false
            }
            if (supportFragmentManager.findFragmentByTag("updateCalendarFragment") != null) {
                supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentByTag("updateCalendarFragment")!!)
                        .commit()
                visible()
                quit = false
            }
            if (supportFragmentManager.findFragmentByTag("selectTestScheduleFragment") != null) {
                supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentByTag("selectTestScheduleFragment")!!)
                        .commit()
                visible()
                quit = false
            }
            if (supportFragmentManager.findFragmentByTag("testScheduleFragment") != null) {
                supportFragmentManager.beginTransaction().remove(supportFragmentManager.findFragmentByTag("testScheduleFragment")!!)
                        .commit()
                visible()
                quit = false
            }

            if (quit) {
                if (calendarView.selectedDate == CalendarDay.today())
                    super.onBackPressed()
                else
                    toDay()
            }
        }
    }

}
