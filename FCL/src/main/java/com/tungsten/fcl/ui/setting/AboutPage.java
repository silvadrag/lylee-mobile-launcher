package com.tungsten.fcl.ui.setting;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tungsten.fcl.R;
import com.tungsten.fcl.util.AndroidUtils;
import com.tungsten.fclcore.task.Task;
import com.tungsten.fcllibrary.component.ui.FCLPage;

/**
 * 关于页：RecyclerView 行级复用，说明（about_desc）置顶，下方为链接行。
 */
public class AboutPage extends FCLPage {

    private static final int TYPE_DESC = 0;
    private static final int TYPE_LINK = 1;

    // Bỏ hẳn about_launcher (từng trỏ tới fcl-team.github.io — sai hoàn toàn
    // sau khi đổi thương hiệu, label ghi "Lylee Launcher" nhưng bấm vào lại ra
    // trang FCL), community_discord/community_qq (kênh cộng đồng CỦA FCL-Team,
    // không phải Lylee — không có Discord/QQ thật của Lylee để thay vào, thà
    // KHÔNG có nút còn hơn trỏ sai), về_sponsor/afdian (trang donate CÁ NHÂN
    // của dev FCL — để nguyên sẽ khiến người chơi tưởng đang ủng hộ Lylee mà
    // thực ra chuyển tiền cho người khác, rất dễ gây hiểu lầm nghiêm trọng).
    // Giữ lại about_developer (credit đúng nghĩa vụ GPL-v3, không được xóa) và
    // about_source (đổi link sang repo Lylee thật, xem openLink()).
    private static final int[] TITLES = {
            R.string.about_desc,
            R.string.about_developer,
            R.string.about_source
    };

    public AboutPage(Context context, int id, int resId) {
        super(context, id, resId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        RecyclerView recyclerView = findViewById(R.id.about_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(new AboutAdapter());
    }

    @Override
    public Task<?> refresh(Object... param) {
        return null;
    }

    private class AboutAdapter extends RecyclerView.Adapter<AboutAdapter.Holder> {

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? TYPE_DESC : TYPE_LINK;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int layout = viewType == TYPE_DESC ? R.layout.item_about_desc : R.layout.item_about;
            return new Holder(LayoutInflater.from(parent.getContext()).inflate(layout, parent, false), viewType);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return TITLES.length;
        }

        class Holder extends RecyclerView.ViewHolder {
            private final TextView title;
            private final boolean link;

            Holder(@NonNull View itemView, int viewType) {
                super(itemView);
                title = itemView.findViewById(R.id.title);
                link = viewType == TYPE_LINK;
                if (link) {
                    itemView.setOnClickListener(v -> openLink(getBindingAdapterPosition()));
                }
            }

            void bind(int position) {
                title.setText(TITLES[position]);
            }
        }
    }

    private void openLink(int position) {
        switch (position) {
            case 1:
                // Credit đúng nghĩa vụ GPL-v3 — trỏ về team gốc đã tạo ra
                // Fold Craft Launcher, KHÔNG được đổi/xóa.
                AndroidUtils.openLink(getContext(), "https://github.com/FCL-Team");
                break;
            case 2:
                // Mã nguồn bản Lylee (đã sửa đổi) — repo private lúc mới tạo,
                // cần bật public trên GitHub thì link này người chơi mới vào
                // xem được (xem docs/PLAN.md).
                AndroidUtils.openLink(getContext(), "https://github.com/silvadrag/lylee-mobile-launcher");
                break;
        }
    }
}
