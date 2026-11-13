package com.messenger.prime;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.view.Window;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.net.Uri;
import com.google.android.material.transition.platform.MaterialContainerTransform;

public class RichEditorActivity extends AppCompatActivity {
    public static final String EXTRA_CONTENT = "extra_content";
    public static final String RESULT_CONTENT = "result_content";

    private WebView webViewRich;
    private String currentHtml = "";

    private final ActivityResultLauncher<String> mediaPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    showImageSizeDialog(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        getWindow().requestFeature(Window.FEATURE_ACTIVITY_TRANSITIONS);
        
        MaterialContainerTransform transform = new MaterialContainerTransform();
        transform.addTarget(R.id.layoutFloatingBottomIsland);
        transform.setDuration(280);
        transform.setInterpolator(new androidx.interpolator.view.animation.FastOutSlowInInterpolator());
        transform.setAllContainerColors(android.graphics.Color.TRANSPARENT);
        transform.setScrimColor(android.graphics.Color.TRANSPARENT);
        transform.setFadeMode(MaterialContainerTransform.FADE_MODE_THROUGH);
        
        getWindow().setSharedElementEnterTransition(transform);
        getWindow().setSharedElementReturnTransition(transform);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_rich_editor);
        setupBlurViews();

        boolean isDark = isDarkTheme(this);
        UIExtensionsKt.setupEdgeToEdge(this, !isDark);

        View root = findViewById(R.id.layoutRichRoot);
        View bottomIsland = findViewById(R.id.layoutFloatingBottomIsland);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                v.setPadding(0, systemBars.top, 0, 0);
                
                int bottomMargin = Math.max(systemBars.bottom, ime.bottom) + (int)(12 * getResources().getDisplayMetrics().density);
                if (bottomIsland != null) {
                    androidx.constraintlayout.widget.ConstraintLayout.LayoutParams lp = (androidx.constraintlayout.widget.ConstraintLayout.LayoutParams) bottomIsland.getLayoutParams();
                    lp.bottomMargin = bottomMargin;
                    bottomIsland.setLayoutParams(lp);
                }
                return insets;
            });
        }

        webViewRich = findViewById(R.id.webViewRich);
        setupWebView();

        // Retrieve initial content if any
        String init = getIntent().getStringExtra(EXTRA_CONTENT);
        if (init != null) {
            if (init.startsWith("[RICH]")) {
                init = init.substring(6);
            }
            currentHtml = init;
        }

        // Formatting actions
        if (findViewById(R.id.btnHeading) != null) {
            findViewById(R.id.btnHeading).setOnClickListener(v -> insertHeading());
        }
        if (findViewById(R.id.btnBold) != null) {
            findViewById(R.id.btnBold).setOnClickListener(v -> showTextStylesMenu());
        }
        if (findViewById(R.id.btnList) != null) {
            findViewById(R.id.btnList).setOnClickListener(v -> showListMenu());
        }
        if (findViewById(R.id.btnAlign) != null) {
            findViewById(R.id.btnAlign).setOnClickListener(v -> showAlignMenu());
        }
        if (findViewById(R.id.btnTable) != null) {
            findViewById(R.id.btnTable).setOnClickListener(v -> showTableMenu());
        }
        if (findViewById(R.id.btnInsertImage) != null) {
            findViewById(R.id.btnInsertImage).setOnClickListener(v -> mediaPickerLauncher.launch("image/*"));
        }

        View toolbarRich = findViewById(R.id.toolbarRich);
        View btnToggleToolbar = findViewById(R.id.btnToggleToolbar);
        if (btnToggleToolbar != null && toolbarRich != null) {
            btnToggleToolbar.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                if (toolbarRich.getVisibility() == View.VISIBLE) {
                    toolbarRich.animate().cancel();
                    toolbarRich.animate()
                            .alpha(0f)
                            .scaleY(0f)
                            .setDuration(180)
                            .withEndAction(() -> toolbarRich.setVisibility(View.GONE))
                            .start();
                } else {
                    toolbarRich.setVisibility(View.VISIBLE);
                    toolbarRich.setAlpha(0f);
                    toolbarRich.setScaleY(0f);
                    toolbarRich.animate().cancel();
                    toolbarRich.animate()
                            .alpha(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start();
                }
            });
        }

        if (findViewById(R.id.btnQuote) != null) {
            findViewById(R.id.btnQuote).setOnClickListener(v -> webViewRich.evaluateJavascript("document.execCommand('formatBlock', false, 'BLOCKQUOTE');", null));
        }
        if (findViewById(R.id.btnColor) != null) {
            findViewById(R.id.btnColor).setOnClickListener(v -> showColorMenu());
        }

        // Navigation actions
        if (findViewById(R.id.btnCancel) != null) {
            findViewById(R.id.btnCancel).setOnClickListener(v -> exitDirectly());
        }

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                exitDirectly();
            }
        });

        if (findViewById(R.id.btnDone) != null) {
            findViewById(R.id.btnDone).setOnClickListener(v -> webViewRich.evaluateJavascript("document.getElementById('editor').innerHTML", value -> {
                String html = value != null ? value.replaceAll("^\"|\"$", "").replace("\\\"", "\"").replace("\\u003C", "<") : "";
                html = html.replace("\\n", "\n").replace("\\'", "'");
                if (!html.trim().isEmpty() && !html.equals("null") && !html.equals("<br>")) {
                    html = "[RICH]" + html;
                } else {
                    html = "";
                }
                Intent data = new Intent();
                data.putExtra(RESULT_CONTENT, html);
                data.putExtra("EXTRA_SEND_IMMEDIATELY", true);
                setResult(RESULT_OK, data);
                finish();
            }));
        }
    }

    private boolean isFinishingAfterTransition = false;

    @Override
    public void finish() {
        if (!isFinishingAfterTransition) {
            isFinishingAfterTransition = true;
            supportFinishAfterTransition();
        } else {
            super.finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (webViewRich != null) {
            try {
                webViewRich.loadUrl("about:blank");
                webViewRich.stopLoading();
                webViewRich.setWebChromeClient(null);
                webViewRich.destroy();
            } catch (Exception ignored) {}
            webViewRich = null;
        }
        super.onDestroy();
    }

    public static boolean isDarkTheme(android.content.Context context) {
        if (context == null) return true;
        android.content.SharedPreferences sp = context.getSharedPreferences("PrimeLocalDB", android.content.Context.MODE_PRIVATE);
        String themePref = sp.getString("app_theme", "system");
        if ("dark".equalsIgnoreCase(themePref)) {
            return true;
        } else if ("light".equalsIgnoreCase(themePref)) {
            return false;
        }
        return (context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private void setupBlurViews() {
        ViewGroup rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView == null) rootView = (ViewGroup) getWindow().getDecorView();

        boolean isDark = isDarkTheme(this);
        Drawable windowBg = getWindow().getDecorView().getBackground();
        int overlayColor = BlurViewKt.getDefaultBlurOverlayColor(this);

        eightbitlab.com.blurview.BlurView blurBottomIsland = findViewById(R.id.layoutFloatingBottomIsland);

        if (blurBottomIsland != null) {
            blurBottomIsland.setClipToOutline(true);
            BlurViewKt.setupBlur(blurBottomIsland, rootView, 16f, overlayColor, windowBg);
        }
    }

    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        if (webViewRich == null) return;
        webViewRich.setBackgroundColor(0);
        webViewRich.getSettings().setJavaScriptEnabled(true);
        webViewRich.getSettings().setDomStorageEnabled(true);
        webViewRich.setWebChromeClient(new android.webkit.WebChromeClient());
        webViewRich.setWebViewClient(new android.webkit.WebViewClient() {
            @Override
            public void onPageFinished(android.webkit.WebView view, String url) {
                if (!currentHtml.isEmpty()) {
                    String escaped = currentHtml.replace("'", "\\'").replace("\n", "\\n");
                    webViewRich.evaluateJavascript("document.getElementById('editor').innerHTML = '" + escaped + "';", null);
                }
            }
        });

        boolean isDark = isDarkTheme(this);
        String textColor = isDark ? "#FFFFFF" : "#0F172A";
        String bgColor = "transparent";
        String tableBg = isDark ? "#1E293B" : "#F1F5F9";
        String thBg = isDark ? "#334155" : "#E2E8F0";
        String borderColor = isDark ? "#475569" : "#CBD5E1";
        String blockquoteColor = isDark ? "#00E676" : "#154B87";

        String htmlTemplate = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no\">" +
                "<style>" +
                "body { margin: 0; padding: 24px 16px 120px 16px; font-family: sans-serif; font-size: 16px; color: " + textColor + "; background-color: " + bgColor + "; }" +
                "#editor { min-height: 100vh; outline: none; line-height: 1.4; }" +
                "img { max-width: 100%; height: auto; display: block; border-radius: 8px; margin: 8px 0; border: 1px solid " + borderColor + "; }" +
                "table { width: 100%; border-collapse: collapse; margin: 10px 0; background-color: " + tableBg + "; border-radius: 6px; overflow: hidden; }" +
                "th, td { border: 1px solid " + borderColor + "; padding: 8px; text-align: left; color: " + textColor + "; }" +
                "th { background-color: " + thBg + "; font-weight: bold; }" +
                "blockquote { border-left: 4px solid " + blockquoteColor + "; margin: 0; padding-left: 10px; color: " + (isDark ? "#94A3B8" : "#475569") + "; font-style: italic; }" +
                "pre { background-color: " + tableBg + "; padding: 10px; border-radius: 6px; overflow-x: auto; color: " + textColor + "; }" +
                "code { font-family: monospace; background-color: " + tableBg + "; padding: 2px 4px; border-radius: 4px; color: " + textColor + "; }" +
                "[placeholder]:empty:before { content: attr(placeholder); color: " + (isDark ? "#64748B" : "#94A3B8") + "; pointer-events: none; display: block; }" +
                "</style></head>" +
                "<body><div id=\"editor\" contenteditable=\"true\" placeholder=\"Напишите текст с форматированием...\"></div>" +
                "<script>" +
                "document.execCommand('defaultParagraphSeparator', false, 'br');" +
                "</script>" +
                "</body></html>";

        webViewRich.loadDataWithBaseURL("file:///android_asset/", htmlTemplate, "text/html", "UTF-8", null);
    }

    private void exitDirectly() {
        if (webViewRich != null) {
            webViewRich.evaluateJavascript("document.getElementById('editor').innerText", value -> {
                String plainText = value != null ? value.replaceAll("^\"|\"$", "").replace("\\n", "\n").replace("\\\"", "\"").trim() : "";
                Intent data = new Intent();
                data.putExtra(RESULT_CONTENT, plainText.equals("null") ? "" : plainText);
                data.putExtra("EXTRA_SEND_IMMEDIATELY", false);
                setResult(RESULT_OK, data);
                finish();
            });
        } else {
            setResult(RESULT_CANCELED);
            finish();
        }
    }

    private void showColorMenu() {
        if (webViewRich == null) return;
        View anchor = findViewById(R.id.btnColor);
        if (anchor == null) return;
        PrimePopupMenu.create(this, anchor)
                .addItem("Красный", R.drawable.ic_format_color, false, () -> { webViewRich.evaluateJavascript("document.execCommand('foreColor', false, '#FF5252');", null); return kotlin.Unit.INSTANCE; })
                .addItem("Синий", R.drawable.ic_format_color, false, () -> { webViewRich.evaluateJavascript("document.execCommand('foreColor', false, '#448AFF');", null); return kotlin.Unit.INSTANCE; })
                .addItem("Зеленый", R.drawable.ic_format_color, false, () -> { webViewRich.evaluateJavascript("document.execCommand('foreColor', false, '#69F0AE');", null); return kotlin.Unit.INSTANCE; })
                .addItem("Желтый", R.drawable.ic_format_color, false, () -> { webViewRich.evaluateJavascript("document.execCommand('foreColor', false, '#FFD700');", null); return kotlin.Unit.INSTANCE; })
                .addItem("Сбросить цвет", R.drawable.ic_cancel, false, () -> { webViewRich.evaluateJavascript("document.execCommand('removeFormat', false, null);", null); return kotlin.Unit.INSTANCE; })
                .show();
    }

    private void showImageSizeDialog(Uri uri) {
        String[] sizes = {"Маленькое (30%)", "Среднее (60%)", "Крупное (100%)"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Выберите размер фото")
                .setItems(sizes, (dialog, which) -> {
                    String widthStyle;
                    switch (which) {
                        case 0: widthStyle = "width:30%;"; break;
                        case 1: widthStyle = "width:60%;"; break;
                        default: widthStyle = "width:100%;"; break;
                    }
                    String jsImg = "<img src=\\'" + uri.toString() + "\\' style=\\'" + widthStyle + " max-width:100%; height:auto; border-radius:8px; display:block; margin:8px 0;\\' />";
                    if (webViewRich != null) {
                        webViewRich.evaluateJavascript("document.execCommand('insertHTML', false, '" + jsImg + "');", null);
                    }
                })
                .show();
    }

    @Override
    public void onActionModeStarted(android.view.ActionMode mode) {
        super.onActionModeStarted(mode);
        android.view.Menu menu = mode.getMenu();
        if (menu != null) {
            menu.add(0, 101, 0, "Жирный").setOnMenuItemClickListener(item -> {
                if (webViewRich != null) webViewRich.evaluateJavascript("document.execCommand('bold', false, null);", null);
                return true;
            });
            menu.add(0, 102, 1, "Курсив").setOnMenuItemClickListener(item -> {
                if (webViewRich != null) webViewRich.evaluateJavascript("document.execCommand('italic', false, null);", null);
                return true;
            });
            menu.add(0, 103, 2, "Зачеркнутый").setOnMenuItemClickListener(item -> {
                if (webViewRich != null) webViewRich.evaluateJavascript("document.execCommand('strikeThrough', false, null);", null);
                return true;
            });
        }
    }

    private void insertHeading() {
        if (webViewRich == null) return;
        View anchor = findViewById(R.id.btnHeading);
        if (anchor == null) return;
        PrimePopupMenu.create(this, anchor)
                .addItem("Заголовок 1 (Крупный)", R.drawable.ic_format_heading, false, () -> { webViewRich.evaluateJavascript("document.execCommand('formatBlock', false, 'H1');", null); return kotlin.Unit.INSTANCE; })
                .addItem("Заголовок 2", R.drawable.ic_format_heading, false, () -> { webViewRich.evaluateJavascript("document.execCommand('formatBlock', false, 'H2');", null); return kotlin.Unit.INSTANCE; })
                .addItem("Заголовок 3 (Средний)", R.drawable.ic_format_heading, false, () -> { webViewRich.evaluateJavascript("document.execCommand('formatBlock', false, 'H3');", null); return kotlin.Unit.INSTANCE; })
                .addItem("Заголовок 4 (Мелкий)", R.drawable.ic_format_heading, false, () -> { webViewRich.evaluateJavascript("document.execCommand('formatBlock', false, 'H4');", null); return kotlin.Unit.INSTANCE; })
                .show();
    }

    private void showTextStylesMenu() {
        if (webViewRich == null) return;
        View anchor = findViewById(R.id.btnBold);
        if (anchor == null) return;
        PrimePopupMenu.create(this, anchor)
                .addItem("Жирный", R.drawable.ic_format_bold, false, () -> { webViewRich.evaluateJavascript("document.execCommand('bold', false, null);", null); return kotlin.Unit.INSTANCE; })
                .addItem("Курсив", R.drawable.ic_format_extended, false, () -> { webViewRich.evaluateJavascript("document.execCommand('italic', false, null);", null); return kotlin.Unit.INSTANCE; })
                .addItem("Зачеркнутый", R.drawable.ic_format_extended, false, () -> { webViewRich.evaluateJavascript("document.execCommand('strikeThrough', false, null);", null); return kotlin.Unit.INSTANCE; })
                .addItem("Код (Моноширинный)", R.drawable.ic_file, false, () -> { webViewRich.evaluateJavascript("document.execCommand('formatBlock', false, 'PRE');", null); return kotlin.Unit.INSTANCE; })
                .show();
    }

    private void showListMenu() {
        if (webViewRich == null) return;
        View anchor = findViewById(R.id.btnList);
        if (anchor == null) return;
        PrimePopupMenu.create(this, anchor)
                .addItem("Маркированный список", R.drawable.ic_format_list_bulleted, false, () -> { webViewRich.evaluateJavascript("document.execCommand('insertUnorderedList', false, null);", null); return kotlin.Unit.INSTANCE; })
                .addItem("Нумерованный список", R.drawable.ic_format_list_bulleted, false, () -> { webViewRich.evaluateJavascript("document.execCommand('insertOrderedList', false, null);", null); return kotlin.Unit.INSTANCE; })
                .show();
    }

    private void showAlignMenu() {
        if (webViewRich == null) return;
        View anchor = findViewById(R.id.btnAlign);
        if (anchor == null) return;
        PrimePopupMenu.create(this, anchor)
                .addItem("По левому краю", R.drawable.ic_format_align_left, false, () -> { webViewRich.evaluateJavascript("document.execCommand('justifyLeft', false, null);", null); return kotlin.Unit.INSTANCE; })
                .addItem("По центру", R.drawable.ic_format_align_left, false, () -> { webViewRich.evaluateJavascript("document.execCommand('justifyCenter', false, null);", null); return kotlin.Unit.INSTANCE; })
                .addItem("По правому краю", R.drawable.ic_format_align_left, false, () -> { webViewRich.evaluateJavascript("document.execCommand('justifyRight', false, null);", null); return kotlin.Unit.INSTANCE; })
                .show();
    }

    private void showTableMenu() {
        View anchor = findViewById(R.id.btnTable);
        if (anchor == null) return;
        PrimePopupMenu.create(this, anchor)
                .addItem("Создать таблицу", R.drawable.ic_format_table, false, () -> { insertTable(); return kotlin.Unit.INSTANCE; })
                .show();
    }

    private void insertTable() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Размер таблицы");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText rowsInput = new EditText(this);
        rowsInput.setHint("Количество строк");
        rowsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        rowsInput.setText("2");
        layout.addView(rowsInput);

        final EditText colsInput = new EditText(this);
        colsInput.setHint("Количество столбцов");
        colsInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        colsInput.setText("2");
        layout.addView(colsInput);

        builder.setView(layout);
        builder.setPositiveButton("Вставить", (dialog, which) -> {
            int rows = 2;
            int cols = 2;
            try {
                rows = Integer.parseInt(rowsInput.getText().toString());
                cols = Integer.parseInt(colsInput.getText().toString());
            } catch (NumberFormatException ignored) {}

            rows = Math.max(1, Math.min(rows, 10));
            cols = Math.max(1, Math.min(cols, 10));

            StringBuilder table = new StringBuilder("<table border='1' style='width:100%; border-collapse:collapse; margin-bottom:10px;'>");
            for (int r = 1; r <= rows; r++) {
                table.append("<tr>");
                for (int c = 1; c <= cols; c++) {
                    if (r == 1) {
                        table.append("<th style='padding:5px;'>Заголовок ").append(c).append("</th>");
                    } else {
                        table.append("<td style='padding:5px;'>Ячейка ").append(r).append("-").append(c).append("</td>");
                    }
                }
                table.append("</tr>");
            }
            table.append("</table><br>");
            
            String jsTable = table.toString().replace("'", "\\'");
            webViewRich.evaluateJavascript("document.execCommand('insertHTML', false, '" + jsTable + "');", null);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
}