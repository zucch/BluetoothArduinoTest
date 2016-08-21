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

import android.app.Application;

/**
 * Created on 21/08/2016.
 */
public class App extends Application {
    // Dati il cui condivisi periodo di vita è identico a quello dell'intera applicazione

    // Stringa ricevuta da arduino contenente i dati
    public String ReceivedData;
}
