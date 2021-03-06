package com.indieteam.mytask.datas.datasources.parseData

import android.annotation.SuppressLint
import com.prolificinteractive.materialcalendarview.CalendarDay
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ParseScheduleJson(private val schedule: JSONObject) {
    var subjectName = ArrayList<String>()
    var subjectTime = ArrayList<String>()
    var subjectPlace = ArrayList<String>()
    var teacher = ArrayList<String>()
    var subjectId = ArrayList<String>()

    fun getSubject(key: String) {
        subjectName = arrayListOf()
        subjectTime = arrayListOf()
        subjectPlace = arrayListOf()
        teacher = arrayListOf()
        subjectId = arrayListOf()

        val calendarValue = schedule.getJSONArray("calendar")

        for (i in 0 until calendarValue.length()) {
            if (key == calendarValue.getJSONObject(i).getString("subjectDate")) {
                //subjectDate.add(calendarValue.getJSONObject(i).getString("subjectDate"))
                subjectName.add(calendarValue.getJSONObject(i).getString("subjectName"))
                subjectTime.add(calendarValue.getJSONObject(i).getString("subjectTime"))
                subjectPlace.add(calendarValue.getJSONObject(i).getString("subjectPlace"))
                teacher.add(calendarValue.getJSONObject(i).getString("teacher"))
                subjectId.add(calendarValue.getJSONObject(i).getString("subjectId"))
            }
        }
        sort()
    }

    private fun sort() {
        if (subjectName.isNotEmpty() && subjectPlace.isNotEmpty() && subjectTime.isNotEmpty() && teacher.isNotEmpty()) {
            for (j in 0 until subjectTime.size - 1) {
                for (k in j + 1 until subjectTime.size) {

                    val firstI = if (subjectTime[j].indexOf(",") > -1)
                        subjectTime[j].substring(0, subjectTime[j].indexOf(",")).toInt()
                    else
                        subjectTime[j].toInt()

                    val firstJ = if (subjectTime[k].indexOf(",") > -1)
                        subjectTime[k].substring(0, subjectTime[k].indexOf(",")).toInt()
                    else
                        subjectTime[k].toInt()

                    if (firstI > firstJ) {
                        var temp = subjectName[j]
                        subjectName[j] = subjectName[k]
                        subjectName[k] = temp

                        temp = subjectTime[j]
                        subjectTime[j] = subjectTime[k]
                        subjectTime[k] = temp

                        temp = subjectPlace[j]
                        subjectPlace[j] = subjectPlace[k]
                        subjectPlace[k] = temp

                        temp = teacher[j]
                        teacher[j] = teacher[k]
                        teacher[k] = temp

                        temp = subjectId[j]
                        subjectId[j] = subjectId[k]
                        subjectId[k] = temp
                    }
                }
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun initDots(): MutableMap<CalendarDay, Int> {
        val dots = mutableMapOf<CalendarDay, Int>()
        val calendarValue = schedule.getJSONArray("calendar")
        for (i in 0 until calendarValue.length()) {
            val simpleDateParse = SimpleDateFormat("dd/MM/yyyy")
            val calendar = Calendar.getInstance()

            val dateParsed = simpleDateParse.parse(calendarValue.getJSONObject(i).getString("subjectDate"))
            calendar.time = dateParsed

            val calendarDay = CalendarDay.from(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

            if (dots.isEmpty()) {
                dots[calendarDay] = 1
            } else {
                if (dots.containsKey(calendarDay)) {
                    var value = dots[calendarDay]!!.toInt()
                    value++
                    dots[calendarDay] = value
                } else
                    dots[calendarDay] = 1
            }
        }
        return dots
    }

}