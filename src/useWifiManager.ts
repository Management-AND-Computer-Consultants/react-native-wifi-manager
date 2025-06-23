import { useState, useCallback } from 'react';
import WifiManager from '../index';

interface WifiNetwork {
  ssid: string;
  bssid: string;
  signalStrength: number;
  capabilities: string;
  frequency: number;
  channel: number;
}

interface WifiInfo {
  ssid: string;
  signalStrength: number;
  networkId: number;
}

export const useWifiManager = () => {
  const [networks, setNetworks] = useState<WifiNetwork[]>([]);
  const [currentWifi, setCurrentWifi] = useState<WifiInfo | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const scanNetworks = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const results = await WifiManager.scanWifiNetworks();
      setNetworks(results);
      return results;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to scan networks';
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const connectToWifi = useCallback(async (ssid: string, password: string) => {
    setLoading(true);
    setError(null);
    try {
      await WifiManager.connectToWifi(ssid, password);
      // Refresh current WiFi info after connection
      await getCurrentWifiInfo();
      return true;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to connect to WiFi';
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const disconnectFromWifi = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      await WifiManager.disconnectFromWifi();
      setCurrentWifi(null);
      return true;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to disconnect from WiFi';
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const getCurrentWifiInfo = useCallback(async () => {
    try {
      const info = await WifiManager.getCurrentWifiInfo();
      setCurrentWifi(info);
      return info;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to get current WiFi info';
      setError(errorMessage);
      throw err;
    }
  }, []);

  const isWifiEnabled = useCallback(async () => {
    try {
      return await WifiManager.isWifiEnabled();
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to check WiFi status';
      setError(errorMessage);
      throw err;
    }
  }, []);

  const setWifiEnabled = useCallback(async (enabled: boolean) => {
    setLoading(true);
    setError(null);
    try {
      const result = await WifiManager.setWifiEnabled(enabled);
      return result;
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : 'Failed to set WiFi status';
      setError(errorMessage);
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);

  const clearError = useCallback(() => {
    setError(null);
  }, []);

  return {
    // State
    networks,
    currentWifi,
    loading,
    error,
    
    // Actions
    scanNetworks,
    connectToWifi,
    disconnectFromWifi,
    getCurrentWifiInfo,
    isWifiEnabled,
    setWifiEnabled,
    clearError,
  };
}; 