package com.termux.zerocore.data

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.zerocore.bean.ZeroRunCommandBean

object LinuxCommandData {


    public fun getZeroRunCommandBeanData():ArrayList<ZeroRunCommandBean>{



        var mArrayList:ArrayList<ZeroRunCommandBean> = ArrayList()


        /**
         *
         * 标题 [软件开发分类]
         *
         *
         *  ■ □ □ □
         *
         *
         */

        val rjTitle = ZeroRunCommandBean()

        rjTitle.type = 0

        rjTitle.title = UUtils.getString(R.string.软件开发)

        mArrayList.add(rjTitle)

        // ■ ■ □ □
        mArrayList.add(getEmpty())
        // ■ ■ ■ □
        mArrayList.add(getEmpty())
        // ■ ■ ■ ■
        mArrayList.add(getEmpty())


        /**
         *
         * MyServer
         *
         * https://github.com/rajkumardusad/MyServer
         *
         * 本地命令
         *
         *
         */


        val myServerBean = ZeroRunCommandBean()

        myServerBean.type = 1

        myServerBean.name = UUtils.getString(R.string.MyServer)

        myServerBean.fileName = "MyServer.zip"

        myServerBean.address = "https://github.com/rajkumardusad/MyServer"

        myServerBean.assetsName = "zipcommand/MyServer_master.zip"

        myServerBean.runCommand = "cd ~ && cd ~ && unzip MyServer.zip && cd MyServer-master && chmod 777 install && ./install \n"

        mArrayList.add(myServerBean)



        /**
         *
         * termux-ADB
         *
         * https://github.com/MasterDevX/Termux-ADB
         *
         * 本地命令
         *
         *
         */


        val aDBBean = ZeroRunCommandBean()

        aDBBean.type = 1

        aDBBean.name = UUtils.getString(R.string.Termux_ADB)

        aDBBean.fileName = "Termux-ADB-master.zip"

        aDBBean.address = "https://github.com/MasterDevX/Termux-ADB"

        aDBBean.assetsName = "zipcommand/Termux-ADB-master.zip"

        aDBBean.runCommand = "cd ~ && cd ~ && pkg update && pkg install wget -y && unzip Termux-ADB-master.zip && cd Termux-ADB-master && chmod 777 InstallTools.sh && ./InstallTools.sh \n"

        mArrayList.add(aDBBean)


        return mArrayList


    }


    /**
     *
     *
     * 获取占位 方块
     *
     *
     */

    private fun getEmpty():ZeroRunCommandBean{

        val empty1 = ZeroRunCommandBean()

        empty1.type = 0

        empty1.isShow = false

        empty1.title = UUtils.getString(R.string.软件开发)


        return empty1

    }



}
