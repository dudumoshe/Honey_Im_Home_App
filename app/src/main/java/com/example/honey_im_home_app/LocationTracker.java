package com.example.honey_im_home_app;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.Serializable;

class LocationInfo implements Serializable {
    double latitude;
    double longitude;
    float accuracy;

    public LocationInfo(double latitude, double longitude, float accuracy) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
    }

    @NonNull
    @Override
    public String toString() {
        return "Location:\nlatitude: " + latitude + "\nlongitude: "+ longitude + "\naccuracy: " + accuracy;
    }
}

public class LocationTracker extends LocationCallback {
    public static String LOCATION_ACTION = "HoneyImHomeLocations";
    public static String BROADCAST_LOCATION_EXTRA = "location";
    private Context context;
    boolean isTracking;
    FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;

    LocationTracker(Context context) {
        this.context = context;
        this.isTracking = false;
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5 * 1000);
    }

    public void startTracking() {
        if (isTracking) {
            Log.d("LocationTracker", "Tracker is already turned on");
            return;
        }
        boolean hasPermissions = ActivityCompat.checkSelfPermission(this.context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if(!hasPermissions) {
            Log.e("LocationTracker", "Cant start tracking without permissions");
            return;
        }

        fusedLocationProviderClient.requestLocationUpdates(locationRequest,
                this, Looper.getMainLooper());
        isTracking = true;
    }

    public void stopTracking() {
        if (!isTracking) {
            Log.d("LocationTracker", "Tracker is already turned off");
            return;
        }
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(this);
        }
        isTracking = false;
    }

    @Override
    public void onLocationResult(LocationResult locationResult) {
        if (locationResult == null) {
            return;
        }
        for (Location location : locationResult.getLocations()) {
            if (location != null) {
                double wayLatitude = location.getLatitude();
                double wayLongitude = location.getLongitude();
                float accuracy = location.getAccuracy();
                LocationInfo locationInfo = new LocationInfo(wayLatitude, wayLongitude, accuracy);
                Intent intent = new Intent();
                intent.setAction(LOCATION_ACTION);
                intent.putExtra(BROADCAST_LOCATION_EXTRA, locationInfo);
                context.sendBroadcast(intent);
                Log.d("Location Tracker", "got location " + wayLatitude + " " +
                        wayLongitude + " " + accuracy);
            }
        }
        super.onLocationResult(locationResult);
    }
}
