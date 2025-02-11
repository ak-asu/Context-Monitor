# Context Monitor

Context Monitor is an Android application designed to monitor and record health-related data such as heart rate and respiratory rate. The app uses the device's camera and accelerometer to capture and calculate these metrics, and allows users to log symptoms and view past records.

## Features

- **Heart Rate Measurement**: Uses the device's camera to record a video and calculate the heart rate.
- **Respiratory Rate Measurement**: Uses the device's accelerometer to capture data and calculate the respiratory rate.
- **Symptom Logging**: Allows users to log symptoms along with their heart rate and respiratory rate.
- **View Past Records**: Users can view their past health records.

## Installation

1. Clone the repository:
    ```sh
    git clone https://github.com/ak-asu/Context-Monitor.git
    ```
2. Open the project in Android Studio.
3. Build the project and run it on an Android device or emulator.

## Usage

1. **Measure Heart Rate**:
    - Grant camera permission if prompted.
    - Click on "MEASURE HEART RATE" to start recording.
    - The app will calculate and display the heart rate.

2. **Measure Respiratory Rate**:
    - Click on "MEASURE RESPIRATORY RATE" to start capturing accelerometer data.
    - The app will calculate and display the respiratory rate.

3. **Log Symptoms**:
    - After measuring heart rate and respiratory rate, click on "UPLOAD SIGNS".
    - Select symptoms and their severity, then upload the data.

4. **View Past Records**:
    - Click on "VIEW PAST RECORDS" to see a list of previously logged health data.

## Dependencies

- AndroidX libraries
- CameraX
- Room Database
- Gson
- Jetpack Compose
