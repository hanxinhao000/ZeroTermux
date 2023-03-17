package com.termux.zerocore.utils

import com.example.xh_lib.utils.LogUtils
import com.termux.zerocore.url.FileUrl
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

object BashFileUtils {
    private val TAG = "BashFileUtils"
    public fun setStartCommand(list: ArrayList<String>) {
        LogUtils.d(TAG, "getStartCommand bash.bashrc ZT Command is Empty")
        val file = File(FileUrl.mainFilesUrl, "/usr/etc/bash.bashrc")
        if (!file.exists()) {
            LogUtils.d(TAG, "getStartCommand bash.bashrc is not exists")
            return
        }
        val startCommand = getStartCommand()
        val lines: List<String> = file.readLines()
        if (startCommand == null || startCommand.isEmpty()) {
            val arrayList = LinkedList<String>()
            arrayList.addAll(lines)
            arrayList.add(arrayList.size, "# ZeroTermux[Start]")
            list.forEach {
                arrayList.add(arrayList.size, it)
            }
            arrayList.add(arrayList.size, "# ZeroTermux[End]")
            LogUtils.d(TAG, "getStartCommand add command 1:$arrayList")
            file.printWriter().use { out ->
                arrayList.forEach {
                    out.print(it)
                    LogUtils.d(TAG, "getStartCommand writer command 1:$it")
                    out.println()
                }
                out.flush()
                out.close()
            }
        } else {
            LogUtils.d(TAG, "getStartCommand bash.bashrc ZT Command is:$startCommand")
            var end = 0
            for (i in lines.indices) {
                if (lines[i].contains("ZeroTermux[End]")) {
                    LogUtils.d(TAG, "getStartCommand ZeroTermux[Start] is:$end")
                    end = i - 1
                }
            }
            //获取的所有文本 lines = arrayListLines
            //获取ZT当前的  startCommand
           // startCommand.addAll(list)
            val arrayListLines = LinkedList<String>()
            arrayListLines.addAll(lines)
            list.reverse()
            list.forEach {
                arrayListLines.add(end, it)
            }
           // val newList1: ArrayList<String> = UUUtils.removeDuplicate_1(arrayListLines)

            LogUtils.d(TAG, "getStartCommand add command 2:$arrayListLines")
            file.printWriter().use { out ->
                arrayListLines.forEach {
                    out.print(it)
                    out.println()
                }
                out.flush()
                out.close()
            }
        }

    }

    public fun getStartCommand(): ArrayList<String>? {
        val file = File(FileUrl.mainFilesUrl, "/usr/etc/bash.bashrc")
        if (!file.exists()) {
            LogUtils.d(TAG, "getStartCommand bash.bashrc is not exists")
            return null
        }
        val lines: List<String> = file.readLines()
        var start = 0
        var end = 0
        for (i in lines.indices) {
            if (lines[i].contains("ZeroTermux[Start]")) {
                LogUtils.d(TAG, "getStartCommand ZeroTermux[Start] is:$start")
                start = i + 1
            }
            if (lines[i].contains("ZeroTermux[End]")) {
                LogUtils.d(TAG, "getStartCommand ZeroTermux[End] is:$end")
                end = i - 1
            }
        }
        if (start == 0 && end == 0) {
            LogUtils.d(TAG, "getStartCommand ZeroTermux[End][Start] not find!")
            return null
        }
        val arrayList = ArrayList<String>()
        for (i in 0..(end - start)) {
            arrayList.add(lines[start + i])
        }

        return arrayList
    }

}
