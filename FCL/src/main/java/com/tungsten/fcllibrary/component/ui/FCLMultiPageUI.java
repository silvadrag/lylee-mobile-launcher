package com.tungsten.fcllibrary.component.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.tungsten.fclcore.task.Task;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Lớp cơ sở UI nhiều trang: ViewPager2 bên trong chứa các trang thường (bấm tab
 * trượt mượt để chuyển), lớp phủ chứa trang tạm (ngăn xếp điều hướng).
 * <p>
 * Trang không có vòng đời riêng, tạo/hủy theo ViewPager2 (không giữ trạng thái):
 * lớp con hiện thực {@link #getPageCount()} / {@link #createPage(int)} làm
 * factory trang, gọi {@link #setupPages(ViewGroup, TabLayout)} trong onCreate
 * để gắn container trang.
 */
public abstract class FCLMultiPageUI extends FCLCommonUI {

    /**
     * Thời lượng hoạt ảnh chuyển trang tạm (mili giây)
     */
    private static final int TEMP_PAGE_ANIM_DURATION = 200;

    /**
     * Vị trí trang lần onPageSelected trước đó, dùng để lọc bỏ việc ViewPager2
     * dispatch lại trang hiện tại (VD khi bàn phím ảo bật lên làm đổi layout)
     */
    private int lastSelectedPosition = -1;

    /**
     * Bảng đăng ký vị trí trang → instance trang, xóa khỏi bảng khi trang bị
     * thu hồi (không giữ trạng thái)
     */
    private final ArrayList<FCLPage> pageRegistry = new ArrayList<>();

    private final ArrayList<FCLPage> tempPageStack = new ArrayList<>();

    private ViewPager2 pagePager;
    private FrameLayout overlay;

    public FCLMultiPageUI(Context context, @LayoutRes int id) {
        super(context, id);
    }

    /**
     * Lớp con gọi trong onCreate: gắn ViewPager2 bên trong và lớp phủ trang tạm
     * vào container; nếu có tabLayout thì liên kết qua TabLayoutMediator (bấm
     * tab trượt mượt để chuyển).
     */
    protected void setupPages(ViewGroup container, TabLayout tabLayout) {
        pagePager = new ViewPager2(getContext());
        pagePager.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        pagePager.setOffscreenPageLimit(ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT);
        // Tắt cử chỉ vuốt: nội dung cuộn trong trang xung đột với vuốt chuyển trang,
        // chỉ chuyển qua tab / showPage
        pagePager.setUserInputEnabled(false);
        pagePager.setAdapter(new PageAdapter());
        container.addView(pagePager);

        overlay = new FrameLayout(getContext());
        overlay.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        overlay.setVisibility(View.GONE);
        container.addView(overlay);

        if (tabLayout != null) {
            // tab được định nghĩa tĩnh trong layout XML (TabItem), ở đây chỉ tiếp
            // quản sự kiện bấm: chuyển tức thì (không tạo trang trung gian),
            // hoạt ảnh chuyển cảnh xử lý trong onPageSelected
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    pagePager.setCurrentItem(tab.getPosition(), false);
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {

                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {

                }
            });
        }

        // Xóa hết trang tạm khi chuyển trang (trang tạm thuộc ngữ cảnh trang hiện tại)
        pagePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                dismissAllTempPages();
                // đồng bộ tab đang sáng
                if (tabLayout != null) {
                    TabLayout.Tab tab = tabLayout.getTabAt(position);
                    if (tab != null && tabLayout.getSelectedTabPosition() != position) {
                        tab.select();
                    }
                }
                // Hoạt ảnh chuyển trang: không tạo trang trung gian (nhảy tức thì),
                // chỉ làm mờ dần + trượt lên cho trang đích.
                // Chạy đồng bộ (không post): lúc onPageSelected trang đã gắn nhưng
                // chưa vẽ, đặt trong suốt trước khung hình đầu nên không bị chớp
                // hiện-rồi-mất.
                // Chỉ phát khi vị trí trang thực sự đổi: ViewPager2 dispatch lại
                // trang hiện tại khi layout đổi (VD bàn phím ảo bật, nội dung trang
                // làm mới) — lúc đó không phát hoạt ảnh để tránh chớp hình
                if (position != lastSelectedPosition) {
                    FCLPage page = getPage(position);
                    if (page != null) {
                        View contentView = page.getContentView();
                        contentView.animate().cancel();
                        contentView.setAlpha(0f);
                        contentView.setTranslationY(contentView.getResources().getDisplayMetrics().density * 30f);
                        contentView.animate().alpha(1f).translationY(0f).setDuration(250).start();
                    }
                }
                lastSelectedPosition = position;
            }
        });
    }

    /**
     * Số lượng trang (khớp số tab hoặc số trang ViewPager2)
     */
    public abstract int getPageCount();

    /**
     * Tạo trang theo vị trí (id trang do hằng số trang của lớp con quyết định)
     */
    public abstract FCLPage createPage(int position);

    /**
     * Tiêu đề tab, UI không có tab thì trả về null
     */
    public String[] getTabTitles() {
        return null;
    }

    /**
     * Lấy trang tại vị trí, chưa có thì tạo mới (tạo trang là khởi tạo xong luôn)
     */
    public FCLPage getPage(int position) {
        while (pageRegistry.size() <= position) {
            pageRegistry.add(null);
        }
        if (pageRegistry.get(position) == null) {
            FCLPage page = createPage(position);
            pageRegistry.set(position, page);
            onPageCreated(page);
        }
        return pageRegistry.get(position);
    }

    /**
     * Callback sau khi trang được tạo, lớp con có thể truyền dữ liệu ngữ cảnh
     * (VD version) tại đây
     */
    protected void onPageCreated(FCLPage page) {

    }

    /**
     * Duyệt qua các trang đã tạo (không kích hoạt tạo mới)
     */
    public void forEachCreatedPage(Consumer<FCLPage> action) {
        for (FCLPage page : pageRegistry) {
            if (page != null) {
                action.accept(page);
            }
        }
    }

    /**
     * Chuyển tới trang tại vị trí chỉ định (thay cho switchPage cũ)
     */
    public void showPage(int position) {
        if (pagePager != null) {
            pagePager.setCurrentItem(position, false);
        }
    }

    public int getCurrentPagePosition() {
        return pagePager == null ? 0 : pagePager.getCurrentItem();
    }

    public boolean canReturn() {
        return !tempPageStack.isEmpty();
    }

    /**
     * Hiện trang tạm trên lớp phủ và đẩy vào ngăn xếp điều hướng (ẩn trang dưới,
     * trang tạm chiếm toàn bộ hiển thị)
     */
    public void showTempPage(FCLPage page) {
        if (overlay == null) return;
        // Ẩn trang tạm đang ở đỉnh ngăn xếp và trang bên trong, tránh lộ nội dung
        // qua nền trong suốt (giữ đúng cơ chế PageManager cũ: ẩn trang hiện tại
        // và trang tạm đỉnh ngăn xếp)
        if (!tempPageStack.isEmpty()) {
            tempPageStack.get(tempPageStack.size() - 1).getContentView().setVisibility(View.GONE);
        }
        if (pagePager != null) {
            pagePager.setVisibility(View.GONE);
        }
        // Trang tạm mới hiện mờ dần vào
        View view = page.getContentView();
        view.setAlpha(0f);
        overlay.setVisibility(View.VISIBLE);
        overlay.addView(view, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        view.animate().alpha(1f).setDuration(TEMP_PAGE_ANIM_DURATION).start();
        tempPageStack.add(page);
    }

    /**
     * Đóng trang tạm ở đỉnh ngăn xếp (mờ dần rồi gỡ, khôi phục trang dưới)
     */
    public void dismissCurrentTempPage() {
        if (tempPageStack.isEmpty()) return;
        FCLPage page = tempPageStack.remove(tempPageStack.size() - 1);
        View view = page.getContentView();
        view.animate().alpha(0f).setDuration(TEMP_PAGE_ANIM_DURATION).withEndAction(() -> {
            overlay.removeView(view);
            if (!tempPageStack.isEmpty()) {
                // Khôi phục hiển thị trang tạm bên dưới
                tempPageStack.get(tempPageStack.size() - 1).getContentView().setVisibility(View.VISIBLE);
            }
            if (tempPageStack.isEmpty()) {
                overlay.setVisibility(View.GONE);
                // Đóng hết trang tạm thì khôi phục hiển thị trang bên trong
                if (pagePager != null) {
                    pagePager.setVisibility(View.VISIBLE);
                }
            }
        }).start();
    }

    /**
     * Đóng hết mọi trang tạm
     */
    public void dismissAllTempPages() {
        while (!tempPageStack.isEmpty()) {
            dismissCurrentTempPage();
        }
    }

    @Override
    public boolean isShowing() {
        return super.isShowing();
    }

    @Override
    public abstract Task<?> refresh(Object... param);

    @Override
    public void onBackPressed() {
        if (canReturn()) {
            dismissCurrentTempPage();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Adapter ViewPager2 bên trong: trang tạo/hủy theo vòng đời (không giữ trạng thái)
     */
    private class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

        private class Holder extends RecyclerView.ViewHolder {
            final FrameLayout container;
            int boundPosition = -1;

            Holder(FrameLayout container) {
                super(container);
                this.container = container;
            }
        }

        @Override
        public int getItemCount() {
            return getPageCount();
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            // ViewPager2 yêu cầu View con trực tiếp của trang phải MATCH_PARENT
            FrameLayout container = new FrameLayout(parent.getContext());
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            return new Holder(container);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            holder.boundPosition = position;
            holder.container.removeAllViews();
            View contentView = getPage(position).getContentView();
            // Phòng ngừa: GapWorker prefetch có thể gắn cùng 1 instance trang vào
            // container khác (bind prefetch và bind chính thức tranh chấp nhau) —
            // gỡ parent cũ trước để tránh addView ném lỗi "child already has a parent"
            if (contentView.getParent() != null) {
                ((ViewGroup) contentView.getParent()).removeView(contentView);
            }
            holder.container.addView(contentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        @Override
        public void onViewRecycled(Holder holder) {
            // Trang bị thu hồi là hủy luôn (không giữ trạng thái), lần sau vào lại sẽ tạo mới hoàn toàn
            if (holder.boundPosition >= 0 && holder.boundPosition < pageRegistry.size()) {
                pageRegistry.set(holder.boundPosition, null);
            }
        }
    }
}
