package ru.zarabotok.calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import org.json.JSONArray;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(0, 153, 0); // HSV(120, 100%, 60%)
    private static final int DARK = Color.rgb(242, 242, 242);
    private static final int MUTED = Color.rgb(155, 155, 155);
    private static final int BG = Color.rgb(16, 16, 16);
    private static final int BORDER = Color.rgb(76, 76, 76);
    private static final int CELL = Color.rgb(51, 51, 51);
    private static final int INPUT_BG = Color.BLACK;
    private AlertDialog photosDialog;
    private LinearLayout photosList;
    private String photosDialogDate;
    private SharedPreferences photoPrefs;
    private final String[] rateKeys = {"rate_glass", "rate_pet", "rate_km", "rate_litre", "rate_kegs", "rate_fixed"};
    private final String[] rateNames = {"Стекло", "Пэт", "Км", "Литр", "Кеги", "Фиксированная сумма за день"};
    private final String[] rateUnits = {"₽ за единицу", "₽ за единицу", "₽ за единицу", "₽ за единицу", "₽ за единицу", "₽ за записанный день"};
    private final double[] defaultRates = {2.0, 0.15, 1.5, 0.55, 16.0, 300.0};
    private SharedPreferences prefs;
    private JSONObject entries;
    private LinearLayout root, content;
    private LocalDate shownMonth;
    private String pendingPhotoDate;
    private final NumberFormat money = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
    private final DateTimeFormatter keyFormat = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru", "RU"));

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("earnings_data", MODE_PRIVATE);
        photoPrefs = getSharedPreferences("day_photos", MODE_PRIVATE);
        shownMonth = LocalDate.now().withDayOfMonth(1);
        money.setMaximumFractionDigits(2);
        money.setMinimumFractionDigits(0);
        try { entries = new JSONObject(prefs.getString("entries", "{}")); }
        catch (JSONException e) { entries = new JSONObject(); }
        buildShell(); showCalendar();
    }

    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(8), dp(6), dp(8), dp(5));
        root.addView(text("Мой заработок", 20, DARK, true));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 0, 1); cp.topMargin = dp(12); root.addView(content, cp);
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(0, dp(8), 0, dp(2));
        Button calendar = button("Календарь"), settings = button("Настройки");
        calendar.setOnClickListener(v -> showCalendar()); settings.setOnClickListener(v -> showSettings());
        nav.addView(calendar, new LinearLayout.LayoutParams(0, dp(42), 1));
        nav.addView(settings, new LinearLayout.LayoutParams(0, dp(42), 1)); root.addView(nav); setContentView(root);
    }

    private void showCalendar() {
        content.removeAllViews();
        LinearLayout monthBar = new LinearLayout(this); monthBar.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = smallButton("‹"), next = smallButton("›");
        TextView monthTitle = text(capitalize(shownMonth.format(monthFormat)), 16, DARK, true); monthTitle.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> { shownMonth = shownMonth.minusMonths(1); showCalendar(); });
        next.setOnClickListener(v -> { shownMonth = shownMonth.plusMonths(1); showCalendar(); });
        monthBar.addView(prev, new LinearLayout.LayoutParams(dp(36), dp(36)));
        monthBar.addView(monthTitle, new LinearLayout.LayoutParams(0, -2, 1));
        monthBar.addView(next, new LinearLayout.LayoutParams(dp(36), dp(36))); content.addView(monthBar);

        

        GridLayout grid = new GridLayout(this); grid.setColumnCount(7); grid.setUseDefaultMargins(false);
        for (String wd : new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"}) {
            TextView w = text(wd, 9, MUTED, true); w.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width = 0; p.height = dp(18); p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); grid.addView(w, p);
        }
        YearMonth ym = YearMonth.from(shownMonth); int offset = ym.atDay(1).getDayOfWeek().getValue() - 1;
        int days = ym.lengthOfMonth(), cells = ((offset + days + 6) / 7) * 7;
        for (int cell = 0; cell < cells; cell++) {
            int dn = cell - offset + 1; GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0; p.height = dp(52); p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); p.setMargins(dp(1), dp(1), dp(1), dp(1));
            if (dn < 1 || dn > days) { TextView blank = new TextView(this); blank.setBackground(roundRect(CELL, CELL, 0, 5)); grid.addView(blank, p); continue; }
            LocalDate date = ym.atDay(dn); String key = date.format(keyFormat); boolean has = entries.has(key);
            LinearLayout dayBox = new LinearLayout(this); dayBox.setOrientation(LinearLayout.VERTICAL); dayBox.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL); dayBox.setPadding(dp(2), dp(4), dp(2), dp(3));
            dayBox.setBackground(roundRect(CELL, date.equals(LocalDate.now()) ? GREEN : CELL, date.equals(LocalDate.now()) ? 1 : 0, 5));
            dayBox.addView(text(String.valueOf(dn), 11, DARK, true));
            TextView amount = text(has ? compactMoney(totalForDate(date)) : "", 8, GREEN, true); amount.setGravity(Gravity.CENTER); dayBox.addView(amount);
            if (photoCount(key) > 0) { TextView ph = text("▣ " + photoCount(key), 7, DARK, false); ph.setGravity(Gravity.CENTER); dayBox.addView(ph); }
            if (date.getDayOfWeek().getValue() == 7) { TextView week = text("Нед. " + compactMoney(totalForWeek(date)), 7, GREEN, true); week.setGravity(Gravity.CENTER); dayBox.addView(week); }
            if (dn == days) { TextView month = text("Мес. " + compactMoney(totalForMonth(ym)), 7, GREEN, true); month.setGravity(Gravity.CENTER); dayBox.addView(month); }
            dayBox.setOnClickListener(v -> showEntryDialog(date)); grid.addView(dayBox, p);
        }
        ScrollView scroll = new ScrollView(this); scroll.addView(grid); content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout summary = new LinearLayout(this); summary.setOrientation(LinearLayout.VERTICAL); summary.setPadding(dp(4),dp(5),dp(4),dp(2)); summary.addView(text("Заработано за месяц",11,MUTED,false)); summary.addView(text(formatMoney(totalForMonth(YearMonth.from(shownMonth)))+" ₽",19,GREEN,true)); content.addView(summary);
        TextView hint = text("Нажми на день, чтобы внести показатели или посмотреть документы.", 10, MUTED, false); hint.setPadding(0, dp(3), 0, 0); content.addView(hint);
    }

    private void showEntryDialog(LocalDate date) {
        String key = date.format(keyFormat); JSONObject old = entries.optJSONObject(key);
        String[] keys = {"glass", "pet", "km", "litre", "kegs"}; String[] labels = {"Стекло", "Пэт", "Км", "Литр", "Кеги"};
        double[] rates = getRates(); EditText[] fields = new EditText[5];
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), dp(4));
        TextView dateLabel = text(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru", "RU"))), 16, Color.BLACK, true);
        dateLabel.setGravity(Gravity.CENTER_VERTICAL); dateLabel.setPadding(dp(12), 0, dp(12), 0); dateLabel.setBackground(roundRect(Color.WHITE, Color.WHITE, 1, 5));
        form.addView(dateLabel, new LinearLayout.LayoutParams(-1, dp(42)));
        for (int i = 0; i < fields.length; i++) {
            form.addView(text(labels[i] + " · " + formatMoney(rates[i]) + " ₽ за единицу", 14, DARK, true));
            EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            input.setTextColor(Color.WHITE); input.setHintTextColor(Color.rgb(170,170,170)); input.setBackground(roundRect(INPUT_BG, BORDER, 1, 5)); input.setPadding(dp(10), 0, dp(10), 0);
            double value = old == null ? 0 : old.optDouble(keys[i], 0); input.setText(value == 0 ? "" : numberForInput(value)); input.setHint("Количество");
            form.addView(input, new LinearLayout.LayoutParams(-1, dp(48))); fields[i] = input;
        }
        form.addView(text("Фиксированная сумма при сохранении: " + formatMoney(rates[5]) + " ₽", 13, GREEN, true));
        Button photoButton = button("Фотографии документов (" + photoCount(key) + ")"); form.addView(photoButton, new LinearLayout.LayoutParams(-1, dp(44)));
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru", "RU"))))
                .setView(scroll).setPositiveButton("Сохранить", null).setNegativeButton("Отмена", null)
                .setNeutralButton(old == null ? " " : "Удалить запись", null).create();
        photoButton.setOnClickListener(v -> { dialog.dismiss(); showPhotos(key); });
        dialog.setOnShowListener(v -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                JSONObject entry = new JSONObject(); boolean any = false;
                try {
                    for (int i = 0; i < fields.length; i++) {
                        double value; try { value = parseNumber(fields[i].getText().toString()); } catch (NumberFormatException ex) { fields[i].setError("Введи число"); return; }
                        if (value < 0) { fields[i].setError("Количество не может быть отрицательным"); return; }
                        if (value > 0) any = true; entry.put(keys[i], value);
                    }
                    if (!any) { entries.remove(key); saveEntries(); dialog.dismiss(); showCalendar(); Toast.makeText(this, "Пустой день: 0 ₽, без фиксированной суммы", Toast.LENGTH_LONG).show(); return; }
                    entries.put(key, entry); saveEntries(); dialog.dismiss(); showCalendar(); Toast.makeText(this, "Запись сохранена", Toast.LENGTH_SHORT).show();
                } catch (JSONException e) { Toast.makeText(this, "Не удалось сохранить запись", Toast.LENGTH_SHORT).show(); }
            });
            if (old == null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);
            else dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> new AlertDialog.Builder(this).setTitle("Удалить запись?")
                    .setMessage("Заработок будет удалён. Фотографии документов останутся.").setNegativeButton("Отмена", null).setPositiveButton("Удалить", (dd, ww) -> { entries.remove(key); saveEntries(); dialog.dismiss(); showCalendar(); }).show());
        }); dialog.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(roundRect(BG, BORDER, 1, 8));
    }

    private JSONArray getPhotos(String key) { try { return new JSONArray(photoPrefs.getString(key, "[]")); } catch (JSONException e) { return new JSONArray(); } }
    private int photoCount(String key) { return getPhotos(key).length(); }
    private void savePhotos(String key, JSONArray a) { photoPrefs.edit().putString(key, a.toString()).apply(); }
    private void showPhotos(String key) {
        photosDialogDate = key;
        // Закрываем предыдущий экземпляр, чтобы окна не накладывались друг на друга.
        if (photosDialog != null) {
            try { if (photosDialog.isShowing()) photosDialog.dismiss(); } catch (Exception ignored) { }
            photosDialog = null;
        }

        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(12), dp(8), dp(12), dp(8));
        list.setBackgroundColor(BG);

        Button add = button("＋ Добавить фото");
        list.addView(add, new LinearLayout.LayoutParams(-1, dp(46)));
        add.setOnClickListener(v -> {
            pendingPhotoDate = key;
            // Закрываем список перед открытием системного выбора файлов.
            if (photosDialog != null) photosDialog.dismiss();
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(i, 4102);
        });

        JSONArray a = getPhotos(key);
        int visible = 0;
        for (int n = 0; n < a.length(); n++) {
            final String path = a.optString(n, "");
            File f = new File(path);
            if (!f.exists()) continue;
            visible++;
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            ImageView im = new ImageView(this);
            im.setImageBitmap(decodePhoto(path, 240, 240));
            im.setScaleType(ImageView.ScaleType.CENTER_CROP);
            row.addView(im, new LinearLayout.LayoutParams(dp(72), dp(72)));
            TextView name = text(f.getName(), 10, Color.WHITE, false);
            name.setPadding(dp(8), 0, dp(4), 0);
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            Button open = smallButton("Открыть");
            row.addView(open, new LinearLayout.LayoutParams(dp(78), dp(42)));
            open.setOnClickListener(v -> viewPhoto(key, path));
            row.setOnClickListener(v -> viewPhoto(key, path));
            list.addView(row);
        }
        if (visible == 0) list.addView(text("Для этого дня фотографий пока нет.", 13, MUTED, false));

        ScrollView sc = new ScrollView(this);
        sc.setBackgroundColor(BG);
        sc.addView(list);
        TextView title = text("Документы · " + key, 18, Color.WHITE, true);
        title.setPadding(dp(22), dp(16), dp(18), dp(8));

        photosDialog = new AlertDialog.Builder(this)
                .setCustomTitle(title)
                .setView(sc)
                .setPositiveButton("Готово", null)
                .setNegativeButton("Назад", null)
                .create();
        photosDialog.setOnShowListener(v -> {
            photosDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w -> {
                String returnDate = photosDialogDate;
                photosDialog.dismiss(); photosDialog = null; photosList = null;
                showEntryDialog(LocalDate.parse(returnDate, keyFormat));
            });
            photosDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(w -> {
                String returnDate = photosDialogDate;
                photosDialog.dismiss(); photosDialog = null; photosList = null;
                showEntryDialog(LocalDate.parse(returnDate, keyFormat));
            });
            if (photosDialog.getWindow() != null) {
                photosDialog.getWindow().setBackgroundDrawable(roundRect(BG, BORDER, 1, 8));
            }
        });
        photosDialog.show();
        if (photosDialog.getWindow() != null) photosDialog.getWindow().setBackgroundDrawable(roundRect(BG, BORDER, 1, 8));
    }
    private void viewPhoto(String key,String path) {
        File f=new File(path);
        if(!f.exists()){showPhotos(key);return;}
        ImageView im=new ImageView(this); im.setImageBitmap(decodePhoto(path,1400,1800)); im.setAdjustViewBounds(true); im.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ScrollView sc=new ScrollView(this); sc.setBackgroundColor(Color.BLACK); sc.addView(im);
        AlertDialog photoView = new AlertDialog.Builder(this).setTitle("Фотография документа").setView(sc)
                .setNegativeButton("Назад", null).setNeutralButton("Удалить фото", null).create();
        photoView.setOnShowListener(v -> {
            photoView.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(w -> {
                photoView.dismiss(); showPhotos(key);
            });
            photoView.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(w -> {
            new AlertDialog.Builder(this).setTitle("Удалить фотографию?")
                    .setNegativeButton("Отмена", (dd,ww) -> { photoView.dismiss(); showPhotos(key); })
                    .setPositiveButton("Удалить", (dd,ww) -> {
                        JSONArray old=getPhotos(key), updated=new JSONArray();
                        for(int i=0;i<old.length();i++){String q=old.optString(i,"");if(!q.equals(path))updated.put(q);}
                        f.delete(); savePhotos(key,updated); photoView.dismiss(); showPhotos(key);
                    }).show();
            });
        });
        photoView.show();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 4102) return;
        String dateKey = pendingPhotoDate;
        if (resultCode == RESULT_OK && data != null && dateKey != null) {
            int count = 0;
            try {
                if (data.getClipData() != null) {
                    for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                        if (copyPhoto(data.getClipData().getItemAt(i).getUri(), dateKey)) count++;
                    }
                } else if (data.getData() != null && copyPhoto(data.getData(), dateKey)) {
                    count++;
                }
            } catch (Exception e) {
                Toast.makeText(this, "Не все фотографии удалось добавить", Toast.LENGTH_LONG).show();
            }
            Toast.makeText(this, "Добавлено фотографий: " + count, Toast.LENGTH_SHORT).show();
        }
        if (dateKey != null) showPhotos(dateKey);
    }
    private boolean copyPhoto(Uri uri,String key) {
        try(InputStream in=getContentResolver().openInputStream(uri)){ if(in==null)return false; File dir=new File(getFilesDir(),"day_photos/"+key); if(!dir.exists()&&!dir.mkdirs())return false; File f=new File(dir,"document_"+System.currentTimeMillis()+".img"); try(FileOutputStream out=new FileOutputStream(f)){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);} JSONArray a=getPhotos(key);a.put(f.getAbsolutePath());savePhotos(key,a);return true;}catch(Exception e){return false;}
    }
    private Bitmap decodePhoto(String path,int w,int h){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(path,o);int sample=1;while(o.outWidth/sample>w*2||o.outHeight/sample>h*2)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;return BitmapFactory.decodeFile(path,o);}

    private void showSettings() {
        content.removeAllViews(); content.addView(text("Настройки расчёта", 21, DARK, true));
        TextView desc = text("Новые тарифы применяются ко всем сохранённым дням, в том числе прошлым.", 13, MUTED, false); desc.setPadding(0, dp(6), 0, dp(14)); content.addView(desc);
        double[] rates = getRates(); EditText[] fields = new EditText[rates.length]; ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < rates.length; i++) {
            LinearLayout row = card(); row.addView(text(rateNames[i], 15, DARK, true)); row.addView(text(rateUnits[i], 12, MUTED, false));
            EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); input.setText(numberForInput(rates[i]));
            input.setTextColor(Color.WHITE); input.setHintTextColor(Color.rgb(170,170,170)); input.setBackground(roundRect(Color.BLACK, BORDER, 1, 5)); input.setPadding(dp(10), 0, dp(10), 0);
            row.addView(input, new LinearLayout.LayoutParams(-1, dp(48))); LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, -2); rp.bottomMargin = dp(8); form.addView(row, rp); fields[i] = input;
        }
        scroll.addView(form); content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        Button save = button("Сохранить настройки"); save.setOnClickListener(v -> {
            double[] updated = new double[fields.length];
            for (int i = 0; i < fields.length; i++) { try { updated[i] = parseNumber(fields[i].getText().toString()); } catch (NumberFormatException e) { fields[i].setError("Введи число"); return; } if (updated[i] < 0) { fields[i].setError("Значение не может быть отрицательным"); return; } }
            SharedPreferences.Editor editor = prefs.edit(); for (int i = 0; i < updated.length; i++) editor.putString(rateKeys[i], Double.toString(updated[i])); editor.apply();
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show(); showSettings();
        }); LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(50)); bp.topMargin = dp(8); content.addView(save, bp);
        Button reset = smallButton("Вернуть тарифы по умолчанию"); reset.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("Сбросить тарифы?").setMessage("Все коэффициенты вернутся к исходным значениям.")
                .setNegativeButton("Отмена", null).setPositiveButton("Сбросить", (d, w) -> { SharedPreferences.Editor editor = prefs.edit(); for (String k : rateKeys) editor.remove(k); editor.apply(); showSettings(); }).show()); content.addView(reset);
    }

    private double[] getRates() { double[] r = new double[defaultRates.length]; for (int i = 0; i < r.length; i++) { try { r[i] = Double.parseDouble(prefs.getString(rateKeys[i], Double.toString(defaultRates[i]))); } catch (NumberFormatException e) { r[i] = defaultRates[i]; } } return r; }
    private double totalForDate(LocalDate date) { JSONObject e = entries.optJSONObject(date.format(keyFormat)); if (e == null) return 0; double[] r = getRates(); return e.optDouble("glass",0)*r[0] + e.optDouble("pet",0)*r[1] + e.optDouble("km",0)*r[2] + e.optDouble("litre",0)*r[3] + e.optDouble("kegs",0)*r[4] + r[5]; }
    private double totalForWeek(LocalDate sunday) { double total = 0; LocalDate monday = sunday.minusDays(6); for (int i = 0; i < 7; i++) total += totalForDate(monday.plusDays(i)); return total; }
    private double totalForMonth(YearMonth month) { double total = 0; for (int d = 1; d <= month.lengthOfMonth(); d++) total += totalForDate(month.atDay(d)); return total; }
    private void saveEntries() { prefs.edit().putString("entries", entries.toString()).apply(); }
    private double parseNumber(String value) { if (value == null || value.trim().isEmpty()) return 0; return Double.parseDouble(value.trim().replace(',', '.')); }
    private String formatMoney(double value) { return money.format(value); }
    private String compactMoney(double value) { return Math.abs(value) >= 100000 ? String.format(Locale.getDefault(), "%.0fк", value / 1000.0) : formatMoney(value); }
    private String numberForInput(double value) { return value == Math.rint(value) ? Long.toString((long)value) : Double.toString(value); }
    private String capitalize(String value) { return value == null || value.isEmpty() ? value : value.substring(0,1).toUpperCase(new Locale("ru","RU")) + value.substring(1); }
    private TextView text(String value, int size, int color, boolean bold) { TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t; }
    private Button button(String label) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.BLACK); b.setBackground(roundRect(GREEN, GREEN, 1, 7)); return b; }
    private Button smallButton(String label) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(DARK); b.setBackground(roundRect(CELL, BORDER, 1, 6)); return b; }
    private LinearLayout card() { LinearLayout b = new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); b.setPadding(dp(12), dp(10), dp(12), dp(10)); b.setBackground(roundRect(CELL, BORDER, 1, 7)); return b; }
    private GradientDrawable roundRect(int color, int strokeColor, int strokeWidth, int radiusDp) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radiusDp)); if (strokeWidth > 0) d.setStroke(dp(strokeWidth), strokeColor); return d; }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
}
