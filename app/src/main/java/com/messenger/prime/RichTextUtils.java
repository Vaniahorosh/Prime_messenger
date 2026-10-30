package com.messenger.prime;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.text.Html;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class RichTextUtils {
    public static class GlideImageGetter implements Html.ImageGetter {
        private final TextView textView;

        public GlideImageGetter(TextView textView) {
            this.textView = textView;
        }

        @Override
        public Drawable getDrawable(String source) {
            LevelListDrawable d = new LevelListDrawable();
            Drawable empty = ContextCompat.getDrawable(textView.getContext(), R.drawable.ic_format_image);
            if (empty != null) {
                empty.setTint(ContextCompat.getColor(textView.getContext(), R.color.prime_text_secondary));
                d.addLevel(0, 0, empty);
                d.setBounds(0, 0, empty.getIntrinsicWidth() * 2, empty.getIntrinsicHeight() * 2);
            }

            Glide.with(textView.getContext())
                .asDrawable()
                .load(source)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        int width = resource.getIntrinsicWidth();
                        int height = resource.getIntrinsicHeight();
                        int maxWidth = textView.getWidth() - textView.getPaddingLeft() - textView.getPaddingRight();
                        
                        if (maxWidth > 0) {
                            if (width > maxWidth) {
                                height = (int) (height * ((float) maxWidth / width));
                                width = maxWidth;
                            } else if (width < maxWidth / 2) { // Optional: scale up slightly small images
                                float scale = (maxWidth / 2f) / width;
                                width = (int) (width * scale);
                                height = (int) (height * scale);
                            }
                        }
                        
                        d.addLevel(1, 1, resource);
                        d.setBounds(0, 0, width, height);
                        d.setLevel(1);
                        textView.invalidate();
                        textView.setText(textView.getText());
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
            return d;
        }
    }
}
