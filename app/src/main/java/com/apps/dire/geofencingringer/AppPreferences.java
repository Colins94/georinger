package com.apps.dire.geofencingringer;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Colin on 14/02/2016.
 */

// A Class that holds most share preferences data to share between other classes
public class AppPreferences {

    private SharedPreferences AppSharedPreferences;



    public AppPreferences(Context context) {
        super();
        AppSharedPreferences = context.getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
    }
    //Get status method
    public boolean getSystemStatus(){
        return AppSharedPreferences.getBoolean("sysStat",false);
    }
    //Set status method
    public void setSystemStatus(Boolean bool){
        SharedPreferences.Editor editor = AppSharedPreferences.edit();
        editor.putBoolean("sysStat", bool);
        editor.commit();
    }
    //Get notification method
    public boolean getNotifcatonSettings(){
        return AppSharedPreferences.getBoolean("notifications",true);
    }
    //Set notification method
    public void setNotifcationSettings(Boolean bool){
        SharedPreferences.Editor editor = AppSharedPreferences.edit();
        editor.putBoolean("notifications",bool);
        editor.commit();
    }
    //Get ringmode method
    public int getRingmode(String id){
        return  AppSharedPreferences.getInt(id,0);
    }
    //Set ringmode method
    public void setRingmode(String id,int mode){
        SharedPreferences.Editor editor = AppSharedPreferences.edit();
        editor.putInt(id,mode);
        editor.commit();
    }
    //Get swapback method
    public int getSwapback(String id){
        return AppSharedPreferences.getInt(id,0);
    }
    //Set swapback method
    public void setSwapback(String string, int mode){
        SharedPreferences.Editor editor = AppSharedPreferences.edit();
        editor.putInt(string,mode);
        editor.commit();
    }
}
