package com.tungsten.fcllibrary.component.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.tungsten.fclcore.task.Task;
import com.tungsten.fcllibrary.component.FCLActivity;

/**
 * Lớp cơ sở trang: chỉ chứa contentView và logic nghiệp vụ, không có phương thức vòng đời.
 *
 * Trang do ViewPager2 / lớp phủ gắn vào, tạo/hủy theo (không giữ trạng thái),
 * hàm khởi tạo là hoàn tất toàn bộ việc khởi tạo (setContentView + onCreate).
 */
public abstract class FCLPage {

    /** Id chung cho trang tạm (trang tạm không tham gia bảng đăng ký trang, chỉ dùng làm tham số khởi tạo) */
    public static final int PAGE_ID_TEMP = -10000;

    private final Context context;
    private final FCLActivity activity;
    private final int id;

    private View contentView;

    public FCLPage(Context context, int id, @LayoutRes int resId) {
        this.context = context;
        this.activity = (FCLActivity) context;
        this.id = id;
        setContentView(resId);
        onCreate();
    }

    public Context getContext() {
        return context;
    }

    public FCLActivity getActivity() {
        return activity;
    }

    public int getId() {
        return id;
    }

    public void setContentView(@LayoutRes int id) {
        contentView = LayoutInflater.from(context).inflate(id, null);
    }

    public View getContentView() {
        return contentView;
    }

    @NonNull
    public final <T extends View> T findViewById(int id) {
        return contentView.findViewById(id);
    }

    public boolean isShowing() {
        return contentView != null && contentView.isShown();
    }

    /** Khởi tạo khi trang được tạo (findViewById, gắn listener,...), thực thi trong hàm khởi tạo */
    public void onCreate() {

    }

    public abstract Task<?> refresh(Object... param);
}
