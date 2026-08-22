package com.tungsten.fcl.ui.main;

import android.content.Context;
import android.view.View;

import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.viewpager2.widget.ViewPager2;

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
 * <p>
 * 1 trang = 1 tin (vuốt trái/phải để chuyển, KHÔNG tự chuyển — khác thẻ trang
 * chủ) — trước đây xếp chồng dọc khiến tin cũ nhất bị đẩy khuất phía dưới.
 */
public class AnnouncementHistoryDialog extends FCLDialog {

    private final FCLImageButton close;
    private final FCLProgressBar progress;
    private final FCLTextView empty;
    private final ViewPager2 pager;
    private final LinearLayoutCompat dotsPanel;

    public AnnouncementHistoryDialog(Context context) {
        super(context);
        setContentView(R.layout.dialog_announcement_history);

        close = findViewById(R.id.close);
        progress = findViewById(R.id.progress);
        empty = findViewById(R.id.empty);
        pager = findViewById(R.id.pager);
        dotsPanel = findViewById(R.id.dots);
        close.setOnClickListener(v -> dismiss());

        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        empty.setVisibility(View.GONE);
        pager.setVisibility(View.GONE);
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
                    pager.setVisibility(View.VISIBLE);
                    pager.setAdapter(new AnnouncementPagerAdapter(result, 4000));
                    buildDots(result.size());
                }).start();
    }

    private void buildDots(int count) {
        dotsPanel.removeAllViews();
        dotsPanel.setVisibility(count > 1 ? View.VISIBLE : View.GONE);
        if (count <= 1) return;

        View[] dots = new View[count];
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            int size = ConvertUtils.dip2px(getContext(), 6);
            LinearLayoutCompat.LayoutParams params = new LinearLayoutCompat.LayoutParams(size, size);
            params.setMargins(ConvertUtils.dip2px(getContext(), 3), 0, ConvertUtils.dip2px(getContext(), 3), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundColor(i == 0 ? 0xFFFFFFFF : 0x80FFFFFF);
            dotsPanel.addView(dot);
            dots[i] = dot;
        }

        pager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                for (int i = 0; i < dots.length; i++) {
                    dots[i].setBackgroundColor(i == position ? 0xFFFFFFFF : 0x80FFFFFF);
                }
            }
        });
    }
}
