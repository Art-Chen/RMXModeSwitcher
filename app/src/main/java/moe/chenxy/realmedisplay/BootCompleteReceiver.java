package moe.chenxy.realmedisplay;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            SharedPreferences sharedPrefs = context.getSharedPreferences("chen_display_config", Context.MODE_PRIVATE);
            Log.i("Art_Chen","Realme Display Mode Boot Set Start!!");
            MainActivity main = new MainActivity();
            main.getAndSetCurrentMode(sharedPrefs, true);
        }
    }
}
