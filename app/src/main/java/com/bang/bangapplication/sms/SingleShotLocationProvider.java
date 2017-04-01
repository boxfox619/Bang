package com.bang.bangapplication.sms;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class SingleShotLocationProvider {

    public static interface LocationCallback {
        public void onNewLocationAvailable(GPSCoordinates location);
    }

    public static void requestSingleUpdate(final Context context, final LocationCallback callback) {
        final LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.i("Test", "Register");
            locationManager.requestSingleUpdate(criteria, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.i("Test", "Callback");
                    callback.onNewLocationAvailable(new GPSCoordinates(location.getLatitude(), location.getLongitude()));
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {
                }

                @Override
                public void onProviderEnabled(String provider) {
                }

                @Override
                public void onProviderDisabled(String provider) {
                }
            }, null);
        }
    }


    // consider returning Location instead of this dummy wrapper class
    public static class GPSCoordinates {
        public float longitude = -1;
        public float latitude = -1;

        public GPSCoordinates(float theLatitude, float theLongitude) {
            longitude = theLongitude;
            latitude = theLatitude;
        }

        public GPSCoordinates(double theLatitude, double theLongitude) {
            longitude = (float) theLongitude;
            latitude = (float) theLatitude;
        }
    }
}