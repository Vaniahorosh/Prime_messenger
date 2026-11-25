package com.messenger.prime;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.Base64;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.BackEventCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.messenger.prime.shapes.M3EDrawable;
import com.messenger.prime.shapes.M3EShapes;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import eightbitlab.com.blurview.BlurView;

public class RichEditorActivity extends AppCompatActivity {
    public static final String EXTRA_CONTENT = "extra_content";
    public static final String RESULT_CONTENT = "result_content";
    public static final String EXTRA_SEND_IMMEDIATELY = "EXTRA_SEND_IMMEDIATELY";
    public static final String EXTRA_ORIGIN_RECT = "extra_origin_rect";
    public static final String EXTRA_ORIGIN_RADIUS = "extra_origin_radius";

    private static final PathInterpolator M3E_OPEN_INTERPOLATOR = new PathInterpolator(0.05f, 0.7f, 0.1f, 1.0f);
    private static final PathInterpolator M3E_CLOSE_INTERPOLATOR = new PathInterpolator(0.32f, 0.0f, 0.18f, 1.0f);

    private View vBackdropScrim;
    private ViewGroup editorSheetContainer;
    private View vSheetDragHandle;
    private View layoutBottomDock;
    private WebView webViewRich;
    private BlurView blurTopHeader;
    private BlurView blurBottomIsland;
    private View btnDone;
    private ImageButton btnToggleToolbar;
    private TextView tvEditorStats;

    private ImageButton btnBold;
    private ImageButton btnItalic;
    private ImageButton btnUnderline;
    private ImageButton btnStrike;
    private ImageButton btnHeading;
    private ImageButton btnColor;
    private ImageButton btnList;
    private ImageButton btnQuote;
    private ImageButton btnCode;
    private ImageButton btnAlign;
    private ImageButton btnLink;
    private ImageButton btnTable;
    private ImageButton btnInsertImage;
    private ImageButton btnClearFormat;
    private ImageButton btnRedo;

    private String initialRawContent = "";
    private boolean isInitialRich = false;
    private boolean isInitialContentLoaded = false;
    private boolean isClosing = false;
    private boolean isToolbarVisible = true;
    private int accentColor;
    private int primaryTextColor;
    private int secondaryTextColor;
    @Nullable
    private int[] originScreenRect = null;
    private float originCornerRadiusPx = 0f;
    @Nullable
    private ValueAnimator activeSheetAnimator = null;

    // Live formatting state synced from WebView
    private boolean stateBold = false;
    private boolean stateItalic = false;
    private boolean stateUnderline = false;
    private boolean stateStrike = false;
    private boolean stateUl = false;
    private boolean stateOl = false;
    private boolean stateQuote = false;
    private boolean stateCode = false;
    private boolean statePre = false;
    private String stateBlock = "";
    private String stateAlign = "left";
    private String stateSelectedText = "";

    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> mediaPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && !isFinishing() && !isDestroyed()) {
                    showImageSizeDialog(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ColorAccentManager.applyAccentToActivity(this);
        super.onCreate(savedInstanceState);
        disableWindowTransitions();

        setContentView(R.layout.fragment_rich_editor);

        boolean isDark = isDarkTheme(this);
        UIExtensionsKt.setupEdgeToEdge(this, !isDark);

        accentColor = ColorAccentManager.getCurrentAccentColor(this);
        primaryTextColor = ContextCompat.getColor(this, R.color.prime_text_primary);
        secondaryTextColor = ContextCompat.getColor(this, R.color.prime_text_secondary);

        vBackdropScrim = findViewById(R.id.vBackdropScrim);
        editorSheetContainer = findViewById(R.id.editorSheetContainer);
        vSheetDragHandle = findViewById(R.id.vSheetDragHandle);
        layoutBottomDock = findViewById(R.id.layoutBottomDock);
        webViewRich = findViewById(R.id.webViewRich);
        blurTopHeader = findViewById(R.id.blurTopHeader);
        blurBottomIsland = findViewById(R.id.layoutFloatingBottomIsland);
        btnDone = findViewById(R.id.btnDone);
        btnToggleToolbar = findViewById(R.id.btnToggleToolbar);
        tvEditorStats = findViewById(R.id.tvEditorStats);

        int[] passedRect = getIntent().getIntArrayExtra(EXTRA_ORIGIN_RECT);
        if (passedRect != null && passedRect.length == 4
                && passedRect[2] > passedRect[0] && passedRect[3] > passedRect[1]) {
            originScreenRect = passedRect;
            float defaultRadius = 24f * getResources().getDisplayMetrics().density;
            originCornerRadiusPx = getIntent().getFloatExtra(EXTRA_ORIGIN_RADIUS, defaultRadius);
        }

        if (vBackdropScrim != null) {
            vBackdropScrim.setBackgroundColor(isDark ? 0x7A000000 : 0x4D0F172A);
            vBackdropScrim.setOnClickListener(v -> exitDirectly());
        }

        if (editorSheetContainer != null) {
            editorSheetContainer.setClipToOutline(true);
        }

        setupInsets();
        setupBlurViews();
        setupExpressiveButtons();
        bindFormattingButtons();
        setupHeaderDragToDismiss();
        setupPredictiveBack();

        String init = getIntent().getStringExtra(EXTRA_CONTENT);
        if (init != null) {
            if (init.startsWith("[RICH]")) {
                isInitialRich = true;
                initialRawContent = init.substring(6);
            } else {
                isInitialRich = false;
                initialRawContent = init;
            }
        }

        setupWebView();
        playOpenAnimation();
    }

    private void disableWindowTransitions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0);
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0);
        } else {
            @SuppressWarnings("deprecation")
            Runnable clearAnim = () -> overridePendingTransition(0, 0);
            clearAnim.run();
        }
    }

    private void setupInsets() {
        View root = findViewById(R.id.layoutRichRoot);
        if (root == null || editorSheetContainer == null) return;

        final float density = getResources().getDisplayMetrics().density;
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets cutout = insets.getInsets(WindowInsetsCompat.Type.displayCutout());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());

            int topMargin = Math.max(systemBars.top, cutout.top) + (int) (8f * density);
            ViewGroup.LayoutParams rawLp = editorSheetContainer.getLayoutParams();
            if (rawLp instanceof FrameLayout.LayoutParams) {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) rawLp;
                if (lp.topMargin != topMargin || lp.leftMargin != cutout.left || lp.rightMargin != cutout.right) {
                    lp.topMargin = topMargin;
                    lp.leftMargin = cutout.left;
                    lp.rightMargin = cutout.right;
                    editorSheetContainer.setLayoutParams(lp);
                }
            }

            int bottomInset = Math.max(systemBars.bottom, ime.bottom);
            if (editorSheetContainer.getPaddingBottom() != bottomInset) {
                editorSheetContainer.setPadding(0, 0, 0, bottomInset);
            }

            adjustWebViewBottomPadding();
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void adjustWebViewBottomPadding() {
        if (webViewRich == null || !isInitialContentLoaded) return;
        int padBottomDp = isToolbarVisible ? 96 : 80;
        webViewRich.evaluateJavascript("if (window.primeSetBottomPadding) window.primeSetBottomPadding(" + padBottomDp + ");", null);
    }

    private void setupBlurViews() {
        if (editorSheetContainer == null) return;
        Drawable sheetBg = editorSheetContainer.getBackground();
        if (sheetBg == null) {
            sheetBg = new ColorDrawable(ContextCompat.getColor(this, R.color.prime_base));
        }
        int overlayColor = BlurViewKt.getDefaultBlurOverlayColor(this);

        if (blurTopHeader != null) {
            blurTopHeader.setClipToOutline(true);
            BlurViewKt.setupBlur(blurTopHeader, editorSheetContainer, 18f, overlayColor, sheetBg);
        }
        if (blurBottomIsland != null) {
            blurBottomIsland.setClipToOutline(true);
            BlurViewKt.setupBlur(blurBottomIsland, editorSheetContainer, 18f, overlayColor, sheetBg);
        }
    }

    private void setupExpressiveButtons() {
        if (btnDone != null) {
            float density = getResources().getDisplayMetrics().density;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                M3EDrawable m3eDrawable = new M3EDrawable(M3EShapes.INSTANCE.getVerySunny(), accentColor);
                btnDone.setBackground(m3eDrawable);
                btnDone.setOutlineProvider(null);
                btnDone.setClipToOutline(false);
                btnDone.setElevation(0f);
            } else {
                GradientDrawable pill = new GradientDrawable();
                pill.setShape(GradientDrawable.OVAL);
                pill.setColor(accentColor);
                btnDone.setBackground(pill);
                btnDone.setElevation(6f * density);
            }
            UIExtensionsKt.addM3EBounceEffect(btnDone, 0.92f, null);
        }

        if (btnToggleToolbar != null) {
            btnToggleToolbar.setImageTintList(ColorStateList.valueOf(accentColor));
        }
    }

    private void bindFormattingButtons() {
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);
        btnUnderline = findViewById(R.id.btnUnderline);
        btnStrike = findViewById(R.id.btnStrike);
        btnHeading = findViewById(R.id.btnHeading);
        btnColor = findViewById(R.id.btnColor);
        btnList = findViewById(R.id.btnList);
        btnQuote = findViewById(R.id.btnQuote);
        btnCode = findViewById(R.id.btnCode);
        btnAlign = findViewById(R.id.btnAlign);
        btnLink = findViewById(R.id.btnLink);
        btnTable = findViewById(R.id.btnTable);
        btnInsertImage = findViewById(R.id.btnInsertImage);
        btnClearFormat = findViewById(R.id.btnClearFormat);
        ImageButton btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);

        View[] touchEffectViews = new View[]{
                findViewById(R.id.btnCancel), btnUndo, btnRedo, btnToggleToolbar,
                btnBold, btnItalic, btnUnderline, btnStrike, btnHeading, btnColor,
                btnList, btnQuote, btnCode, btnAlign, btnLink, btnTable, btnInsertImage, btnClearFormat
        };
        for (View v : touchEffectViews) {
            if (v != null) {
                UIExtensionsKt.addBounceTouchEffect(v);
            }
        }

        if (btnBold != null) btnBold.setOnClickListener(v -> runToolbarExec("bold"));
        if (btnItalic != null) btnItalic.setOnClickListener(v -> runToolbarExec("italic"));
        if (btnUnderline != null) btnUnderline.setOnClickListener(v -> runToolbarExec("underline"));
        if (btnStrike != null) btnStrike.setOnClickListener(v -> runToolbarExec("strikeThrough"));

        if (btnHeading != null) btnHeading.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); showHeadingMenu(); });
        if (btnColor != null) btnColor.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); showColorMenu(); });
        if (btnList != null) btnList.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); showListMenu(); });
        if (btnQuote != null) btnQuote.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            runEditorJs("window.primeToggleBlockquote();");
        });
        if (btnCode != null) btnCode.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); showCodeMenu(); });
        if (btnAlign != null) btnAlign.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); showAlignMenu(); });
        if (btnLink != null) btnLink.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); showInsertLinkDialog(); });
        if (btnTable != null) btnTable.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); showInsertTableDialog(); });
        if (btnInsertImage != null) btnInsertImage.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            mediaPickerLauncher.launch("image/*");
        });
        if (btnClearFormat != null) btnClearFormat.setOnClickListener(v -> {
            v.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            runEditorJs("window.primeClearFormatting();");
        });

        if (btnUndo != null) btnUndo.setOnClickListener(v -> runToolbarExec("undo"));
        if (btnRedo != null) btnRedo.setOnClickListener(v -> runToolbarExec("redo"));

        if (btnToggleToolbar != null && blurBottomIsland != null) {
            btnToggleToolbar.setOnClickListener(v -> toggleToolbarVisibility());
        }

        View btnCancel = findViewById(R.id.btnCancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                exitDirectly();
            });
        }

        if (btnDone != null) {
            btnDone.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                finishAndSend();
            });
            btnDone.setOnLongClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                PrimePopupMenu.create(this, btnDone)
                        .addItem("Отправить сразу", R.drawable.ic_send, true, () -> {
                            finishAndSend();
                            return kotlin.Unit.INSTANCE;
                        })
                        .addItem("Вставить в поле ввода", R.drawable.ic_edit, false, () -> {
                            exitDirectly();
                            return kotlin.Unit.INSTANCE;
                        })
                        .show();
                return true;
            });
        }
    }

    private void toggleToolbarVisibility() {
        if (blurBottomIsland == null || btnToggleToolbar == null) return;
        btnToggleToolbar.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        isToolbarVisible = !isToolbarVisible;

        float density = getResources().getDisplayMetrics().density;
        boolean noAnim = PrimeTransitions.areAnimationsDisabled(this);

        if (isToolbarVisible) {
            blurBottomIsland.setVisibility(View.VISIBLE);
            if (noAnim) {
                blurBottomIsland.setAlpha(1f);
                blurBottomIsland.setTranslationY(0f);
                blurBottomIsland.setScaleX(1f);
                blurBottomIsland.setScaleY(1f);
                btnToggleToolbar.setRotation(0f);
            } else {
                blurBottomIsland.setAlpha(0f);
                blurBottomIsland.setTranslationY(14f * density);
                blurBottomIsland.setScaleX(0.95f);
                blurBottomIsland.setScaleY(0.95f);
                blurBottomIsland.animate().cancel();
                blurBottomIsland.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .setInterpolator(M3E_OPEN_INTERPOLATOR)
                        .start();
                btnToggleToolbar.animate().rotation(0f).setDuration(200).setInterpolator(M3E_OPEN_INTERPOLATOR).start();
            }
        } else {
            if (noAnim) {
                blurBottomIsland.setVisibility(View.GONE);
                btnToggleToolbar.setRotation(180f);
            } else {
                blurBottomIsland.animate().cancel();
                blurBottomIsland.animate()
                        .alpha(0f)
                        .translationY(14f * density)
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(160)
                        .setInterpolator(M3E_CLOSE_INTERPOLATOR)
                        .withEndAction(() -> blurBottomIsland.setVisibility(View.GONE))
                        .start();
                btnToggleToolbar.animate().rotation(180f).setDuration(180).setInterpolator(M3E_CLOSE_INTERPOLATOR).start();
            }
        }
        adjustWebViewBottomPadding();
    }

    private void runToolbarExec(String command) {
        if (btnBold != null) btnBold.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        runEditorJs("window.primeExec(" + JSONObject.quote(command) + ", null);");
    }

    private void runEditorJs(String jsBody) {
        if (webViewRich == null || isClosing) return;
        webViewRich.evaluateJavascript(jsBody, null);
    }

    private void playOpenAnimation() {
        if (editorSheetContainer == null || vBackdropScrim == null) return;
        if (PrimeTransitions.areAnimationsDisabled(this)) {
            vBackdropScrim.setAlpha(1f);
            editorSheetContainer.setAlpha(1f);
            editorSheetContainer.setTranslationY(0f);
            editorSheetContainer.setScaleX(1f);
            editorSheetContainer.setScaleY(1f);
            return;
        }

        final float density = getResources().getDisplayMetrics().density;
        vBackdropScrim.setAlpha(0f);

        if (originScreenRect != null) {
            editorSheetContainer.setAlpha(0f);
            if (webViewRich != null) webViewRich.setAlpha(0f);
            if (vSheetDragHandle != null) vSheetDragHandle.setAlpha(0f);
            if (blurTopHeader != null) blurTopHeader.setAlpha(0f);
            if (layoutBottomDock != null) layoutBottomDock.setAlpha(0f);

            editorSheetContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    if (editorSheetContainer.getViewTreeObserver().isAlive()) {
                        editorSheetContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                    }
                    if (isFinishing() || isDestroyed() || isClosing) return true;
                    startContainerExpandMorph(density);
                    return true;
                }
            });
            return;
        }

        editorSheetContainer.setAlpha(0f);
        editorSheetContainer.setTranslationY(48f * density);
        editorSheetContainer.setScaleX(0.96f);
        editorSheetContainer.setScaleY(0.96f);

        editorSheetContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (editorSheetContainer.getViewTreeObserver().isAlive()) {
                    editorSheetContainer.getViewTreeObserver().removeOnPreDrawListener(this);
                }
                if (isFinishing() || isDestroyed() || isClosing) return true;

                vBackdropScrim.animate()
                        .alpha(1f)
                        .setDuration(220)
                        .setInterpolator(new DecelerateInterpolator())
                        .start();

                editorSheetContainer.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(270)
                        .setInterpolator(M3E_OPEN_INTERPOLATOR)
                        .start();
                return true;
            }
        });
    }

    private void startContainerExpandMorph(float density) {
        if (editorSheetContainer == null || originScreenRect == null) return;
        int destW = editorSheetContainer.getWidth();
        int destH = editorSheetContainer.getHeight();
        if (destW <= 0 || destH <= 0) {
            editorSheetContainer.setAlpha(1f);
            vBackdropScrim.setAlpha(1f);
            return;
        }

        int[] destLoc = new int[2];
        editorSheetContainer.getLocationOnScreen(destLoc);
        final int startL = originScreenRect[0] - destLoc[0];
        final int startT = originScreenRect[1] - destLoc[1];
        final int startR = originScreenRect[2] - destLoc[0];
        final int startB = originScreenRect[3] - destLoc[1];

        final float startRadius = originCornerRadiusPx > 0f ? originCornerRadiusPx : 24f * density;
        final float endTopRadius = 28f * density;
        final MorphSheetDrawable morphBg = new MorphSheetDrawable(
                ContextCompat.getColor(this, R.color.prime_base),
                ContextCompat.getColor(this, R.color.chat_island_stroke),
                Math.max(1, Math.round(density))
        );
        editorSheetContainer.setBackground(morphBg);

        final int[] curBounds = new int[] { startL, startT, startR, startB };
        final float[] curRadii = new float[] { startRadius, 0f };
        editorSheetContainer.setClipToOutline(true);
        editorSheetContainer.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int r = Math.max(curBounds[0] + 1, curBounds[2]);
                int b = Math.max(curBounds[1] + 1, curBounds[3]) + Math.round(curRadii[1]);
                outline.setRoundRect(curBounds[0], curBounds[1], r, b, curRadii[0]);
            }
        });

        if (activeSheetAnimator != null) {
            activeSheetAnimator.cancel();
        }
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        activeSheetAnimator = animator;
        animator.setDuration(380);
        animator.setInterpolator(M3E_OPEN_INTERPOLATOR);
        animator.addUpdateListener(anim -> {
            if (isClosing || editorSheetContainer == null) return;
            float t = (float) anim.getAnimatedValue();
            int l = Math.round(lerp(startL, 0, t));
            int top = Math.round(lerp(startT, 0, t));
            int r = Math.round(lerp(startR, destW, t));
            int b = Math.round(lerp(startB, destH, t));
            float topRadius = lerp(startRadius, endTopRadius, t);
            float bottomRadius = lerp(startRadius, 0f, t);

            curBounds[0] = l;
            curBounds[1] = top;
            curBounds[2] = r;
            curBounds[3] = b;
            curRadii[0] = topRadius;
            curRadii[1] = lerp(0f, endTopRadius, t);

            morphBg.setMorphRect(l, top, r, b, topRadius, bottomRadius);
            editorSheetContainer.invalidateOutline();

            editorSheetContainer.setAlpha(rangeProgress(t, 0f, 0.14f));
            if (vBackdropScrim != null) {
                vBackdropScrim.setAlpha(rangeProgress(t, 0f, 0.68f));
            }
            if (webViewRich != null) {
                float p = rangeProgress(t, 0.14f, 0.72f);
                webViewRich.setAlpha(p);
                float s = lerp(0.94f, 1f, p);
                webViewRich.setScaleX(s);
                webViewRich.setScaleY(s);
                webViewRich.setTranslationY(lerp(18f * density, 0f, p));
            }
            if (vSheetDragHandle != null) {
                vSheetDragHandle.setAlpha(0.35f * rangeProgress(t, 0.26f, 0.78f));
            }
            if (blurTopHeader != null) {
                float p = rangeProgress(t, 0.26f, 0.82f);
                blurTopHeader.setAlpha(p);
                blurTopHeader.setTranslationY(lerp(-14f * density, 0f, p));
            }
            if (layoutBottomDock != null) {
                float p = rangeProgress(t, 0.20f, 0.78f);
                layoutBottomDock.setAlpha(p);
                layoutBottomDock.setTranslationY(lerp(20f * density, 0f, p));
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            private boolean canceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                canceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (activeSheetAnimator == animation) {
                    activeSheetAnimator = null;
                }
                if (canceled || isClosing || editorSheetContainer == null) return;
                restoreNormalSheetState();
            }
        });
        animator.start();
    }

    private void restoreNormalSheetState() {
        if (editorSheetContainer != null) {
            editorSheetContainer.setBackgroundResource(R.drawable.bg_editor_sheet_surface);
            editorSheetContainer.setOutlineProvider(ViewOutlineProvider.BACKGROUND);
            editorSheetContainer.setClipToOutline(true);
            editorSheetContainer.invalidateOutline();
            editorSheetContainer.setAlpha(1f);
            editorSheetContainer.setTranslationY(0f);
            editorSheetContainer.setScaleX(1f);
            editorSheetContainer.setScaleY(1f);
        }
        if (vBackdropScrim != null) vBackdropScrim.setAlpha(1f);
        if (webViewRich != null) {
            webViewRich.setAlpha(1f);
            webViewRich.setScaleX(1f);
            webViewRich.setScaleY(1f);
            webViewRich.setTranslationY(0f);
        }
        if (vSheetDragHandle != null) vSheetDragHandle.setAlpha(0.35f);
        if (blurTopHeader != null) {
            blurTopHeader.setAlpha(1f);
            blurTopHeader.setTranslationY(0f);
        }
        if (layoutBottomDock != null) {
            layoutBottomDock.setAlpha(1f);
            layoutBottomDock.setTranslationY(0f);
        }
    }

    private void closeWithAnimation(int resultCode, @Nullable Intent data) {
        if (isClosing) return;
        isClosing = true;
        hideSoftKeyboard();

        if (activeSheetAnimator != null) {
            activeSheetAnimator.cancel();
            activeSheetAnimator = null;
        }

        if (data != null) {
            setResult(resultCode, data);
        } else {
            setResult(resultCode);
        }

        if (editorSheetContainer == null || vBackdropScrim == null || PrimeTransitions.areAnimationsDisabled(this)) {
            finishImmediate();
            return;
        }

        if (blurTopHeader != null) blurTopHeader.setBlurAutoUpdate(false);
        if (blurBottomIsland != null) blurBottomIsland.setBlurAutoUpdate(false);

        float density = getResources().getDisplayMetrics().density;

        if (originScreenRect != null && editorSheetContainer.getWidth() > 0 && editorSheetContainer.getHeight() > 0) {
            startContainerCollapseMorph(density);
            return;
        }

        vBackdropScrim.animate().cancel();
        vBackdropScrim.animate()
                .alpha(0f)
                .setDuration(170)
                .setInterpolator(M3E_CLOSE_INTERPOLATOR)
                .start();

        editorSheetContainer.animate().cancel();
        editorSheetContainer.animate()
                .alpha(0f)
                .translationY(44f * density)
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(190)
                .setInterpolator(M3E_CLOSE_INTERPOLATOR)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        finishImmediate();
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {
                        finishImmediate();
                    }
                })
                .start();
    }

    private void startContainerCollapseMorph(float density) {
        if (editorSheetContainer == null || originScreenRect == null) {
            finishImmediate();
            return;
        }
        int destW = editorSheetContainer.getWidth();
        int destH = editorSheetContainer.getHeight();
        float curScale = editorSheetContainer.getScaleX();
        float curTy = editorSheetContainer.getTranslationY();

        editorSheetContainer.animate().cancel();
        if (vBackdropScrim != null) vBackdropScrim.animate().cancel();

        editorSheetContainer.setTranslationY(0f);
        editorSheetContainer.setScaleX(1f);
        editorSheetContainer.setScaleY(1f);

        int[] baseLoc = new int[2];
        editorSheetContainer.getLocationOnScreen(baseLoc);
        final int endL = originScreenRect[0] - baseLoc[0];
        final int endT = originScreenRect[1] - baseLoc[1];
        final int endR = originScreenRect[2] - baseLoc[0];
        final int endB = originScreenRect[3] - baseLoc[1];

        final int insetX = Math.round(destW * (1f - curScale) * 0.5f);
        final int insetY = Math.round(destH * (1f - curScale) * 0.5f);
        final int startT = Math.round(curTy) + insetY;
        final int startR = destW - insetX;
        final int startB = Math.round(curTy) + destH - insetY;

        final float startTopRadius = 28f * density;
        final float endRadius = originCornerRadiusPx > 0f ? originCornerRadiusPx : 24f * density;
        final float initialScrimAlpha = vBackdropScrim != null ? vBackdropScrim.getAlpha() : 1f;
        final float initialSheetAlpha = editorSheetContainer.getAlpha();
        final float initialWebAlpha = webViewRich != null ? webViewRich.getAlpha() : 1f;
        final float initialHeaderAlpha = blurTopHeader != null ? blurTopHeader.getAlpha() : 1f;
        final float initialDockAlpha = layoutBottomDock != null ? layoutBottomDock.getAlpha() : 1f;

        final MorphSheetDrawable morphBg = new MorphSheetDrawable(
                ContextCompat.getColor(this, R.color.prime_base),
                ContextCompat.getColor(this, R.color.chat_island_stroke),
                Math.max(1, Math.round(density))
        );
        editorSheetContainer.setBackground(morphBg);

        final int[] curBounds = new int[] { insetX, startT, startR, startB };
        final float[] curRadii = new float[] { startTopRadius, startTopRadius };
        editorSheetContainer.setClipToOutline(true);
        editorSheetContainer.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                int r = Math.max(curBounds[0] + 1, curBounds[2]);
                int b = Math.max(curBounds[1] + 1, curBounds[3]) + Math.round(curRadii[1]);
                outline.setRoundRect(curBounds[0], curBounds[1], r, b, curRadii[0]);
            }
        });

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        activeSheetAnimator = animator;
        animator.setDuration(260);
        animator.setInterpolator(M3E_CLOSE_INTERPOLATOR);
        animator.addUpdateListener(anim -> {
            if (editorSheetContainer == null) return;
            float t = (float) anim.getAnimatedValue();
            int l = Math.round(lerp(insetX, endL, t));
            int top = Math.round(lerp(startT, endT, t));
            int r = Math.round(lerp(startR, endR, t));
            int b = Math.round(lerp(startB, endB, t));
            float topRadius = lerp(startTopRadius, endRadius, t);
            float bottomRadius = lerp(0f, endRadius, t);

            curBounds[0] = l;
            curBounds[1] = top;
            curBounds[2] = r;
            curBounds[3] = b;
            curRadii[0] = topRadius;
            curRadii[1] = lerp(startTopRadius, 0f, t);

            morphBg.setMorphRect(l, top, r, b, topRadius, bottomRadius);
            editorSheetContainer.invalidateOutline();

            float contentFade = 1f - rangeProgress(t, 0f, 0.42f);
            if (webViewRich != null) webViewRich.setAlpha(initialWebAlpha * contentFade);
            if (vSheetDragHandle != null) vSheetDragHandle.setAlpha(0.35f * contentFade);
            if (blurTopHeader != null) blurTopHeader.setAlpha(initialHeaderAlpha * contentFade);
            if (layoutBottomDock != null) layoutBottomDock.setAlpha(initialDockAlpha * contentFade);

            editorSheetContainer.setAlpha(initialSheetAlpha * (1f - rangeProgress(t, 0.65f, 1f)));
            if (vBackdropScrim != null) {
                vBackdropScrim.setAlpha(initialScrimAlpha * (1f - t));
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                finishImmediate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                finishImmediate();
            }
        });
        animator.start();
    }

    private static float lerp(float start, float end, float fraction) {
        return start + (end - start) * fraction;
    }

    private static float rangeProgress(float t, float start, float end) {
        if (t <= start) return 0f;
        if (t >= end) return 1f;
        return (t - start) / (end - start);
    }

    private static final class MorphSheetDrawable extends Drawable {
        private final GradientDrawable inner = new GradientDrawable();
        private final Rect morphBounds = new Rect();
        private boolean useMorphBounds = false;

        MorphSheetDrawable(int fillColor, int strokeColor, int strokeWidthPx) {
            inner.setShape(GradientDrawable.RECTANGLE);
            inner.setColor(fillColor);
            inner.setStroke(strokeWidthPx, strokeColor);
        }

        void setMorphRect(int l, int t, int r, int b, float topRadius, float bottomRadius) {
            useMorphBounds = true;
            morphBounds.set(l, t, Math.max(l + 1, r), Math.max(t + 1, b));
            inner.setCornerRadii(new float[] {
                    topRadius, topRadius, topRadius, topRadius,
                    bottomRadius, bottomRadius, bottomRadius, bottomRadius
            });
            inner.setBounds(morphBounds);
            invalidateSelf();
        }

        @Override
        protected void onBoundsChange(@NonNull Rect bounds) {
            super.onBoundsChange(bounds);
            inner.setBounds(useMorphBounds ? morphBounds : bounds);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            if (useMorphBounds) {
                inner.setBounds(morphBounds);
            }
            inner.draw(canvas);
        }

        @Override
        public void setAlpha(int alpha) {
            inner.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(@Nullable ColorFilter colorFilter) {
            inner.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }

    private void finishImmediate() {
        if (!isFinishing()) {
            super.finish();
        }
        disableWindowTransitions();
    }

    @Override
    public void finish() {
        if (!isClosing) {
            closeWithAnimation(RESULT_CANCELED, null);
        } else {
            super.finish();
            disableWindowTransitions();
        }
    }

    private void hideSoftKeyboard() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                View target = getCurrentFocus();
                if (target == null) target = webViewRich;
                if (target != null) {
                    imm.hideSoftInputFromWindow(target.getWindowToken(), 0);
                    target.clearFocus();
                }
            }
        } catch (Throwable ignored) {}
    }

    private void setupPredictiveBack() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackStarted(@NonNull BackEventCompat backEvent) {
                hideSoftKeyboard();
            }

            @Override
            public void handleOnBackProgressed(@NonNull BackEventCompat backEvent) {
                if (editorSheetContainer == null || vBackdropScrim == null || isClosing) return;
                float progress = backEvent.getProgress();
                float density = getResources().getDisplayMetrics().density;
                float scale = 1f - (progress * 0.08f);
                editorSheetContainer.setScaleX(scale);
                editorSheetContainer.setScaleY(scale);
                editorSheetContainer.setTranslationY(progress * 28f * density);
                vBackdropScrim.setAlpha(1f - (progress * 0.55f));
            }

            @Override
            public void handleOnBackCancelled() {
                if (editorSheetContainer == null || vBackdropScrim == null || isClosing) return;
                editorSheetContainer.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .translationY(0f)
                        .setDuration(180)
                        .setInterpolator(M3E_OPEN_INTERPOLATOR)
                        .start();
                vBackdropScrim.animate()
                        .alpha(1f)
                        .setDuration(180)
                        .start();
            }

            @Override
            public void handleOnBackPressed() {
                exitDirectly();
            }
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupHeaderDragToDismiss() {
        View dragTarget = blurTopHeader;
        if (dragTarget == null || editorSheetContainer == null) return;
        dragTarget.setClickable(true);

        final int touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        final float density = getResources().getDisplayMetrics().density;

        dragTarget.setOnTouchListener(new View.OnTouchListener() {
            private float startY = 0f;
            private float startX = 0f;
            private boolean isDragging = false;
            private VelocityTracker velocityTracker = null;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (isClosing) return false;
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startY = event.getRawY();
                        startX = event.getRawX();
                        isDragging = false;
                        if (velocityTracker != null) velocityTracker.recycle();
                        velocityTracker = VelocityTracker.obtain();
                        velocityTracker.addMovement(event);
                        return false;

                    case MotionEvent.ACTION_MOVE:
                        if (velocityTracker != null) velocityTracker.addMovement(event);
                        float dy = event.getRawY() - startY;
                        float dx = event.getRawX() - startX;
                        if (!isDragging && dy > touchSlop && Math.abs(dy) > Math.abs(dx)) {
                            isDragging = true;
                            hideSoftKeyboard();
                        }
                        if (isDragging) {
                            float clampedDy = Math.max(0f, dy);
                            editorSheetContainer.setTranslationY(clampedDy);
                            if (vBackdropScrim != null) {
                                float alpha = Math.max(0.15f, 1f - (clampedDy / (420f * density)));
                                vBackdropScrim.setAlpha(alpha);
                            }
                            return true;
                        }
                        return false;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        float velY = 0f;
                        if (velocityTracker != null) {
                            velocityTracker.addMovement(event);
                            velocityTracker.computeCurrentVelocity(1000);
                            velY = velocityTracker.getYVelocity();
                            velocityTracker.recycle();
                            velocityTracker = null;
                        }
                        if (isDragging) {
                            float currentTy = editorSheetContainer.getTranslationY();
                            if (currentTy > 110f * density || velY > 900f) {
                                exitDirectly();
                            } else {
                                editorSheetContainer.animate()
                                        .translationY(0f)
                                        .setDuration(220)
                                        .setInterpolator(M3E_OPEN_INTERPOLATOR)
                                        .start();
                                if (vBackdropScrim != null) {
                                    vBackdropScrim.animate().alpha(1f).setDuration(200).start();
                                }
                            }
                            isDragging = false;
                            return true;
                        }
                        return false;
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        ioExecutor.shutdownNow();
        if (webViewRich != null) {
            try {
                webViewRich.removeJavascriptInterface("PrimeEditorBridge");
                webViewRich.loadUrl("about:blank");
                webViewRich.stopLoading();
                webViewRich.setWebChromeClient(null);
                webViewRich.destroy();
            } catch (Exception ignored) {}
            webViewRich = null;
        }
        super.onDestroy();
    }

    public static boolean isDarkTheme(Context context) {
        if (context == null) return true;
        SharedPreferences sp = context.getSharedPreferences("PrimeLocalDB", Context.MODE_PRIVATE);
        String themePref = sp.getString("app_theme", "system");
        if ("dark".equalsIgnoreCase(themePref) || "teto_dark".equalsIgnoreCase(themePref)) {
            return true;
        } else if ("light".equalsIgnoreCase(themePref) || "teto_light".equalsIgnoreCase(themePref)) {
            return false;
        }
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        if (webViewRich == null) return;
        webViewRich.setBackgroundColor(Color.TRANSPARENT);

        WebSettings settings = webViewRich.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(true);

        webViewRich.addJavascriptInterface(new EditorJsBridge(), "PrimeEditorBridge");
        webViewRich.setWebChromeClient(new WebChromeClient());
        webViewRich.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (isFinishing() || isDestroyed() || webViewRich == null) return;
                if (!isInitialContentLoaded) {
                    isInitialContentLoaded = true;
                    String htmlToLoad = isInitialRich ? initialRawContent : plainTextToSafeHtml(initialRawContent);
                    if (!htmlToLoad.isEmpty()) {
                        webViewRich.evaluateJavascript("window.primeSetInitialHtml(" + JSONObject.quote(htmlToLoad) + ");", null);
                    } else {
                        webViewRich.evaluateJavascript("window.primeNotifyState();", null);
                    }
                    adjustWebViewBottomPadding();
                    webViewRich.postDelayed(() -> {
                        if (!isFinishing() && !isDestroyed() && !isClosing && webViewRich != null) {
                            webViewRich.requestFocus();
                            webViewRich.evaluateJavascript("window.primeFocusEnd();", null);
                        }
                    }, 180);
                }
            }
        });

        boolean isDark = isDarkTheme(this);
        String textColor = isDark ? "#F8FAFC" : "#0F172A";
        String secondaryColor = isDark ? "#94A3B8" : "#64748B";
        String placeholderColor = isDark ? "#64748B" : "#94A3B8";
        String cardBg = isDark ? "#1E293B" : "#F1F5F9";
        String thBg = isDark ? "#334155" : "#E2E8F0";
        String borderColor = isDark ? "#475569" : "#CBD5E1";
        String accentHex = String.format(Locale.US, "#%06X", (0xFFFFFF & accentColor));
        String linkColor = isDark ? "#38BDF8" : "#0284C7";

        String htmlTemplate = "<!DOCTYPE html><html><head>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no\">" +
                "<style>" +
                "html, body { margin: 0; padding: 0; background: transparent; color: " + textColor + "; font-family: -apple-system, Roboto, sans-serif; font-size: 16px; line-height: 1.5; -webkit-tap-highlight-color: transparent; }" +
                "#editor { min-height: calc(100vh - 180px); padding: 84px 18px 96px 18px; outline: none; word-wrap: break-word; overflow-wrap: break-word; box-sizing: border-box; }" +
                "#editor:empty:before { content: attr(data-placeholder); color: " + placeholderColor + "; pointer-events: none; display: block; }" +
                "h1 { font-size: 1.55em; line-height: 1.25; margin: 10px 0 6px 0; font-weight: 600; }" +
                "h2 { font-size: 1.32em; line-height: 1.3; margin: 8px 0 5px 0; font-weight: 600; }" +
                "h3 { font-size: 1.15em; line-height: 1.35; margin: 6px 0 4px 0; font-weight: 600; }" +
                "h4 { font-size: 1.02em; line-height: 1.4; margin: 4px 0 2px 0; font-weight: 600; }" +
                "p { margin: 4px 0; }" +
                "a { color: " + linkColor + "; text-decoration: underline; }" +
                "img { max-width: 100%; height: auto; display: block; border-radius: 12px; margin: 8px 0; border: 1px solid " + borderColor + "; }" +
                "table { width: 100%; border-collapse: collapse; margin: 10px 0; background-color: " + cardBg + "; border-radius: 10px; overflow: hidden; }" +
                "th, td { border: 1px solid " + borderColor + "; padding: 8px 10px; text-align: left; color: " + textColor + "; min-width: 40px; }" +
                "th { background-color: " + thBg + "; font-weight: 600; }" +
                "blockquote { border-left: 3px solid " + accentHex + "; margin: 8px 0; padding: 6px 12px; background-color: " + cardBg + "; border-radius: 0 10px 10px 0; color: " + secondaryColor + "; }" +
                "pre { background-color: " + cardBg + "; padding: 10px 12px; border-radius: 10px; border: 1px solid " + borderColor + "; overflow-x: auto; font-family: monospace; font-size: 0.9em; color: " + textColor + "; margin: 8px 0; white-space: pre-wrap; }" +
                "code { font-family: monospace; font-size: 0.9em; background-color: " + cardBg + "; padding: 2px 5px; border-radius: 5px; color: " + accentHex + "; }" +
                "ul, ol { margin: 6px 0; padding-left: 22px; }" +
                "li { margin: 2px 0; }" +
                "</style></head>" +
                "<body><div id=\"editor\" contenteditable=\"true\" data-placeholder=\"Напишите и отформатируйте сообщение...\"></div>" +
                "<script>" +
                "(function() {" +
                "  var editor = document.getElementById('editor');" +
                "  var savedRange = null;" +
                "  var stateTimer = null;" +
                "  try { document.execCommand('defaultParagraphSeparator', false, 'div'); } catch(e) {}" +
                "  window.primeSaveSelection = function() {" +
                "    var sel = window.getSelection();" +
                "    if (sel && sel.rangeCount > 0) {" +
                "      var r = sel.getRangeAt(0);" +
                "      if (editor.contains(r.commonAncestorContainer)) {" +
                "        savedRange = r.cloneRange();" +
                "      }" +
                "    }" +
                "  };" +
                "  window.primeRestoreSelection = function() {" +
                "    editor.focus();" +
                "    if (savedRange) {" +
                "      var sel = window.getSelection();" +
                "      sel.removeAllRanges();" +
                "      sel.addRange(savedRange);" +
                "    }" +
                "  };" +
                "  window.primeFocusEnd = function() {" +
                "    editor.focus();" +
                "    var range = document.createRange();" +
                "    range.selectNodeContents(editor);" +
                "    range.collapse(false);" +
                "    var sel = window.getSelection();" +
                "    sel.removeAllRanges();" +
                "    sel.addRange(range);" +
                "    savedRange = range.cloneRange();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeSetInitialHtml = function(html) {" +
                "    editor.innerHTML = html;" +
                "    window.primeFocusEnd();" +
                "  };" +
                "  window.primeSetBottomPadding = function(px) {" +
                "    editor.style.paddingBottom = px + 'px';" +
                "  };" +
                "  function findAncestorTag(node, tags) {" +
                "    while (node && node !== editor) {" +
                "      if (node.nodeType === 1 && tags.indexOf(node.tagName) !== -1) return node;" +
                "      node = node.parentNode;" +
                "    }" +
                "    return null;" +
                "  }" +
                "  window.primeExec = function(cmd, val) {" +
                "    window.primeRestoreSelection();" +
                "    document.execCommand(cmd, false, val);" +
                "    window.primeSaveSelection();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeToggleBlockquote = function() {" +
                "    window.primeRestoreSelection();" +
                "    var sel = window.getSelection();" +
                "    var bq = sel && sel.rangeCount > 0 ? findAncestorTag(sel.anchorNode, ['BLOCKQUOTE']) : null;" +
                "    if (bq) {" +
                "      document.execCommand('formatBlock', false, 'DIV');" +
                "    } else {" +
                "      document.execCommand('formatBlock', false, 'BLOCKQUOTE');" +
                "    }" +
                "    window.primeSaveSelection();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeTogglePre = function() {" +
                "    window.primeRestoreSelection();" +
                "    var sel = window.getSelection();" +
                "    var pre = sel && sel.rangeCount > 0 ? findAncestorTag(sel.anchorNode, ['PRE']) : null;" +
                "    if (pre) {" +
                "      document.execCommand('formatBlock', false, 'DIV');" +
                "    } else {" +
                "      document.execCommand('formatBlock', false, 'PRE');" +
                "    }" +
                "    window.primeSaveSelection();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeToggleInlineCode = function() {" +
                "    window.primeRestoreSelection();" +
                "    var sel = window.getSelection();" +
                "    if (!sel || sel.rangeCount === 0) return;" +
                "    var codeEl = findAncestorTag(sel.anchorNode, ['CODE']);" +
                "    if (codeEl) {" +
                "      var textNode = document.createTextNode(codeEl.textContent);" +
                "      codeEl.parentNode.replaceChild(textNode, codeEl);" +
                "    } else {" +
                "      var selected = sel.toString();" +
                "      if (selected.length > 0) {" +
                "        var escaped = selected.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');" +
                "        document.execCommand('insertHTML', false, '<code>' + escaped + '</code>&nbsp;');" +
                "      } else {" +
                "        document.execCommand('insertHTML', false, '<code>код</code>&nbsp;');" +
                "      }" +
                "    }" +
                "    window.primeSaveSelection();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeResetColors = function() {" +
                "    window.primeRestoreSelection();" +
                "    document.execCommand('foreColor', false, " + JSONObject.quote(textColor) + ");" +
                "    try { document.execCommand('hiliteColor', false, 'transparent'); } catch(e) {}" +
                "    window.primeSaveSelection();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeClearFormatting = function() {" +
                "    window.primeRestoreSelection();" +
                "    document.execCommand('removeFormat', false, null);" +
                "    document.execCommand('formatBlock', false, 'DIV');" +
                "    window.primeSaveSelection();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeInsertLink = function(url, label) {" +
                "    window.primeRestoreSelection();" +
                "    var safeUrl = url.replace(/\"/g, '&quot;');" +
                "    var selText = window.getSelection().toString();" +
                "    var text = (label && label.trim().length > 0) ? label : (selText.length > 0 ? selText : url);" +
                "    var escapedText = text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');" +
                "    document.execCommand('insertHTML', false, '<a href=\"' + safeUrl + '\">' + escapedText + '</a> ');" +
                "    window.primeSaveSelection();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeInsertHtml = function(html) {" +
                "    window.primeRestoreSelection();" +
                "    document.execCommand('insertHTML', false, html);" +
                "    window.primeSaveSelection();" +
                "    window.primeNotifyState();" +
                "  };" +
                "  window.primeNotifyState = function() {" +
                "    try {" +
                "      var sel = window.getSelection();" +
                "      var blockVal = '';" +
                "      try { blockVal = (document.queryCommandValue('formatBlock') || '').toUpperCase(); } catch(e) {}" +
                "      var anchor = sel && sel.rangeCount > 0 ? sel.anchorNode : null;" +
                "      var inQuote = !!findAncestorTag(anchor, ['BLOCKQUOTE']) || blockVal === 'BLOCKQUOTE';" +
                "      var inPre = !!findAncestorTag(anchor, ['PRE']) || blockVal === 'PRE';" +
                "      var inCode = !!findAncestorTag(anchor, ['CODE']);" +
                "      var align = 'left';" +
                "      if (document.queryCommandState('justifyCenter')) align = 'center';" +
                "      else if (document.queryCommandState('justifyRight')) align = 'right';" +
                "      var rawText = (editor.innerText || '').replace(/\\u00a0/g, ' ');" +
                "      var trimmed = rawText.trim();" +
                "      var chars = trimmed.length;" +
                "      var words = trimmed.length === 0 ? 0 : trimmed.split(/\\s+/).length;" +
                "      var state = {" +
                "        bold: document.queryCommandState('bold')," +
                "        italic: document.queryCommandState('italic')," +
                "        underline: document.queryCommandState('underline')," +
                "        strike: document.queryCommandState('strikeThrough')," +
                "        ul: document.queryCommandState('insertUnorderedList')," +
                "        ol: document.queryCommandState('insertOrderedList')," +
                "        quote: inQuote," +
                "        pre: inPre," +
                "        code: inCode," +
                "        block: blockVal," +
                "        align: align," +
                "        chars: chars," +
                "        words: words," +
                "        selText: sel ? sel.toString() : ''" +
                "      };" +
                "      PrimeEditorBridge.onEditorStateChanged(JSON.stringify(state));" +
                "    } catch(e) {}" +
                "  };" +
                "  function scheduleNotify() {" +
                "    window.primeSaveSelection();" +
                "    clearTimeout(stateTimer);" +
                "    stateTimer = setTimeout(window.primeNotifyState, 40);" +
                "  }" +
                "  editor.addEventListener('input', scheduleNotify);" +
                "  editor.addEventListener('keyup', scheduleNotify);" +
                "  editor.addEventListener('mouseup', scheduleNotify);" +
                "  editor.addEventListener('touchend', scheduleNotify);" +
                "  document.addEventListener('selectionchange', scheduleNotify);" +
                "})();" +
                "</script></body></html>";

        webViewRich.loadDataWithBaseURL("file:///android_asset/", htmlTemplate, "text/html", "UTF-8", null);
    }

    private class EditorJsBridge {
        @JavascriptInterface
        public void onEditorStateChanged(String json) {
            if (isFinishing() || isDestroyed() || isClosing) return;
            runOnUiThread(() -> {
                try {
                    JSONObject obj = new JSONObject(json);
                    stateBold = obj.optBoolean("bold", false);
                    stateItalic = obj.optBoolean("italic", false);
                    stateUnderline = obj.optBoolean("underline", false);
                    stateStrike = obj.optBoolean("strike", false);
                    stateUl = obj.optBoolean("ul", false);
                    stateOl = obj.optBoolean("ol", false);
                    stateQuote = obj.optBoolean("quote", false);
                    statePre = obj.optBoolean("pre", false);
                    stateCode = obj.optBoolean("code", false);
                    stateBlock = obj.optString("block", "");
                    stateAlign = obj.optString("align", "left");
                    stateSelectedText = obj.optString("selText", "");
                    int chars = obj.optInt("chars", 0);
                    int words = obj.optInt("words", 0);

                    if (tvEditorStats != null) {
                        tvEditorStats.setText(String.format(Locale.getDefault(), "%d симв. · %d слов", chars, words));
                    }
                    updateToolbarActiveStates();
                } catch (Exception ignored) {}
            });
        }
    }

    private void updateToolbarActiveStates() {
        applyChipActiveState(btnBold, stateBold);
        applyChipActiveState(btnItalic, stateItalic);
        applyChipActiveState(btnUnderline, stateUnderline);
        applyChipActiveState(btnStrike, stateStrike);
        applyChipActiveState(btnHeading, stateBlock.startsWith("H"));
        applyChipActiveState(btnList, stateUl || stateOl);
        applyChipActiveState(btnQuote, stateQuote);
        applyChipActiveState(btnCode, stateCode || statePre);
        applyChipActiveState(btnAlign, !"left".equals(stateAlign));
        if (btnAlign != null) {
            if ("center".equals(stateAlign)) {
                btnAlign.setImageResource(R.drawable.ic_format_align_center);
            } else if ("right".equals(stateAlign)) {
                btnAlign.setImageResource(R.drawable.ic_format_align_right);
            } else {
                btnAlign.setImageResource(R.drawable.ic_format_align_left);
            }
        }
    }

    private void applyChipActiveState(@Nullable ImageButton btn, boolean active) {
        if (btn == null) return;
        if (active) {
            float density = getResources().getDisplayMetrics().density;
            int bgTint = Color.argb(46, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setCornerRadius(12f * density);
            bg.setColor(bgTint);
            btn.setBackground(bg);
            btn.setImageTintList(ColorStateList.valueOf(accentColor));
        } else {
            android.util.TypedValue outValue = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, outValue, true);
            btn.setBackgroundResource(outValue.resourceId);
            btn.setImageTintList(ColorStateList.valueOf(primaryTextColor));
        }
    }

    private static String plainTextToSafeHtml(String text) {
        if (text == null || text.isEmpty()) return "";
        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
        return escaped.replace("\r\n", "<br>").replace("\n", "<br>");
    }

    private static String decodeJsString(@Nullable String jsValue) {
        if (jsValue == null || jsValue.isEmpty() || "null".equals(jsValue)) return "";
        try {
            Object parsed = new JSONTokener(jsValue).nextValue();
            return parsed != null ? String.valueOf(parsed) : "";
        } catch (Exception e) {
            return jsValue.replaceAll("^\"|\"$", "");
        }
    }

    private static boolean isHtmlVisuallyEmpty(String html) {
        if (html == null) return true;
        String stripped = html
                .replaceAll("(?i)<br\\s*/?>", "")
                .replaceAll("(?i)</?(div|p|span|b|i|u|s|strong|em)[^>]*>", "")
                .replace("&nbsp;", " ")
                .trim();
        return stripped.isEmpty() && !html.contains("<img") && !html.contains("<table");
    }

    private void finishAndSend() {
        if (isClosing) return;
        if (webViewRich == null) {
            closeWithAnimation(RESULT_CANCELED, null);
            return;
        }
        webViewRich.evaluateJavascript("document.getElementById('editor').innerHTML", value -> {
            String html = decodeJsString(value);
            if (!isHtmlVisuallyEmpty(html)) {
                html = "[RICH]" + html;
            } else {
                html = "";
            }
            if (html.isEmpty()) {
                PrimeNotification.show(this, "Введите текст сообщения");
                return;
            }
            Intent data = new Intent();
            data.putExtra(RESULT_CONTENT, html);
            data.putExtra(EXTRA_SEND_IMMEDIATELY, true);
            closeWithAnimation(RESULT_OK, data);
        });
    }

    private void exitDirectly() {
        if (isClosing) return;
        if (webViewRich != null) {
            webViewRich.evaluateJavascript("document.getElementById('editor').innerText", value -> {
                String plainText = decodeJsString(value).replace('\u00A0', ' ').trim();
                Intent data = new Intent();
                data.putExtra(RESULT_CONTENT, plainText);
                data.putExtra(EXTRA_SEND_IMMEDIATELY, false);
                closeWithAnimation(RESULT_OK, data);
            });
        } else {
            closeWithAnimation(RESULT_CANCELED, null);
        }
    }

    private void showHeadingMenu() {
        if (webViewRich == null || btnHeading == null) return;
        PrimePopupMenu.create(this, btnHeading)
                .addItem("Обычный текст", R.drawable.ic_format_align_left, "DIV".equals(stateBlock) || "P".equals(stateBlock) || stateBlock.isEmpty(), () -> {
                    runEditorJs("window.primeExec('formatBlock', 'DIV');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Заголовок 1 (Крупный)", R.drawable.ic_format_heading, "H1".equals(stateBlock), () -> {
                    runEditorJs("window.primeExec('formatBlock', 'H1');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Заголовок 2", R.drawable.ic_format_heading, "H2".equals(stateBlock), () -> {
                    runEditorJs("window.primeExec('formatBlock', 'H2');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Заголовок 3 (Средний)", R.drawable.ic_format_heading, "H3".equals(stateBlock), () -> {
                    runEditorJs("window.primeExec('formatBlock', 'H3');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Заголовок 4 (Мелкий)", R.drawable.ic_format_heading, "H4".equals(stateBlock), () -> {
                    runEditorJs("window.primeExec('formatBlock', 'H4');");
                    return kotlin.Unit.INSTANCE;
                })
                .show();
    }

    private void showColorMenu() {
        if (webViewRich == null || btnColor == null) return;
        String highlightRgba = String.format(Locale.US, "rgba(%d,%d,%d,0.28)",
                Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor));

        PrimePopupMenu.create(this, btnColor)
                .addItem("Красный", R.drawable.ic_format_color, false, () -> {
                    runEditorJs("window.primeExec('foreColor', '#EF4444');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Синий", R.drawable.ic_format_color, false, () -> {
                    runEditorJs("window.primeExec('foreColor', '#3B82F6');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Зеленый", R.drawable.ic_format_color, false, () -> {
                    runEditorJs("window.primeExec('foreColor', '#10B981');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Желтый", R.drawable.ic_format_color, false, () -> {
                    runEditorJs("window.primeExec('foreColor', '#F59E0B');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Фиолетовый", R.drawable.ic_format_color, false, () -> {
                    runEditorJs("window.primeExec('foreColor', '#8B5CF6');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Розовый", R.drawable.ic_format_color, false, () -> {
                    runEditorJs("window.primeExec('foreColor', '#EC4899');");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Маркер (Выделение)", R.drawable.ic_format_color, false, () -> {
                    runEditorJs("window.primeExec('hiliteColor', " + JSONObject.quote(highlightRgba) + ");");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Сбросить цвет", R.drawable.ic_format_clear, false, () -> {
                    runEditorJs("window.primeResetColors();");
                    return kotlin.Unit.INSTANCE;
                })
                .show();
    }

    private void showListMenu() {
        if (webViewRich == null || btnList == null) return;
        PrimePopupMenu.create(this, btnList)
                .addItem("Маркированный список", R.drawable.ic_format_list_bulleted, stateUl, () -> {
                    runEditorJs("window.primeExec('insertUnorderedList', null);");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Нумерованный список", R.drawable.ic_format_list_numbered, stateOl, () -> {
                    runEditorJs("window.primeExec('insertOrderedList', null);");
                    return kotlin.Unit.INSTANCE;
                })
                .show();
    }

    private void showCodeMenu() {
        if (webViewRich == null || btnCode == null) return;
        PrimePopupMenu.create(this, btnCode)
                .addItem("Строчный код", R.drawable.ic_format_code, stateCode, () -> {
                    runEditorJs("window.primeToggleInlineCode();");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Блок кода", R.drawable.ic_format_code, statePre, () -> {
                    runEditorJs("window.primeTogglePre();");
                    return kotlin.Unit.INSTANCE;
                })
                .show();
    }

    private void showAlignMenu() {
        if (webViewRich == null || btnAlign == null) return;
        PrimePopupMenu.create(this, btnAlign)
                .addItem("По левому краю", R.drawable.ic_format_align_left, "left".equals(stateAlign), () -> {
                    runEditorJs("window.primeExec('justifyLeft', null);");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("По центру", R.drawable.ic_format_align_center, "center".equals(stateAlign), () -> {
                    runEditorJs("window.primeExec('justifyCenter', null);");
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("По правому краю", R.drawable.ic_format_align_right, "right".equals(stateAlign), () -> {
                    runEditorJs("window.primeExec('justifyRight', null);");
                    return kotlin.Unit.INSTANCE;
                })
                .show();
    }

    private void showInsertLinkDialog() {
        if (webViewRich == null) return;
        float density = getResources().getDisplayMetrics().density;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padH = (int) (24 * density);
        int padV = (int) (12 * density);
        layout.setPadding(padH, padV, padH, 0);

        final EditText etLabel = new EditText(this);
        etLabel.setHint("Текст ссылки");
        etLabel.setText(stateSelectedText);
        etLabel.setTextColor(primaryTextColor);
        etLabel.setHintTextColor(secondaryTextColor);
        layout.addView(etLabel);

        final EditText etUrl = new EditText(this);
        etUrl.setHint("https://example.com");
        etUrl.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        etUrl.setTextColor(primaryTextColor);
        etUrl.setHintTextColor(secondaryTextColor);
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        urlLp.topMargin = (int) (8 * density);
        etUrl.setLayoutParams(urlLp);
        layout.addView(etUrl);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Вставить ссылку")
                .setView(layout)
                .setPositiveButton("Вставить", (dialog, which) -> {
                    String rawUrl = etUrl.getText().toString().trim();
                    if (rawUrl.isEmpty()) return;
                    if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://") && !rawUrl.startsWith("mailto:")) {
                        rawUrl = "https://" + rawUrl;
                    }
                    String label = etLabel.getText().toString().trim();
                    runEditorJs("window.primeInsertLink(" + JSONObject.quote(rawUrl) + ", " + JSONObject.quote(label) + ");");
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showInsertTableDialog() {
        if (webViewRich == null || btnTable == null) return;
        PrimePopupMenu.create(this, btnTable)
                .addItem("Таблица 2 × 2", R.drawable.ic_format_table, false, () -> {
                    insertHtmlTable(2, 2);
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Таблица 3 × 3", R.drawable.ic_format_table, false, () -> {
                    insertHtmlTable(3, 3);
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Таблица 4 × 3", R.drawable.ic_format_table, false, () -> {
                    insertHtmlTable(4, 3);
                    return kotlin.Unit.INSTANCE;
                })
                .addItem("Свой размер...", R.drawable.ic_edit, false, () -> {
                    showCustomTableDialog();
                    return kotlin.Unit.INSTANCE;
                })
                .show();
    }

    private void showCustomTableDialog() {
        float density = getResources().getDisplayMetrics().density;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding((int) (24 * density), (int) (12 * density), (int) (24 * density), 0);

        final EditText rowsInput = new EditText(this);
        rowsInput.setHint("Количество строк (1–10)");
        rowsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        rowsInput.setText("3");
        rowsInput.setTextColor(primaryTextColor);
        rowsInput.setHintTextColor(secondaryTextColor);
        layout.addView(rowsInput);

        final EditText colsInput = new EditText(this);
        colsInput.setHint("Количество столбцов (1–8)");
        colsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        colsInput.setText("3");
        colsInput.setTextColor(primaryTextColor);
        colsInput.setHintTextColor(secondaryTextColor);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        colLp.topMargin = (int) (8 * density);
        colsInput.setLayoutParams(colLp);
        layout.addView(colsInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Размер таблицы")
                .setView(layout)
                .setPositiveButton("Вставить", (dialog, which) -> {
                    int rows = 3;
                    int cols = 3;
                    try {
                        rows = Integer.parseInt(rowsInput.getText().toString().trim());
                        cols = Integer.parseInt(colsInput.getText().toString().trim());
                    } catch (NumberFormatException ignored) {}
                    rows = Math.max(1, Math.min(rows, 10));
                    cols = Math.max(1, Math.min(cols, 8));
                    insertHtmlTable(rows, cols);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void insertHtmlTable(int rows, int cols) {
        StringBuilder table = new StringBuilder("<table><tr>");
        for (int c = 1; c <= cols; c++) {
            table.append("<th>Заголовок ").append(c).append("</th>");
        }
        table.append("</tr>");
        for (int r = 2; r <= rows; r++) {
            table.append("<tr>");
            for (int c = 1; c <= cols; c++) {
                table.append("<td>Ячейка</td>");
            }
            table.append("</tr>");
        }
        table.append("</table><div><br></div>");
        runEditorJs("window.primeInsertHtml(" + JSONObject.quote(table.toString()) + ");");
    }

    private void showImageSizeDialog(Uri uri) {
        String[] sizes = {"Компактное (35%)", "Среднее (65%)", "На всю ширину (100%)"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Размер изображения")
                .setItems(sizes, (dialog, which) -> {
                    String widthStyle;
                    switch (which) {
                        case 0:
                            widthStyle = "width:35%;";
                            break;
                        case 1:
                            widthStyle = "width:65%;";
                            break;
                        default:
                            widthStyle = "width:100%;";
                            break;
                    }
                    ioExecutor.execute(() -> {
                        String imgSrc = encodeUriToDataUrl(uri);
                        if (imgSrc == null) imgSrc = uri.toString();
                        String htmlImg = "<img src=\"" + imgSrc + "\" style=\"" + widthStyle + " max-width:100%; height:auto; border-radius:10px; display:block; margin:8px 0;\" /><div><br></div>";
                        runOnUiThread(() -> {
                            if (!isFinishing() && !isDestroyed() && !isClosing) {
                                runEditorJs("window.primeInsertHtml(" + JSONObject.quote(htmlImg) + ");");
                            }
                        });
                    });
                })
                .show();
    }

    @Nullable
    private String encodeUriToDataUrl(Uri uri) {
        try (InputStream boundsStream = getContentResolver().openInputStream(uri)) {
            if (boundsStream == null) return null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(boundsStream, null, opts);

            int maxDim = 640;
            int sampleSize = 1;
            int w = opts.outWidth;
            int h = opts.outHeight;
            while (w / sampleSize > maxDim * 2 || h / sampleSize > maxDim * 2) {
                sampleSize *= 2;
            }

            BitmapFactory.Options decodeOpts = new BitmapFactory.Options();
            decodeOpts.inSampleSize = sampleSize;
            Bitmap decoded;
            try (InputStream dataStream = getContentResolver().openInputStream(uri)) {
                if (dataStream == null) return null;
                decoded = BitmapFactory.decodeStream(dataStream, null, decodeOpts);
            }
            if (decoded == null) return null;

            Bitmap scaled = decoded;
            if (decoded.getWidth() > maxDim || decoded.getHeight() > maxDim) {
                float ratio = Math.min((float) maxDim / decoded.getWidth(), (float) maxDim / decoded.getHeight());
                int nw = Math.max(1, Math.round(decoded.getWidth() * ratio));
                int nh = Math.max(1, Math.round(decoded.getHeight() * ratio));
                scaled = Bitmap.createScaledBitmap(decoded, nw, nh, true);
                if (scaled != decoded) decoded.recycle();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 72, baos);
            scaled.recycle();
            String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            return "data:image/jpeg;base64," + b64;
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public void onActionModeStarted(android.view.ActionMode mode) {
        super.onActionModeStarted(mode);
        android.view.Menu menu = mode.getMenu();
        if (menu != null) {
            menu.add(0, 101, 0, "Жирный").setOnMenuItemClickListener(item -> {
                runToolbarExec("bold");
                return true;
            });
            menu.add(0, 102, 1, "Курсив").setOnMenuItemClickListener(item -> {
                runToolbarExec("italic");
                return true;
            });
            menu.add(0, 103, 2, "Подчеркнутый").setOnMenuItemClickListener(item -> {
                runToolbarExec("underline");
                return true;
            });
            menu.add(0, 104, 3, "Зачеркнутый").setOnMenuItemClickListener(item -> {
                runToolbarExec("strikeThrough");
                return true;
            });
            menu.add(0, 105, 4, "Код").setOnMenuItemClickListener(item -> {
                runEditorJs("window.primeToggleInlineCode();");
                return true;
            });
        }
    }
}
