package com.tungsten.fcl.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tungsten.fcl.R;
import com.tungsten.fcl.lylee.LyleeImageSliderView;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fcllibrary.component.view.FCLTextView;

import java.util.List;

/**
 * 1 trang = 1 thông báo (tiêu đề + ảnh + nội dung + ngày) — dùng chung cho cả
 * thẻ trang chủ (ViewPager2 tự chuyển) lẫn dialog "Tin tức" (vuốt tay), thay
 * cho kiểu xếp chồng dọc cũ (nội dung cũ nhất bị đẩy khuất phía dưới).
 */
public class AnnouncementPagerAdapter extends RecyclerView.Adapter<AnnouncementPagerAdapter.PageViewHolder> {

    private final List<Announcement> announcements;
    private final long imageSliderIntervalMs;

    public AnnouncementPagerAdapter(List<Announcement> announcements, long imageSliderIntervalMs) {
        this.announcements = announcements;
        this.imageSliderIntervalMs = imageSliderIntervalMs;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        Announcement announcement = announcements.get(position);
        Context context = holder.itemView.getContext();

        holder.title.setText(announcement.getDisplayTitle(context));
        holder.content.setText(announcement.getDisplayContent(context));
        holder.date.setText(AndroidUtils.getLocalizedText(context, "update_date", announcement.getDate()));

        holder.imageContainer.removeAllViews();
        LyleeImageSliderView slider = new LyleeImageSliderView(context, imageSliderIntervalMs);
        holder.imageContainer.addView(slider, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, com.tungsten.fcllibrary.util.ConvertUtils.dip2px(context, 160)));
        slider.setImages(announcement.getImageUrls());
    }

    @Override
    public int getItemCount() {
        return announcements.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final FCLTextView title;
        final FrameLayout imageContainer;
        final FCLTextView content;
        final FCLTextView date;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.page_title);
            imageContainer = itemView.findViewById(R.id.page_image_container);
            content = itemView.findViewById(R.id.page_content);
            date = itemView.findViewById(R.id.page_date);
        }
    }
}
