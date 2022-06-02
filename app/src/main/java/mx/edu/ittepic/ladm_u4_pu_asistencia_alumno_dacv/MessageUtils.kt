package mx.edu.ittepic.ladm_u4_pu_asistencia_alumno_dacv

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

public class MessageUtils(context: Context?, handler: Handler?) {

    private val APP_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val APP_NAME = "BluetoothChatApp"

    private var context: Context? = null
    private var handler: Handler? = null
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null

    val STATE_NONE = 0
    val STATE_LISTEN = 1
    val STATE_CONNECTING = 2
    val STATE_CONNECTED = 3

    val MESSAGE_STATE_CHANGED = 0
    val MESSAGE_READ = 1
    val MESSAGE_WRITE = 2
    val MESSAGE_DEVICE_NAME = 3
    val MESSAGE_TOAST = 4

    val DEVICE_NAME = "deviceName"
    val TOAST = "toast"

    private var stateValue = 0


   init {
        this.context = context
        this.handler = handler
        stateValue = STATE_NONE
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    fun getState(): Int {
        return stateValue
    }

    @Synchronized
    fun setState(stateValue: Int) {
        this.stateValue = stateValue
        handler!!.obtainMessage(MESSAGE_STATE_CHANGED, stateValue, -1).sendToTarget()
    }

    @Synchronized
    private fun start() {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (acceptThread == null) {
            acceptThread = AcceptThread()
            acceptThread!!.start()
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_LISTEN)
    }

    @Synchronized
    fun stop() {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (acceptThread != null) {
            acceptThread!!.cancel()
            acceptThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_NONE)
    }

    fun connect(device: BluetoothDevice?) {
        if (stateValue == STATE_CONNECTING) {
            connectThread?.cancel()
            connectThread = null
        }
        connectThread = device?.let { ConnectThread(it) }
        connectThread?.start()
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        setState(STATE_CONNECTING)
    }

    fun write(buffer: ByteArray?) {
        var connThread: MessageUtils.ConnectedThread
        synchronized(this) {
            if (stateValue != STATE_CONNECTED) {
                return
            }
            connThread = connectedThread!!
        }
        connThread.write(buffer)
    }

    inner class AcceptThread : Thread() {
        private val serverSocket: BluetoothServerSocket?
        override fun run() {
            var socket: BluetoothSocket? = null
            try {
                socket = serverSocket!!.accept()
            } catch (e: IOException) {
                Log.e("Accept->Run", e.toString())
                try {
                    serverSocket!!.close()
                } catch (e1: IOException) {
                    Log.e("Accept->Close", e.toString())
                }
            }
            if (socket != null) {
                when (stateValue) {
                    STATE_LISTEN, STATE_CONNECTING -> connected(
                        socket,
                        socket.remoteDevice
                    )
                    STATE_NONE, STATE_CONNECTED -> try {
                        socket.close()
                    } catch (e: IOException) {
                        Log.e("Accept->CloseSocket", e.toString())
                    }
                }
            }
        }

        fun cancel() {
            try {
                serverSocket!!.close()
            } catch (e: IOException) {
                Log.e("Accept->CloseServer", e.toString())
            }
        }

        init {
            var tmp: BluetoothServerSocket? = null
            try {
                tmp = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
            } catch (e: IOException) {
                Log.e("Accept->Constructor", e.toString())
            }
            serverSocket = tmp
        }
    }

    inner class ConnectThread(private val device: BluetoothDevice) : Thread() {
        private val socket: BluetoothSocket?
        override fun run() {
            try {
                socket!!.connect()
            } catch (e: IOException) {
                Log.e("Connect->Run", e.toString())
                try {
                    socket!!.close()
                } catch (e1: IOException) {
                    Log.e("Connect->CloseSocket", e.toString())
                }
                connectionFailed()
                return
            }
            synchronized(this) { connectThread = null }
            connected(socket, device)
        }

        fun cancel() {
            try {
                socket!!.close()
            } catch (e: IOException) {
                Log.e("Connect->Cancel", e.toString())
            }
        }

        init {
            var tmp: BluetoothSocket? = null
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID)
            } catch (e: IOException) {
                Log.e("Connect->Constructor", e.toString())
            }
            socket = tmp
        }
    }

    inner class ConnectedThread(private val socket: BluetoothSocket?) : Thread() {

        private var inputStream: InputStream? = null
        private var outputStream: OutputStream? = null
        override fun run() {
            val buffer = ByteArray(1024)
            val bytes: Int
            try {
                bytes = inputStream!!.read(buffer)
                handler?.obtainMessage(MESSAGE_READ, bytes, -1, buffer)?.sendToTarget()
            } catch (e: IOException) {
                connectionLost()
            }
        }

        fun write(buffer: ByteArray?) {
            try {
                outputStream!!.write(buffer)
                handler?.obtainMessage(MESSAGE_WRITE, -1, -1, buffer)?.sendToTarget()
            } catch (e: IOException) {
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
            }
        }

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null
            try {
                tmpIn = socket?.inputStream
                tmpOut = socket?.outputStream
            } catch (e: IOException) {
            }
            inputStream = tmpIn
            outputStream = tmpOut
        }
    }

    private fun connectionLost() {
        val message: Message? = handler?.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, "Connection Lost")
        message!!.data = bundle
        handler?.sendMessage(message)
        this.start()
    }

    @Synchronized
    private fun connectionFailed() {
        val message: Message? = handler?.obtainMessage(MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(TOAST, "Cant connect to the device")
        message!!.data = bundle
        handler?.sendMessage(message)
        this@MessageUtils.start()
    }

    @Synchronized
    private fun connected(socket: BluetoothSocket, device: BluetoothDevice) {
        if (connectThread != null) {
            connectThread!!.cancel()
            connectThread = null
        }
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }
        connectedThread = ConnectedThread(socket)
        connectedThread!!.start()
        val message = handler!!.obtainMessage(MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(DEVICE_NAME, device.name)
        message.data = bundle
        handler!!.sendMessage(message)
        setState(STATE_CONNECTED)
    }
}