package com.tungsten.fcllibrary.component.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import com.tungsten.fclcore.fakefx.beans.property.BooleanProperty;
import com.tungsten.fclcore.fakefx.beans.property.BooleanPropertyBase;
import com.tungsten.fclcore.fakefx.beans.property.IntegerProperty;
import com.tungsten.fclcore.fakefx.beans.property.IntegerPropertyBase;
import com.tungsten.fclcore.fakefx.beans.property.ObjectProperty;
import com.tungsten.fclcore.fakefx.beans.property.ObjectPropertyBase;
import com.tungsten.fclcore.task.Schedulers;
import com.tungsten.fcl.R;
import com.tungsten.fcllibrary.component.theme.ThemeEngine;
import com.tungsten.fcllibrary.util.ConvertUtils;

public class FCLImageButton extends AppCompatImageButton {

    private ObjectProperty<Drawable> image;
    private boolean autoTint;
    private boolean noPadding;
    private boolean useThemeColor;
    private BooleanProperty visibilityProperty;
    private BooleanProperty disableProperty;
    // Nền khai báo trong XML (android:background) bị mất nếu không giữ lại: refreshStyle()
    // luôn ghi đè background bằng 1 RippleDrawable trắng tay (không lớp nội dung), nên phải
    // chụp lại nền gốc 1 LẦN DUY NHẤT trước khi registerEvent() chạy refreshStyle() lần đầu,
    // rồi dùng lại nó làm lớp content của RippleDrawable ở mỗi lần refresh theme sau này.
    private Drawable originalBackground;

    public void refreshStyle() {
        int[][] state = {
                {

                }
        };
        int[] colorSrc = {
                ThemeEngine.getInstance().getTheme().getAutoTint()
        };
        int[] colorRipple = {
                ThemeEngine.getInstance().getTheme().getLtColor()
        };
        if (autoTint) {
            setImageTintList(new ColorStateList(state, colorSrc));
        }
        if (useThemeColor && getDrawable() != null) {
            getDrawable().setTint(ThemeEngine.getInstance().getTheme().getColor2());
        }
        RippleDrawable drawable = new RippleDrawable(new ColorStateList(state, colorRipple), originalBackground, null);
        drawable.setRadius(ConvertUtils.dip2px(getContext(), noPadding ? 12 : 20));
        setBackgroundDrawable(drawable);
    }

    private void init(@Nullable AttributeSet attrs) {
        // Chỉ giữ lại nền nếu XML khai báo THẬT SỰ có android:background — nếu không kiểm tra
        // attrs mà cứ lấy getBackground(), sẽ vô tình giữ luôn nền mặc định của platform
        // ImageButton (1 ô vuông xám) cho những nút không hề khai nền, trông như bug mới.
        // Phải chụp trước khi registerEvent() bên dưới chạy refreshStyle() lần đầu, nếu không
        // getBackground() ở đây sẽ trả về chính RippleDrawable vừa tạo ở lần refresh trước.
        boolean hasXmlBackground = attrs != null &&
                attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "background") != null;
        originalBackground = hasXmlBackground ? getBackground() : null;
        if (!noPadding) {
            setPadding(
                    ConvertUtils.dip2px(getContext(), 8f),
                    ConvertUtils.dip2px(getContext(), 8f),
                    ConvertUtils.dip2px(getContext(), 8f),
                    ConvertUtils.dip2px(getContext(), 8f)
            );
        } else {
            setPadding(0, 0, 0, 0);
        }
        setScaleType(ScaleType.FIT_XY);
        // Làm mới style khi theme đổi (registerEvent chạy ngay 1 lần, thay cho fakefx bind cũ)
        ThemeEngine.getInstance().registerEvent(this, this::refreshStyle);
    }

    public FCLImageButton(@NonNull Context context) {
        super(context);
        init(null);
    }

    public FCLImageButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FCLImageButton);
        autoTint = typedArray.getBoolean(R.styleable.FCLImageButton_auto_tint, false);
        noPadding = typedArray.getBoolean(R.styleable.FCLImageButton_no_padding, false);
        useThemeColor = typedArray.getBoolean(R.styleable.FCLImageButton_use_theme_color, false);
        typedArray.recycle();
        init(attrs);
    }

    public FCLImageButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FCLImageButton);
        autoTint = typedArray.getBoolean(R.styleable.FCLImageButton_auto_tint, false);
        noPadding = typedArray.getBoolean(R.styleable.FCLImageButton_no_padding, false);
        useThemeColor = typedArray.getBoolean(R.styleable.FCLImageButton_use_theme_color, false);
        typedArray.recycle();
        init(attrs);
    }

    public void setAutoTint(boolean autoTint) {
        this.autoTint = autoTint;
        refreshStyle();
    }

    public boolean isAutoTint() {
        return autoTint;
    }

    public void setNoPadding(boolean noPadding) {
        this.noPadding = noPadding;
        refreshStyle();
    }

    public boolean isNoPadding() {
        return noPadding;
    }

    public void setUseThemeColor(boolean useThemeColor) {
        this.useThemeColor = useThemeColor;
        refreshStyle();
    }

    public boolean isUseThemeColor() {
        return useThemeColor;
    }

    public final void setImage(Drawable drawable) {
        imageProperty().set(drawable);
    }

    public final Drawable getImage() {
        return image == null ? null : image.get();
    }

    public final ObjectProperty<Drawable> imageProperty() {
        if (image == null) {
            image = new ObjectPropertyBase<Drawable>() {

                public void invalidated() {
                    Schedulers.androidUIThread().execute(() -> {
                        Drawable drawable = get();
                        setImageDrawable(drawable);
                    });
                }

                public Object getBean() {
                    return this;
                }

                public String getName() {
                    return "image";
                }
            };
        }

        return this.image;
    }

    public final void setVisibilityValue(boolean visibility) {
        visibilityProperty().set(visibility);
    }

    public final boolean getVisibilityValue() {
        return visibilityProperty == null || visibilityProperty.get();
    }

    public final BooleanProperty visibilityProperty() {
        if (visibilityProperty == null) {
            visibilityProperty = new BooleanPropertyBase() {

                public void invalidated() {
                    Schedulers.androidUIThread().execute(() -> {
                        boolean visible = get();
                        setVisibility(visible ? VISIBLE : GONE);
                    });
                }

                public Object getBean() {
                    return this;
                }

                public String getName() {
                    return "visibility";
                }
            };
        }

        return visibilityProperty;
    }

    public final void setDisableValue(boolean disableValue) {
        disableProperty().set(disableValue);
    }

    public final boolean getDisableValue() {
        return disableProperty == null || disableProperty.get();
    }

    public final BooleanProperty disableProperty() {
        if (disableProperty == null) {
            disableProperty = new BooleanPropertyBase() {

                public void invalidated() {
                    Schedulers.androidUIThread().execute(() -> {
                        boolean disable = get();
                        setEnabled(!disable);
                    });
                }

                public Object getBean() {
                    return this;
                }

                public String getName() {
                    return "disable";
                }
            };
        }

        return disableProperty;
    }
}
