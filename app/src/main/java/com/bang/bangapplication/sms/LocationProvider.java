package com.bang.bangapplication.sms;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class LocationProvider implements LocationListener {
    private double latitude, longitude;
    private Context context;
    private LocationManager locationManager;
    private static LocationProvider instance;

    private LocationProvider(Context context) {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        }
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0, this);
        } catch (SecurityException e) {

        }
    }

    @Override
    public void onLocationChanged(Location location) {
        longitude = location.getLongitude();
        latitude = location.getLatitude();
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public String getCurrentLocation() {
        if (locationManager != null) {
            if (context != null && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                return "";

            Location loc = locationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (loc != null) {
                latitude = loc.getLatitude();
                longitude = loc.getLongitude();
            }
        }
        String currentLocation = "위도:" + latitude + " 경도:" + longitude;
        String url = "https://apis.daum.net/local/geo/coord2addr?apikey=a46781c4a9b4ab4e6ade548e20c8efdf&longitude=" + longitude + "&latitude=" + latitude + "&format=fullname&output=json&inputCoordSystem=WGS84";
        try {
            URLConnection connection = new URL(url).openConnection();
            connection.setDoInput(true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line = null;
            StringBuilder builder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            try {
                JSONObject object = new JSONObject(builder.toString());
                currentLocation = object.getString("fullName") + "\n" + currentLocation;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return currentLocation;
    }

    public static LocationProvider getInstance(Context context) {
        if (instance == null)
            instance = new LocationProvider(context);
        return instance;
    }

}