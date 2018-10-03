package com.indieteam.mytask.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.style.LineBackgroundSpan
import android.util.Log
import android.view.MotionEvent
import android.view.View.*
import android.view.ViewTreeObserver
import android.widget.CalendarView
import android.widget.Toast
import com.github.pwittchen.swipe.library.rx2.Swipe
import com.indieteam.mytask.R
import com.indieteam.mytask.adapter.CalendarListViewAdapter
import com.indieteam.mytask.dataObj.v2.TimeDetails
import com.indieteam.mytask.dataObj.v2.StudentCalendarObj
import com.indieteam.mytask.process.parseJson.ParseCalendarJson
import com.indieteam.mytask.process.domHTML.DomUpdateCalendar
import com.indieteam.mytask.process.runInBackground.AppService
import com.indieteam.mytask.sqlite.SqlLite
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.CalendarMode
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView.SELECTION_MODE_SINGLE
import kotlinx.android.synthetic.main.activity_week.*
import kotlinx.android.synthetic.main.fragment_process_bar.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION")
class WeekActivity : AppCompatActivity() {

    private val REQUEST_CODE = 1
    private lateinit var sqlLite: SqlLite
    private var calendarJson: JSONObject? = null
    private var parseCalendarJson: ParseCalendarJson? = null
    var mapDateForDots = mutableMapOf<CalendarDay, String>()
    private var studentCalendarObjArr = ArrayList<StudentCalendarObj>()
    private val dateStart = CalendarDay.from(Calendar.getInstance().get(Calendar.YEAR) - 1, 0, 1)
    private val dateEnd = CalendarDay.from(Calendar.getInstance().get(Calendar.YEAR) + 1, 11, 31)
    private lateinit var calendarListViewAdapter: CalendarListViewAdapter
    private var allPermission = 0
    private val swipe = Swipe()
    private lateinit var customSwipe: CustomSwipe
    private var screenHeight = 0
    private var statusBarHeight = 0
    private var navigationBarHeight = 0
    private var calendarMode = 0
    private lateinit var sharedPref: SharedPreferences
    private var startTouchY = 0f
    private val background = listOf(R.drawable.bg_a, R.drawable.bg_b, R.drawable.bg_c, R.drawable.bg_e, R.drawable.bg_i)
    @SuppressLint("SimpleDateFormat")
    private val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
    private val timeDetails = TimeDetails()

    inner class OnSwipeListener: com.github.pwittchen.swipe.library.rx2.SwipeListener{
        override fun onSwipedUp(event: MotionEvent?): Boolean {
            //Toast.makeText(this@WeekActivity, "Swiped up", Toast.LENGTH_SHORT).show()
            return true
        }

        override fun onSwipedDown(event: MotionEvent?): Boolean {
            //Toast.makeText(this@WeekActivity, "Swiped down", Toast.LENGTH_SHORT).show()
            return true
        }
        override fun onSwipingUp(event: MotionEvent?) {
            //Toast.makeText(this@WeekActivity, "Swiping right", Toast.LENGTH_SHORT).show()
        }

        override fun onSwipedRight(event: MotionEvent?): Boolean {
            //Toast.makeText(this@WeekActivity, "Swiped right", Toast.LENGTH_SHORT).show()
            if (startTouchY > content_layout.y && event!!.y > content_layout.y) {
                customSwipe.right()
            }
            countSwipeRight = 0
            return true
        }

        private var countSwipeLeft = 0
        override fun onSwipingLeft(event: MotionEvent?) {
            if(countSwipeLeft == 0) {
                event?.let {
                    startTouchY = event.y
                }
            }
            countSwipeLeft++
            //Toast.makeText(this@WeekActivity, "Swiping left", Toast.LENGTH_SHORT).show()
        }

        private var countSwipeRight = 0
        override fun onSwipingRight(event: MotionEvent?) {
            if(countSwipeRight == 0){
                event?.let {
                    startTouchY = event.y
                }
            }
            countSwipeRight++
            //Toast.makeText(this@WeekActivity, "Swiping right", Toast.LENGTH_SHORT).show()
        }

        override fun onSwipingDown(event: MotionEvent?) {
            //Toast.makeText(this@WeekActivity, "Swiping down", Toast.LENGTH_SHORT).show()
        }

        override fun onSwipedLeft(event: MotionEvent?): Boolean {
            //Toast.makeText(this@WeekActivity, "Swipe left", Toast.LENGTH_SHORT).show()
            if (startTouchY > content_layout.y && event!!.y > content_layout.y) {
                customSwipe.left()
            }
            countSwipeLeft = 0
            return true
        }

    }

    inner class DrawDots(private val colors: List<Int>, private val text: String) : LineBackgroundSpan {

        override fun drawBackground(c: Canvas, p: Paint,
                                    left: Int, right: Int, top: Int,
                                    baseline: Int, bottom: Int,
                                    charSequence: CharSequence,
                                    start: Int, end: Int, lineNum: Int) {
            val lastColor = p.color
            if (text.length == 1)
                p.color = colors[0]
            if (text.length == 2)
                p.color = colors[1]
            if (text.length >= 3)
                p.color = colors[2]
            var totalWidth = 0f
            for (i in 0 until text.length)
                if (i != 0)
                    totalWidth += (right.toFloat()/100f) * 7.5f

            var cX = right/2f - totalWidth/2
            for (i in 0 until text.length){
                c.drawCircle(cX, bottom.toFloat() + (bottom.toFloat()/100f) * 10f, (right.toFloat()/100f) * 2.1f  , p)
                cX += (right.toFloat()/100f) * 7.5f
            }
            p.color = lastColor
        }
    }

    inner class EventDecorator(private val mode: String, private val colors: List<Int>, val date: CalendarDay, private val dot: String): DayViewDecorator {
        override fun shouldDecorate(day: CalendarDay): Boolean {
            return date == day
        }

        override fun decorate(view: DayViewFacade) {
            if (mode == "Dots")
                view.addSpan(DrawDots(colors, dot))
            if (mode == "ToDay")
                view.setBackgroundDrawable(resources.getDrawable(R.drawable.shape_bg_cal_today))
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        swipe.dispatchTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if(requestCode == REQUEST_CODE){
            if(grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                run()
                //Toast.makeText(this@WeekActivity, "Permissions is granted", Toast.LENGTH_LONG).show()
            }else{
                Toast.makeText(this@WeekActivity, "Permissions is not granted", Toast.LENGTH_LONG).show()
            }
        }else{
            Toast.makeText(this@WeekActivity, "Permissions is not granted", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onBackPressed() {
        if (calendarView.selectedDate == CalendarDay.today())
            super.onBackPressed()
        else
            toDay()
    }

    private fun checkPermission(){
        if(Build.VERSION.SDK_INT >= 23) {
            if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
            }else{
                allPermission = 1
            }
        }else{
            run()
        }
    }

    private fun init(){
        sqlLite = SqlLite(this)
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
        calendarMode = sharedPref.getInt("CalendarMode", 0)
        calendarListViewAdapter = CalendarListViewAdapter(this@WeekActivity, studentCalendarObjArr)
        calender_list_view.adapter = calendarListViewAdapter
    }

    fun preDate(){
        val calendarSelected = Calendar.getInstance()
        calendarSelected.set(calendarView.selectedDate.year, calendarView.selectedDate.month, calendarView.selectedDate.day)
        calendarSelected.add(Calendar.DAY_OF_MONTH, -1)
        val newCalendarDate = CalendarDay.from(calendarSelected)
        calendarView.currentDate = newCalendarDate
        calendarView.selectedDate = newCalendarDate

        val date = "${newCalendarDate.day}/${newCalendarDate.month + 1}/${newCalendarDate.year}"
        updateListView(date)
    }

    fun nextDate(){
        val calendarSelected = Calendar.getInstance()
        calendarSelected.set(calendarView.selectedDate.year, calendarView.selectedDate.month, calendarView.selectedDate.day)
        calendarSelected.add(Calendar.DAY_OF_MONTH, 1)
        val newCalendarDate = CalendarDay.from(calendarSelected)
        calendarView.currentDate = newCalendarDate
        calendarView.selectedDate = newCalendarDate

        val date = "${newCalendarDate.day}/${newCalendarDate.month + 1}/${newCalendarDate.year}"
        updateListView(date)
    }

    private fun calendarSetting(){
        calendarView.state().edit().
                setMinimumDate(dateStart)
                .setMaximumDate(dateEnd)
                .commit()
        view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                //view.height //height is ready
                if (calendarMode == 0) {
                    calendarView.state().edit()
                            .setCalendarDisplayMode(CalendarMode.WEEKS)
                            .commit()
                }
                else {
                    calendarView.layoutParams.height = (screenHeight / 2f).toInt()
                    content_layout.layoutParams.height = (screenHeight - (screenHeight / 2f) - view.height - statusBarHeight).toInt()
                    calendarView.state().edit()
                            .setCalendarDisplayMode(CalendarMode.MONTHS)
                            .commit()
                }
                Log.d("view_height", view.height.toString())
            }
        })
        calendarView.currentDate = CalendarDay.today()
        calendarView.selectedDate = CalendarDay.today()
        calendarView.selectionMode = SELECTION_MODE_SINGLE
    }

    private fun changeBackground(){
        calender_list_view.background = resources.getDrawable(background[random(0, background.size - 1)])
    }

    private fun random(from: Int, to: Int): Int{
        return Random().nextInt(to - from) + from
    }

    private fun setCalendarDots(){
        for(i in mapDateForDots){
            calendarView.addDecorator(EventDecorator("Dots", listOf(resources.getColor(R.color.colorGreen), resources.getColor(R.color.colorOrangeDark), resources.getColor(R.color.colorRedDark)), i.key, i.value))
            //Log.d("valuedot", i.value)
        }
    }

    private fun drawBackgroundToday(){
        calendarView.addDecorator(EventDecorator("ToDay", listOf(resources.getColor(R.color.colorGrayDark)), CalendarDay.today(), "null"))
    }

    private fun calenderEvents(){
        calendarView.setOnDateChangedListener { /*materialCalendarView*/_, calendarDay, /*b*/_ ->
            val date = "${calendarDay.day}/${calendarDay.month+1}/${calendarDay.year}"
            //Toast.makeText(this, "$date", Toast.LENGTH_LONG).show()
            updateListView(date)
        }
        calendarView.setOnMonthChangedListener { /*materialCalendarView*/_, calendarDay ->
            object: CalendarView(this) {
                fun setTitleFormatter(){
                    return
                }
            }
            calendarView.setTitleFormatter{ "Tháng ${calendarDay.month+1} Năm ${calendarDay.year}" }
        }
    }

    private fun updateListView(date: String){
        if (parseCalendarJson != null){
            studentCalendarObjArr.removeAll(studentCalendarObjArr)
            parseCalendarJson!!.apply {
                getSubject(date)
                if (/*!subjectDate.isEmpty() &&*/
                        !subjectName.isEmpty() &&
                        !subjectTime.isEmpty() &&
                        !subjectPlace.isEmpty() &&
                        !teacher.isEmpty()) {
                    //var result = ""
                    if (/*subjectName.size == subjectDate.size &&*/
                            subjectName.size == subjectTime.size &&
                            subjectName.size == subjectPlace.size &&
                            subjectName.size == teacher.size) {
                        for (j in 0 until subjectName.size) {
                            //Log.d("result", "${subjectDate[j]}, ${subjectName[j]}, ${subjectTime[j]}, ${subjectPlace[j]}")
                            //result += "${subjectDate[j]}, ${subjectName[j]}, ${subjectTime[j]}, ${subjectPlace[j]} \n"
                            val firstTime = subjectTime[j].substring(0, subjectTime[j].indexOf(",")).toInt() -1
                            val endTime = subjectTime[j].substring(subjectTime[j].lastIndexOf(",") + 1, subjectTime[j].length).toInt() - 1
                            if (simpleDateFormat.parse(date) >= CalendarDay.from(CalendarDay().year, 3, 15).date &&
                                    simpleDateFormat.parse(date) < CalendarDay.from(CalendarDay().year, 9, 15).date)
                                studentCalendarObjArr.add(StudentCalendarObj(subjectName[j], /*subjectDate[j]*/"", subjectTime[j] + " (${timeDetails.timeSummerArr[firstTime].timeIn} -> ${timeDetails.timeSummerArr[endTime].timeOut})", subjectPlace[j], teacher[j]))
                            else
                                studentCalendarObjArr.add(StudentCalendarObj(subjectName[j], /*subjectDate[j]*/"", subjectTime[j] + " (${timeDetails.timeWinterArr[firstTime].timeIn} -> ${timeDetails.timeWinterArr[endTime].timeOut})", subjectPlace[j], teacher[j]))
                        }
                        //log.text = result
                        calendarListViewAdapter.notifyDataSetChanged()
                    }
                } else {
                    //Log.d("result", "$date Nghỉ")
                    //log.text = "$date Nghỉ"
                    calendarListViewAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initFloatButton(){
        val listItem =
                listOf(SpeedDialActionItem.Builder(R.id.fab_logout, R.drawable.ic_logout)
                        .setLabel("Đăng xuất")
                        .setFabBackgroundColor(resources.getColor(R.color.colorBlue))
                        .create(),
                        SpeedDialActionItem.Builder(R.id.fab_setting, R.drawable.ic_switch)
                                .setLabel("Tuần/Tháng")
                                .setFabBackgroundColor(resources.getColor(R.color.colorPurple))
                                .create(),
                        SpeedDialActionItem.Builder(R.id.fab_update, R.drawable.ic_update)
                                .setLabel("Cập nhật lịch")
                                .setFabBackgroundColor(resources.getColor(R.color.colorOrangeDark))
                                .create(),
                        SpeedDialActionItem.Builder(R.id.fab_info, R.drawable.ic_info)
                                .setLabel("Cá nhân")
                                .setFabBackgroundColor(resources.getColor(R.color.colorBlue))
                                .create()

        )
        float_button.addAllActionItems(listItem)

        float_button.setOnActionSelectedListener { it ->
            when(it.id){
                R.id.fab_setting ->{
                    //Toast.makeText(this@WeekActivity, "Setting", Toast.LENGTH_SHORT).show()
                    if(calendarMode == 1) {
                        calendarView.layoutParams.height = ((screenHeight / 100f) * 25f).toInt()
                        content_layout.layoutParams.height = ((screenHeight - (screenHeight / 100f) * 25f) - view.height - statusBarHeight).toInt()
                        calendarView.state().edit()
                                .setCalendarDisplayMode(CalendarMode.WEEKS)
                                .commit()
                        sharedPref.apply {
                            with(edit()){
                                putInt("CalendarMode", 0)
                                apply()
                            }
                        }
                        calendarMode = sharedPref.getInt("CalendarMode", 0)
                    } else {
                        calendarView.layoutParams.height = (screenHeight / 2f).toInt()
                        content_layout.layoutParams.height = (screenHeight - (screenHeight / 2f) - view.height - statusBarHeight).toInt()
                        calendarView.state().edit()
                                .setCalendarDisplayMode(CalendarMode.MONTHS)
                                .commit()
                        sharedPref.apply {
                            with(edit()){
                                putInt("CalendarMode", 1)
                                apply()
                            }
                        }
                        calendarMode = sharedPref.getInt("CalendarMode", 0)
                    }
                }
                R.id.fab_info ->{
                    val intent = Intent(this@WeekActivity, InfoStudentActivity::class.java)
                    startActivity(intent)
                }
                R.id.fab_update ->{
                    supportFragmentManager.beginTransaction().add(R.id.calendar_root_view, ProcessBarFragment(), "processBarUpdate")
                            .commit()
                    supportFragmentManager.executePendingTransactions()
                    supportFragmentManager.findFragmentByTag("processBarUpdate")?.apply{
                        process?.text = "Cập nhật..."
                    }
                    gone()
                    try {
                        DomUpdateCalendar(this, sqlLite.readCookie()).start()
                    }catch (e: Exception){
                        visible()
                        supportFragmentManager.findFragmentByTag("processBarUpdate")?.let {
                            supportFragmentManager.beginTransaction().remove(it).commit()
                        }
                        Toast.makeText(this, "Err update", Toast.LENGTH_SHORT).show()
                        Log.d("Err", e.toString())
                    }
                }
                R.id.fab_logout ->{
                    try {
                        sqlLite.deleteCalendar()
                        sqlLite.deleteInfo()
                        if (checkServiceRunning())
                            stopService(Intent(this, AppService::class.java))
                    }catch (e: Exception){ Log.d("Err", e.toString()) }
                    moveToLogin()
                }
            }
            false //false to close float button
        }
    }

    private fun gone(){
        calendarView.visibility = GONE
        view.visibility = GONE
        content_layout.visibility = GONE
        float_button.visibility = GONE
    }

    fun visible(){
        calendarView.visibility = VISIBLE
        view.visibility = VISIBLE
        content_layout.visibility = VISIBLE
        float_button.visibility = VISIBLE
    }

    private fun moveToLogin(){
        val intent = Intent(this@WeekActivity, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun toDay(){
            calendarView.currentDate = CalendarDay.today()
            calendarView.selectedDate = CalendarDay.today()
            val date = "${CalendarDay.today().day}/${CalendarDay.today().month+1}/${CalendarDay.today().year}"
            updateListView(date)
    }

    private fun startService(){
        val intent = Intent(this, AppService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ContextCompat.startForegroundService(this, intent)
        else
            startService(intent)
    }

    private fun checkServiceRunning(): Boolean {
        try {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (AppService::class.java.name == service.service.className) {
                    Log.d("service", "running")
                    return true
                }
            }
        }catch (e: Exception){e.printStackTrace()}
        Log.d("service", "not running")
        return false
    }

    private fun run(){
        changeBackground()
        var readDb: Int
        var valueDb= ""
        try{
            valueDb = sqlLite.readCalendar()
            readDb = 1
            Log.d("readdb", "readCalendar db done")
        }catch (e: Exception){
            readDb = 0
            Log.d("readdb", "db is not exits, cannot readCalendar")
            Log.d("err", e.toString())
        }

        if(readDb == 0){ moveToLogin()
        }else{
            calendarJson = JSONObject(valueDb)
            parseCalendarJson = ParseCalendarJson(calendarJson!!)
        }
        initFloatButton()
        toDay()
        drawBackgroundToday()
        mapDateForDots = parseCalendarJson!!.addToMapDots()
        setCalendarDots()
        swipe.setListener(OnSwipeListener())
        Log.d("service", checkServiceRunning().toString())
        if (!checkServiceRunning())
            startService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_week)
        init()
        calendarSetting()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission()
            if (allPermission == 1)
                run()
        }else
            run()
    }

}
