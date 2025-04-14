# Wound Scanner App

An Android application that uses AI to analyze wounds through the device's camera and provides treatment recommendations.

## Features

- **Wound Scanning**: Capture high-quality images of wounds using the device camera
- **AI Analysis**: Advanced wound analysis providing:
  - Wound type classification
  - Size estimation
  - Severity assessment
  - Treatment recommendations
- **History Tracking**: Keep track of wound healing progress over time
- **Material Design 3**: Modern UI with dynamic color theming and adaptive layouts
- **Offline Support**: Local storage for wound history and analysis results

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM with Clean Architecture principles
- **Dependencies**:
  - CameraX for camera functionality
  - ML Kit for image analysis
  - Room Database for local storage
  - Kotlin Coroutines for asynchronous operations
  - Navigation Compose for screen navigation

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/woundscanner/
│   │   │   ├── ai/
│   │   │   │   └── WoundAnalysis.kt
│   │   │   ├── camera/
│   │   │   │   └── CameraActivity.kt
│   │   │   ├── data/
│   │   │   │   ├── WoundDao.kt
│   │   │   │   ├── WoundDatabase.kt
│   │   │   │   └── WoundEntity.kt
│   │   │   ├── history/
│   │   │   │   └── WoundHistory.kt
│   │   │   ├── navigation/
│   │   │   │   └── Navigation.kt
│   │   │   ├── ui/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── HistoryScreen.kt
│   │   │   │   │   └── AnalysisResultScreen.kt
│   │   │   │   └── theme/
│   │   │   │       ├── Color.kt
│   │   │   │       ├── Theme.kt
│   │   │   │       └── Typography.kt
│   │   │   ├── utils/
│   │   │   │   ├── ImageUtils.kt
│   │   │   │   └── PermissionUtils.kt
│   │   │   ├── viewmodels/
│   │   │   │   └── HistoryViewModel.kt
│   │   │   ├── MainActivity.kt
│   │   │   └── WoundApplication.kt
│   │   └── res/
│   │       └── ...
└── build.gradle

```

## Setup Instructions

1. Clone the repository
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Run the app on an emulator or physical device

## Requirements

- Android Studio Arctic Fox or newer
- Minimum SDK: 21 (Android 5.0)
- Target SDK: 33 (Android 13)
- Physical device with camera for testing wound scanning features

## Permissions Required

- Camera
- Storage (for saving wound images)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Google ML Kit for image analysis capabilities
- CameraX team for the excellent camera API
- Material Design team for the UI components and guidelines
