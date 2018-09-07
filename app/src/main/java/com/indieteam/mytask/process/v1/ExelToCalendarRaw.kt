package com.indieteam.mytask.process.v1

import android.util.Log
import com.indieteam.mytask.modeldata.v1.CalendarRaw
import com.indieteam.mytask.modeldata.v1.OnlyCalendar
import com.indieteam.mytask.ui.WeekActivity

class ExelToCalendarRaw(private val activity: WeekActivity){

    fun trimTkbData(){
        var index2Add = -1
        activity.apply {
            for (i in 0 until calendarRawArr.size) {
                if(!calendarRawArr[i].subjectName.isBlank() && !calendarRawArr[i].tc.isBlank() && !calendarRawArr[i].info.isBlank()){
                    calendarRaw.add(CalendarRaw(calendarRawArr[i].subjectName, calendarRawArr[i].tc, calendarRawArr[i].info))
                    index2Add ++
                }

                if(calendarRawArr[i].subjectName.isBlank() && calendarRawArr[i].tc.isBlank() && !calendarRawArr[i].info.isBlank()){
                    if(calendarRaw[index2Add].info.lastIndexOf("\n") == calendarRaw[index2Add].info.length - 3)
                        calendarRaw[index2Add].info += calendarRawArr[i].info + "\n"
                    else
                        calendarRaw[index2Add].info += "\n" + calendarRawArr[i].info + "\n"
                }
            }
        }
    }

    fun addToMap(){
        activity.apply {
            Log.d("size", calendarRaw.size.toString() + "\n\n")

            //Debug use it:
//        for (i in calendarRaw) {
//            Log.d("_______info________", "___start____"+i.info + "____end____\n\n")
//        }

            Log.d("------", "-----------------------------------Try parse calender-------------------------------------------------------------------------------------------------------------------------")

            var info: String
            for (i in calendarRaw) {
                //Log.d("___", "________________start item____________________")
                //Log.d("name subject ", i.subjectName)
                val arrDate = ArrayList<String>()
                val arrPlace = ArrayList<String>()
                info = i.info
                var nameSubj: String
                var dateTemp: String
                var placeTemp: String
                var onlyCalendar: OnlyCalendar
                var indexDate: Int
                var indexPlace: Int
                var indexBreakLineDate: Int
                var indexBreakLinePlace: Int
                indexDate = info.indexOf("Từ")
                indexPlace = info.indexOf("\n")
                indexBreakLineDate = info.indexOf("\n")
                indexBreakLinePlace = info.indexOf("\n", indexBreakLineDate + 1)

                Log.d("indexDate", indexDate.toString())
                Log.d("indexBreakLineDate", indexBreakLineDate.toString())

                Log.d("indexPlace", indexPlace.toString())
                Log.d("indexBreakLinePlace", indexBreakLinePlace.toString())

                dateTemp = info.substring(indexDate, indexBreakLineDate)
                if(indexBreakLinePlace == -1)
                    placeTemp = info.substring(indexPlace + 2, info.length - 1)
                else
                    placeTemp = info.substring(indexPlace + 2, indexBreakLinePlace)
                arrDate.add(dateTemp)
                arrPlace.add(placeTemp)
                //Log.d("parse date", timeTemp)
                nameSubj = i.subjectName
                while (indexDate >= 0) {
                    indexDate = info.indexOf("Từ", indexDate + 1)
                    indexBreakLinePlace = info.indexOf("\n", indexBreakLineDate + 1)
                    indexBreakLineDate = info.indexOf("\n", indexBreakLinePlace + 1)
                    if (indexDate != -1 && indexBreakLineDate != -1 && indexDate < indexBreakLineDate) {
                        //Log.d("parse date ", lineTime.toString())
                        dateTemp = info.substring(indexDate, indexBreakLineDate)
                        //Log.d("parse date", timeTemp)
                        arrDate.add(dateTemp)
                    }

                }
                indexBreakLineDate = info.indexOf("\n")
                indexBreakLinePlace = info.indexOf("\n", indexBreakLineDate + 1)

                //Log.d("parse place", placeTemp)
                while (indexPlace >= 0) {
                    indexPlace = info.indexOf(":\n", indexBreakLineDate + 1)
                    indexBreakLineDate = info.indexOf("\n", indexBreakLinePlace + 1)
                    indexBreakLinePlace = info.indexOf("\n", indexBreakLineDate + 1)
                    if (indexPlace != -1 && indexBreakLinePlace != -1 && indexPlace < indexBreakLinePlace) {
                        //Log.d("parse place ", linePlace.toString())
                        placeTemp = info.substring(indexPlace + 1, indexBreakLinePlace)
                        //Log.d("parse place", placeTemp)
                        arrPlace.add(placeTemp)
                    }
                }
                //Log.d("___", "end item____________________")
                onlyCalendar = OnlyCalendar(arrDate, arrPlace)
                calendarMap[nameSubj] = onlyCalendar
            }
            // Debug use it:
//            for (i in calendarMap) {
//                Log.d("name subject", i.key)
//                val onlyCalendar = i.value
//                if (onlyCalendar.arrDateRaw.size == onlyCalendar.arrPlaceRaw.size) {
//                    for (j in 0 until onlyCalendar.arrDateRaw.size) {
//                        Log.d("Date raw", onlyCalendar.arrDateRaw[j])
//                        Log.d("Place raw", onlyCalendar.arrPlaceRaw[j])
//
//                    }
//                }
//            }
        }
    }

}
