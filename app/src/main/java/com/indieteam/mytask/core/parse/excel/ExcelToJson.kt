package com.indieteam.mytask.core.parse.excel

import android.annotation.SuppressLint
import com.indieteam.mytask.core.StringRandom
import com.indieteam.mytask.collection.RawCalendarStruct
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ExcelToJson{

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

    fun toJson(rawCalendarStructArr: ArrayList<RawCalendarStruct>) {
        for (i in rawCalendarStructArr){
            //debug use it
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
            val calendarStart = calendar
            calendar2.time = dateEnd
            val calendarEnd = calendar2

            while (calendarStart.time <= calendarEnd.time){
                //Log.d("date", calendar.time.toString())
                if(calendarStart.get(Calendar.DAY_OF_WEEK) == dayOfWeekMap[i.subjectDayOfWeek]){
//                    Log.d("size", size.toString())
//                    Log.d("subjectName", i.subjectName)
//                    Log.d("subjectDate", "${dateStartCalendar.get(Calendar.DAY_OF_MONTH)}/"+
//                            "${dateStartCalendar.get(Calendar.MONTH) + 1}/" +
//                            "${dateStartCalendar.get(Calendar.YEAR)}")
//                    Log.d("subjectDayOfWeek", i.subjectDayOfWeek)
//                    Log.d("subjectTime", i.subjectTime)
//                    Log.d("subjectPlace", i.subjectPlace)
//                    Log.d("teacher", i.teacher)

                    val jsonObjectChild = JSONObject()
                    jsonObjectChild.put("subjectId", StringRandom.get(40))
                    jsonObjectChild.put("subjectName", i.subjectName)
                    jsonObjectChild.put("subjectDate", "${calendarStart.get(Calendar.DAY_OF_MONTH)}/"+
                            "${calendarStart.get(Calendar.MONTH) + 1}/" +
                            "${calendarStart.get(Calendar.YEAR)}")
                    jsonObjectChild.put("subjectDayOfWeek", i.subjectDayOfWeek)
                    jsonObjectChild.put("subjectTime", i.subjectTime)
                    jsonObjectChild.put("subjectPlace", i.subjectPlace)
                    jsonObjectChild.put("teacher", i.teacher)

                    jsonArray.put(size, jsonObjectChild)
                    size++
                }
                calendarStart.add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        //Log.d("exelToJson", jsonObject.toString())
    }

}