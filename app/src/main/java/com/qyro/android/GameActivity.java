package com.qyro.android;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class GameActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        Bundle extras = getIntent().getExtras();
        String host     = extras != null ? extras.getString("host",     "rd.saturn.lat") : "rd.saturn.lat";
        int    port     = extras != null ? extras.getInt   ("port",     9090)            : 9090;
        String username = extras != null ? extras.getString("username", "player")        : "player";

        gameView = new GameView(this, host, port, username);
        setContentView(gameView);
    }

    @Override protected void onResume() { super.onResume(); gameView.onResume(); }
    @Override protected void onPause()  { super.onPause();  gameView.onPause();  }
}
