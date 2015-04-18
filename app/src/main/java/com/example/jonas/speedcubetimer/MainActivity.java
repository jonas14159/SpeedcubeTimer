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
import android.widget.TextView;

// 3014159 26535 89793 23846

public class MainActivity extends Activity {

    private final TouchSensor touchSensor = new TouchSensor();
    boolean isUseMilliseconds = false;
    private SpeedcubeTimer speedcubeTimer = SpeedcubeApplication.instance().getSpeedcubeTimer();
    private TimeSession session = SpeedcubeApplication.instance().getTimeSession();
    private TextView timerView;
    private TextView timerViewAverage5;
    private TextView timerViewAverage12;
    private MySpeedcubeListener speedcubeListener = new MySpeedcubeListener();
    private SharedPreferences myPreference;

    private TimeSession.OnAverageChangedListener onAverageChangedListener = new TimeSession.OnAverageChangedListener() {
        @Override
        public void onAverageChanged() {
            timerViewAverage5.setText(Time.toStringMs(session.getAverage5()));
            timerViewAverage12.setText(Time.toStringMs(session.getAverage12()));
        }
    };

    @Override
    protected void onPause() {
        super.onPause();

        speedcubeTimer.setListener(null);
        session.setOnChangeLister(null);
    }

    @Override
    protected void onResume() {

        // load settings
        isUseMilliseconds = myPreference.getBoolean("useMilliseconds", true);
        speedcubeTimer.setIsUseInspectionTime(myPreference.getBoolean("inspectionTimeEnable", true));
        speedcubeTimer.setIsAcousticSignalsEnable(myPreference.getBoolean("inspectionAcousticSignals", true));

        speedcubeTimer.setTouchPad(touchSensor);
        speedcubeTimer.setListener(speedcubeListener);

        session.setOnChangeLister(onAverageChangedListener);

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

        timerViewAverage12 = (TextView) findViewById(R.id.textViewAverage12);
        timerViewAverage5 = (TextView) findViewById(R.id.textViewAverage5);

        timerView = (TextView) findViewById(R.id.timerView);
        timerView.setOnClickListener(new TimeViewOnClickListener());

        touchSensor.setView(this.getWindow().getDecorView());

        myPreference = PreferenceManager.getDefaultSharedPreferences(this);

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

    private void updateTypeView() {
        if(speedcubeTimer.getTime().getType() == Time.Type.DNF) {
            ((TextView) findViewById(R.id.textViewType)).setText(getString(R.string.DNF));
        }else if(speedcubeTimer.getTime().getType() == Time.Type.plus2) {
            ((TextView) findViewById(R.id.textViewType)).setText(getString(R.string.plus2));
        }
        else
        {
            ((TextView) findViewById(R.id.textViewType)).setText("");
        }
    }

    private class MySpeedcubeListener implements SpeedcubeTimer.Listener {
        @Override
        public void onStatusChanged(SpeedcubeTimer.TimerState oldState, SpeedcubeTimer.TimerState newState) {

            if (newState == SpeedcubeTimer.TimerState.ready || newState == SpeedcubeTimer.TimerState.solved) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }

            updateTypeView();
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
                    text += getString(R.string.DNF);
                } else if (inspectionTime < 0) {
                    text += getString(R.string.plus2);
                } else {
                    text = Time.toString(inspectionTime + 999, 0);
                }
            } else if (state == SpeedcubeTimer.TimerState.solving
                    || state == SpeedcubeTimer.TimerState.solved
                    || state == SpeedcubeTimer.TimerState.ready) {
                text = Time.toString(speedcubeTimer.getCurrentSolvingTime(), isUseMilliseconds ? 3 : 2);
            } else if (state == SpeedcubeTimer.TimerState.ready) {
                text = Time.toString(speedcubeTimer.getTime().getTimeMs(), isUseMilliseconds ? 3 : 2);
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

                final Time time = speedcubeTimer.getTime();

                time.showPopupMenu(MainActivity.this, timerView, new Time.PopupMenuLister() {
                    @Override
                    public void onAction(int id) {

                        if(id == R.id.delete){
                            speedcubeTimer.reset();
                        }

                        updateTypeView();
                    }
                });
            }

//            speedcubeTimer.reset();
        }
    }
}
