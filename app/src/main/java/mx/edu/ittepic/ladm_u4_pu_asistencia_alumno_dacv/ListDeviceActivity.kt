package mx.edu.ittepic.ladm_u4_pu_asistencia_alumno_dacv

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import mx.edu.ittepic.ladm_u4_pu_asistencia_alumno_dacv.databinding.ActivityListDeviceBinding

class ListDeviceActivity : AppCompatActivity() {

    lateinit var binding: ActivityListDeviceBinding

    private lateinit var listPairedDevices :ListView
    private lateinit var listAvailableDevices :ListView
    private lateinit var progressScanDevices : ProgressBar

    private var adapterPairedDevices: ArrayAdapter<String>? = null
    private var adapterAvailableDevices: ArrayAdapter<String>? = null
    private var context: Context? = null
    private var bluetoothAdapter: BluetoothAdapter? = null

    private lateinit var mySelectedBluetoothDevice: BluetoothDevice


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListDeviceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = this

        listPairedDevices = binding.listPairedDevices
       // listAvailableDevices = binding.listAvailableDevices
        progressScanDevices = binding.progressScanDevices

        adapterPairedDevices = ArrayAdapter(context!!, R.layout.item_list_device)
        adapterAvailableDevices = ArrayAdapter(context!!, R.layout.item_list_device)

        listPairedDevices.adapter = adapterPairedDevices
        //listAvailableDevices.adapter = adapterAvailableDevices

        /*listAvailableDevices.onItemClickListener =
            OnItemClickListener { adapterView, view, i, l ->
                val info = (view as TextView).text.toString()
                val address = info.substring(info.length - 17)
                val intent = Intent()
                Log.i("###### 56",address)
                intent.putExtra("deviceAddress", address)
                setResult(RESULT_OK, intent)
                //finish()
            }
*/
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.BLUETOOTH_CONNECT),
                192
            )
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            val deviceName = device.name
            val deviceHardwareAddress = device.address // MAC address
            //Log.i("$$$", "$deviceName $deviceHardwareAddress")
            adapterPairedDevices!!.add(
                """
                     ${device.name}
                     ${device.address}
                     """.trimIndent()
            )
            mySelectedBluetoothDevice = device
        }

        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(bluetoothDeviceListener, intentFilter)
        val intentFilter1 = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(bluetoothDeviceListener, intentFilter1)

        listPairedDevices.onItemClickListener =
            OnItemClickListener { adapterView, view, i, l ->
                //bluetoothAdapter.cancelDiscovery()
                val info = (view as TextView).text.toString()
                val address = info.substring(info.length - 17)
                Log.i("Address", address)
                val intent = Intent(this,MainActivity::class.java)
                //val intent = Intent()
                intent.putExtra("deviceAddress", address)
                intent.putExtra("device", mySelectedBluetoothDevice)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                //setResult(RESULT_OK, intent)
                startActivity(intent)
                finish()
            }

    }

    private val bluetoothDeviceListener: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device =
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device!!.bondState != BluetoothDevice.BOND_BONDED) {
                    adapterAvailableDevices!!.add(
                        """
                        ${device.name}
                        ${device.address}
                        """.trimIndent()
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            192->{
                Toast.makeText(context, "Ya hay permiso", Toast.LENGTH_SHORT).show()
            }
        }
    }
}