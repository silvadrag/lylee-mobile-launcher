package com.tungsten.fcl.ui.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;

import com.tungsten.fcl.upgrade.UpdateChecker;
import com.tungsten.fcllibrary.util.LocaleUtils;

import java.util.ArrayList;

/**
 * @author Tungsten
 *
 * Announcement v2.
 */
public class Announcement {

    private final int id;
    private final boolean significant;
    private final boolean outdated;
    private final int minVersion;
    private final int maxVersion;
    private final ArrayList<String> specificLang;
    private final ArrayList<Content> title;
    private final String date;
    private final ArrayList<Content> content;
    /** Ảnh admin chèn vào nội dung rich-text bên AdminTool (xem Database.parseXaml
     *  phía mod) — trước đây bị bóc bỏ luôn cùng markup XAML, giờ tách riêng ra
     *  đây. Null-safe: field cũ trên server chưa deploy vẫn thiếu key này, Gson
     *  để null thay vì lỗi parse. */
    private final ArrayList<String> imageUrls;

    public Announcement(int id, boolean significant, boolean outdated, int minVersion, int maxVersion, ArrayList<String> specificLang, ArrayList<Content> title, String date, ArrayList<Content> content, ArrayList<String> imageUrls) {
        this.id = id;
        this.significant = significant;
        this.outdated = outdated;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.specificLang = specificLang;
        this.title = title;
        this.date = date;
        this.content = content;
        this.imageUrls = imageUrls;
    }

    public ArrayList<String> getImageUrls() {
        return imageUrls != null ? imageUrls : new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public boolean isSignificant() {
        return significant;
    }

    public boolean isOutdated() {
        return outdated;
    }

    public int getMinVersion() {
        return minVersion;
    }

    public int getMaxVersion() {
        return maxVersion;
    }

    public ArrayList<String> getSpecificLang() {
        return specificLang;
    }

    public ArrayList<Content> getTitle() {
        return title;
    }

    public String getDate() {
        return date;
    }

    public ArrayList<Content> getContent() {
        return content;
    }

    public String getDisplayTitle(Context context) {
        if (title.isEmpty()) {
            throw new IllegalStateException("No title list!");
        }
        for (Content c : title) {
            if (LocaleUtils.getLocale(LocaleUtils.getLanguage(context)).toString().contains(c.getLang())) {
                return c.getText();
            }
        }
        return title.get(0).getText();
    }

    public String getDisplayContent(Context context) {
        if (content.isEmpty()) {
            throw new IllegalStateException("No content list!");
        }
        for (Content c : content) {
            if (LocaleUtils.getLocale(LocaleUtils.getLanguage(context)).toString().contains(c.getLang())) {
                return c.getText();
            }
        }
        return content.get(0).getText();
    }

    public boolean shouldDisplay(Context context) {
        if (outdated)
            return false;
        if (minVersion != -1 && minVersion > UpdateChecker.getCurrentVersionCode(context))
            return false;
        if (maxVersion != -1 && maxVersion < UpdateChecker.getCurrentVersionCode(context))
            return false;
        if (!specificLang.isEmpty()) {
            boolean cancel = true;
            for (String lang : specificLang) {
                if (LocaleUtils.getLocale(LocaleUtils.getLanguage(context)).toString().contains(lang)) {
                    cancel = false;
                    break;
                }
            }
            if (cancel)
                return false;
        }
        // Ẩn kiểu "nhắc nhở mỗi ngày": bấm ẩn chỉ tắt card trong ngày hôm đó, hôm
        // sau (còn hạn/còn active) sẽ tự hiện lại thay vì mất hẳn tới khi có
        // thông báo mới — xem lại lịch sử đầy đủ qua AnnouncementListPage (nút
        // chuông) nếu muốn đọc lại bất kể đã ẩn hôm nào.
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher", Context.MODE_PRIVATE);
        int hiddenId = sharedPreferences.getInt("ignore_announcement", 0);
        if (hiddenId != id) return true;
        String hiddenDate = sharedPreferences.getString("ignore_announcement_date", "");
        return !java.time.LocalDate.now().toString().equals(hiddenDate);
    }

    public void hide(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("launcher", Context.MODE_PRIVATE);
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("ignore_announcement", id);
        editor.putString("ignore_announcement_date", java.time.LocalDate.now().toString());
        editor.apply();
    }

    public final static class Content {

        private final String lang;
        private final String text;

        public Content(String lang, String text) {
            this.lang = lang;
            this.text = text;
        }

        public String getLang() {
            return lang;
        }

        public String getText() {
            return text;
        }

    }

}
