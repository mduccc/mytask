package com.indieteam.mytask.model.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import com.indieteam.mytask.model.notification.AppNotification
import com.indieteam.mytask.model.parse.ParseCalendarJson
import com.indieteam.mytask.model.sqlite.SqLite
import org.json.JSONObject
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

class AppService: Service(){

    private lateinit var sqLite: SqLite
    private var valueDb = ""
    private lateinit var calendarJson: JSONObject
    private lateinit var parseCalendarJson: ParseCalendarJson
    private val calendarForTomorrow = Calendar.getInstance()!!
    private  lateinit var calendarForNow: Calendar
    private var result = ""
    private lateinit var appNotification: AppNotification
    private var countNotification = 0
    private lateinit var sharedPreferences: SharedPreferences

    init {
        calendarForTomorrow.set(calendarForTomorrow.get(Calendar.YEAR), calendarForTomorrow.get(Calendar.MONTH), calendarForTomorrow.get(Calendar.DAY_OF_MONTH))
        calendarForTomorrow.add(Calendar.DAY_OF_MONTH, 1)
    }

    private fun checkTimeBackground(){
        Timer().scheduleAtFixedRate(0, 20000) {
            calendarForNow = Calendar.getInstance()!!
            Log.d("hour", calendarForNow.get(Calendar.HOUR_OF_DAY).toString() + " " + calendarForNow.get(Calendar.MINUTE))
            if (calendarForNow.get(Calendar.HOUR_OF_DAY) == 20 && calendarForNow.get(Calendar.MINUTE) == 0) {
                if (countNotification == 0) {
                    pushNotification()
                    countNotification++
                }
            }else
                countNotification = 0
        }
    }


    private fun pushNotification(){
        var numberSubjects = 0
        result = ""
        val date = "${calendarForTomorrow.get(Calendar.DAY_OF_MONTH)}/${calendarForTomorrow.get(Calendar.MONTH)+1}/${calendarForTomorrow.get(Calendar.YEAR)}"
        sqLite = SqLite(this)
        try { valueDb = sqLite.readCalendar()
        }catch (e: Exception){ Log.d("Err", e.toString())}
        if (valueDb.isNotBlank()){
            calendarJson = JSONObject(valueDb)
            parseCalendarJson = ParseCalendarJson(calendarJson)
            parseCalendarJson.getSubject(date)
            if (parseCalendarJson.subjectName.isNotEmpty() && parseCalendarJson.subjectPlace.isNotEmpty() &&
                    parseCalendarJson.subjectTime.isNotEmpty() && parseCalendarJson.teacher.isNotEmpty()) {
                if (parseCalendarJson.subjectName.size == parseCalendarJson.subjectPlace.size &&
                        parseCalendarJson.subjectName.size == parseCalendarJson.subjectTime.size &&
                        parseCalendarJson.subjectName.size == parseCalendarJson.teacher.size) {
                    for (i in 0 until parseCalendarJson.subjectName.size) {
                        numberSubjects ++
                        result += "$numberSubjects. " + "${parseCalendarJson.subjectName[i]} (${parseCalendarJson.subjectTime[i]})\n"
                    }
                }
            }else{
                result += "Nghỉ \n"
            }
            result = result.substring(0, result.lastIndexOf("\n"))
            Log.d("AppService_Log", "Date $date: $result")
            appNotification = AppNotification(this)
            appNotification.subject(result, numberSubjects.toString())

        }else{
            Log.d("AppService_Log", "null")
        }
    }

    override fun onCreate() {
        super.onCreate()
        appNotification = AppNotification(this)
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channelName  = "App Notification"
            val channelId = "App Notification"
            val description = ""
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val chanel = NotificationChannel(channelId, channelName, importance)
            chanel.description = description
            val notificationManager = this.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(chanel)

            startForeground(1, appNotification.foreground().build())
        }
    }

    override fun onBind(intent: Intent?): IBinder? { return null }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("service", "started")
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        checkTimeBackground()
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("service", "stopped")
        super.onDestroy()
        try {
            stopSelf()
        }catch (e: java.lang.Exception){ e.printStackTrace() }
    }
}