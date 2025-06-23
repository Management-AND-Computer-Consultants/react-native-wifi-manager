# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.6] - 2024-01-XX

### Fixed
- Resolved npm publishing issues with cache conflicts
- Fixed version conflict errors during package publishing
- Improved publish script error handling for registry timing issues

### Technical Improvements
- Added npm cache clearing to prevent publishing conflicts
- Enhanced version management for smoother releases

## [1.0.5] - 2024-01-XX

### Fixed
- Fixed build configuration issues for Android SDK 35
- Resolved import and compilation errors
- Improved error handling in publish script

## [1.0.4] - 2024-01-XX

### Updated
- **Android SDK Support**: Updated to target SDK 35 (Android 15) and minimum SDK 28 (Android 9)
- **Build Tools**: Updated to buildToolsVersion "35.0.0"
- **Java Version**: Upgraded to Java 11 compatibility
- **Dependencies**: Updated androidx.annotation to 1.7.1 and added androidx.core 1.12.0

### Added
- **Android 13+ Support**: Added NEARBY_WIFI_DEVICES permission for WiFi scanning
- **Android 14+ Support**: Added POST_NOTIFICATIONS permission
- **Modern Android**: Updated to use namespace approach instead of package attribute
- **Enhanced Permissions**: Improved permission checking for different Android versions

### Technical Improvements
- Updated permission handling to support Android 13+ requirements
- Added comprehensive permission checking methods
- Improved compatibility with latest Android versions
- Enhanced build configuration for modern Android development

## [1.0.3] - 2024-01-XX

### Fixed
- **CRITICAL**: Fixed fatal exception in Android broadcast receiver during WiFi scanning
- Added proper null checks and error handling in broadcast receiver
- Fixed race conditions when module is destroyed during scanning
- Improved thread safety and memory management
- Added comprehensive logging for debugging
- Fixed multiple receiver registration/unregistration issues
- Enhanced error handling for scan failures

### Technical Improvements
- Separated scan success/failure handling into dedicated methods
- Added proper cleanup methods to prevent memory leaks
- Improved promise handling to prevent crashes after module destruction
- Added validation for broadcast intent and context

## [1.0.2] - 2024-01-XX

### Fixed
- Improved publish script error handling for npm registry timing issues
- Better feedback for false positive publishing errors
- Enhanced user experience during package publishing

## [1.0.1] - 2024-01-XX

### Fixed
- Fixed publish script to check for correct file paths
- Updated package structure for proper npm publishing
- Added missing files to package distribution

## [1.0.0] - 2024-01-XX

### Added
- Initial release of React Native WiFi Manager
- WiFi network scanning functionality (Android)
- WiFi connection management with password authentication
- Beautiful popup interface for network selection
- Cross-platform support (Android & iOS)
- TypeScript definitions
- Custom React Hook (`useWifiManager`)
- React Component (`WifiManagerComponent`)
- Permission handling for location access
- Real-time signal strength monitoring
- Network capabilities detection
- Current WiFi information retrieval
- WiFi enable/disable functionality (Android only)

### Features
- **Android Support**: Full WiFi scanning, connection, and management
- **iOS Support**: WiFi connection and basic management (scanning not available due to iOS limitations)
- **TypeScript**: Complete type definitions for all APIs
- **Modern UI**: Native-looking popup interface
- **Permission Management**: Automatic handling of required permissions
- **Error Handling**: Comprehensive error handling and user feedback

### Technical Details
- React Native 0.60+ auto-linking support
- Peer dependencies for React and React Native
- Proper npm package structure with exports
- Installation script for easy setup
- Comprehensive documentation and examples

### Platform Limitations
- **iOS**: WiFi scanning is not available due to iOS security restrictions
- **iOS**: WiFi enable/disable functionality is not available
- **Android**: Requires location permissions for WiFi scanning 