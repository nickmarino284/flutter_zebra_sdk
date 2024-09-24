package com.tlt.flutter_zebra_sdk

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.zebra.sdk.btleComm.BluetoothLeConnection
import com.zebra.sdk.comm.Connection
import com.zebra.sdk.comm.ConnectionException
import com.zebra.sdk.comm.TcpConnection
import com.zebra.sdk.printer.discovery.*
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

interface JSONConvertable {
  fun toJSON(): String = Gson().toJson(this)
}

inline fun <reified T : JSONConvertable> String.toObject(): T = Gson().fromJson(this, T::class.java)

data class ZebreResult(
        var type: String? = null,
        var success: Boolean? = null,
        var message: String? = null,
        var content: Any? = null
) : JSONConvertable

class ZebraPrinterInfo(
        var address: String? = null,
        var productName: String? = null,
        var serialNumber: String? = null,
        var availableInterfaces: Any? = null,
        var darkness: String? = null,
        var availableLanguages: Any? = null,
        val linkOSMajorVer: Long? = null,
        val firmwareVer: String? = null,
        var jsonPortNumber: String? = null,
        val primaryLanguage: String? = null
): JSONConvertable

class FlutterZebraSdkPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
  // / The MethodChannel that will the communication between Flutter and native Android
  // /
  // / This local reference serves to register the plugin with the Flutter Engine and unregister it
  // / when the Flutter Engine is detached from the Activity
  private lateinit var channel: MethodChannel
  private var logTag: String = "ZebraSDK"
  private lateinit var context: Context
  private var activity: Activity? = null
  var printers: MutableList<ZebraPrinterInfo> = ArrayList()

  override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "flutter_zebra_sdk")
    channel.setMethodCallHandler(this)
    context = flutterPluginBinding.applicationContext
  }

  override fun onMethodCall(@NonNull call: MethodCall, @NonNull rawResult: Result) {
    val result: MethodResultWrapper = MethodResultWrapper(rawResult)
    Thread(MethodRunner(call, result)).start()
  }

  override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
  }

  override fun onDetachedFromActivity() {
    activity = null
  }

  override fun onDetachedFromActivityForConfigChanges() {
    onDetachedFromActivity()
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    onAttachedToActivity(binding)
  }

  inner class MethodRunner(call: MethodCall, result: Result) : Runnable {
    private val call: MethodCall = call
    private val result: Result = result

    override fun run() {
      when (call.method) {
        "printZPLOverTCPIP" -> {
          onPrintZPLOverTCPIP(call, result)
        }
        "printZPLOverBluetooth" -> {
          onPrintZplDataOverBluetooth(call, result)
        }
        "onDiscovery" -> {
          onDiscovery(call, result)
        }
        "onDiscoveryUSB" -> {
          onDiscoveryUSB(call, result)
        }
        "onGetPrinterInfo" -> {
          onGetPrinterInfo(call, result)
        }
        "isPrinterConnected" -> {
          isPrinterConnected(call, result)
        }
        else -> result.notImplemented()
      }
    }
  }

  class MethodResultWrapper(methodResult: Result) : Result {
    private val methodResult: Result = methodResult
    private val handler: Handler = Handler(Looper.getMainLooper())

    override fun success(result: Any?) {
      handler.post { methodResult.success(result) }
    }

    override fun error(errorCode: String, errorMessage: String?, errorDetails: Any?) {
      handler.post { methodResult.error(errorCode, errorMessage, errorDetails) }
    }

    override fun notImplemented() {
      handler.post { methodResult.notImplemented() }
    }
  }

  private fun createTcpConnect(ip: String, port: Int): TcpConnection {
    return TcpConnection(ip, port)
  }

  private fun onPrintZPLOverTCPIP(@NonNull call: MethodCall, @NonNull result: Result) {
    val ip = call.argument<String>("ip")
    val data = call.argument<String>("data")

    if (ip.isNullOrEmpty()) {
      result.error("PrintZPLOverTCPIP", "IP Address is required", null)
      return
    }

    if (data.isNullOrEmpty()) {
      result.error("PrintZPLOverTCPIP", "Data is required", null)
      return
    }

    val conn: Connection = createTcpConnect(ip, TcpConnection.DEFAULT_ZPL_TCP_PORT)

    try {
      conn.open()
      Log.d(logTag, "Sending data to printer at $ip: $data")

      conn.write(data.toByteArray())
      result.success("Print successful")
    } catch (e: ConnectionException) {
      Log.e(logTag, "Connection failed: ${e.message}")
      result.error("ConnectionError", "Connection failed", e.message)
    } finally {
      conn.close()
    }
  }


  private fun onPrintZplDataOverBluetooth(@NonNull call: MethodCall, @NonNull result: Result) {
    val macAddress: String? = call.argument("mac")
    val data: String? = call.argument("data")

    if (data == null || macAddress == null) {
      result.error("INVALID_ARGUMENTS", "MAC Address and Data are required", null)
      return
    }

    var conn: BluetoothLeConnection? = null
    try {
      conn = BluetoothLeConnection(macAddress, context)
      conn.open()
      conn.write(data.toByteArray())
      Thread.sleep(500)
      result.success("Successfully sent data to printer")
    } catch (e: Exception) {
      e.printStackTrace()
      result.error("PRINT_ERROR", "Failed to print over Bluetooth", e)
    } finally {
      conn?.close()
    }
  }

  private fun onGetPrinterInfo(@NonNull call: MethodCall, @NonNull result: Result) {
    val ipAddress: String? = call.argument("ip")
    val port: Int = call.argument("port") ?: TcpConnection.DEFAULT_ZPL_TCP_PORT

    if (ipAddress == null) {
      result.error("INVALID_ARGUMENTS", "IP Address is required", null)
      return
    }

    val conn: Connection = TcpConnection(ipAddress, port)
    try {
      conn.open()
      if (!conn.isConnected) {
        Log.e(logTag, "Connection to printer at $ipAddress:$port failed.")
        result.error("CONNECTION_ERROR", "Failed to connect to printer", null)
        return
      }
      else {
        Log.d(logTag, "Connected to printer at $ipAddress:$port")
      }

      try {
        val dataMap = DiscoveryUtil.getDiscoveryDataMap(conn)
        Log.d(logTag, "Received Discovery Data: $dataMap")

        // Validate keys in the dataMap
        val printerInfo = ZebraPrinterInfo(
          serialNumber = dataMap["SERIAL_NUMBER"] ?: "N/A",
          address = dataMap["ADDRESS"] ?: "N/A",
          availableInterfaces = dataMap["AVAILABLE_INTERFACES"] ?: "N/A",
          availableLanguages = dataMap["AVAILABLE_LANGUAGES"] ?: "N/A",
          darkness = dataMap["DARKNESS"] ?: "N/A",
          jsonPortNumber = dataMap["JSON_PORT_NUMBER"] ?: "N/A",
          productName = dataMap["PRODUCT_NAME"] ?: "N/A"
        )

        result.success(Gson().toJson(printerInfo))
      } catch (e: DiscoveryPacketDecodeException) {
        Log.e(logTag, "Failed to parse discovery packet: ${e.message}")
        result.error("DISCOVERY_ERROR", "Invalid discovery packet received", e.message)
      }
    } catch (e: ConnectionException) {
      Log.e(logTag, "Connection error: ${e.message}")
      result.error("CONNECTION_ERROR", "Failed to connect to printer", e.message)
    } finally {
      conn.close()
    }
  }



  private fun isPrinterConnected(@NonNull call: MethodCall, @NonNull result: Result) {
    var ipE: String? = call.argument("ip")
    var ipPort: Int? = call.argument("port")
    var ipAddress: String = ""
    var port: Int = TcpConnection.DEFAULT_ZPL_TCP_PORT

    if (ipE != null) {
      ipAddress = ipE
    } else {
      result.error("isPrinterConnected", "IP Address is required", "Data Content")
      return
    }

    if (ipPort != null) {
      port = ipPort
    }

    val conn: Connection = createTcpConnect(ipAddress, port)
    var resp = ZebreResult()

    try {
      Log.d(logTag, "Connecting to printer at $ipAddress:$port")
      conn.open()

      if (!conn.isConnected) {
        Log.e(logTag, "Connection to printer failed.")
        resp.success = false
        resp.message = "Unconnected"
        result.success(resp.toJSON())
        return
      }

      try {
        val dataMap = DiscoveryUtil.getDiscoveryDataMap(conn)
        Log.d(logTag, "Discovery Data Map: $dataMap")
      } catch (e: DiscoveryPacketDecodeException) {
        Log.e(logTag, "Failed to parse discovery packet: ${e.message}")
        resp.success = false
        resp.message = "Invalid discovery packet"
        result.success(resp.toJSON())
        return
      }

      resp.success = true
      resp.message = "Connected"
      result.success(resp.toJSON())
    } catch (e: ConnectionException) {
      Log.e(logTag, "Connection error: ${e.message}")
      resp.success = false
      resp.message = "Unconnected"
      result.success(resp.toJSON())
    } finally {
      conn.close()
    }
  }



  private fun onDiscovery(@NonNull call: MethodCall, @NonNull result: Result) {
    var handleNet = object : DiscoveryHandler {
      override fun foundPrinter(p0: DiscoveredPrinter) {
        Log.d(logTag, "foundPrinter $p0")
        var dataMap = p0.discoveryDataMap
        var address = dataMap["ADDRESS"]
        var isExist = printers.any { s -> s.address == address }
        if(!isExist){
          var printer: ZebraPrinterInfo = ZebraPrinterInfo()
          printer.serialNumber = dataMap["SERIAL_NUMBER"]
          printer.address = address
          printer.availableInterfaces = dataMap["AVAILABLE_INTERFACES"]
          printer.availableLanguages = dataMap["AVAILABLE_LANGUAGES"]
          printer.darkness = dataMap["DARKNESS"]
          printer.jsonPortNumber = dataMap["JSON_PORT_NUMBER"]
          printer.productName = dataMap["PRODUCT_NAME"]
          printers.add(printer)
        }
      }

      override fun discoveryFinished() {
        Log.d(logTag, "discoveryFinished $printers")
        var resp = ZebreResult()
        resp.success = true
        resp.message= "Successfully!"
        var printersJSON = Gson().toJson(printers)
        resp.content = printersJSON
        result.success(resp.toJSON())
      }

      override fun discoveryError(p0: String?) {
        Log.d(logTag, "discoveryError $p0")
        result.error("discoveryError", "discoveryError", p0)
      }
    }
    try {
      printers.clear()
      NetworkDiscoverer.findPrinters(handleNet)
    } catch (e: Exception) {
      e.printStackTrace()
      result.error("Error", "onDiscovery", e)
    }
     var net =  DiscoveredPrinterNetwork("a", 1)

  }


  private fun onDiscoveryUSB(@NonNull call: MethodCall, @NonNull result: Result) {
    var handleNet = object : DiscoveryHandler {
      override fun foundPrinter(p0: DiscoveredPrinter) {
        Log.d(logTag, "foundPrinter $p0")
        var dataMap = p0.discoveryDataMap
        var address = dataMap["ADDRESS"]
        var isExist = printers.any { s -> s.address == address }
        if(!isExist){
          var printer: ZebraPrinterInfo = ZebraPrinterInfo()
          printer.serialNumber = dataMap["SERIAL_NUMBER"]
          printer.address = address
          printer.availableInterfaces = dataMap["AVAILABLE_INTERFACES"]
          printer.availableLanguages = dataMap["AVAILABLE_LANGUAGES"]
          printer.darkness = dataMap["DARKNESS"]
          printer.jsonPortNumber = dataMap["JSON_PORT_NUMBER"]
          printer.productName = dataMap["PRODUCT_NAME"]
          printers.add(printer)
        }
      }

      override fun discoveryFinished() {
        Log.d(logTag, "discoveryUSBFinished $printers")
        var resp = ZebreResult()
        resp.success = true
        resp.message= "Successfully!"
        var printersJSON = Gson().toJson(printers)
        resp.content = printersJSON
        result.success(resp.toJSON())
      }

      override fun discoveryError(p0: String?) {
        Log.d(logTag, "discoveryUSBError $p0")
        result.error("discoveryUSBError", "discoveryUSBError", p0)
      }
    }
    try {
      printers.clear()
      UsbDiscoverer.findPrinters(context, handleNet)
    } catch (e: Exception) {
      e.printStackTrace()
      result.error("Error", "onDiscoveryUSB", e)
    }

  }
}
