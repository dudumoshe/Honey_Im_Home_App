package com.example.honey_im_home_app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences sp;
    private BroadcastReceiver locationBroadcastReciever;
    private LocationInfo lastGoodLocation = null;
    private static int REQUEST_LOCATION_PERMISSION = 3654;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupSetHomeLocationButton();
        setupClearHomeLocationButton();
        sp = getSharedPreferences("HomeLocationData", MODE_PRIVATE);
        String homeLocation = sp.getString("homeLocation", null);
        onSetHomeLocation(homeLocation);
        listenLocationChanges();

        findViewById(R.id.start_tracking_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hasPermissions = ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

                if(!hasPermissions) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{ Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                } else {
                    Log.d("Main Activity","got permissions");
                    trackerStartOrStop();
                }
            }
        });

        if (savedInstanceState != null) {
            boolean hasPermissions = ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            String currentLocation = savedInstanceState.getString("currentLocation");
            if (currentLocation != null && !currentLocation.isEmpty() && hasPermissions) {
                trackerStartOrStop();
                TextView statusLocation = findViewById(R.id.status_message);
                if(statusLocation != null) {
                    statusLocation.setText(currentLocation);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Main Activity","got permissions");
            trackerStartOrStop();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                    .setTitle("Location Permission Importance")
                    .setMessage("Location permission is important for using this application")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //Prompt the user once explanation has been shown
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION);
                        }
                    })
                    .create()
                    .show();
            }
        }
    }

    private void trackerStartOrStop() {
        LocationTracker locationTracker = ((HoneyImHomeApp)getApplicationContext()).locationTracker;
        Button turnTrackerOnButton = findViewById(R.id.start_tracking_location);
        if(locationTracker.isTracking) {
            locationTracker.stopTracking();
            turnTrackerOnButton.setText("Start Tracking");
            final Button setHomeLocation = findViewById(R.id.set_location_as_home);
            setHomeLocation.setVisibility(View.GONE);
            final TextView statusLocation = findViewById(R.id.status_message);
            statusLocation.setText("");
        } else {
            locationTracker.startTracking();
            turnTrackerOnButton.setText("Stop Tracking");
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        TextView statusLocation = findViewById(R.id.status_message);
        if(statusLocation != null) {
            String currentLocation = (String)statusLocation.getText();
            outState.putString("currentLocation", currentLocation);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocationTracker locationTracker = ((HoneyImHomeApp)getApplicationContext()).locationTracker;
        locationTracker.stopTracking();
        unregisterReceiver(this.locationBroadcastReciever);
    }

    private void listenLocationChanges() {
        final Button setHomeLocation = findViewById(R.id.set_location_as_home);
        this.locationBroadcastReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(LocationTracker.LOCATION_ACTION)) {
                    return;
                }
                LocationInfo location = (LocationInfo)intent.getSerializableExtra(
                        LocationTracker.BROADCAST_LOCATION_EXTRA);
                TextView statusText = MainActivity.this.findViewById(R.id.status_message);
                statusText.setText(location.toString());
                if(location.accuracy < 50) {
                    lastGoodLocation = location;
                    setHomeLocation.setVisibility(View.VISIBLE);
                } else {
                    setHomeLocation.setVisibility(View.GONE);
                }
                Log.d("Main Activity","got location with acc:" + location.accuracy);
            }
        };
        registerReceiver(this.locationBroadcastReciever, new IntentFilter("HoneyImHomeLocations"));
    }

    private void onSetHomeLocation(String homeLocation) {
        if (homeLocation != null) {
            TextView homeLocationText = findViewById(R.id.home_location_text);
            homeLocationText.setText("Your home location is defined as " + homeLocation);
            Button clearHome = findViewById(R.id.clear_home);
            clearHome.setVisibility(View.VISIBLE);
        }
    }

    private void setupClearHomeLocationButton() {
        final Button clearHome = findViewById(R.id.clear_home);
        clearHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sp.edit().remove("homeLocation").apply();
                TextView homeLocationText = findViewById(R.id.home_location_text);
                homeLocationText.setText("");
                clearHome.setVisibility(View.GONE);
            }
        });
    }

    private void setupSetHomeLocationButton() {
        final Button setHome = findViewById(R.id.set_location_as_home);
        setHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(lastGoodLocation == null) {
                    return;
                }
                String location = lastGoodLocation.latitude + "," + lastGoodLocation.longitude;
                sp.edit().putString("homeLocation", location).apply();
                onSetHomeLocation(location);
            }
        });
    }
}
