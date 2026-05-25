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

        gameView = new GameView(this);
        setContentView(gameView);

        // Connect with extras from ConnectActivity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            gameView.post(() -> {
                GameRenderer r = gameView.getRenderer();
                if (r != null) r.connect(
                    extras.getString("host", "rd.saturn.lat"),
                    extras.getInt("port", 9090),
                    extras.getString("username", "player"));
            });
        }
    }

    @Override protected void onResume() { super.onResume(); gameView.onResume(); }
    @Override protected void onPause()  { super.onPause();  gameView.onPause(); }
}
