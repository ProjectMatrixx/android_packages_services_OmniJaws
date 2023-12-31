/*
 *  Copyright (C) 2015 The OmniROM Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.omnirom.omnijaws;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

class WeatherLocationListener implements LocationListener {
    private static final String TAG = "WeatherService:WeatherLocationListener";
    private Context mContext;
    private static WeatherLocationListener sInstance = null;
    public static final long LOCATION_REQUEST_TIMEOUT = 5L * 60L * 1000L; // request for at most 5 minutes

    static void registerIfNeeded(Context context, String provider) {
        synchronized (WeatherLocationListener.class) {
            Log.d(TAG, "Registering location listener");
            if (sInstance == null) {
                final Context appContext = context.getApplicationContext();
                final LocationManager locationManager =
                        (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);

                // Check location provider after set sInstance, so, if the provider is not
                // supported, we never enter here again.
                sInstance = new WeatherLocationListener(appContext);
                // Check whether the provider is supported.
                // NOTE!!! Actually only WeatherUpdateService class is calling this function
                // with the NETWORK_PROVIDER, so setting the instance is safe. We must
                // change this if this call receive different providers
                LocationProvider lp = locationManager.getProvider(provider);
                if (lp != null) {
                    Log.d(TAG, "LocationManager - Requesting single update");
                    locationManager.requestSingleUpdate(provider, sInstance,
                            appContext.getMainLooper());
                    sInstance.setTimeoutAlarm();
                }
            }
        }
    }

    static void cancel(Context context) {
        synchronized (WeatherLocationListener.class) {
            if (sInstance != null) {
                final Context appContext = context.getApplicationContext();
                final LocationManager locationManager =
                    (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
                Log.d(TAG, "Aborting location request after timeout");
                locationManager.removeUpdates(sInstance);
                sInstance.cancelTimeoutAlarm();
                sInstance = null;
            }
        }
    }

    private WeatherLocationListener(Context context) {
        super();
        mContext = context;
    }

    private void setTimeoutAlarm() {
        WeatherUpdateService.scheduleUpdateOnce(mContext, LOCATION_REQUEST_TIMEOUT, WeatherUpdateService.DELAYED_LOCATION_UPDATE_JOB_ID);
    }

    private void cancelTimeoutAlarm() {
        WeatherUpdateService.cancelDelayedLocationUpdate(mContext);
    }

    @Override
    public void onLocationChanged(Location location) {
        // Now, we have a location to use. Schedule a weather update right now.
        Log.d(TAG, "The location has changed, schedule an update ");
        synchronized (WeatherLocationListener.class) {
            cancelTimeoutAlarm();
            WeatherUpdateService.scheduleUpdateNow(mContext);
            sInstance = null;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Now, we have a location to use. Schedule a weather update right now.
        Log.d(TAG, "The location service has become available, schedule an update ");
        if (status == LocationProvider.AVAILABLE) {
            synchronized (WeatherLocationListener.class) {
                cancelTimeoutAlarm();
                WeatherUpdateService.scheduleUpdateNow(mContext);
                sInstance = null;
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Not used
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Not used
    }
}
