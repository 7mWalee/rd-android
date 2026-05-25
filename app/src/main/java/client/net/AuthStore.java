package client.net;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthStore {
    private static final String PREFS = "rd_auth";
    private static Context context;

    public static void init(Context ctx) { context = ctx; }

    public static String getToken(String serverId, String username) {
        if (context == null) return null;
        String val = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .getString(serverId + "_" + username, null);
        return (val == null || val.isEmpty()) ? null : val;
    }

    public static void saveToken(String serverId, String username, String token) {
        if (context == null) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
               .edit().putString(serverId + "_" + username, token).apply();
    }
}
