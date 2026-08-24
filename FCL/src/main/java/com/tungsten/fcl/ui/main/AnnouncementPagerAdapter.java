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

    // Số vòng ảo cấp cho ViewPager2 khi bật infinite — đủ lớn để không ai chạm biên
    // thật trong 1 phiên sử dụng (auto-advance mỗi 6s thì cũng phải >80 giờ liên tục
    // mới hết), không dùng Integer.MAX_VALUE vì dễ lệch modulo khi kích cỡ danh sách
    // thực không chia hết số đó.
    private static final int INFINITE_LOOPS = 10_000;

    private final List<Announcement> announcements;
    private final long imageSliderIntervalMs;
    private final boolean infinite;

    public AnnouncementPagerAdapter(List<Announcement> announcements, long imageSliderIntervalMs) {
        this(announcements, imageSliderIntervalMs, false);
    }

    /**
     * @param infinite true = ViewPager2 vuốt/tự chuyển vô hạn theo 1 vòng lặp tròn
     *                 (dùng cho thẻ trang chủ tự chuyển) — khi false, hành vi y hệt
     *                 constructor cũ (dùng cho dialog "Tin tức" vuốt tay, hữu hạn).
     *                 Lý do cần vòng lặp thật thay vì chỉ setCurrentItem(0) khi hết
     *                 danh sách: ViewPager2 không biết "vòng lại", nên nhảy từ trang
     *                 cuối về trang đầu bằng smooth scroll sẽ cuộn NGƯỢC qua hết mọi
     *                 trang ở giữa — đúng thứ tạo cảm giác "bị kéo lướt nhanh" người
     *                 dùng mô tả, không phải reload gì cả.
     */
    public AnnouncementPagerAdapter(List<Announcement> announcements, long imageSliderIntervalMs, boolean infinite) {
        this.announcements = announcements;
        this.imageSliderIntervalMs = imageSliderIntervalMs;
        this.infinite = infinite && announcements.size() > 1;
    }

    /** Vị trí ViewPager2 thật ↔ chỉ số thật trong danh sách (luôn dùng cái này để
     *  đọc announcements, dù infinite hay không — an toàn cho cả 2 chế độ). */
    public int toRealPosition(int viewPagerPosition) {
        return viewPagerPosition % announcements.size();
    }

    /** Vị trí ViewPager2 nên đặt lúc khởi tạo (setCurrentItem) để tương ứng đúng
     *  chỉ số thật 0 — nằm giữa dải ảo để vuốt lùi cũng còn nhiều dư địa. */
    public int getStartPosition() {
        if (!infinite) return 0;
        int mid = getItemCount() / 2;
        return mid - (mid % announcements.size());
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement_page, parent, false);
        return new PageViewHolder(view, imageSliderIntervalMs);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        Announcement announcement = announcements.get(toRealPosition(position));
        Context context = holder.itemView.getContext();

        holder.title.setText(announcement.getDisplayTitle(context));
        holder.content.setText(announcement.getDisplayContent(context));
        holder.date.setText(AndroidUtils.getLocalizedText(context, "update_date", announcement.getDate()));
        // slider được TÁI SỬ DỤNG (tạo đúng 1 lần trong onCreateViewHolder, xem
        // PageViewHolder) chứ không dựng mới ở đây nữa — xem giải thích ở
        // PageViewHolder về lý do đây từng là nguồn giật khi chuyển trang.
        holder.slider.setImages(announcement.getImageUrls());
    }

    @Override
    public int getItemCount() {
        return infinite ? announcements.size() * INFINITE_LOOPS : announcements.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        final FCLTextView title;
        final FCLTextView content;
        final FCLTextView date;
        final LyleeImageSliderView slider;

        // Trước đây onBindViewHolder tự removeAllViews() + "new LyleeImageSliderView(...)"
        // MỖI LẦN bind — kể cả bind lại 1 ViewHolder đã tồn tại (RecyclerView tái sử
        // dụng ViewHolder liên tục khi cuộn/prefetch), tức là dựng lại TOÀN BỘ 1 custom
        // View (ImageView + panel chấm + Handler + ràng buộc Glide) đúng lúc animation
        // chuyển trang đang chạy — chính là nguyên nhân cảm giác "cà giật" khi vuốt/tự
        // chuyển, đúng anti-pattern kinh điển của RecyclerView (việc dựng view thuộc về
        // onCreateViewHolder, onBindViewHolder chỉ nên đổi DỮ LIỆU hiển thị). Giờ tạo
        // slider đúng 1 lần ở đây, mỗi lần bind chỉ gọi setImages() trên slider có sẵn.
        PageViewHolder(@NonNull View itemView, long imageSliderIntervalMs) {
            super(itemView);
            title = itemView.findViewById(R.id.page_title);
            content = itemView.findViewById(R.id.page_content);
            date = itemView.findViewById(R.id.page_date);
            FrameLayout imageContainer = itemView.findViewById(R.id.page_image_container);
            slider = new LyleeImageSliderView(itemView.getContext(), imageSliderIntervalMs);
            imageContainer.addView(slider, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    com.tungsten.fcllibrary.util.ConvertUtils.dip2px(itemView.getContext(), 160)));
        }
    }
}
