package com.apps.dire.geofencingringer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import java.util.ArrayList;
//Home activity contains most application funtionality
public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    //PlayServices
    protected GoogleApiClient mGoogleApiClient;
    //location service
    //Aray of fences
    protected ArrayList<Geofence> GeofenceList;
    //check if fences are added
    private boolean mGeofencesAdded;
    //intent to add remove fence
    private PendingIntent GeofencePendingIntent;
    //persistant storage
    private SharedPreferences mSharedPreferences;
    //GPS long/lat
    double gpslong, gpslat;
    //key
    public static final String GEOFENCES_ADDED_KEY =  "LocationRinger.GEOFENCES_ADDED_KEY";
    //string for name
    String fencename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Sharedpref
        mSharedPreferences = getSharedPreferences("sharedPref", MODE_PRIVATE);
        mGeofencesAdded = mSharedPreferences.getBoolean(GEOFENCES_ADDED_KEY, false);
        GeofenceList = new ArrayList<Geofence>();
        setPrefs();
        switches();
        buildGoogleApiClient();
        //Floating button clicklisterner
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputPopup();
            }
        });
    }
    //creates options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main,menu);
        return true;
    }
    //option menu item selection handle
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.remove:
                removeFence();
                return true;
            case R.id.helppage:
                Intent intent = new Intent(this,Help.class);
                startActivity(intent);
               return true;
        }
        return false;
    }
   //Builds API client
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
    //onStart api method
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }
    //onStop api method
    @Override
    protected void onStop() {
        super.onStop();
        mGoogleApiClient.disconnect();
    }
    //On Conected api method
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i("tg", "Connected to GoogleApiClient");
    }
    //failed connection handle
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i("tg", "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }
    //suspend connection handle
    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason.
        Log.i("tg", "Connection suspended");

        // onConnected() will be called again automatically when the service reconnects
    }
    //gets last location
    public void lastLocation() {
        //error handle
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("PERMISSIONS", "not got permissions");
            return;
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            gpslong = mLastLocation.getLongitude();
            gpslat = mLastLocation.getLatitude();
            Log.d("SETLONG", "long is " + gpslong);
            Log.d("SETLAT", "lat is " + gpslat);
        }


    }
    //fence creation
    public void createFence() {
        lastLocation();
        GeofenceList.add(new Geofence.Builder()
                //build a geofence
                .setRequestId(fencename)
                .setCircularRegion(
                        //1609 mtrs = 1 mile
                        gpslat, gpslong, 1609
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                        Geofence.GEOFENCE_TRANSITION_EXIT)
                .build());
        Log.i("geofence", "geofence built at lat " + gpslat + " long " + gpslong);
        addFences();
    }
    //geofence requester
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(GeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }
    //Use to monitor switches
    public void switches() {
        Switch sysRun = (Switch) findViewById(R.id.swApp);
        Switch sysnotif = (Switch) findViewById(R.id.swNotif);
        final TextView status = (TextView) findViewById(R.id.tvSystemStatus);
        final TextView notif = (TextView) findViewById(R.id.tvNotifcation);

        //application switch listener
        sysRun.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Context context = getApplicationContext();
                AppPreferences appPreference = new AppPreferences(context);
                if (isChecked) {
                    appPreference.setSystemStatus(true);
                    status.setText("System Running");

                } else {
                    appPreference.setSystemStatus(false);
                    status.setText("System Off");
                }

            }
        });
        //notifcation switch listener
        sysnotif.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Context context = getApplicationContext();
                AppPreferences appPreference = new AppPreferences(context);
                if (isChecked) {
                    appPreference.setNotifcationSettings(true);
                    notif.setText("Notifications On");
                } else {
                    appPreference.setNotifcationSettings(false);
                    notif.setText("Notifications Off");
                }

            }
        });
    }
    //adds the fences
    public void addFences() {
        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }
    //remove fences
    public void removeFence(){
        try {
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    getGeofencePendingIntent());
                    //clear aray list incase application is left running
                    GeofenceList.clear();
        }
        catch (SecurityException securityExeption){
            logSecurityException(securityExeption);
        }
    }
    //Intent for geofences
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (GeofencePendingIntent != null) {
            return GeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
    //Security exeption
    private void logSecurityException(SecurityException securityException) {
        Log.e("Tag", "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }
    //Resutl handle
    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.apply();
            Toast.makeText(
                    this,
                    "Location Added",
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e("tag", errorMessage);
        }
    }
    //Sets switches to correct state
   public void setPrefs(){
        Switch sysRun = (Switch) findViewById(R.id.swApp);
        Switch sysnotif = (Switch) findViewById(R.id.swNotif);
        TextView sysStat = (TextView) findViewById(R.id.tvSystemStatus);
        TextView notStat = (TextView) findViewById(R.id.tvNotifcation);
        Context context = getApplicationContext();
        AppPreferences appPreference = new AppPreferences(context);
        Boolean sysrn = appPreference.getSystemStatus();
        Boolean notificationset = appPreference.getNotifcatonSettings();
        if(sysrn){
            sysStat.setText("System Running");
            sysRun.setChecked(true);
        }
        else if (!sysrn){
            sysStat.setText("System Off");
            sysRun.setChecked(false);
        }

        if(notificationset){
            notStat.setText("Notifications On");
            sysnotif.setChecked(true);

        }
        else if (!notificationset){
            notStat.setText("Notifications Off");
            sysnotif.setChecked(false);
        }
    }
    //Creates popup window to create a new geofence
    protected void inputPopup(){
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.popup_menu, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);
        final EditText fenceinput = (EditText) promptView.findViewById(R.id.edittext);
        final Switch ringervol = (Switch) promptView.findViewById(R.id.swringer);
        final CheckBox ckswap = (CheckBox) promptView.findViewById(R.id.ckSwapback);
        Context context = getApplicationContext();
        final AppPreferences appPreferences = new AppPreferences(context);
        //builds popup box
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        fencename = fenceinput.getText().toString();
                        Log.i("fencename", fencename);
                        if (ringervol.isChecked()) {
                            Log.i("ringerck", "ringer is checked true");
                            createFence();
                            appPreferences.setRingmode(fencename, 2);
                            if (ckswap.isChecked()) {
                                appPreferences.setSwapback(fencename+"swp", 1);
                            }
                        }
                        else {
                            Log.i("ringerck", "ringer is checked false");
                            createFence();
                            appPreferences.setRingmode(fencename, 1);
                            if (ckswap.isChecked()) {
                                appPreferences.setSwapback(fencename+"swp", 1);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public void openmaps(View view){
        lastLocation();
        Intent intent = new Intent(this,MapsActivity.class);
        intent.putExtra("lng", gpslong);
        intent.putExtra("lat", gpslat);
        startActivity(intent);
    }



}


