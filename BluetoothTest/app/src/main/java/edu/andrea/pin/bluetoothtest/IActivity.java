/*
 * Copyright (C) 2016
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

package edu.andrea.pin.bluetoothtest;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Surface;
import android.view.WindowManager;

/**
 * Created on 19/08/2016.
 */
public abstract class IActivity extends Activity {
    // parti comuni a tutte le attività

    protected App application;

    // inizializzazione delle parti comuni alla creazione
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        application = (App) getApplication();
    }

    // inizializzazione delle parti comuni al resume
    @Override
    public void onResume(){
        super .onResume();

        // ridondante rispetto al create per essere sicuri che venga ricaricato correttamente
        // se la app viene portata in background e ripristinata
        application = (App) getApplication();
    }

    // LOCK UNLOCK SCREEN ORIENTATION
    protected void lockScreenOrientation(){
        int orientation = getRequestedOrientation();
        int rotation = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        int configuration = getResources().getConfiguration().orientation;

        int defaultOrientation = Configuration.ORIENTATION_PORTRAIT;
        if( (((rotation == Surface.ROTATION_0) || (rotation == Surface.ROTATION_180)) && (configuration == Configuration.ORIENTATION_LANDSCAPE)) ||
                (((rotation == Surface.ROTATION_90) || (rotation == Surface.ROTATION_270)) && (configuration == Configuration.ORIENTATION_PORTRAIT)))
        {
            defaultOrientation = Configuration.ORIENTATION_LANDSCAPE;
        }

        if (defaultOrientation == Configuration.ORIENTATION_LANDSCAPE){
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
            }
        }
        else {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
            }
        }

        setRequestedOrientation(orientation);
    }

    protected void unlockScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

}
