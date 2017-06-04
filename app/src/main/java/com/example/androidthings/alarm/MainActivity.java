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
public class MainActivity extends Activity {
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
            // Step 3. Enable edge trigger events.
            mButtonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
            // Step 4. Register an event callback.
            mButtonGpio.registerGpioCallback(mCallback);

            // Step 1. Create GPIO connection.
            mRedLedGpio = service.openGpio(RED_LED_PIN_NAME);
            // Step 2. Configure as an output.
            mRedLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);

            // Step 4. Repeat using a handler.
            mHandler.post(mBlinkRedRunnable);


        } catch (IOException e) {
            Log.e(TAG, "Error on PeripheralIO API", e);
        }
    }

    // Step 4. Register an event callback.
    private GpioCallback mCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            Log.i(TAG, "GPIO changed, button pressed");

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        // Step 6. Close the resource
        if (mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mCallback);
            try {
                mButtonGpio.close();
            } catch (IOException e) {
                Log.e(TAG, "Error on PeripheralIO API", e);
            }
        }
    }
}
