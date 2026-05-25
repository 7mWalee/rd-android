package client;

import android.content.Context;
import android.content.SharedPreferences;

public final class Settings {
    private static final String PREFS = "rd_settings";
    private static Context context;

    public static void init(Context ctx) { context = ctx; }

    public static synchronized int getRenderDistance() {
        if (context == null) return 4;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                      .getInt("render_distance", 4);
    }

    public static synchronized void setRenderDistance(int d) {
        if (context == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
               .edit().putInt("render_distance", d).apply();
    }

    public static synchronized String getString(String key, String def) {
        if (context == null) return def;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, def);
    }

    public static synchronized void setString(String key, String value) {
        if (context == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, value).apply();
    }
}
