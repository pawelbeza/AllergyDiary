package com.example.allergydiary.Fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.allergydiary.Notifications.AlarmReceiver;
import com.example.allergydiary.Notifications.DeviceBootReceiver;
import com.example.allergydiary.R;
import com.example.allergydiary.TimeHelper;

import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;

public class SettingsFragment extends Fragment {
    private final int cornerRadius = 40;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void assignClickHandler(int tvID, int swID, int toBeColored) {
        final TextView textView = Objects.requireNonNull(getActivity()).findViewById(tvID);
        final Switch sw = getActivity().findViewById(swID);
        textView.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), (view, hourOfDay, minute) -> {
                textView.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
                sw.setChecked(true);
            }, 0, 0, DateFormat.is24HourFormat(getActivity()));
            timePickerDialog.show();
        });

        assignSwitchOnClickListener(Objects.requireNonNull(getActivity()).findViewById(swID), getActivity().findViewById(toBeColored));
    }

    private void assignSwitchOnClickListener(Switch simpleSwitch, final View view) {
        int colorFrom = ContextCompat.getColor(Objects.requireNonNull(getActivity()), R.color.bright_red);
        int colorTo = ContextCompat.getColor(getActivity(), R.color.bright_green);
        final ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(250);

        colorAnimation.addUpdateListener(animation -> {
            GradientDrawable shape = new GradientDrawable();
            shape.setColor((int) animation.getAnimatedValue());
            shape.setCornerRadius(cornerRadius);
            view.setBackground(shape);
        });

        simpleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked)   colorAnimation.start();
            else    colorAnimation.reverse();
        });
    }

    private void setSwitchBackground(boolean b, int toBeColoredID, int switchID) {
        Switch simpleSwitch = Objects.requireNonNull(getActivity()).findViewById(switchID);
        simpleSwitch.setChecked(b);
        simpleSwitch.jumpDrawablesToCurrentState();

        View view = getActivity().findViewById(toBeColoredID);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(cornerRadius);
        int color = b ? ContextCompat.getColor(getActivity(), R.color.bright_green) :
                ContextCompat.getColor(getActivity(), R.color.bright_red);
        shape.setColor(color);
        view.setBackground(shape);
    }

    @Override
    public void onPause() {
        super.onPause();

        int[] switchIDs = {R.id.switch1, R.id.switch2, R.id.switch3};
        int[] tvIDs = {R.id.everyDay, R.id.morning, R.id.evening};

        SharedPreferences sharedPreferences = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (int i = 0; i < switchIDs.length; i++) {
            Switch sw = getActivity().findViewById(switchIDs[i]);
            editor.putBoolean("PopUpScheduleChecked" + i, sw.isChecked());

            TextView tv = getActivity().findViewById(tvIDs[i]);
            editor.putString("PopUpSchedule" + i, tv.getText().toString());

            String[] notificationContent = i == 0 ? getActivity().getResources().getStringArray(
                    R.array.questionnaireNotification) :
                    getActivity().getResources().getStringArray(R.array.medicineNotification);

            setAlarm(sw.isChecked(), tv, i, notificationContent);
        }

        setDeviceBootReceiver();

        editor.apply();
    }


    private void setAlarm(Boolean dailyNotify, TextView tv, int notificationId, String[] notificationContent) {
        Intent alarmIntent = new Intent(getActivity(), AlarmReceiver.class);
        alarmIntent.putExtra("notificationContent", notificationContent);
        alarmIntent.putExtra("id", notificationId);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), notificationId, alarmIntent, 0);

        AlarmManager manager = (AlarmManager) Objects.requireNonNull(getActivity()).getSystemService(Context.ALARM_SERVICE);

        if (dailyNotify) {
            //enable daily notifications
            Calendar calendar = TimeHelper.stringToCalendar(tv.getText().toString());
            // if notification time is before selected time, send notification the next day
            if (calendar.before(Calendar.getInstance())) {
                calendar.add(Calendar.DATE, 1);
            }
            if (manager != null) {
                manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY, pendingIntent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                }
            }
        } else { //disable Daily Notification
            if (PendingIntent.getBroadcast(getActivity(), notificationId, alarmIntent, 0) != null && manager != null) {
                manager.cancel(pendingIntent);
            }
        }
    }

    private void setDeviceBootReceiver() {
        PackageManager pm = Objects.requireNonNull(getActivity()).getPackageManager();
        ComponentName receiver = new ComponentName(getActivity(), DeviceBootReceiver.class);

        int[] switchIDs = {R.id.switch1, R.id.switch2, R.id.switch3};

        for (int id : switchIDs) {
            Switch sw = getActivity().findViewById(id);
            if (sw.isChecked()) {
                //To enable Boot Receiver class
                pm.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                return;
            }
        }

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onResume() {
        super.onResume();

        int[] switchIDs = {R.id.switch1, R.id.switch2, R.id.switch3};
        int[] toBeColoredIDs = {R.id.switch1Layout, R.id.switch2Layout, R.id.switch3Layout};
        int[] tvIDs = {R.id.everyDay, R.id.morning, R.id.evening};
        String[] time = {"20:00", "8:00", "20:00"};

        SharedPreferences sharedPref;
        try {
            sharedPref = Objects.requireNonNull(getActivity()).getPreferences(Context.MODE_PRIVATE);
        } catch (NullPointerException e) {
            for (int i = 0; i < switchIDs.length; i++)
                setSwitchBackground(false, toBeColoredIDs[i], switchIDs[i]);
            return;
        }

        for (int i = 0; i < 3; i++) {
            boolean isChecked = sharedPref.getBoolean("PopUpScheduleChecked" + i, false);
            View view = getActivity().findViewById(toBeColoredIDs[i]);

            if (isChecked)
                setSwitchBackground(true, toBeColoredIDs[i], switchIDs[i]);
            else
                setSwitchBackground(false, toBeColoredIDs[i], switchIDs[i]);

            String hour = sharedPref.getString("PopUpSchedule" + i, time[i]);
            TextView tv = view.findViewById(tvIDs[i]);
            tv.setText(hour);
        }
        assignClickHandler(R.id.everyDay, R.id.switch1, R.id.switch1Layout);
        assignClickHandler(R.id.morning, R.id.switch2, R.id.switch2Layout);
        assignClickHandler(R.id.evening, R.id.switch3, R.id.switch3Layout);
    }
}
