package com.tungsten.fcl.lylee;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import java.time.Instant;

/**
 * Lưu token đăng nhập player (kết bạn/nhắn tin) — mirror đúng
 * {@code AppSettings.PlayerToken}/{@code PlayerTokenUsername}/
 * {@code PlayerTokenExpiresAt} bên PC. Lần đầu mobile cần loại token này
 * (trước giờ chỉu gọi API ẩn danh session/playtime, không cần đăng nhập) —
 * xem docs/PLAN.md mục 20.
 */
public final class LyleeFriendsSession {

    private LyleeFriendsSession() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("launcher", Context.MODE_PRIVATE);
    }

    public static void save(Context context, String token, String username, String expiresAt) {
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = prefs(context).edit();
        editor.putString("friends_player_token", token);
        editor.putString("friends_player_token_username", username);
        editor.putString("friends_player_token_expires_at", expiresAt);
        editor.apply();
    }

    public static void clear(Context context) {
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = prefs(context).edit();
        editor.remove("friends_player_token");
        editor.remove("friends_player_token_username");
        editor.remove("friends_player_token_expires_at");
        editor.apply();
    }

    public static String getToken(Context context) {
        return prefs(context).getString("friends_player_token", null);
    }

    public static String getUsername(Context context) {
        return prefs(context).getString("friends_player_token_username", null);
    }

    /** Có token hợp lệ (còn hạn, đúng username hiện đang chọn) hay không. */
    public static boolean isValid(Context context, String forUsername) {
        String token = getToken(context);
        String tokenUsername = getUsername(context);
        String expiresAt = prefs(context).getString("friends_player_token_expires_at", null);
        if (token == null || tokenUsername == null || expiresAt == null) return false;
        if (!tokenUsername.equals(forUsername)) return false;
        try {
            return Instant.parse(expiresAt).isAfter(Instant.now());
        } catch (Exception e) {
            return false;
        }
    }
}
