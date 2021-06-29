package com.termux.zerocore.dialog

import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.xh_lib.utils.SaveData.getStringOther
import com.example.xh_lib.utils.UUtils
import com.google.gson.Gson
import com.termux.R
import com.termux.zerocore.bean.EditPromptBean
import com.termux.zerocore.popuwindow.EditPromptWindow
import java.util.ArrayList

class EditDialog : BaseDialogCentre {
    public var edit_text:EditText? = null
    public var ok:TextView? = null
    public var cancel:TextView? = null

    var editPromptWindow:EditPromptWindow? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, themeResId: Int) : super(context, themeResId)

    override fun initViewDialog(mView: View?) {
        edit_text = mView?.findViewById(R.id.edit_text)
        ok = mView?.findViewById(R.id.ok)
        cancel = mView?.findViewById(R.id.cancel)

        edit_text?.setOnFocusChangeListener { v, hasFocus ->


            val list = getList("")

            if(list == null){

                try {
                    if(editPromptWindow != null && editPromptWindow!!.isShowing){
                        editPromptWindow?.dismiss()
                        editPromptWindow = null
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                    editPromptWindow = null
                }


            }else{

                try {
                    if(editPromptWindow != null && editPromptWindow!!.isShowing){

                        editPromptWindow?.dismiss()
                        editPromptWindow = null
                    }
                }catch (e:Exception){
                    e.printStackTrace()
                    editPromptWindow = null
                }


                editPromptWindow = EditPromptWindow(mContext)

                editPromptWindow?.setEditPromptWindowListener(object : EditPromptWindow.EditPromptWindowListener{
                    override fun itemClick(data: EditPromptBean.EditPromptData) {
                        UUtils.showLog("点击事件(外):${data.ip}")
                        edit_text?.setText(data.ip)

                        try {
                            if(editPromptWindow != null && editPromptWindow!!.isShowing){

                                editPromptWindow!!.dismiss()
                                editPromptWindow = null
                            }
                        }catch (e:Exception){
                            e.printStackTrace()
                            editPromptWindow = null
                        }
                    }

                })
                editPromptWindow?.setList(list)
                editPromptWindow?.showAsDropDown(edit_text,0,20)
            }


        }

        edit_text?.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {

                val list = getList(s.toString())



                if(list == null){

                    try {
                        if(editPromptWindow != null && editPromptWindow!!.isShowing){
                            editPromptWindow!!.dismiss()
                            editPromptWindow = null
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                        editPromptWindow = null
                    }


                }else{

                    try {
                        if(editPromptWindow != null && editPromptWindow!!.isShowing){

                            editPromptWindow!!.dismiss()
                            editPromptWindow = null
                        }
                    }catch (e:Exception){
                        e.printStackTrace()
                        editPromptWindow = null
                    }

                    editPromptWindow = EditPromptWindow(mContext)

                    editPromptWindow?.setEditPromptWindowListener(object : EditPromptWindow.EditPromptWindowListener{
                        override fun itemClick(data: EditPromptBean.EditPromptData) {
                            UUtils.showLog("点击事件(外):${data.ip}")
                            edit_text?.setText(data.ip)

                            try {
                                if(editPromptWindow != null && editPromptWindow!!.isShowing){

                                    editPromptWindow!!.dismiss()
                                    editPromptWindow = null
                                }
                            }catch (e:Exception){
                                e.printStackTrace()
                                editPromptWindow = null
                            }
                        }

                    })
                    editPromptWindow?.setList(list)
                    editPromptWindow?.showAsDropDown(edit_text,0,20)
                }


            }


        })
    }

    //获取
    private fun getList(text:String): ArrayList<EditPromptBean.EditPromptData>? {


        var tempList:ArrayList<EditPromptBean.EditPromptData> = ArrayList()

        val ip_save = getStringOther("ip_save")

        if(ip_save == null || ip_save.isEmpty() || ip_save == "def"){


            return null
        }else{


            try {


                val fromJson = Gson().fromJson<EditPromptBean>(ip_save, EditPromptBean::class.java)

                val arrayList = fromJson.arrayList

                if(arrayList.isEmpty()){
                    return null
                }


                if(text.isEmpty()){
                   return arrayList
                }else{
                    for (i in 0 until arrayList.size){



                        if(arrayList[i].ip.contains(text)){

                            tempList.add((arrayList[i]))
                        }
                    }
                }


                if(tempList.isEmpty()){
                    return null
                }else{
                    return tempList
                }



            }catch (e:Exception){
                e.printStackTrace()
                return null
            }


        }



    }

    override fun getContentView(): Int {

        return R.layout.dialog_edit
    }
}
