package com.indieteam.mytask.datas.datasources.parseData

import android.annotation.SuppressLint
import com.indieteam.mytask.datas.Random
import com.indieteam.mytask.models.RawCalendarCollection
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ConvertExcel {

    @SuppressLint("SimpleDateFormat")
    private val simpleDateFormat = SimpleDateFormat("dd/MM/yyyy")
    private var dayOfWeekMap = mutableMapOf("2" to Calendar.MONDAY,
            "3" to Calendar.TUESDAY,
            "4" to Calendar.WEDNESDAY,
            "5" to Calendar.THURSDAY,
            "6" to Calendar.FRIDAY,
            "7" to Calendar.SATURDAY,
            "CN" to Calendar.SUNDAY)

    var jsonObject = JSONObject()
    var jsonArray = JSONArray()
    var size = 0

    fun toJson(rawCalendarCollectionArr: ArrayList<RawCalendarCollection>) {
        for (i in rawCalendarCollectionArr) {
//            Log.d("subjectName", i.subjectName)
//            Log.d("subjectDate", i.subjectDate)
//            Log.d("subjectDayOfWeek", i.subjectDayOfWeek)
//            Log.d("subjectTime", i.subjectTime)
//            Log.d("subjectPlace", i.subjectPlace)
//            Log.d("teacher", i.teacher)

            val dateStartString = i.subjectDate.substring(0, i.subjectDate.indexOf("-")).trim()
            val dateEndString = i.subjectDate.substring(i.subjectDate.indexOf("-") + 1, i.subjectDate.length).trim()

            val dateStart = simpleDateFormat.parse((dateStartString))
            val dateEnd = simpleDateFormat.parse((dateEndString))

//            Log.d("dateStartString", dateStartString)
//            Log.d("dateEndString", dateEndString)

            val calendar = Calendar.getInstance()
            val calendar2 = Calendar.getInstance()
            calendar.time = dateStart
            val firstCalendar = calendar
            calendar2.time = dateEnd
            val lastCalendar = calendar2

            while (firstCalendar.time <= lastCalendar.time) {
                //Log.d("date", calendar.time.toString())
                if (firstCalendar.get(Calendar.DAY_OF_WEEK) == dayOfWeekMap[i.subjectDayOfWeek]) {
//                    Log.d("size", size.toString())
//                    Log.d("subjectName", i.subjectName)
//                    Log.d("subjectDate", "${dateStartCalendar.get_string(Calendar.DAY_OF_MONTH)}/"+
//                            "${dateStartCalendar.get_string(Calendar.MONTH) + 1}/" +
//                            "${dateStartCalendar.get_string(Calendar.YEAR)}")
//                    Log.d("subjectDayOfWeek", i.subjectDayOfWeek)
//                    Log.d("subjectTime", i.subjectTime)
//                    Log.d("subjectPlace", i.subjectPlace)
//                    Log.d("teacher", i.teacher)

                    val jsonObjectChild = JSONObject()
                    jsonObjectChild.put("subjectId", Random.get_string(40))
                    jsonObjectChild.put("subjectName", i.subjectName)
                    jsonObjectChild.put("subjectDate", "${firstCalendar.get(Calendar.DAY_OF_MONTH)}/" +
                            "${firstCalendar.get(Calendar.MONTH) + 1}/" +
                            "${firstCalendar.get(Calendar.YEAR)}")
                    jsonObjectChild.put("subjectDayOfWeek", i.subjectDayOfWeek)
                    jsonObjectChild.put("subjectTime", i.subjectTime)
                    jsonObjectChild.put("subjectPlace", i.subjectPlace)
                    jsonObjectChild.put("teacher", i.teacher)

                    jsonArray.put(size, jsonObjectChild)
                    size++
                }
                firstCalendar.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        //Log.d("exelToJson", jsonObject.toString())
    }

}