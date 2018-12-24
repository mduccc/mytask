package com.indieteam.mytask.core.calendar.domHTML

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.Html
import android.util.Log
import android.widget.Toast
import com.indieteam.mytask.R
import com.indieteam.mytask.address.UrlAddress
import com.indieteam.mytask.core.parse.excel.ReadExel
import com.indieteam.mytask.core.sqlite.SqLite
import com.indieteam.mytask.ui.LoginActivity
import com.indieteam.mytask.ui.ProcessBarFragment
import com.indieteam.mytask.ui.WeekActivity
import kotlinx.android.synthetic.main.fragment_process_bar.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream

@Suppress("DEPRECATION")
class DomDownloadExcel(val context: Context, private val sessionUrl: String, private val signIn: String,
                       val semesterSelected: String): Thread() {

    var pageHeader1drpNgonNgu = "010527EFBEB84BCA8919321CFD5C3A34"
    private var drpSemester = ""
    private var drpTerm = "1"
    private var drpTermArr = ArrayList<String>()
    private var characterDolla = Html.fromHtml("&#36;")
    private var err = 0
    private val sqlLite = SqLite(context)
    private var classContextName = ""
    private var readExel = ReadExel(context)
    private val dataMap = mutableMapOf<String, String>()
    private var emptyCalendar = false
    private var requestTime = 0

    init {
        classContextName = context.javaClass.name.substring(context.javaClass.name.lastIndexOf(".") + 1, context.javaClass.name.length)
    }

    @SuppressLint("SetTextI18n")
    override fun run() {
        if (classContextName == "LoginActivity") {
            (context as LoginActivity).runOnUiThread {
                context.supportFragmentManager.beginTransaction().add(R.id.login_root_view, ProcessBarFragment(), "processBarLogin")
                        .commit()
                context.supportFragmentManager.executePendingTransactions()
                context.supportFragmentManager.findFragmentByTag("processBarLogin")?.let {
                    context.process.text = "Lưu Excel..."
                }
            }
        }

        if (classContextName == "WeekActivity") {
            (context as WeekActivity).runOnUiThread {
                context.supportFragmentManager.beginTransaction().add(R.id.calendar_root_view, ProcessBarFragment(), "processBarUpdate")
                        .commit()
                context.supportFragmentManager.executePendingTransactions()
                context.supportFragmentManager.findFragmentByTag("processBarUpdate")?.let {
                    context.process.text = "Cập nhật..."
                }
            }
        }
        loadPageToGetParams()
        loadPageWithSemester()
        download()
        this.join()
    }

    private fun loadPageToGetParams(){
        if(sessionUrl.isNotBlank()) {
            try {
                val request = Jsoup.connect(UrlAddress.urlSemester(sessionUrl))
                        .cookie("SignIn", signIn)
                        .method(Connection.Method.GET)
                        .execute()

                val pageParse = request.parse()

                for (i in pageParse.select("input")) {
                    dataMap[i.attr("name")] = i.`val`()
                }
            }catch (e: Exception){
                appException("Mất kết nối")
            }
        }
    }

    private fun loadPageWithSemester(){
        if(sessionUrl.isNotBlank()) {
            try {
                requestTime++
                val request: Connection.Response
                if (!emptyCalendar) {
                    request = Jsoup.connect(UrlAddress.urlDownloadExel(sessionUrl))
                            .data(dataMap)
                            .data("PageHeader1${characterDolla}drpNgonNgu", pageHeader1drpNgonNgu)
                            .data("drpTerm", drpTerm)
                            .data("drpType", "B")
                            .data("drpSemester", semesterSelected)
                            .cookie("SignIn", signIn)
                            .method(Connection.Method.POST)
                            .ignoreContentType(true)
                            .execute()
                } else{
                    request = Jsoup.connect(UrlAddress.urlDownloadExel(sessionUrl))
                            .data(dataMap)
                            .data("PageHeader1${characterDolla}drpNgonNgu", pageHeader1drpNgonNgu)
                            .data("drpType", "B")
                            .data("drpSemester", semesterSelected)
                            .cookie("SignIn", signIn)
                            .method(Connection.Method.POST)
                            .ignoreContentType(true)
                            .execute()
                }

                val pageParse = request.parse()

                for (i in pageParse.select("select")) {
                    //Hoc ky
                    if (i.attr("name") == "drpSemester") {
                        for (j in i.select("option")) {
                            //Log.d("drpSemester", j.attr("value"))
                            if (j.attr("selected") == "selected") {
                                drpSemester = j.attr("value")
                                Log.d("Hoc Ky", j.text())
                            }
                        }
                    }
                    //Dot hoc
                    if (i.attr("name") == "drpTerm") {
                        for (j in i.select("option")) {
                            drpTermArr.add(j.attr("value"))
                            Log.d("Dot hoc", j.text())
                        }
                    }
                }

                dataMap.clear()

                if (drpSemester == "") {
                    Log.d("drpSemester", "blank")
                    emptyCalendar = true
                    if (emptyCalendar && requestTime == 1)
                        loadPageWithSemester()
                    else {
                        for (i in pageParse.select("input")) {
                            dataMap[i.attr("name")] = i.`val`()
                        }
                    }
                } else {
                    Log.d("drpSemester", "not blank")
                    for (i in pageParse.select("input")) {
                        dataMap[i.attr("name")] = i.`val`()
                    }
                }

            }catch (e: java.lang.Exception){
                if (drpSemester.isBlank()) {
                    Log.d("drpSemester", "blank")
                    emptyCalendar = true
                    if (emptyCalendar && requestTime == 1)
                        loadPageWithSemester()
                } else
                    appException("Mất kết nối")
            }
        }
    }

    private fun download(){
        try {
            Log.d("sessionUrl", sessionUrl)
            Log.d("drpSemester", drpSemester)
            Log.d("semesterSelected", semesterSelected)
            dataMap.forEach{
                Log.d("dataMap", it.value)
            }
            drpTermArr.forEach {
                Log.d("drpTermArr", it)
            }
            if(sessionUrl.isNotBlank() && dataMap.isNotEmpty() &&
                    drpSemester.isNotBlank() && drpTermArr.isNotEmpty()) {

                //loop all dot hoc
                for (drpTerm in drpTermArr) {
                    try {
                        val resDownloadExel = Jsoup.connect(UrlAddress.urlDownloadExel(sessionUrl))
                                .data(dataMap)
                                .data("PageHeader1${characterDolla}drpNgonNgu", pageHeader1drpNgonNgu)
                                .data("drpSemester", semesterSelected)
                                .data("drpTerm", drpTerm)
                                .data("drpType", "B")
                                .data("btnView", "Xuất file Excel")
                                .cookie("SignIn", signIn)
                                .method(Connection.Method.POST)
                                .ignoreContentType(true)
                                .execute()
                        Log.d("contentType", resDownloadExel.contentType())
                        if (resDownloadExel.contentType() == "application/vnd.ms-excel; charset=utf-8") {
                            try {
                                val dir = File(context.filesDir, "exel")
                                if (!dir.exists())
                                    dir.mkdirs()

                                val file = File(context.filesDir, "exel/tkb_v2.xls")
                                if (file.exists())
                                    file.delete()

                                val fos = FileOutputStream(file)
                                fos.write(resDownloadExel.bodyAsBytes())
                                fos.close()
                                // read
                                readExel.readTkb()
                                if (readExel.readExelCallBack == -1 || readExel.readExelCallBack == 0) {
                                    appException("Error read Excel")
                                }
                            } catch (e: Exception) {
                                appException("Error file")

                            }
                        } else {
                            appException("Cannot Download Excel")
                        }
                    }catch (e: Exception){
                        appException("Mất kết nối")
                        break
                    }
                }
                if(err == 0)
                    save(); done()
            }else{
                appException("Lịch rỗng")
            }
        }catch (e: Exception){
            appException("Mất kết nối")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun done(){
        if (classContextName == "LoginActivity") {
            (context as LoginActivity).apply {
                supportFragmentManager.findFragmentByTag("processBarFragment")?.let {
                    runOnUiThread {
                        process.text = "Lưu Excel...Ok"
                    }
                }
                val intent = Intent(context, WeekActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
        if (classContextName == "WeekActivity") {
            (context as WeekActivity).apply {
                val intent = Intent(context, WeekActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun save(){
        readExel.exelToJson.toJson(readExel.rawCalendarObjArr)
        readExel.exelToJson.jsonObject.put("info", readExel.infoObj)
        readExel.exelToJson.jsonObject.put("calendar", readExel.exelToJson.jsonArray)
        try {
            sqlLite.deleteCalendar()
            sqlLite.insertCalender(readExel.exelToJson.jsonObject.toString())
        }catch (e: Exception){
            Log.d("Error save", e.toString())
        }
    }

    private fun appException(errorName: String){
        if (classContextName == "LoginActivity") {
            (context as LoginActivity).runOnUiThread {
                context.supportFragmentManager.findFragmentByTag("processBarLogin")?.let {
                    context.supportFragmentManager.beginTransaction().remove(it)
                            .commit()
                }
                context.visible()
                context.clickLogin = 0
                Toast.makeText(context, errorName, Toast.LENGTH_SHORT).show()
            }
        }
        if (classContextName == "WeekActivity") {
            (context as WeekActivity).runOnUiThread {
                context.supportFragmentManager.findFragmentByTag("processBarUpdate")?.let {
                    context.supportFragmentManager.beginTransaction().remove(it)
                            .commit()
                }
                context.visible()
                Toast.makeText(context, errorName, Toast.LENGTH_SHORT).show()
            }
        }
    }
}