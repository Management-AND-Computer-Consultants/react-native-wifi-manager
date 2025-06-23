import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Modal,
  FlatList,
  TextInput,
  StyleSheet,
  Alert,
  ActivityIndicator,
  PermissionsAndroid,
  Platform,
} from 'react-native';
import WifiManager from '../index';

interface WifiNetwork {
  ssid: string;
  bssid: string;
  signalStrength: number;
  capabilities: string;
  frequency: number;
  channel: number;
}

interface WifiManagerProps {
  visible: boolean;
  onClose: () => void;
  onConnect?: (ssid: string) => void;
}

const WifiManagerComponent: React.FC<WifiManagerProps> = ({
  visible,
  onClose,
  onConnect,
}) => {
  const [networks, setNetworks] = useState<WifiNetwork[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedNetwork, setSelectedNetwork] = useState<WifiNetwork | null>(null);
  const [password, setPassword] = useState('');
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [connecting, setConnecting] = useState(false);

  useEffect(() => {
    if (visible) {
      requestPermissions();
    }
  }, [visible]);

  const requestPermissions = async () => {
    if (Platform.OS === 'android') {
      try {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
          {
            title: 'Location Permission',
            message: 'This app needs location permission to scan WiFi networks.',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK',
          }
        );
        if (granted === PermissionsAndroid.RESULTS.GRANTED) {
          scanNetworks();
        } else {
          Alert.alert('Permission Denied', 'Location permission is required to scan WiFi networks.');
        }
      } catch (err) {
        console.warn(err);
      }
    } else {
      scanNetworks();
    }
  };

  const scanNetworks = async () => {
    setLoading(true);
    try {
      const isEnabled = await WifiManager.isWifiEnabled();
      if (!isEnabled) {
        Alert.alert('WiFi Disabled', 'Please enable WiFi to scan for networks.');
        setLoading(false);
        return;
      }

      const results = await WifiManager.scanWifiNetworks();
      setNetworks(results);
    } catch (error) {
      console.error('Scan error:', error);
      Alert.alert('Scan Error', 'Failed to scan WiFi networks. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleNetworkPress = (network: WifiNetwork) => {
    setSelectedNetwork(network);
    setPassword('');
    setShowPasswordModal(true);
  };

  const handleConnect = async () => {
    if (!selectedNetwork || !password.trim()) {
      Alert.alert('Error', 'Please enter a password.');
      return;
    }

    setConnecting(true);
    try {
      await WifiManager.connectToWifi(selectedNetwork.ssid, password);
      Alert.alert('Success', `Connected to ${selectedNetwork.ssid}`);
      setShowPasswordModal(false);
      setSelectedNetwork(null);
      setPassword('');
      onConnect?.(selectedNetwork.ssid);
      onClose();
    } catch (error) {
      console.error('Connection error:', error);
      Alert.alert('Connection Failed', 'Failed to connect to the network. Please check your password and try again.');
    } finally {
      setConnecting(false);
    }
  };

  const getSignalStrengthIcon = (strength: number) => {
    if (strength >= -50) return '📶';
    if (strength >= -60) return '📶';
    if (strength >= -70) return '📶';
    return '📶';
  };

  const getSecurityType = (capabilities: string) => {
    if (capabilities.includes('WPA3')) return 'WPA3';
    if (capabilities.includes('WPA2')) return 'WPA2';
    if (capabilities.includes('WPA')) return 'WPA';
    if (capabilities.includes('WEP')) return 'WEP';
    return 'Open';
  };

  const renderNetworkItem = ({ item }: { item: WifiNetwork }) => (
    <TouchableOpacity
      style={styles.networkItem}
      onPress={() => handleNetworkPress(item)}
    >
      <View style={styles.networkInfo}>
        <Text style={styles.networkName}>{item.ssid}</Text>
        <Text style={styles.networkDetails}>
          {getSecurityType(item.capabilities)} • Channel {item.channel}
        </Text>
      </View>
      <View style={styles.networkSignal}>
        <Text style={styles.signalIcon}>{getSignalStrengthIcon(item.signalStrength)}</Text>
        <Text style={styles.signalText}>{item.signalStrength} dBm</Text>
      </View>
    </TouchableOpacity>
  );

  return (
    <Modal
      visible={visible}
      animationType="slide"
      presentationStyle="pageSheet"
      onRequestClose={onClose}
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.title}>WiFi Networks</Text>
          <TouchableOpacity onPress={onClose} style={styles.closeButton}>
            <Text style={styles.closeButtonText}>✕</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.content}>
          {loading ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="large" color="#007AFF" />
              <Text style={styles.loadingText}>Scanning for networks...</Text>
            </View>
          ) : (
            <FlatList
              data={networks}
              renderItem={renderNetworkItem}
              keyExtractor={(item) => item.bssid}
              showsVerticalScrollIndicator={false}
              ListEmptyComponent={
                <View style={styles.emptyContainer}>
                  <Text style={styles.emptyText}>No networks found</Text>
                  <TouchableOpacity onPress={scanNetworks} style={styles.retryButton}>
                    <Text style={styles.retryButtonText}>Retry</Text>
                  </TouchableOpacity>
                </View>
              }
            />
          )}
        </View>

        {/* Password Modal */}
        <Modal
          visible={showPasswordModal}
          animationType="slide"
          presentationStyle="formSheet"
          onRequestClose={() => setShowPasswordModal(false)}
        >
          <View style={styles.passwordContainer}>
            <View style={styles.passwordHeader}>
              <Text style={styles.passwordTitle}>Connect to {selectedNetwork?.ssid}</Text>
              <TouchableOpacity
                onPress={() => setShowPasswordModal(false)}
                style={styles.cancelButton}
              >
                <Text style={styles.cancelButtonText}>Cancel</Text>
              </TouchableOpacity>
            </View>

            <View style={styles.passwordContent}>
              <Text style={styles.passwordLabel}>Password</Text>
              <TextInput
                style={styles.passwordInput}
                value={password}
                onChangeText={setPassword}
                placeholder="Enter WiFi password"
                secureTextEntry
                autoFocus
                autoCapitalize="none"
                autoCorrect={false}
              />

              <TouchableOpacity
                style={[styles.connectButton, connecting && styles.connectButtonDisabled]}
                onPress={handleConnect}
                disabled={connecting}
              >
                {connecting ? (
                  <ActivityIndicator size="small" color="white" />
                ) : (
                  <Text style={styles.connectButtonText}>Connect</Text>
                )}
              </TouchableOpacity>
            </View>
          </View>
        </Modal>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  title: {
    fontSize: 18,
    fontWeight: '600',
    color: '#000',
  },
  closeButton: {
    padding: 8,
  },
  closeButtonText: {
    fontSize: 20,
    color: '#007AFF',
  },
  content: {
    flex: 1,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#8E8E93',
  },
  networkItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  networkInfo: {
    flex: 1,
  },
  networkName: {
    fontSize: 16,
    fontWeight: '500',
    color: '#000',
    marginBottom: 4,
  },
  networkDetails: {
    fontSize: 14,
    color: '#8E8E93',
  },
  networkSignal: {
    alignItems: 'flex-end',
  },
  signalIcon: {
    fontSize: 20,
    marginBottom: 2,
  },
  signalText: {
    fontSize: 12,
    color: '#8E8E93',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  emptyText: {
    fontSize: 16,
    color: '#8E8E93',
    marginBottom: 16,
  },
  retryButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 8,
  },
  retryButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '500',
  },
  passwordContainer: {
    flex: 1,
    backgroundColor: '#F2F2F7',
  },
  passwordHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    backgroundColor: 'white',
    borderBottomWidth: 1,
    borderBottomColor: '#E5E5EA',
  },
  passwordTitle: {
    fontSize: 18,
    fontWeight: '600',
    color: '#000',
  },
  cancelButton: {
    padding: 8,
  },
  cancelButtonText: {
    fontSize: 16,
    color: '#007AFF',
  },
  passwordContent: {
    padding: 16,
  },
  passwordLabel: {
    fontSize: 16,
    fontWeight: '500',
    color: '#000',
    marginBottom: 8,
  },
  passwordInput: {
    backgroundColor: 'white',
    borderWidth: 1,
    borderColor: '#E5E5EA',
    borderRadius: 8,
    padding: 16,
    fontSize: 16,
    marginBottom: 24,
  },
  connectButton: {
    backgroundColor: '#007AFF',
    paddingVertical: 16,
    borderRadius: 8,
    alignItems: 'center',
  },
  connectButtonDisabled: {
    backgroundColor: '#C7C7CC',
  },
  connectButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default WifiManagerComponent; 