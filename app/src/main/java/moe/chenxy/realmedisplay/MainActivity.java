package moe.chenxy.realmedisplay;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.google.android.material.snackbar.Snackbar;
import com.stericson.RootShell.exceptions.RootDeniedException;
import com.stericson.RootShell.execution.Command;
import com.stericson.RootTools.RootTools;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences = getSharedPreferences("chen_display_config",Activity.MODE_PRIVATE);
        editor = preferences.edit();
        initListener();
    }

    RadioButton mDCIMode;
    RadioButton mSRGBMode;
    RadioButton mNormalMode;
    SwitchCompat dcSwitcher;
    SwitchCompat otgSwitcher;
    private void initListener() {
        dcSwitcher = findViewById(R.id.dc_mode);
        otgSwitcher = findViewById(R.id.otg_mode);
        dcSwitcher.setOnCheckedChangeListener((compoundButton, enabled) -> setDCDimming(enabled));
        otgSwitcher.setOnCheckedChangeListener((compoundButton, enabled) -> setOTG(enabled));

        RadioGroup mModeRadioGroup = findViewById(R.id.radio_group);
        mDCIMode = findViewById(R.id.dci_mode);
        mSRGBMode = findViewById(R.id.srgb_mode);
        mNormalMode = findViewById(R.id.off_mode);

        mModeRadioGroup.setOnCheckedChangeListener((group, id) ->
                setDisplayMode(id == mDCIMode.getId() ? 1 : id == mSRGBMode.getId() ? 2 : 0));
        getAndSetCurrentMode(preferences,false);
    }

    public void getAndSetCurrentMode(SharedPreferences preferences, boolean isBootSet) {
        int displayMode = preferences.getInt("displayMode", 0);
        boolean dcDimming = preferences.getBoolean("dcDimming", false);
        boolean otg = preferences.getBoolean("OTG", false);

        if (displayMode == 0 && !dcDimming) {
            Log.i("Art_Chen","Try Getting Status from node");
            dcDimming = FileUtils.readOneLine("/sys/kernel/oppo_display/dimlayer_bl_en").equals("1");
            displayMode = Integer.parseInt(FileUtils.readOneLine("/sys/kernel/oppo_display/seed"));
            otg = FileUtils.readOneLine("/sys/class/power_supply/usb/otg_switch").equals("1");
        }
        if (!isBootSet) {
            setCurrentDisplayMode(displayMode);
            dcSwitcher.setChecked(dcDimming);
            otgSwitcher.setChecked(otg);
        } else {
            editor = preferences.edit();
        }
        setDCDimming(dcDimming);
        setDisplayMode(displayMode);
        setOTG(otg);
    }

    private void setCurrentDisplayMode(int mode) {
        mDCIMode.setChecked(false);
        mSRGBMode.setChecked(false);
        mNormalMode.setChecked(false);
        switch (mode) {
            case 1:
                mDCIMode.setChecked(true);
                break;
            case 2:
                mSRGBMode.setChecked(true);
                break;
            default:
                mNormalMode.setChecked(true);
                break;
        }
    }

    private void setDisplayMode(int mode) {
        try {
            FileUtils.stringToFile("/sys/kernel/oppo_display/seed",Integer.toString(mode));
            editor.putInt("displayMode", mode);
        } catch (IOException e) {
            Log.e("Art_Chen","setMode without root failed! try root");
            if (!execRootShell("echo " + mode + "/sys/kernel/oppo_display/seed")) {
                Snackbar mSnackbar = Snackbar.make(getWindow().getDecorView(),R.string.set_mode_fail,Snackbar.LENGTH_LONG);
                mSnackbar.setTextColor(Color.parseColor("#eee"));
            } else {
                editor.putInt("displayMode", mode);
            }
        }
        editor.commit();
    }

    private void setDCDimming(boolean enable) {
        try {
            FileUtils.stringToFile("/sys/kernel/oppo_display/dimlayer_bl_en",enable ? "1" : "0");
            editor.putBoolean("dcDimming", enable);
        } catch (IOException e) {
            if (!execRootShell("echo " + (enable ? "1" : "0") + "/sys/kernel/oppo_display/dimlayer_bl_en")) {
                Snackbar mSnackbar = Snackbar.make(getWindow().getDecorView(),R.string.set_mode_fail,Snackbar.LENGTH_LONG);
                mSnackbar.setTextColor(Color.parseColor("#eee"));
            } else {
                editor.putBoolean("dcDimming", enable);
            }
        }
        editor.commit();
    }

    private void setOTG(boolean enable) {
        try {
            FileUtils.stringToFile("/sys/class/power_supply/usb/otg_switch",enable ? "1" : "0");
            editor.putBoolean("OTG", enable);
        } catch (IOException e) {
            if (!execRootShell("echo " + (enable ? "1" : "0") + "/sys/class/power_supply/usb/otg_switch")) {
                Snackbar mSnackbar = Snackbar.make(getWindow().getDecorView(),R.string.set_mode_fail,Snackbar.LENGTH_LONG);
                mSnackbar.setTextColor(Color.parseColor("#eee"));
            } else {
                editor.putBoolean("OTG", enable);
            }
        }
        editor.commit();
    }

    private boolean execRootShell(String cmd) {
        Command command = new Command(0, cmd);
        try {
            RootTools.getShell(true).add(command);
        } catch (IOException | TimeoutException | RootDeniedException e) {
            return false;
        }
        return true;
    }
}