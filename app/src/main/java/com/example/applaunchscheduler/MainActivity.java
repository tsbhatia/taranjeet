package com.example.applaunchscheduler;

import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private TextView startTimeView;
    private TextView endTimeView;
    private ArrayAdapter<String> adapter;
    private Set<String> packages;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("schedule", MODE_PRIVATE);
        startTimeView = findViewById(R.id.start_time);
        endTimeView = findViewById(R.id.end_time);
        ListView listView = findViewById(R.id.app_list);

        packages = new HashSet<>(prefs.getStringSet("packages", new HashSet<>()));
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>(packages));
        listView.setAdapter(adapter);

        Button addApp = findViewById(R.id.button_add_app);
        addApp.setOnClickListener(v -> pickApp());

        findViewById(R.id.button_set_start).setOnClickListener(v -> pickTime(true));
        findViewById(R.id.button_set_end).setOnClickListener(v -> pickTime(false));
        findViewById(R.id.button_start_service).setOnClickListener(v -> {
            prefs.edit().putStringSet("packages", packages).apply();
            startService(new Intent(this, LaunchMonitorService.class));
        });

        updateTimeViews();
    }

    private void pickApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        startActivityForResult(Intent.createChooser(intent, "Select app"), 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            ComponentName component = data.getComponent();
            if (component != null) {
                packages.add(component.getPackageName());
                adapter.clear();
                adapter.addAll(packages);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private void pickTime(boolean start) {
        int minutes = prefs.getInt(start ? "startMinutes" : "endMinutes", start ? 0 : 23 * 60 + 59);
        int hour = minutes / 60;
        int min = minutes % 60;
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            int m = hourOfDay * 60 + minute;
            prefs.edit().putInt(start ? "startMinutes" : "endMinutes", m).apply();
            updateTimeViews();
        }, hour, min, true).show();
    }

    private void updateTimeViews() {
        int startMin = prefs.getInt("startMinutes", 0);
        int endMin = prefs.getInt("endMinutes", 23 * 60 + 59);
        startTimeView.setText(String.format(Locale.US, "Start: %02d:%02d", startMin / 60, startMin % 60));
        endTimeView.setText(String.format(Locale.US, "End: %02d:%02d", endMin / 60, endMin % 60));
    }
}
