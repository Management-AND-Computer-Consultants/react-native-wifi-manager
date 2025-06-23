package com.wifimanager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.List;

public class WifiManagerModule extends ReactContextBaseJavaModule {
    private static final String TAG = "WifiManagerModule";
    private static final String MODULE_NAME = "WifiManager";
    
    private final ReactApplicationContext reactContext;
    private WifiManager wifiManager;
    private BroadcastReceiver wifiScanReceiver;
    private boolean isScanning = false;
    private boolean isReceiverRegistered = false;
    private Promise currentScanPromise = null;

    public WifiManagerModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        this.wifiManager = (WifiManager) reactContext.getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    @NonNull
    public String getName() {
        return MODULE_NAME;
    }

    @ReactMethod
    public void scanWifiNetworks(Promise promise) {
        Log.d(TAG, "Starting WiFi scan...");
        
        // Check if WiFi is enabled first
        if (!wifiManager.isWifiEnabled()) {
            Log.e(TAG, "WiFi is not enabled");
            promise.reject("WIFI_DISABLED", "WiFi is not enabled. Please enable WiFi first.");
            return;
        }

        if (!checkPermissions()) {
            String missingPermissions = getMissingPermissions();
            Log.e(TAG, "Permission denied. Missing: " + missingPermissions);
            promise.reject("PERMISSION_DENIED", "Required permissions not granted: " + missingPermissions);
            return;
        }

        if (isScanning) {
            Log.w(TAG, "Scan already in progress");
            promise.reject("ALREADY_SCANNING", "WiFi scan is already in progress");
            return;
        }

        try {
            isScanning = true;
            currentScanPromise = promise;
            
            Log.d(TAG, "Setting up scan receiver...");
            
            // Unregister any existing receiver first
            unregisterScanReceiver();
            
            wifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    try {
                        Log.d(TAG, "Received broadcast: " + intent.getAction());
                        
                        // Check if context and intent are valid
                        if (context == null || intent == null) {
                            Log.e(TAG, "Invalid context or intent in broadcast receiver");
                            handleScanFailure("Invalid broadcast data");
                            return;
                        }

                        // Check if the action matches
                        if (!WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                            Log.w(TAG, "Received unexpected broadcast action: " + intent.getAction());
                            return;
                        }

                        boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                        Log.d(TAG, "Scan success: " + success);
                        
                        if (success) {
                            handleScanSuccess();
                        } else {
                            handleScanFailure("WiFi scan failed - no results updated");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error in broadcast receiver", e);
                        handleScanFailure("Broadcast receiver error: " + e.getMessage());
                    }
                }
            };

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            
            try {
                reactContext.registerReceiver(wifiScanReceiver, intentFilter);
                isReceiverRegistered = true;
                Log.d(TAG, "Broadcast receiver registered successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to register broadcast receiver", e);
                isScanning = false;
                currentScanPromise = null;
                promise.reject("RECEIVER_ERROR", "Failed to register broadcast receiver: " + e.getMessage());
                return;
            }

            // Start the scan
            Log.d(TAG, "Starting WiFi scan...");
            boolean scanStarted = wifiManager.startScan();
            Log.d(TAG, "Scan started: " + scanStarted);
            
            if (!scanStarted) {
                Log.e(TAG, "Failed to start WiFi scan");
                unregisterScanReceiver();
                isScanning = false;
                currentScanPromise = null;
                promise.reject("SCAN_FAILED", "Failed to start WiFi scan. Please check if WiFi is enabled and try again.");
            }
            
            // Set a timeout to prevent hanging
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isScanning && currentScanPromise != null) {
                        Log.w(TAG, "Scan timeout - no results received");
                        handleScanFailure("Scan timeout - no results received within 10 seconds");
                    }
                }
            }, 10000); // 10 second timeout
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting WiFi scan", e);
            unregisterScanReceiver();
            isScanning = false;
            currentScanPromise = null;
            promise.reject("SCAN_ERROR", "Error starting WiFi scan: " + e.getMessage());
        }
    }

    private void handleScanSuccess() {
        try {
            if (currentScanPromise == null) {
                Log.w(TAG, "Scan promise is null, ignoring scan results");
                return;
            }

            List<ScanResult> results = wifiManager.getScanResults();
            Log.d(TAG, "Scan results count: " + (results != null ? results.size() : 0));
            
            if (results == null) {
                Log.w(TAG, "Scan results are null");
                currentScanPromise.reject("SCAN_FAILED", "No scan results available");
            } else if (results.isEmpty()) {
                Log.w(TAG, "Scan results are empty - no networks found");
                currentScanPromise.reject("SCAN_FAILED", "No WiFi networks found. Please check if WiFi is enabled and try again.");
            } else {
                Log.d(TAG, "Found " + results.size() + " networks");
                
                // Create separate arrays for event and promise to avoid ObjectAlreadyConsumedException
                WritableArray networksForEvent = convertScanResultsToArray(results);
                WritableArray networksForPromise = convertScanResultsToArray(results);
                
                Log.d(TAG, "Sending scan results: " + networksForPromise.size() + " networks");
                sendEvent("wifiScanResults", networksForEvent);
                currentScanPromise.resolve(networksForPromise);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling scan success", e);
            if (currentScanPromise != null) {
                currentScanPromise.reject("SCAN_ERROR", "Error processing scan results: " + e.getMessage());
            }
        } finally {
            cleanupScan();
        }
    }

    private void handleScanFailure(String error) {
        try {
            if (currentScanPromise != null) {
                currentScanPromise.reject("SCAN_FAILED", error);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling scan failure", e);
        } finally {
            cleanupScan();
        }
    }

    private void cleanupScan() {
        isScanning = false;
        currentScanPromise = null;
        unregisterScanReceiver();
    }

    private void unregisterScanReceiver() {
        if (wifiScanReceiver != null && isReceiverRegistered) {
            try {
                reactContext.unregisterReceiver(wifiScanReceiver);
                isReceiverRegistered = false;
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering scan receiver", e);
            }
        }
        wifiScanReceiver = null;
    }

    @ReactMethod
    public void connectToWifi(String ssid, String password, Promise promise) {
        if (!checkPermissions()) {
            promise.reject("PERMISSION_DENIED", "Location permission is required for WiFi connection");
            return;
        }

        try {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = "\"" + ssid + "\"";
            wifiConfig.preSharedKey = "\"" + password + "\"";

            int networkId = wifiManager.addNetwork(wifiConfig);
            if (networkId == -1) {
                promise.reject("CONNECTION_FAILED", "Failed to add network configuration");
                return;
            }

            boolean connected = wifiManager.enableNetwork(networkId, true);
            if (connected) {
                promise.resolve(true);
                sendEvent("wifiConnected", ssid);
            } else {
                promise.reject("CONNECTION_FAILED", "Failed to connect to WiFi network");
            }
        } catch (Exception e) {
            promise.reject("CONNECTION_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void disconnectFromWifi(Promise promise) {
        try {
            boolean disconnected = wifiManager.disconnect();
            if (disconnected) {
                promise.resolve(true);
                sendEvent("wifiDisconnected", null);
            } else {
                promise.reject("DISCONNECTION_FAILED", "Failed to disconnect from WiFi");
            }
        } catch (Exception e) {
            promise.reject("DISCONNECTION_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void getCurrentWifiInfo(Promise promise) {
        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() != -1) {
                WritableMap info = Arguments.createMap();
                info.putString("ssid", wifiInfo.getSSID().replace("\"", ""));
                info.putInt("signalStrength", wifiInfo.getRssi());
                info.putInt("networkId", wifiInfo.getNetworkId());
                promise.resolve(info);
            } else {
                promise.resolve(null);
            }
        } catch (Exception e) {
            promise.reject("INFO_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void isWifiEnabled(Promise promise) {
        try {
            boolean enabled = wifiManager.isWifiEnabled();
            promise.resolve(enabled);
        } catch (Exception e) {
            promise.reject("ENABLED_CHECK_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void setWifiEnabled(boolean enabled, Promise promise) {
        try {
            boolean success = wifiManager.setWifiEnabled(enabled);
            promise.resolve(success);
        } catch (Exception e) {
            promise.reject("ENABLE_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void checkPermissions(Promise promise) {
        try {
            WritableMap permissionStatus = Arguments.createMap();
            
            // Check WiFi permissions
            boolean hasWifiState = ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
            boolean hasChangeWifiState = ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
            
            // Check location permissions
            boolean hasFineLocation = ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean hasCoarseLocation = ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            
            // Check network permissions
            boolean hasNetworkState = ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
            boolean hasChangeNetworkState = ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.CHANGE_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED;
            
            // Check Android 13+ permissions
            boolean hasNearbyWifiDevices = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                hasNearbyWifiDevices = ActivityCompat.checkSelfPermission(reactContext,
                        Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
            }
            
            // Check WiFi state
            boolean isWifiEnabled = wifiManager.isWifiEnabled();
            
            permissionStatus.putBoolean("hasWifiState", hasWifiState);
            permissionStatus.putBoolean("hasChangeWifiState", hasChangeWifiState);
            permissionStatus.putBoolean("hasFineLocation", hasFineLocation);
            permissionStatus.putBoolean("hasCoarseLocation", hasCoarseLocation);
            permissionStatus.putBoolean("hasNetworkState", hasNetworkState);
            permissionStatus.putBoolean("hasChangeNetworkState", hasChangeNetworkState);
            permissionStatus.putBoolean("hasNearbyWifiDevices", hasNearbyWifiDevices);
            permissionStatus.putBoolean("isWifiEnabled", isWifiEnabled);
            permissionStatus.putBoolean("canScan", checkPermissions());
            
            promise.resolve(permissionStatus);
        } catch (Exception e) {
            promise.reject("PERMISSION_CHECK_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void requestPermissions(Promise promise) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Check what permissions are missing
                String missingPermissions = getMissingPermissions();
                
                if (missingPermissions.isEmpty()) {
                    // All permissions are already granted
                    promise.resolve(true);
                    return;
                }
                
                // Create a list of permissions to request
                List<String> permissionsToRequest = new ArrayList<>();
                
                if (ActivityCompat.checkSelfPermission(reactContext, 
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
                }
                
                if (ActivityCompat.checkSelfPermission(reactContext, 
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
                
                // For Android 13+ (API 33+), also request NEARBY_WIFI_DEVICES
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ActivityCompat.checkSelfPermission(reactContext,
                            Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES);
                    }
                }
                
                if (!permissionsToRequest.isEmpty()) {
                    // Request permissions using ActivityCompat
                    ActivityCompat.requestPermissions(
                        reactContext.getCurrentActivity(),
                        permissionsToRequest.toArray(new String[0]),
                        1001 // Request code
                    );
                    
                    // Note: We can't directly handle the result here due to React Native bridge limitations
                    // The app should handle the permission result in the main activity
                    promise.resolve(false); // Indicates permissions were requested
                } else {
                    promise.resolve(true); // All permissions already granted
                }
            } else {
                // For older Android versions, no runtime permissions needed
                promise.resolve(true);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting permissions", e);
            promise.reject("PERMISSION_REQUEST_ERROR", e.getMessage());
        }
    }

    @ReactMethod
    public void scanWifiNetworksWithPermissionRequest(Promise promise) {
        Log.d(TAG, "Starting WiFi scan with permission request...");
        
        // Check if WiFi is enabled first
        if (!wifiManager.isWifiEnabled()) {
            Log.e(TAG, "WiFi is not enabled");
            promise.reject("WIFI_DISABLED", "WiFi is not enabled. Please enable WiFi first.");
            return;
        }

        // Check permissions and request if needed
        if (!checkPermissions()) {
            Log.d(TAG, "Permissions not granted, requesting permissions...");
            
            // Request permissions first
            requestPermissions(new Promise() {
                @Override
                public void resolve(Object value) {
                    // After requesting permissions, try scanning again
                    Log.d(TAG, "Permission request completed, attempting scan...");
                    
                    // Give a small delay for permission dialog to complete
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Check permissions again after request
                            if (checkPermissions()) {
                                Log.d(TAG, "Permissions granted, proceeding with scan...");
                                // Call the original scan method
                                scanWifiNetworks(promise);
                            } else {
                                Log.e(TAG, "Permissions still not granted after request");
                                promise.reject("PERMISSION_DENIED", "Required permissions not granted after request. Please grant location permissions manually.");
                            }
                        }
                    }, 1000); // 1 second delay
                }

                @Override
                public void reject(String code) {
                    promise.reject("PERMISSION_REQUEST_FAILED", "Failed to request permissions: " + code);
                }

                @Override
                public void reject(String code, String message) {
                    promise.reject("PERMISSION_REQUEST_FAILED", "Failed to request permissions: " + message);
                }

                @Override
                public void reject(String code, Throwable throwable) {
                    promise.reject("PERMISSION_REQUEST_FAILED", "Failed to request permissions: " + throwable.getMessage());
                }

                @Override
                public void reject(String code, String message, Throwable throwable) {
                    promise.reject("PERMISSION_REQUEST_FAILED", "Failed to request permissions: " + message);
                }
            });
            return;
        }

        // If permissions are already granted, proceed with normal scan
        scanWifiNetworks(promise);
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasLocationPermission = ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            
            // For Android 13+ (API 33+), also check for NEARBY_WIFI_DEVICES permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                boolean hasNearbyWifiPermission = ActivityCompat.checkSelfPermission(reactContext,
                        Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED;
                // For backward compatibility, only require location permission if NEARBY_WIFI_DEVICES is not available
                return hasLocationPermission || hasNearbyWifiPermission;
            }
            
            return hasLocationPermission;
        }
        return true;
    }

    private String getMissingPermissions() {
        StringBuilder missingPermissions = new StringBuilder();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(reactContext, 
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.append("ACCESS_FINE_LOCATION ");
            }
            
            // For Android 13+ (API 33+), also check for NEARBY_WIFI_DEVICES permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(reactContext,
                        Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.append("NEARBY_WIFI_DEVICES ");
                }
            }
        }
        
        return missingPermissions.toString().trim();
    }

    private WritableArray convertScanResultsToArray(List<ScanResult> results) {
        WritableArray networks = Arguments.createArray();
        
        for (ScanResult result : results) {
            WritableMap network = Arguments.createMap();
            network.putString("ssid", result.SSID);
            network.putString("bssid", result.BSSID);
            network.putInt("signalStrength", result.level);
            network.putString("capabilities", result.capabilities);
            network.putInt("frequency", result.frequency);
            network.putInt("channel", getChannelFromFrequency(result.frequency));
            networks.pushMap(network);
        }
        
        return networks;
    }

    private int getChannelFromFrequency(int frequency) {
        if (frequency >= 2412 && frequency <= 2484) {
            return (frequency - 2412) / 5 + 1;
        } else if (frequency >= 5170 && frequency <= 5825) {
            return (frequency - 5170) / 5 + 34;
        }
        return 0;
    }

    private void sendEvent(String eventName, Object data) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, data);
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        try {
            // Clean up any ongoing scan
            if (isScanning) {
                Log.w(TAG, "Module destroyed while scan was in progress");
                cleanupScan();
            }
            
            // Unregister receiver if still registered
            unregisterScanReceiver();
        } catch (Exception e) {
            Log.e(TAG, "Error during module destruction", e);
        }
    }
} 