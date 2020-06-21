package com.example.honey_im_home_app;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.ActivityCompat;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import static android.content.Context.MODE_PRIVATE;

public class HomeLocationWorker extends ListenableWorker {
    private CallbackToFutureAdapter.Completer<Result> callback;
    private BroadcastReceiver receiver;

    public HomeLocationWorker(@NonNull Context appContext, @NonNull WorkerParameters workerParams) {
        super(appContext, workerParams);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        ListenableFuture<Result> future = CallbackToFutureAdapter.getFuture(new CallbackToFutureAdapter.Resolver<Result>() {
            @Nullable
            @Override
            public Object attachCompleter(@NonNull CallbackToFutureAdapter.Completer<Result> completer) throws Exception {
                callback = completer;
                return null;
            }
        });
        boolean hasLocationPermissions = ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasSMSPermissions = ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        if(!hasLocationPermissions || !hasSMSPermissions) {
            Log.d("LocationWorker", "no permissions");
            setCallbackSuccess();
            return future;
        }
        SharedPreferences sp = this.getApplicationContext().getSharedPreferences(MainActivity.SP_DATA, MODE_PRIVATE);
        String homeLocation = sp.getString("homeLocation", null);
        String phoneNumber = sp.getString("userPhoneNumber", null);
        if (homeLocation == null || phoneNumber == null) {
            Log.d("LocationWorker", "no phone or location");
            setCallbackSuccess();
            return future;
        }
        placeReceiver();
        return future;
    }

    private void placeReceiver() {
        this.receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!intent.getAction().equals(LocationTracker.LOCATION_ACTION)) {
                    return;
                }
                LocationInfo location = (LocationInfo)intent.getSerializableExtra(
                        LocationTracker.BROADCAST_LOCATION_EXTRA);
                if (location.accuracy >= 50) {
                    return;
                }
                onReceivedBroadcast(location);
            }
        };
        LocationTracker locationTracker = ((HoneyImHomeApp)getApplicationContext()).locationTracker;
        locationTracker.startTracking();
        this.getApplicationContext().registerReceiver(this.receiver, new IntentFilter(LocationTracker.LOCATION_ACTION));
    }

    private void setCallbackSuccess() {
        if (this.callback != null) {
            callback.set(Result.success());
        }
    }

    private void onReceivedBroadcast(LocationInfo location) {
        this.getApplicationContext().unregisterReceiver(this.receiver);
        LocationTracker locationTracker = ((HoneyImHomeApp)getApplicationContext()).locationTracker;
        locationTracker.stopTracking();

        SharedPreferences sp = this.getApplicationContext().getSharedPreferences("HomeLocationData", MODE_PRIVATE);
        String previousLocation = sp.getString("previousWorkerLocation", null);
        sp.edit().putString("previousWorkerLocation", location.latitude +"," + location.longitude).apply();
        if (previousLocation == null) {
            Log.d("LocationWorker", "no previous location");
            setCallbackSuccess();
            return;
        }
        String[] locationBefore = previousLocation.split(",");
        double diff = Math.sqrt(Math.pow(Double.parseDouble(locationBefore[0]) - location.latitude, 2) +
                Math.pow(Double.parseDouble(locationBefore[1]) - location.longitude, 2));
        if (diff < 50) {
            Log.d("LocationWorker", "location changed in less than 50m. finished work");
            setCallbackSuccess();
            return;
        }
        String homeLocationBeforeSplit = sp.getString("homeLocation", null);
        if (homeLocationBeforeSplit == null) {
            Log.d("LocationWorker", "couldn't compare location to home since its not define");
            setCallbackSuccess();
            return;
        }
        String[] homeLocation = homeLocationBeforeSplit.split(",");
        double diffFromHome = Math.sqrt(Math.pow(Double.parseDouble(homeLocation[0]) - location.latitude, 2) +
                Math.pow(Double.parseDouble(homeLocation[1]) - location.longitude, 2));
        if (diffFromHome >= 50) {
            Log.d("LocationWorker", "home is far than 50m from your current location");
            setCallbackSuccess();
            return;
        }

        String phoneNumber = sp.getString("userPhoneNumber", null);
        if (phoneNumber == null) {
            Log.d("LocationWorker", "home is close than 50m from your current location but no phone setted");
            setCallbackSuccess();
            return;
        }
        sendSMSHome(phoneNumber);
        setCallbackSuccess();
    }

    private void sendSMSHome(String phoneNumber) {
        Intent smsIntent = new Intent();
        smsIntent.setAction(LocalSendSmsBroadcastReceiver.actionName);
        smsIntent.putExtra(LocalSendSmsBroadcastReceiver.smsMessageContentKey, "Honey I'm Home!");
        smsIntent.putExtra(LocalSendSmsBroadcastReceiver.phoneNumberKey, phoneNumber);
        getApplicationContext().sendBroadcast(smsIntent);
        Log.d("LocationWorker", "finished asking to send sms to " + phoneNumber);

    }
}