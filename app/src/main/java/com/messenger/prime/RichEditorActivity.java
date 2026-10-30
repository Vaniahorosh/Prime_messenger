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
        transform.addTarget(R.id.layoutRichRoot);
        transform.setDuration(350);
        transform.setAllContainerColors(ContextCompat.getColor(this, R.color.prime_base));
        getWindow().setSharedElementEnterTransition(transform);
        getWindow().setSharedElementReturnTransition(transform);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_rich_editor);
        setupBlurViews();

        View root = findViewById(R.id.layoutRichRoot);
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
                int bottom = Math.max(systemBars.bottom, ime.bottom);
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottom);
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

        View layoutFloatingToolbar = findViewById(R.id.layoutFloatingToolbar);
        View btnToggleToolbar = findViewById(R.id.btnToggleToolbar);
        if (btnToggleToolbar != null && layoutFloatingToolbar != null) {
            btnToggleToolbar.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                if (layoutFloatingToolbar.getVisibility() == View.VISIBLE) {
                    layoutFloatingToolbar.animate().cancel();
                    layoutFloatingToolbar.animate()
                            .alpha(0f)
                            .scaleY(0f)
                            .setDuration(180)
                            .withEndAction(() -> layoutFloatingToolbar.setVisibility(View.GONE))
                            .start();
                } else {
                    layoutFloatingToolbar.setVisibility(View.VISIBLE);
                    layoutFloatingToolbar.setAlpha(0f);
                    layoutFloatingToolbar.setScaleY(0f);
                    layoutFloatingToolbar.animate().cancel();
                    layoutFloatingToolbar.animate()
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
            findViewById(R.id.btnCancel).setOnClickListener(v -> attemptExit());
        }

        // Attach Slidr for left-to-right swipe dismiss only
        com.r0adkll.slidr.Slidr.attach(this, new com.r0adkll.slidr.model.SlidrConfig.Builder()
                .position(com.r0adkll.slidr.model.SlidrPosition.LEFT)
                .edge(false)
                .listener(new com.r0adkll.slidr.model.SlidrListener() {
                    @Override
                    public void onSlideStateChanged(int state) {}
                    @Override
                    public void onSlideChange(float percent) {}
                    @Override
                    public void onSlideOpened() {}
                    @Override
                    public boolean onSlideClosed() {
                        attemptExit();
                        return true;
                    }
                })
                .build());

        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptExit();
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
                supportFinishAfterTransition();
            }));
        }
    }

    @Override
    protected void onDestroy() {
        if (webViewRich != null) {
            webViewRich.loadUrl("about:blank");
            webViewRich.stopLoading();
            webViewRich.setWebChromeClient(null);
            webViewRich.destroy();
            webViewRich = null;
        }
        super.onDestroy();
    }

    private void setupBlurViews() {
        ViewGroup rootView = getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView == null) rootView = (ViewGroup) getWindow().getDecorView();

        boolean isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        Drawable windowBg = getWindow().getDecorView().getBackground();
        int overlayColor = isDark ? Color.parseColor("#400F172A") : Color.parseColor("#40154B87");

        eightbitlab.com.blurview.BlurView blurHeader = findViewById(R.id.headerRich);
        eightbitlab.com.blurview.BlurView blurToolbar = findViewById(R.id.layoutFloatingToolbar);

        eightbitlab.com.blurview.BlurView[] blurViews = new eightbitlab.com.blurview.BlurView[]{ blurHeader, blurToolbar };

        for (eightbitlab.com.blurview.BlurView bv : blurViews) {
            if (bv != null) {
                bv.setClipToOutline(true);
                BlurViewKt.setupBlur(bv, rootView, 16f, overlayColor, windowBg);
            }
        }
    }

    @android.annotation.SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        if (webViewRich == null) return;
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

        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        String textColor = isDark ? "#E0E0E0" : "#121212";
        String bgColor = "transparent";
        String tableBg = isDark ? "#262626" : "#F5F5F5";
        String thBg = isDark ? "#333333" : "#E0E0E0";
        String borderColor = isDark ? "#444444" : "#CCCCCC";
        String blockquoteColor = isDark ? "#00E676" : "#00C853";

        String htmlTemplate = "<html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1, maximum-scale=1.0, user-scalable=no\">" +
                "<style>" +
                "body { margin: 0; padding: 16px; font-family: sans-serif; font-size: 16px; color: " + textColor + "; background-color: " + bgColor + "; }" +
                "#editor { min-height: 100vh; outline: none; line-height: 1.4; }" +
                "img { max-width: 100%; height: auto; display: block; border-radius: 8px; margin: 8px 0; border: 1px solid " + borderColor + "; }" +
                "table { width: 100%; border-collapse: collapse; margin: 10px 0; background-color: " + tableBg + "; border-radius: 6px; overflow: hidden; }" +
                "th, td { border: 1px solid " + borderColor + "; padding: 8px; text-align: left; }" +
                "th { background-color: " + thBg + "; font-weight: bold; }" +
                "blockquote { border-left: 4px solid " + blockquoteColor + "; margin: 0; padding-left: 10px; color: #BBB; font-style: italic; }" +
                "pre { background-color: " + tableBg + "; padding: 10px; border-radius: 6px; overflow-x: auto; }" +
                "code { font-family: monospace; background-color: " + tableBg + "; padding: 2px 4px; border-radius: 4px; }" +
                "[placeholder]:empty:before { content: attr(placeholder); color: #888; pointer-events: none; display: block; }" +
                "</style></head>" +
                "<body><div id=\"editor\" contenteditable=\"true\" placeholder=\"Напишите текст с форматированием...\"></div>" +
                "<script>" +
                "document.execCommand('defaultParagraphSeparator', false, 'br');" +
                "</script>" +
                "</body></html>";
        webViewRich.loadDataWithBaseURL("file:///android_asset/", htmlTemplate, "text/html", "UTF-8", null);
    }

    private void attemptExit() {
        if (webViewRich == null) {
            setResult(RESULT_CANCELED);
            supportFinishAfterTransition();
            return;
        }

        webViewRich.evaluateJavascript("document.getElementById('editor').innerText", value -> {
            String text = value != null ? value.replaceAll("^\"|\"$", "").replace("\\n", "").trim() : "";
            if (!text.isEmpty() && !text.equals("null")) {
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(RichEditorActivity.this)
                        .setTitle("Выйти без сохранения?")
                        .setMessage("Введенный текст и форматирование будут утеряны.")
                        .setPositiveButton("Выйти", (dialog, which) -> {
                            setResult(RESULT_CANCELED);
                            supportFinishAfterTransition();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            } else {
                setResult(RESULT_CANCELED);
                supportFinishAfterTransition();
            }
        });
    }

    private void showColorMenu() {
        if (webViewRich == null) return;
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, findViewById(R.id.btnColor));
        popupMenu.getMenu().add(0, 1, 0, "Красный");
        popupMenu.getMenu().add(0, 2, 0, "Синий");
        popupMenu.getMenu().add(0, 3, 0, "Зеленый");
        popupMenu.getMenu().add(0, 4, 0, "Желтый");
        popupMenu.getMenu().add(0, 5, 0, "Сбросить цвет");
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: webViewRich.evaluateJavascript("document.execCommand('foreColor', false, '#FF5252');", null); break;
                case 2: webViewRich.evaluateJavascript("document.execCommand('foreColor', false, '#448AFF');", null); break;
                case 3: webViewRich.evaluateJavascript("document.execCommand('foreColor', false, '#69F0AE');", null); break;
                case 4: webViewRich.evaluateJavascript("document.execCommand('foreColor', false, '#FFD700');", null); break;
                case 5: webViewRich.evaluateJavascript("document.execCommand('removeFormat', false, null);", null); break;
            }
            return true;
        });
        popupMenu.show();
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
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, findViewById(R.id.btnHeading));
        popupMenu.getMenu().add(0, 1, 0, "Заголовок 1 (Крупный)");
        popupMenu.getMenu().add(0, 2, 0, "Заголовок 2");
        popupMenu.getMenu().add(0, 3, 0, "Заголовок 3 (Средний)");
        popupMenu.getMenu().add(0, 4, 0, "Заголовок 4 (Мелкий)");
        popupMenu.setOnMenuItemClickListener(item -> {
            String command = "document.execCommand('formatBlock', false, 'H" + item.getItemId() + "');";
            webViewRich.evaluateJavascript(command, null);
            return true;
        });
        popupMenu.show();
    }

    private void showTextStylesMenu() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, findViewById(R.id.btnBold));
        popupMenu.getMenu().add(0, 1, 0, "Жирный");
        popupMenu.getMenu().add(0, 2, 0, "Курсив");
        popupMenu.getMenu().add(0, 3, 0, "Зачеркнутый");
        popupMenu.getMenu().add(0, 4, 0, "Код (Моноширинный)");
        popupMenu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1: webViewRich.evaluateJavascript("document.execCommand('bold', false, null);", null); break;
                case 2: webViewRich.evaluateJavascript("document.execCommand('italic', false, null);", null); break;
                case 3: webViewRich.evaluateJavascript("document.execCommand('strikeThrough', false, null);", null); break;
                case 4: webViewRich.evaluateJavascript("document.execCommand('formatBlock', false, 'PRE');", null); break;
            }
            return true;
        });
        popupMenu.show();
    }

    private void showListMenu() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, findViewById(R.id.btnList));
        popupMenu.getMenu().add(0, 1, 0, "Маркированный список");
        popupMenu.getMenu().add(0, 2, 0, "Нумерованный список");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                webViewRich.evaluateJavascript("document.execCommand('insertUnorderedList', false, null);", null);
            } else {
                webViewRich.evaluateJavascript("document.execCommand('insertOrderedList', false, null);", null);
            }
            return true;
        });
        popupMenu.show();
    }

    private void showAlignMenu() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, findViewById(R.id.btnAlign));
        popupMenu.getMenu().add(0, 1, 0, "По левому краю");
        popupMenu.getMenu().add(0, 2, 0, "По центру");
        popupMenu.getMenu().add(0, 3, 0, "По правому краю");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) webViewRich.evaluateJavascript("document.execCommand('justifyLeft', false, null);", null);
            else if (item.getItemId() == 2) webViewRich.evaluateJavascript("document.execCommand('justifyCenter', false, null);", null);
            else if (item.getItemId() == 3) webViewRich.evaluateJavascript("document.execCommand('justifyRight', false, null);", null);
            return true;
        });
        popupMenu.show();
    }

    private void showTableMenu() {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, findViewById(R.id.btnTable));
        popupMenu.getMenu().add(0, 1, 0, "Создать таблицу (Выбор размера)");
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == 1) {
                insertTable();
            }
            return true;
        });
        popupMenu.show();
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