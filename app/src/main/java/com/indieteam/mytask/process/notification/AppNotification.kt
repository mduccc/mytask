package com.indieteam.mytask.process.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.indieteam.mytask.R
import com.indieteam.mytask.ui.WeekActivity

class AppNotification(val context: Context, private val numberSubjects: String) {

    fun build(contents: String){
        //touch
        val intent = Intent(context, WeekActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        //set channelId
        if(Build.VERSION.SDK_INT >= 26){
            val channelName  = "calendarNotification"
            val channelId = "calendar_notification"
            val description = ""
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val chanel = NotificationChannel(channelId, channelName, importance)
            chanel.description = description
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(chanel)
        }

        // builder
        val badgeContent: String = if (numberSubjects != "0")
            "Có $numberSubjects môn"
        else
            "Nghỉ"
        val mBuilder = NotificationCompat.Builder(context, "calendar_notification")
                .setSmallIcon(R.drawable.ic_next_day_256)
                .setContentTitle("Ngày mai")
                .setContentText(badgeContent)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contents))
                .setColor(Color.parseColor("#2c73b3"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                .setVibrate(longArrayOf(1))
                .setAutoCancel(true) // remove notification after touch
                //.setOngoing(true) // disable wipe

        //show
        NotificationManagerCompat.from(context).notify(2, mBuilder.build())
    }

}