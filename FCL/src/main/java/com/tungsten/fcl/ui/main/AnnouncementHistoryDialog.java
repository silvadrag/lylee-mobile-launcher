package com.tungsten.fcl.ui.main;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;

import androidx.appcompat.widget.LinearLayoutCompat;

import com.google.gson.reflect.TypeToken;
import com.tungsten.fcl.R;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fclcore.util.Logging;
import com.tungsten.fclcore.util.io.HttpRequest;
import com.tungsten.fcllibrary.component.dialog.FCLDialog;
import com.tungsten.fcllibrary.component.view.FCLImageButton;
import com.tungsten.fcllibrary.component.view.FCLProgressBar;
import com.tungsten.fcllibrary.component.view.FCLTextView;
import com.tungsten.fcllibrary.util.ConvertUtils;

import java.util.ArrayList;
import java.util.logging.Level;

/**
 * Dialog xem lại toàn bộ tin tức gần đây (mở từ nút chuông trên Trang chủ),
 * khác card announcement ở MainUI chỉ hiện 1 tin mới nhất và có thể bị ẩn.
 * Dùng FCLDialog (giống TaskDialog) thay vì FCLPage/showTempPage vì MainUI là
 * FCLCommonUI (không có overlay trang tạm), với lại showTempPage gọi trên UI
 * khác tab (ManageUI) sẽ không hiện được vì contentView của tab đó chưa được
 * ViewPager2 gắn vào cây view đang hiển thị.
 */
public class AnnouncementHistoryDialog extends FCLDialog {

    private final FCLImageButton close;
    private final FCLProgressBar progress;
    private final FCLTextView empty;
    private final ScrollView scroll;
    private final LinearLayoutCompat container;

    public AnnouncementHistoryDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_announcement_history);

        close = findViewById(R.id.close);
        progress = findViewById(R.id.progress);
        empty = findViewById(R.id.empty);
        scroll = findViewById(R.id.scroll);
        close.setOnClickListener(v -> dismiss());

        container = new LinearLayoutCompat(getContext());
        container.setOrientation(LinearLayoutCompat.VERTICAL);
        scroll.addView(container, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scroll.setVisibility(View.GONE);

        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        scroll.setVisibility(View.GONE);
        Task.supplyAsync(() -> HttpRequest.HttpGetRequest.GET(MainUI.ANNOUNCEMENT_LIST_URL).getJson(new TypeToken<ArrayList<Announcement>>() {
                }))
                .whenComplete(Schedulers.androidUIThread(), (result, exception) -> {
                    if (!isShowing()) return;
                    progress.setVisibility(View.GONE);
                    if (exception != null) {
                        Logging.LOG.log(Level.WARNING, "Failed to load announcement history!", exception);
                        empty.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (result.isEmpty()) {
                        empty.setVisibility(View.VISIBLE);
                        return;
                    }
                    scroll.setVisibility(View.VISIBLE);
                    for (Announcement announcement : result) {
                        container.addView(createItemView(announcement));
                    }
                }).start();
    }

    private View createItemView(Announcement announcement) {
        int padding = ConvertUtils.dip2px(getContext(), 10);

        LinearLayoutCompat item = new LinearLayoutCompat(getContext());
        item.setOrientation(LinearLayoutCompat.VERTICAL);
        item.setBackgroundResource(R.drawable.bg_container_white);
        item.setPadding(padding, padding, padding, padding);
        LinearLayoutCompat.LayoutParams itemParams = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemParams.bottomMargin = padding;
        item.setLayoutParams(itemParams);

        FCLTextView title = new FCLTextView(getContext());
        title.setText(announcement.getDisplayTitle(getContext()));
        title.setAutoTint(true);
        title.setTextSize(16);
        item.addView(title);

        FCLTextView date = new FCLTextView(getContext());
        date.setText(announcement.getDate());
        date.setAutoTint(true);
        date.setTextSize(12);
        item.addView(date);

        FCLTextView content = new FCLTextView(getContext());
        content.setText(announcement.getDisplayContent(getContext()));
        content.setAutoTint(true);
        LinearLayoutCompat.LayoutParams contentParams = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.topMargin = ConvertUtils.dip2px(getContext(), 6);
        content.setLayoutParams(contentParams);
        item.addView(content);

        return item;
    }
}
