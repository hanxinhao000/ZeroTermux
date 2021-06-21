package com.example.xh_lib.fragment

import android.app.Activity
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.xh_lib.utils.UUtils

/**
 * @author ZEL
 * @create By ZEL on 2020/4/3 15:34
 **/
abstract class BaseFragment : Fragment() {


    private  var mView: View? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {


        // TODO 自动生成的方法存根

        // TODO 自动生成的方法存根
        if (mView!= null) {
            if(mView!!.parent != null){
                val parent = mView!!.parent as ViewGroup
                if (parent != null) {
                    parent?.removeView(mView)
                }
            }



            initView(mView!!)
            return mView
        }
        mView = getFragmentView()
        initView(mView!!)
        return mView

      //  mView = getFragmentView()



       // return mView
    }

    abstract fun initView(mView: View)

    abstract fun getFragmentView(): View


    public fun <T : View?> findViewById(id: Int): T {

        return mView!!.findViewById<T>(id)
    }

    abstract fun refreshUI(mBackground: TypedValue, mTextColor: TypedValue)

    open fun switchClick(mHomeActivity: Activity) {

    }
    @Synchronized
    open fun responseMqttData(topic: String, msg: String){


        UUtils.showLog("当前订阅地址:${topic}")

    }

   // public abstract fun onStartMqtt()
  //  public abstract fun onStopMqtt()

}