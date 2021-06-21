package com.blockchain.ub.utils.httputils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message

import com.example.xh_lib.R
import com.example.xh_lib.utils.UUtils
import com.lzy.okgo.OkGo
import com.lzy.okgo.callback.StringCallback
import com.lzy.okgo.model.HttpHeaders
import com.lzy.okgo.model.HttpParams
import com.lzy.okgo.model.Response
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.lang.Exception
import java.util.*

/**
 * @author ZEL
 * @create By ZEL on 2020/3/30 17:21
 **/
open class BaseHttpUtils {


    companion object{

        var token = ""

    }

    private val mSuccessful = 999
    private val mFailure = 998
    private val mFailureLogin = 997
    //成功

    private lateinit var mHttpResponseListener: HttpResponseListenerBase

    private var mHandler: Handler = Handler {


        when (it.arg1) {

            mFailureLogin -> {

             //登录失效

            }

            mSuccessful -> {

                mHttpResponseListener.onSuccessful(it, it.what)

            }
            mFailure -> {



                if (it.obj is Response<*>) {
                    // UUtils.showLog("错误消息1${it.obj as String}")
                    mHttpResponseListener.onFailure(it.obj as Response<String>?, "", it.what)

                }

            }


        }


        false

    }



    //---------------------------------------------请求开始


    public fun postUrl(url: String, mHttpResponseListener: HttpResponseListenerBase, mHashMap: HashMap<String, String>, mWhat: Int) {
        //正常的传参方式   params<>

        val httpHeaders = HttpHeaders()
        httpHeaders.put("clientType", "0")
        httpHeaders.put("Content-Type", "application/json")
        httpHeaders.put("Authorization", "$token")

        this.mHttpResponseListener = mHttpResponseListener
        var mHttpParams = HttpParams()


        val entries = mHashMap.entries

        val iterator = entries.iterator()

        while (iterator.hasNext()) {

            val next = iterator.next()

            mHttpParams.put(next.key, next.value)
        }
        //application/x-www-form-urlencoded;charset=UTF-8
        OkGo.post<String>(url).tag(UUtils.getContext()).headers(httpHeaders).params(mHttpParams).execute(object : StringCallback() {
            override fun onSuccess(response: Response<String>?) {

                if (response!!.rawResponse.code() == 401) {

                    val message = Message()

                    message.arg1 = mFailureLogin

                    message.what = mWhat

                    mHandler.sendMessage(message)

                    return

                }


                if (response!!.rawResponse.code() == 200) {
                    val body = response.body()

                    val message = Message()

                    message.arg1 = mSuccessful

                    message.what = mWhat

                    message.obj = body


                    mHandler.sendMessage(message)

                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体[body]$body")
                    UUtils.showLog("配置信息$body")
                    // mHttpResponseListener.onSuccessful(message, mWhat)

                } else {
                    // mHttpResponseListener.onFailure(null, "服务器繁忙,请稍后再试!", mWhat)
                    val body = response.body()
                    val message = Message()

                    message.arg1 = mFailure

                    message.what = mWhat

                    message.obj = body

                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体,服务器繁忙,请稍后再试!")
                    mHandler.sendMessage(message)

                }
            }


            override fun onError(response: Response<String>?) {
                super.onError(response)
                //mHttpResponseListener.onFailure(response, "", mWhat)

                UUtils.showLog("请求:进入POST返回体,服务器繁忙${response!!.body()}")

                if (response == null || response!!.body() == null) {
                    UUtils.showMsg(UUtils.getString(R.string.server_error))
                }


                val message = Message()

                message.arg1 = mFailure

                message.what = mWhat

                message.obj = response


                mHandler.sendMessage(message)

            }
        })
    }


    public fun postUrlHeadBoy(url: String, mHttpResponseListener: HttpResponseListenerBase, mHashMap: HashMap<String, String>, json: String, mWhat: Int) {
        //正常的传参方式   params<>





        val httpHeaders = HttpHeaders()
        httpHeaders.put("clientType", "0")
        httpHeaders.put("Content-Type", "application/json")
        httpHeaders.put("Authorization", "$token")

        this.mHttpResponseListener = mHttpResponseListener
        var mHttpParams = HttpParams()


        val entries = mHashMap.entries

        val iterator = entries.iterator()

        while (iterator.hasNext()) {

            val next = iterator.next()

            mHttpParams.put(next.key, next.value)
        }
        OkGo.post<String>(url).tag(UUtils.getContext())
                .headers(httpHeaders)
                .isSpliceUrl(true)
                .upRequestBody(
                        RequestBody.create(MediaType.parse("application/json"), json)
                ).params(mHttpParams)
                .execute(object : StringCallback() {
                    override fun onSuccess(response: Response<String>?) {

                        if (response!!.rawResponse.code() == 401) {

                            val message = Message()

                            message.arg1 = mFailureLogin

                            message.what = mWhat

                            mHandler.sendMessage(message)

                            return

                        }


                        if (response!!.rawResponse.code() == 200) {
                            val body = response.body()

                            val message = Message()

                            message.arg1 = mSuccessful

                            message.what = mWhat

                            message.obj = body


                            mHandler.sendMessage(message)

                            UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                            UUtils.showLog("请求:进入POST返回体[body]$body")
                            UUtils.showLog("配置信息$body")
                            mHttpResponseListener.onSuccessful(message, mWhat)

                        } else {
                            mHttpResponseListener.onFailure(null, "服务器繁忙,请稍后再试!", mWhat)
                            val body = response.body()
                            val message = Message()

                            message.arg1 = mFailure

                            message.what = mWhat

                            message.obj = body
                            UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                            UUtils.showLog("请求:进入POST返回体,服务器繁忙,请稍后再试!")
                            mHandler.sendMessage(message)

                        }
                    }


                    override fun onError(response: Response<String>?) {
                        super.onError(response)
                        mHttpResponseListener.onFailure(response, "", mWhat)

                        UUtils.showLog("请求:进入POST返回体,服务器繁忙<失败>${response!!.body()}")

                        val message = Message()

                        message.arg1 = mFailure

                        message.what = mWhat

                        message.obj = response


                        mHandler.sendMessage(message)

                    }
                })
    }

    public fun postUrlHead(url: String, mHttpResponseListener: HttpResponseListenerBase, mHashMap: HashMap<String, String>, mWhat: Int) {
        //只有Headers体传参  post
        val httpHeaders = HttpHeaders()
        httpHeaders.put("clientType", "0")
        httpHeaders.put("Content-Type", "application/json")
        httpHeaders.put("Authorization", "$token")
        this.mHttpResponseListener = mHttpResponseListener

        val entries = mHashMap.entries

        val iterator = entries.iterator()

        while (iterator.hasNext()) {

            val next = iterator.next()

            httpHeaders.put(next.key, next.value)
        }
        //application/x-www-form-urlencoded;charset=UTF-8
        OkGo.post<String>(url).tag(UUtils.getContext()).headers(httpHeaders).execute(object : StringCallback() {
            override fun onSuccess(response: Response<String>?) {

                if (response!!.rawResponse.code() == 401) {

                    val message = Message()

                    message.arg1 = mFailureLogin

                    message.what = mWhat

                    mHandler.sendMessage(message)

                    return

                }


                if (response!!.rawResponse.code() == 200) {
                    val body = response.body()

                    val message = Message()

                    message.arg1 = mSuccessful

                    message.what = mWhat

                    message.obj = body


                    mHandler.sendMessage(message)

                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体[body]$body")
                    UUtils.showLog("配置信息$body")
                    // mHttpResponseListener.onSuccessful(message, mWhat)

                } else {
                    // mHttpResponseListener.onFailure(null, "服务器繁忙,请稍后再试!", mWhat)
                    val body = response.body()
                    val message = Message()

                    message.arg1 = mFailure

                    message.what = mWhat

                    message.obj = body
                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体,服务器繁忙,请稍后再试!")
                    mHandler.sendMessage(message)

                }
            }


            override fun onError(response: Response<String>?) {
                super.onError(response)
                //mHttpResponseListener.onFailure(response, "", mWhat)

                UUtils.showLog("请求:进入POST返回体,服务器繁忙${response!!.body()}")

                val message = Message()

                message.arg1 = mFailure

                message.what = mWhat

                message.obj = response


                mHandler.sendMessage(message)

            }


        })

    }

    public fun getUrlHead(url: String, mHttpResponseListener: HttpResponseListenerBase, mHashMap: HashMap<String, String>, mWhat: Int) {
        //只有Headers体传参 get

        val httpHeaders = HttpHeaders()
        httpHeaders.put("clientType", "0")
        httpHeaders.put("Content-Type", "application/json")
        httpHeaders.put("Authorization", "$token")
        this.mHttpResponseListener = mHttpResponseListener

        val entries = mHashMap.entries

        val iterator = entries.iterator()

        while (iterator.hasNext()) {

            val next = iterator.next()

            httpHeaders.put(next.key, next.value)
        }
        //application/x-www-form-urlencoded;charset=UTF-8
        OkGo.get<String>(url).tag(UUtils.getContext()).headers(httpHeaders).execute(object : StringCallback() {
            override fun onSuccess(response: Response<String>?) {

                if (response!!.rawResponse.code() == 401) {

                    val message = Message()

                    message.arg1 = mFailureLogin

                    message.what = mWhat

                    mHandler.sendMessage(message)

                    return

                }


                if (response!!.rawResponse.code() == 200) {
                    val body = response.body()

                    val message = Message()

                    message.arg1 = mSuccessful

                    message.what = mWhat

                    message.obj = body


                    mHandler.sendMessage(message)

                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体[body]$body")
                    UUtils.showLog("配置信息$body")
                    // mHttpResponseListener.onSuccessful(message, mWhat)

                } else {
                    // mHttpResponseListener.onFailure(null, "服务器繁忙,请稍后再试!", mWhat)
                    val body = response.body()
                    val message = Message()

                    message.arg1 = mFailure

                    message.what = mWhat

                    message.obj = body
                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体,服务器繁忙,请稍后再试!")
                    mHandler.sendMessage(message)

                }
            }


            override fun onError(response: Response<String>?) {
                super.onError(response)
                //mHttpResponseListener.onFailure(response, "", mWhat)

                UUtils.showLog("请求:进入POST返回体,服务器繁忙${response!!.body()}")

                val message = Message()

                message.arg1 = mFailure

                message.what = mWhat

                message.obj = response


                mHandler.sendMessage(message)

            }


        })

    }


    public fun getUrlHeadBoy(url: String, mHttpResponseListener: HttpResponseListenerBase, mHashMap: HashMap<String, String>, mWhat: Int) {

        //Headers传参  get

        val httpHeaders = HttpHeaders()
        httpHeaders.put("clientType", "0")
        httpHeaders.put("Content-Type", "application/json")
        httpHeaders.put("Authorization", "$token")
        this.mHttpResponseListener = mHttpResponseListener

        val entries = mHashMap.entries

        val iterator = entries.iterator()

        while (iterator.hasNext()) {

            val next = iterator.next()

            httpHeaders.put(next.key, next.value)
        }
        //application/x-www-form-urlencoded;charset=UTF-8
        OkGo.get<String>(url).tag(UUtils.getContext()).headers(httpHeaders).execute(object : StringCallback() {
            override fun onSuccess(response: Response<String>?) {


                if (response!!.rawResponse.code() == 401) {

                    val message = Message()

                    message.arg1 = mFailureLogin

                    message.what = mWhat

                    mHandler.sendMessage(message)

                    return

                }


                if (response!!.rawResponse.code() == 200) {
                    val body = response.body()

                    val message = Message()

                    message.arg1 = mSuccessful

                    message.what = mWhat

                    message.obj = body


                    mHandler.sendMessage(message)

                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体[body]$body")
                    UUtils.showLog("配置信息$body")
                    // mHttpResponseListener.onSuccessful(message, mWhat)

                } else {
                    // mHttpResponseListener.onFailure(null, "服务器繁忙,请稍后再试!", mWhat)
                    val body = response.body()
                    val message = Message()

                    message.arg1 = mFailure

                    message.what = mWhat

                    message.obj = body
                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体,服务器繁忙,请稍后再试!")
                    mHandler.sendMessage(message)

                }
            }


            override fun onError(response: Response<String>?) {
                super.onError(response)
                //mHttpResponseListener.onFailure(response, "", mWhat)

                UUtils.showLog("请求:进入POST返回体,服务器繁忙${response!!.body()}")

                val message = Message()

                message.arg1 = mFailure

                message.what = mWhat

                message.obj = response


                mHandler.sendMessage(message)

            }


        })

    }


    public fun postUrlJson(url: String, mHttpResponseListener: HttpResponseListenerBase, json: String, mWhat: Int) {
        //head  +  body 格式 参数




        this.mHttpResponseListener = mHttpResponseListener

        val httpHeaders = HttpHeaders()
        httpHeaders.put("clientType", "0")
        httpHeaders.put("Content-Type", "application/json")
        httpHeaders.put("Authorization", "$token")
        //json
        //application/x-www-form-urlencoded;charset=UTF-8

        OkGo.post<String>(url).tag(UUtils.getContext()).headers(httpHeaders).upRequestBody(RequestBody.create(MediaType.parse("application/json"), json)).execute(object : StringCallback() {
            override fun onSuccess(response: Response<String>?) {


                if (response!!.rawResponse.code() == 401) {

                    val message = Message()

                    message.arg1 = mFailureLogin

                    message.what = mWhat

                    mHandler.sendMessage(message)

                    return

                }


                if (response!!.rawResponse.code() == 200) {
                    val body = response.body()

                    val message = Message()

                    message.arg1 = mSuccessful

                    message.what = mWhat

                    message.obj = body


                    mHandler.sendMessage(message)

                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("请求:进入POST返回体[body]$body")
                    UUtils.showLog("配置信息$body")
                    // mHttpResponseListener.onSuccessful(message, mWhat)

                } else {
                    // mHttpResponseListener.onFailure(null, "服务器繁忙,请稍后再试!", mWhat)
                    val body = response.body()

                    val message = Message()

                    message.arg1 = mFailure

                    message.what = mWhat

                    message.obj = body
                    UUtils.showLog("配置信息${response!!.rawResponse.code()}")
                    UUtils.showLog("配置信息${body}")
                    mHandler.sendMessage(message)

                }
            }


            override fun onError(response: Response<String>?) {
                super.onError(response)
                //mHttpResponseListener.onFailure(response, "", mWhat)

                UUtils.showLog("请求:进入POST返回体,服务器繁忙${response!!.body()}")

                val message = Message()

                message.arg1 = mFailure

                message.what = mWhat

                message.obj = response


                mHandler.sendMessage(message)

            }

        })

    }

    public fun getUrl(url: String, mHttpResponseListener: HttpResponseListenerBase, mHashMap: HashMap<String, String>, mWhat: Int) {

        //params<>  get

        val httpHeaders = HttpHeaders()
        httpHeaders.put("clientType", "0")
        httpHeaders.put("Content-Type", "application/json")
        httpHeaders.put("Authorization", "$token")
        var mHttpParams = HttpParams()

        this.mHttpResponseListener = mHttpResponseListener

        val entries = mHashMap.entries

        val iterator = entries.iterator()

        while (iterator.hasNext()) {

            val next = iterator.next()

            mHttpParams.put(next.key, next.value)
        }

        OkGo.get<String>(url).tag(UUtils.getContext()).headers(httpHeaders).params(mHttpParams).execute(object : StringCallback() {
            override fun onSuccess(response: Response<String>?) {


                if (response!!.rawResponse.code() == 401) {

                    val message = Message()

                    message.arg1 = mFailureLogin

                    message.what = mWhat

                    mHandler.sendMessage(message)

                    return

                }

                if (response!!.rawResponse.code() == 200) {
                    val body = response.body()

                    val message = Message()

                    message.arg1 = mSuccessful

                    message.what = mWhat

                    message.obj = body

                  //  UUtils.showLog("成功消息2222:$body")
                    mHandler.sendMessage(message)

                } else {

                    val body = response.body()
                    val message = Message()

                    message.arg1 = mFailure

                    message.what = mWhat

                    message.obj = body

                    UUtils.showLog("错误消息2222:$body")
                    mHandler.sendMessage(message)

                }

            }

            override fun onError(response: Response<String>?) {
                super.onError(response)
                val message = Message()

                message.arg1 = mFailure

                message.what = mWhat

                message.obj = response


                mHandler.sendMessage(message)


            }


        })
    }

    public fun postUploadImage(url: String, mHttpResponseListener: HttpResponseListenerBase, files: List<File>, mWhat: Int) {
        //post  上传图片
        val httpHeaders = HttpHeaders()
        httpHeaders.put("clientType", "0")
        this.mHttpResponseListener = mHttpResponseListener

        OkGo.post<String>(url).tag(UUtils.getContext()).headers(httpHeaders).addFileParams("files", files).execute(object : StringCallback() {
            override fun onSuccess(response: Response<String>?) {


                if (response!!.rawResponse.code() == 401) {

                    val message = Message()

                    message.arg1 = mFailureLogin

                    message.what = mWhat

                    mHandler.sendMessage(message)

                    return

                }

                if (response!!.rawResponse.code() == 200) {
                    val body = response.body()

                    val message = Message()

                    message.arg1 = mSuccessful

                    message.what = mWhat

                    message.obj = body


                    mHandler.sendMessage(message)

                } else {

                    val body = response.body()
                    val message = Message()

                    message.arg1 = mFailure

                    message.what = mWhat

                    message.obj = body


                    UUtils.showMsg("错误消息2222:$body")
                    mHandler.sendMessage(message)


                }

            }

            override fun onError(response: Response<String>?) {
                super.onError(response)
                val message = Message()

                message.arg1 = mFailure

                message.what = mWhat

                message.obj = response


                mHandler.sendMessage(message)


            }


        })
    }
}

public interface HttpResponseListenerBase {

    fun onSuccessful(msg: Message, mWhat: Int)

    fun onFailure(response: Response<String>?, msg: String, mWhat: Int)


}


