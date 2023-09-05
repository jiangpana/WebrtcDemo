package com.jansir.webrtcdemo

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

object VOIPService {

    private val scope = MainScope()
    private var sock: Socket? = null
    private var serverSock: ServerSocket? = null
    private var input: InputStream? = null
    private var output: OutputStream? = null

    var readCallBack:((ByteArray)->Unit)?=null

    private val buff =ByteArray(1024*64)


    var host ="192.168.1.4"

    val TAG ="VOIPService"
    fun start(isCaller: Boolean) {
        Log.d(TAG, "start() called with: isCaller = $isCaller")
        scope.launch(Dispatchers.IO) {
            if (!isCaller) {
                sock = Socket()
                sock!!.connect(InetSocketAddress(host, 9000))
                sock?.apply {
                    input = getInputStream()
                    output = getOutputStream()
                }
            } else {
                serverSock = ServerSocket(9000)
                serverSock!!.accept()?.apply {
                    input = getInputStream()
                    output = getOutputStream()
                }
            }

            Log.d(TAG, "start() called connected isCaller =$isCaller")
            var length = 0
            while(input!!.read(buff).also {
                    length=it
                }!=-1&&isActive){
                readCallBack?.invoke(buff.copyOfRange(0,length))
            }
        }
    }

    fun sendMessage(byteArray: ByteArray){
        scope.launch(Dispatchers.IO) {
            output?.write(byteArray)
        }
    }

    fun close(){
        sock?.close()
        serverSock?.close()
    }

}