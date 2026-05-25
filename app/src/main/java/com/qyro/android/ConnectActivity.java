package com.qyro.android;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import client.Settings;

public class ConnectActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Settings.init(this);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0D0D12);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(60, 100, 60, 60);

        TextView title = new TextView(this);
        title.setText("Qyro Client");
        title.setTextSize(28f);
        title.setTextColor(0xFF64C8FF);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = 40;
        title.setLayoutParams(titleParams);
        layout.addView(title);

        EditText usernameField = new EditText(this);
        usernameField.setHint("Username");
        usernameField.setText(Settings.getString("username", "player"));
        usernameField.setTextColor(0xFFFFFFFF);
        usernameField.setHintTextColor(0xFF888888);
        usernameField.setBackgroundColor(0xFF1A1A22);
        usernameField.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldParams.bottomMargin = 16;
        usernameField.setLayoutParams(fieldParams);
        layout.addView(usernameField);

        EditText hostField = new EditText(this);
        hostField.setHint("Server (host:port)");
        String h = Settings.getString("host", "rd.saturn.lat");
        String p = Settings.getString("port", "9090");
        hostField.setText(h + ":" + p);
        hostField.setTextColor(0xFFFFFFFF);
        hostField.setHintTextColor(0xFF888888);
        hostField.setBackgroundColor(0xFF1A1A22);
        hostField.setPadding(20, 20, 20, 20);
        LinearLayout.LayoutParams hostParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hostParams.bottomMargin = 32;
        hostField.setLayoutParams(hostParams);
        layout.addView(hostField);

        Button connectBtn = new Button(this);
        connectBtn.setText("CONNECT");
        connectBtn.setBackgroundColor(0xFF2864DC);
        connectBtn.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        connectBtn.setLayoutParams(btnParams);
        layout.addView(connectBtn);

        connectBtn.setOnClickListener(v -> {
            String user   = usernameField.getText().toString().trim();
            String server = hostField.getText().toString().trim();
            String host   = server.contains(":") ? server.split(":")[0] : server;
            int    port   = server.contains(":") ? Integer.parseInt(server.split(":")[1]) : 9090;

            Settings.setString("username", user);
            Settings.setString("host", host);
            Settings.setString("port", String.valueOf(port));

            Intent i = new Intent(this, GameActivity.class);
            i.putExtra("username", user);
            i.putExtra("host", host);
            i.putExtra("port", port);
            startActivity(i);
        });

        scroll.addView(layout);
        setContentView(scroll);
    }
}
