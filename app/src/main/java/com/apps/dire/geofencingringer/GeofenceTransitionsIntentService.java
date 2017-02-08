package com.apps.dire.geofencingringer;

/**
 * Created by Colin on 05/01/2016.
 */


import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Listener for geofence transition changes. Uses incoming intent
 */
public class GeofenceTransitionsIntentService extends IntentService {

    //Audio manager decleration
    public AudioManager audioManager;
    //String Tag
    protected static final String TAG = "GeofenceTransitionsIS";
    //constructor
    public GeofenceTransitionsIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }



    // Handles incoming intents.
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Context context = getApplicationContext();
        AppPreferences appPreference = new AppPreferences(context);

        boolean system = appPreference.getSystemStatus();
        boolean notifcations = appPreference.getNotifcatonSettings();
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);


        // Test that the reported transition was of interest.
        Log.i("current sysState =", " "+Boolean.toString(system));
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            if(system==true) {

                // Get the geofences that were triggered. A single event can trigger multiple geofences.
                List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

                // Get the transition details as a String.
                String geofenceTransitionDetails = getGeofenceTransitionDetails(
                        this,
                        geofenceTransition,
                        triggeringGeofences
                );
                //audio manager implementation
                String[] parts = geofenceTransitionDetails.split(": ");
                String unstriped = parts[1];
                String transitionid = unstriped.trim();
                int audiomode= appPreference.getRingmode(transitionid);
                audioManager.setRingerMode(audiomode);
                Log.i(Integer.toString(appPreference.getRingmode(transitionid)), "test");
                // 1= vibrate, 2= normal(loud) , 3= silent
                // Send notification and log the transition details.
                Log.i("nofication settings",Boolean.toString(notifcations));
                if(notifcations==true)
                {
                    sendNotification(geofenceTransitionDetails);
                }
                Log.i(TAG, geofenceTransitionDetails);
                Log.i("trans setings", geofenceTransitionDetails);
            }
            if(system==false){
                List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
                Log.i("false","fired");
            }
        }
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
        {
            if(system==true) {
                // Get the geofences that were triggered. A single event can trigger multiple geofences.
                List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

                // Get the transition details as a String.
                String geofenceTransitionDetails = getGeofenceTransitionDetails(
                        this,
                        geofenceTransition,
                        triggeringGeofences
                );

                String[] parts = geofenceTransitionDetails.split(": ");
                String unstriped = parts[1];
                String transitionid = unstriped.trim();
                Log.i("transition manipulation", transitionid);
                int ringmode = appPreference.getRingmode(transitionid);
                int finalringmode = 0;
                int systemswapback = appPreference.getSwapback(transitionid+"swp");
                if(systemswapback==1) {
                    if (ringmode == 2)
                        finalringmode = 1;
                    else if (ringmode == 1)
                        finalringmode = 2;
                    audioManager.setRingerMode(finalringmode);

                    Log.i(Integer.toString(appPreference.getRingmode(transitionid)), "test");
                    // Send notification and log the transition details.
                    if (notifcations == true)
                    {
                        sendNotification(geofenceTransitionDetails);
                    }
                }
                Log.i(TAG, geofenceTransitionDetails);
            }

        }
        else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

   //Gets transition details formats to string
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ", triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }
    //Send the notification to device
    private void sendNotification(String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.drawable.ic_room_black_48dp)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setContentTitle(notificationDetails)
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

  //translate int to text
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }

}
