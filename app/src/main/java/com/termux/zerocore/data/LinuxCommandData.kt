package com.termux.zerocore.data

import com.example.xh_lib.utils.UUtils
import com.termux.R
import com.termux.app.TermuxInstaller
import com.termux.zerocore.bean.ZeroRunCommandBean
import com.termux.zerocore.url.FileUrl
import java.io.File

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



        /**
         *
         * vsCode
         *
         * --
         *
         * 本地命令
         *
         *
         */





        val vScodeBean = ZeroRunCommandBean()

        vScodeBean.type = 1

        vScodeBean.name = UUtils.getString(R.string.网络访问linux目录)

        vScodeBean.fileName = "filebrowser.zip"

        vScodeBean.address = "https://github.com/filebrowser/filebrowser"

        var determineTermuxArchName = TermuxInstaller.determineTermuxArchName()

        when(determineTermuxArchName){

            "aarch64"->{
                vScodeBean.assetsName = "zipcommand/filebrowser_arm64.zip"
                vScodeBean.runCommand = "cd ~ && cd ~ && unzip filebrowser.zip && chmod 777 filebrowser_arm64.sh && ./filebrowser_arm64.sh \n"
            }
            "arm"->{
                vScodeBean.assetsName = "zipcommand/filebrowser_arm.zip"
                vScodeBean.runCommand = "cd ~ && cd ~ && unzip filebrowser.zip && chmod 777 filebrowser_arm.sh && ./filebrowser_arm.sh \n"
            }
            "x86_64"->{
                vScodeBean.assetsName = "zipcommand/filebrowser_amd.zip"
                vScodeBean.runCommand = "cd ~ && cd ~ && unzip filebrowser.zip && chmod 777 filebrowser_amd.sh && ./filebrowser_amd.sh \n"
            }
            "i686"->{
                vScodeBean.assetsName = "zipcommand/filebrowser_386.zip"
                vScodeBean.runCommand = "cd ~ && cd ~ && unzip filebrowser.zip && chmod 777 filebrowser_386.sh && ./filebrowser_386.sh \n"
            }

        }

        vScodeBean.setRunCommit {



             val smsBashrcFile = File(FileUrl.smsBashrcFile)
             var fileString = UUtils.getFileString(smsBashrcFile)

             if(!fileString.contains("filebrowser")){
                 fileString += "\n cd ~ > /dev/null && ./.filebrowser/filebrowser -a 0.0.0.0 -p 19951 -r "+FileUrl.mainFilesUrl+" & > /dev/null"
                 fileString += "\n echo '" + UUtils.getString(R.string.filebrowser已运行) + "'"
                 UUtils.setFileString(smsBashrcFile,fileString)
             }




        }



        mArrayList.add(vScodeBean)



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
