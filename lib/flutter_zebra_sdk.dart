import 'dart:async';
import 'dart:io';
import 'dart:convert';


import 'package:flutter/services.dart';

class ZebraSdk {
  static const MethodChannel _channel =
      const MethodChannel('flutter_zebra_sdk');

  static Future<dynamic> printZPLOverTCPIP(String ipAddress, {int? port, String? data}) async {
    final Map<String, dynamic> params = {"ip": ipAddress};
    if (port != null) {
      params['port'] = port;
    }
    if (data != null) {
      params['data'] = data;
    }
    return await _channel.invokeMethod('printZPLOverTCPIP', params);
  }

  static Future<dynamic> printZPLOverBluetooth(String macAddress, {String? data}) async {
    final Map<String, dynamic> params = {"mac": macAddress};
    if (data != null) {
      params['data'] = data;
    }
    return await _channel.invokeMethod('printZPLOverBluetooth', params);
  }

  static Future<dynamic> onDiscovery() async {
    final Map<String, dynamic> params = {};
    return await _channel.invokeMethod('onDiscovery', params);
  }

  static Future<dynamic> onDiscoveryUSB() async {
    final Map<String, dynamic> params = {};
    return await _channel.invokeMethod('onDiscoveryUSB', params);
  }

  static Future<Map<String, dynamic>?> onGetPrinterInfo(String ip, {int? port}) async {
    final Map<String, dynamic> params = {"ip": ip};

    if (port != null) {
      params['port'] = port;
    }

    params['data'] = "";

    try {
      final String response = await _channel.invokeMethod('onGetPrinterInfo', params);
      return json.decode(response);
    } catch (e) {
      print("Error getting printer info: $e");
      return null; // Handle the error gracefully
    }
  }

  static Future<Map<String, dynamic>?>  isPrinterConnected(String ip, {int? port}) async {
    final Map<String, dynamic> params = {"ip": ip};

    if (port != null) {
      params['port'] = port;
    }

    try {
      final String response = await _channel.invokeMethod('isPrinterConnected', params);
      return json.decode(response);
    } catch (e) {
      print("Error checking printer connection: $e");
      return null; // Handle the error gracefully
    }
  }
}
