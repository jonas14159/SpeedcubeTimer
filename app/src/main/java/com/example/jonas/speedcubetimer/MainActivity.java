package com.example.jonas.speedcubetimer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupMenu;
import android.widget.TextView;

// 3014159 26535 89793 23846

public class MainActivity extends Activity {

    private final TouchSensor touchSensor = new TouchSensor();
    boolean isUseMilliseconds = false;
    private SpeedcubeTimer speedcubeTimer;
    private TextView timerView;
    private MySpeedcubeListener speedcubeListener = new MySpeedcubeListener();
    private SharedPreferences myPreference;

    @Override
    protected void onPause() {
        super.onPause();

        speedcubeTimer.setListener(null);
    }

    @Override
    protected void onResume() {

        // load settings
        isUseMilliseconds = myPreference.getBoolean("useMilliseconds", true);
        speedcubeTimer.setIsUseInspectionTime(myPreference.getBoolean("inspectionTimeEnable", true));
        speedcubeTimer.setIsAcousticSignalsEnable(myPreference.getBoolean("inspectionAcousticSignals", true));

        speedcubeTimer.setTouchPad(touchSensor);
        speedcubeTimer.setListener(speedcubeListener);

        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.speedcubeTimer.setContext(null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        timerView = (TextView) findViewById(R.id.timerView);
        timerView.setOnClickListener(new TimeViewOnClickListener());

        touchSensor.setView(this.getWindow().getDecorView());

        myPreference = PreferenceManager.getDefaultSharedPreferences(this);

        speedcubeTimer = SpeedcubeApplication.instance().getSpeedcubeTimer();
        speedcubeTimer.setContext(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_show_timeList) {
            Intent intent = new Intent(this, TimeListActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // deactivate back button while inspection and solving
            if (speedcubeTimer.getTimerState() == SpeedcubeTimer.TimerState.inspection ||
                    speedcubeTimer.getTimerState() == SpeedcubeTimer.TimerState.solving) {
                return true;
            } else if (speedcubeTimer.getTimerState() == SpeedcubeTimer.TimerState.solved) {
                speedcubeTimer.reset();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private class MySpeedcubeListener implements SpeedcubeTimer.Listener {
        @Override
        public void onStatusChanged(SpeedcubeTimer.TimerState oldState, SpeedcubeTimer.TimerState newState) {

            if (newState == SpeedcubeTimer.TimerState.ready || newState == SpeedcubeTimer.TimerState.solved) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }

        @Override
        public void onColorChanged() {
            timerView.setTextColor(getResources().getColor(speedcubeTimer.getColorId()));
        }

        @Override
        public void onTimeChanged() {

            String text = "";

            SpeedcubeTimer.TimerState state = speedcubeTimer.getTimerState();

            if (state == SpeedcubeTimer.TimerState.inspection) {

                long inspectionTime = speedcubeTimer.getInspectionTime();

                if (inspectionTime < -2000) {
                    text += "DNF";
                } else if (inspectionTime < 0) {
                    text += "+2";
                } else {
                    text = Time.toString(inspectionTime + 999, 0);
                }
            } else if (state == SpeedcubeTimer.TimerState.solving
                    || state == SpeedcubeTimer.TimerState.solved
                    || state == SpeedcubeTimer.TimerState.ready) {
                text = Time.toString(speedcubeTimer.getCurrentSolvingTime(), isUseMilliseconds ? 3 : 2);
            } else if (state == SpeedcubeTimer.TimerState.ready) {
                text = Time.toString(speedcubeTimer.getSolvingTime().getTimeMs(), isUseMilliseconds ? 3 : 2);
            }

            if (!text.isEmpty()) {
                timerView.setText(text);
            }
        }
    }

    private class TimeViewOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            if(speedcubeTimer.getTimerState() == SpeedcubeTimer.TimerState.solved) {
                PopupMenu popup = new PopupMenu(MainActivity.this, v);

                popup.inflate(R.menu.menu_time_type);

                final SolvingTime solvingTime = speedcubeTimer.getSolvingTime();

                if (solvingTime.getType() == SolvingTime.Type.valid) {
                    popup.getMenu().findItem(R.id.ok).setChecked(true);
                } else if (solvingTime.getType() == SolvingTime.Type.plus2) {
                    popup.getMenu().findItem(R.id.plus2).setChecked(true);
                } else if (solvingTime.getType() == SolvingTime.Type.DNF) {
                    popup.getMenu().findItem(R.id.DNF).setChecked(true);
                }

                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.ok:
                                solvingTime.setType(SolvingTime.Type.valid);
                                return true;
                            case R.id.plus2:
                                solvingTime.setType(SolvingTime.Type.plus2);
                                return true;
                            case R.id.DNF:
                                solvingTime.setType(SolvingTime.Type.DNF);
                                return true;
                            case R.id.delete:
                                SpeedcubeApplication.instance().getTimeList().remove(solvingTime);
                                speedcubeTimer.reset();
                                return true;
                            default:
                                return false;
                        }
                    }
                });

                popup.show();
            }

//            speedcubeTimer.reset();
        }
    }
}
