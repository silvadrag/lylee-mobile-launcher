package com.tungsten.fcl.lylee;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.tungsten.fcllibrary.util.ConvertUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Slider ảnh tự chuyển (crossfade) + chấm chỉ mục + bấm/vuốt để chuyển tay —
 * cổng lại đúng UX {@code ImageSliderControl.cs} bên PC launcher (dùng cho
 * banner hero lẫn card tin tức mini) cho mobile, thay vì chỉ xếp ảnh tĩnh
 * chồng dọc. 1 ảnh thì hiện tĩnh, không chấm/không hẹn giờ; ≥2 ảnh mới bật
 * chấm + tự chuyển; bấm vào ảnh (hoặc chấm) chuyển tay và reset lại đồng hồ,
 * tránh "đấu" ngược lại thao tác người dùng ngay sau đó.
 */
public class LyleeImageSliderView extends FrameLayout {

    private final ImageView image;
    private final LinearLayout dotsPanel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final long intervalMs;

    private List<String> urls = new ArrayList<>();
    private int index;

    private final Runnable advanceRunnable = new Runnable() {
        @Override
        public void run() {
            goTo(index + 1, false);
            handler.postDelayed(this, intervalMs);
        }
    };

    public LyleeImageSliderView(@NonNull Context context, long intervalMs) {
        super(context);
        this.intervalMs = intervalMs;
        setVisibility(GONE);

        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        addView(image, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        dotsPanel = new LinearLayout(context);
        dotsPanel.setOrientation(LinearLayout.HORIZONTAL);
        dotsPanel.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams dotsParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        dotsParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        dotsParams.bottomMargin = ConvertUtils.dip2px(context, 6);
        addView(dotsPanel, dotsParams);

        image.setOnClickListener(v -> goTo(index + 1, true));
    }

    public void setImages(List<String> newUrls) {
        handler.removeCallbacks(advanceRunnable);
        urls = new ArrayList<>();
        for (String u : newUrls) {
            if (u != null && !u.isBlank()) urls.add(u);
        }
        index = 0;
        dotsPanel.removeAllViews();

        if (urls.isEmpty()) {
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);

        if (urls.size() > 1) {
            for (int i = 0; i < urls.size(); i++) {
                int dotIndex = i;
                View dot = new View(getContext());
                int size = ConvertUtils.dip2px(getContext(), 6);
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(size, size);
                dotParams.setMargins(ConvertUtils.dip2px(getContext(), 3), 0, ConvertUtils.dip2px(getContext(), 3), 0);
                dot.setLayoutParams(dotParams);
                dot.setBackgroundColor(0x80FFFFFF);
                dot.setOnClickListener(v -> goTo(dotIndex, true));
                dotsPanel.addView(dot);
            }
        }

        showFrame(0);
        if (urls.size() > 1) handler.postDelayed(advanceRunnable, intervalMs);
    }

    private void goTo(int i, boolean userInitiated) {
        if (urls.isEmpty()) return;
        int next = ((i % urls.size()) + urls.size()) % urls.size();
        if (next == index && urls.size() > 1) {
            if (userInitiated) {
                handler.removeCallbacks(advanceRunnable);
                handler.postDelayed(advanceRunnable, intervalMs);
            }
            return;
        }
        index = next;
        showFrame(index);
        if (userInitiated && urls.size() > 1) {
            handler.removeCallbacks(advanceRunnable);
            handler.postDelayed(advanceRunnable, intervalMs);
        }
    }

    private void showFrame(int i) {
        Glide.with(getContext())
                .load(urls.get(i))
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .into(image);

        for (int d = 0; d < dotsPanel.getChildCount(); d++) {
            dotsPanel.getChildAt(d).setBackgroundColor(d == i ? 0xFFFFFFFF : 0x80FFFFFF);
        }
    }
}
