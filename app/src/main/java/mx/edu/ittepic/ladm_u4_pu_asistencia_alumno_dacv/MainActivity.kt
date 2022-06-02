package mx.edu.ittepic.ladm_u4_pu_asistencia_alumno_dacv

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import mx.edu.ittepic.ladm_u4_pu_asistencia_alumno_dacv.databinding.ActivityMainBinding
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    var bluetoothHeadset: BluetoothHeadset? = null

    // Get the default adapter
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    private val LOCATION_PERMISSION_REQUEST = 101
    private val BLUETOOTH_PERMISSION_REQUEST = 102
    private val SELECT_DEVICE = 102
    private val REQUEST_ENABLE_BT = 1
    private val MAC_ADDRESS = 301

    private val uuid: UUID =
        UUID.fromString("06ae0a74-7bd4-43aa-ab5d-2511f3f6bab1") // GENERATE NEW UUID IF IT WONT WORK
    private lateinit var mySelectedBluetoothDevice: BluetoothDevice
    private lateinit var socket: BluetoothSocket
    private lateinit var myHandler: Handler

    lateinit var m_address: String
    private var connectedDevice: String? = null
    private var adapterMainChat: ArrayAdapter<String>? = null
    val arrayMessage = ArrayList<String>()

    val MESSAGE_STATE_CHANGED = 0
    val MESSAGE_READ = 1
    val MESSAGE_WRITE = 2
    val MESSAGE_DEVICE_NAME = 3
    val MESSAGE_TOAST = 4


    val STATE_NONE = 0
    val STATE_LISTEN = 1
    val STATE_CONNECTING = 2
    val STATE_CONNECTED = 3

    val DEVICE_NAME = "deviceName"
    val TOAST = "toast"

    lateinit var chatUtils : MessageUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT), BLUETOOTH_PERMISSION_REQUEST
            )
        }

        binding.tvDevice.setOnClickListener {
            Log.i("######",arrayMessage.size.toString())
        }

        bluetoothAdapter?.getProfileProxy(this, profileListener, BluetoothProfile.HEADSET)
        binding.btnPresente.setOnClickListener {
            val message: String = binding.etNoControl.text.toString()
            if (!message.isEmpty()) {
                binding.etNoControl.setText("")
                chatUtils.write(message.toByteArray())
            }

        }


        adapterMainChat = ArrayAdapter(this, android.R.layout.simple_list_item_1)

        binding.list.adapter = adapterMainChat

        chatUtils = MessageUtils(this,handler)
    }

    override fun onStart() {
        super.onStart()
        m_address = intent.getStringExtra("deviceAddress").toString()

        if (m_address == "") {
            Log.i("######## 94", "vacia")
        } else {
            Log.i("######## 96", m_address)
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                val mac = device.address // MAC address
                if (mac == m_address) {
                    mySelectedBluetoothDevice = device
                    binding.tvDevice.setText(device.name.toString())
                    chatUtils.connect(device)
                }

            }
        }
    }

    private fun setState(subTitle: CharSequence) {
       binding.tvState.setText(subTitle)
    }


    private val profileListener = object : BluetoothProfile.ServiceListener {

        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = proxy as BluetoothHeadset
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothHeadset = null
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main_activity, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_search_devices -> {
                checkPermissions()
                true
            }
            R.id.menu_enable_bluetooth -> {
                enableBluetooth()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun enableBluetooth() {
        val BTadapter = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        BTadapter.getAdapter()
        if (BTadapter != null) {
            if (BTadapter.adapter?.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
                        BLUETOOTH_PERMISSION_REQUEST
                    )
                }
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }
            if (BTadapter.adapter.isEnabled) {
                Toast.makeText(this, "El bluetooth ya está encendido", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Device doesnt support bluetooth", Toast.LENGTH_LONG).show()
        }

    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            val intent = Intent(this, ListDeviceActivity::class.java)
            startActivityForResult(intent, SELECT_DEVICE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            BLUETOOTH_PERMISSION_REQUEST -> {
                Toast.makeText(this, "Se otorgo el permiso", Toast.LENGTH_SHORT).show()
            }
            LOCATION_PERMISSION_REQUEST -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("$$$$$", "Dentro")
                    val intent = Intent(this, ListDeviceActivity::class.java)
                    startActivityForResult(intent, SELECT_DEVICE)
                } else {
                    AlertDialog.Builder(this)
                        .setCancelable(false)
                        .setMessage("Location permission is required.\n Please grant")
                        .setPositiveButton("Grant",
                            DialogInterface.OnClickListener { dialogInterface, i -> checkPermissions() })
                        .setNegativeButton("Deny",
                            DialogInterface.OnClickListener { dialogInterface, i -> finish() })
                        .show()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            LOCATION_PERMISSION_REQUEST -> {
                val intent = Intent(this, ListDeviceActivity::class.java)
                startActivityForResult(intent, SELECT_DEVICE)
            }
            SELECT_DEVICE -> {
                if (requestCode == Activity.RESULT_OK) {
                    val address = data?.getStringExtra("deviceAddress")
                    Log.i("####### 205", address.toString())
                    // chatUtils.connect(bluetoothAdapter!!.getRemoteDevice(address))

                }
            }
            MAC_ADDRESS -> {
                val address = data?.getStringExtra("deviceAddress")
                Log.i("####### 212", address.toString())
            }
        }
    }


    private val handler = Handler { message ->
        Log.i("##### 266",message.what.toString())
        when (message.what) {
            MESSAGE_STATE_CHANGED -> when (message.arg1) {
                STATE_NONE -> setState("Not Connected")
                STATE_LISTEN -> setState("Not Connected")
                STATE_CONNECTING -> setState("Connecting...")
                STATE_CONNECTED -> setState("Connected: $connectedDevice")
            }
            MESSAGE_WRITE -> {
                val buffer1 = message.obj as ByteArray
                val outputBuffer = String(buffer1)
                adapterMainChat?.add("Me: $outputBuffer")
                Log.i("####### 278", outputBuffer)
            }
            MESSAGE_READ -> {
                val buffer = message.obj as ByteArray
                val inputBuffer = String(buffer, 0, message.arg1)
                arrayMessage.add(inputBuffer)
                adapterMainChat?.add(connectedDevice + ": " + inputBuffer)
                Log.i("####### 85", inputBuffer)
            }
            MESSAGE_DEVICE_NAME -> {
                connectedDevice = message.data.getString(DEVICE_NAME)
                Toast.makeText(this, connectedDevice, Toast.LENGTH_SHORT).show()
            }
            MESSAGE_TOAST -> Toast.makeText(
                this,
                message.data.getString(TOAST),
                Toast.LENGTH_SHORT
            ).show()
        }
        false
    }


}
