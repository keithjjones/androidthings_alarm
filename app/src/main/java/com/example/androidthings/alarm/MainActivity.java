/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.androidthings.alarm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManagerService;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;

/**
 * Skeleton of the main Android Things activity. Implement your device's logic
 * in this class.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 *
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 */
/* Adapted from https://developer.android.com/things/training/first-device/peripherals.html */
/* Adapted from https://medium.com/@abhi007tyagi/android-things-led-control-via-mqtt-b7509576c135 */
public class MainActivity extends Activity implements MqttCallback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String BUTTON_PIN_NAME = "BCM6";
    private static final String RED_LED_PIN_NAME = "BCM21";
    private static final String YELLOW_LED_PIN_NAME = "BCM16";
    private static final String GREEN_LED_PIN_NAME = "BCM12";
    private static final int INTERVAL_BETWEEN_BLINKS_MS = 1000;

    private Gpio mButtonGpio;

    private Handler mHandler = new Handler();

    private Gpio mRedLedGpio;
    private Gpio mYellowLedGpio;
    private Gpio mGreenLedGpio;

    String username = "csc844";
    String password = "844password";
    MqttClient client;
    MqttConnectOptions options;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        PeripheralManagerService service = new PeripheralManagerService();
        Log.d(TAG, "Available GPIO: " + service.getGpioList());

        try {
            // Step 1. Create GPIO connection.
            mButtonGpio = service.openGpio(BUTTON_PIN_NAME);
            // Step 2. Configure as an input.
            mButtonGpio.setDirection(Gpio.DIRECTION_IN);
            mButtonGpio.setActiveType(Gpio.ACTIVE_LOW);
            // Step 3. Enable edge trigger events.
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            // Step 4. Register an event callback.
            mButtonGpio.registerGpioCallback(mButtonCallback);

            // Step 1. Create GPIO connection.
            mRedLedGpio = service.openGpio(RED_LED_PIN_NAME);
            mYellowLedGpio = service.openGpio(YELLOW_LED_PIN_NAME);
            mGreenLedGpio = service.openGpio(GREEN_LED_PIN_NAME);
            // Step 2. Configure as an output.
            mRedLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mYellowLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mGreenLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            // Step 4. Repeat using a handler.
//            mHandler.post(mBlinkRedRunnable);
//            mHandler.post(mBlinkYellowRunnable);
//            mHandler.post(mBlinkGreenRunnable);


        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
        try {
            String username = "csc844";
            String password = "844password";
            client = new MqttClient("tcp://m11.cloudmqtt.com:16148", "AndroidThingAlarm", new MemoryPersistence());
            options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            client.setCallback(this);
            client.connect(options);

            String topic = "alarm/siren";
            client.subscribe(topic);

        } catch (MqttException e) {
            Log.e(TAG, "Error on MQTT Connection", e);
            e.printStackTrace();
        }
    }

    // Step 4. Register an event callback.
    private GpioCallback mButtonCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            String content;
            try {
                if (gpio.getValue()) {
                    Log.i(TAG, "Button Pressed");
                    content = "ON";
                } else {
                    Log.i(TAG, "Button Not Pressed");
                    content = "OFF";
                }
                MqttMessage message = new MqttMessage(content.getBytes());
                try {
                    client.publish("alarm/switch", message);
                } catch (MqttException e) {
                    Log.e(TAG, "Error on MQTT publish", e);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Step 5. Return true to keep callback active.
            return true;
        }
    };

    private Runnable mBlinkRedRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit if the GPIO is already closed
            if (mRedLedGpio == null) {
                return;
            }

            try {
                // Step 3. Toggle the LED state
                mRedLedGpio.setValue(!mRedLedGpio.getValue());

                // Step 4. Schedule another event after delay.
                mHandler.postDelayed(mBlinkRedRunnable, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private Runnable mBlinkYellowRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit if the GPIO is already closed
            if (mYellowLedGpio == null) {
                return;
            }

            try {
                // Step 3. Toggle the LED state
                mYellowLedGpio.setValue(!mYellowLedGpio.getValue());

                // Step 4. Schedule another event after delay.
                mHandler.postDelayed(mBlinkYellowRunnable, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    private Runnable mBlinkGreenRunnable = new Runnable() {
        @Override
        public void run() {
            // Exit if the GPIO is already closed
            if (mGreenLedGpio == null) {
                return;
            }

            try {
                // Step 3. Toggle the LED state
                mGreenLedGpio.setValue(!mGreenLedGpio.getValue());

                // Step 4. Schedule another event after delay.
                mHandler.postDelayed(mBlinkGreenRunnable, INTERVAL_BETWEEN_BLINKS_MS);
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    };

    @Override
    public void connectionLost(Throwable cause) {
        Log.d(TAG, "MQTT connection lost....");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        Log.d(TAG, payload);
        try {
            // Step 3. Toggle the LED state
            mGreenLedGpio.setValue(!mGreenLedGpio.getValue());

            // Step 4. Schedule another event after delay.
            mHandler.postDelayed(mBlinkGreenRunnable, INTERVAL_BETWEEN_BLINKS_MS);
        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        Log.d(TAG, "MQTT Delivery Complete....");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        try {
            client.disconnect();
        } catch (MqttException e) {
            Log.e(TAG, "Error closing MQTT", e);
        }

        // Step 6. Close the resource
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mButtonCallback);
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    }
}
