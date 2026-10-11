package ru.zarabotok.calendar;

import android.app.Activity;
import android.app.AlertDialog;
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
    private static final int GREEN = Color.rgb(35, 134, 91);
    private static final int DARK = Color.rgb(35, 45, 40);
    private static final int MUTED = Color.rgb(112, 122, 116);
    private static final int BG = Color.rgb(246, 248, 246);
    private static final int BORDER = Color.rgb(222, 229, 223);
    private final String[] rateKeys = {"rate_glass", "rate_pet", "rate_km", "rate_litre", "rate_kegs", "rate_fixed"};
    private final String[] rateNames = {"Стекло", "Пэт", "Км", "Литр", "Кеги", "Фиксированная сумма за день"};
    private final String[] rateUnits = {"₽ за единицу", "₽ за единицу", "₽ за единицу", "₽ за единицу", "₽ за единицу", "₽ за записанный день"};
    private final double[] defaultRates = {2.0, 0.15, 1.5, 0.55, 16.0, 300.0};
    private SharedPreferences prefs;
    private JSONObject entries;
    private LinearLayout root, content;
    private LocalDate shownMonth;
    private final NumberFormat money = NumberFormat.getNumberInstance(new Locale("ru", "RU"));
    private final DateTimeFormatter keyFormat = DateTimeFormatter.ISO_LOCAL_DATE;
    private final DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("LLLL yyyy", new Locale("ru", "RU"));

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("earnings_data", MODE_PRIVATE);
        shownMonth = LocalDate.now().withDayOfMonth(1);
        money.setMaximumFractionDigits(2);
        money.setMinimumFractionDigits(0);
        try { entries = new JSONObject(prefs.getString("entries", "{}")); }
        catch (JSONException e) { entries = new JSONObject(); }
        buildShell(); showCalendar();
    }

    private void buildShell() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(12), dp(16), dp(8));
        root.addView(text("Мой заработок", 25, DARK, true));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, 0, 1); cp.topMargin = dp(12); root.addView(content, cp);
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(0, dp(8), 0, dp(2));
        Button calendar = button("Календарь"), settings = button("Настройки");
        calendar.setOnClickListener(v -> showCalendar()); settings.setOnClickListener(v -> showSettings());
        nav.addView(calendar, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(settings, new LinearLayout.LayoutParams(0, dp(48), 1)); root.addView(nav); setContentView(root);
    }

    private void showCalendar() {
        content.removeAllViews();
        LinearLayout monthBar = new LinearLayout(this); monthBar.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = smallButton("‹"), next = smallButton("›");
        TextView monthTitle = text(capitalize(shownMonth.format(monthFormat)), 18, DARK, true); monthTitle.setGravity(Gravity.CENTER);
        prev.setOnClickListener(v -> { shownMonth = shownMonth.minusMonths(1); showCalendar(); });
        next.setOnClickListener(v -> { shownMonth = shownMonth.plusMonths(1); showCalendar(); });
        monthBar.addView(prev, new LinearLayout.LayoutParams(dp(44), dp(44)));
        monthBar.addView(monthTitle, new LinearLayout.LayoutParams(0, -2, 1));
        monthBar.addView(next, new LinearLayout.LayoutParams(dp(44), dp(44))); content.addView(monthBar);

        LinearLayout summary = card(); summary.addView(text("Заработано за месяц", 13, MUTED, false));
        summary.addView(text(formatMoney(totalForMonth(YearMonth.from(shownMonth))) + " ₽", 26, GREEN, true));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2); sp.topMargin = dp(8); sp.bottomMargin = dp(12); content.addView(summary, sp);

        GridLayout grid = new GridLayout(this); grid.setColumnCount(7); grid.setUseDefaultMargins(false);
        for (String wd : new String[]{"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"}) {
            TextView w = text(wd, 12, MUTED, true); w.setGravity(Gravity.CENTER);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width = 0; p.height = dp(30); p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); grid.addView(w, p);
        }
        YearMonth ym = YearMonth.from(shownMonth); int offset = ym.atDay(1).getDayOfWeek().getValue() - 1;
        int days = ym.lengthOfMonth(), cells = ((offset + days + 6) / 7) * 7;
        for (int cell = 0; cell < cells; cell++) {
            int dn = cell - offset + 1; GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = 0; p.height = dp(78); p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); p.setMargins(dp(2), dp(2), dp(2), dp(2));
            if (dn < 1 || dn > days) { TextView blank = new TextView(this); blank.setBackground(roundRect(Color.WHITE, BORDER, 1, 8)); grid.addView(blank, p); continue; }
            LocalDate date = ym.atDay(dn); String key = date.format(keyFormat); boolean has = entries.has(key);
            LinearLayout dayBox = new LinearLayout(this); dayBox.setOrientation(LinearLayout.VERTICAL); dayBox.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL); dayBox.setPadding(dp(2), dp(4), dp(2), dp(3));
            dayBox.setBackground(roundRect(Color.WHITE, date.equals(LocalDate.now()) ? GREEN : BORDER, date.equals(LocalDate.now()) ? 2 : 1, 8));
            dayBox.addView(text(String.valueOf(dn), 13, DARK, true));
            TextView amount = text(has ? compactMoney(totalForDate(date)) : "—", 10, has ? GREEN : MUTED, true); amount.setGravity(Gravity.CENTER); dayBox.addView(amount);
            if (date.getDayOfWeek().getValue() == 7) { TextView week = text("Нед. " + compactMoney(totalForWeek(date)), 8, DARK, true); week.setGravity(Gravity.CENTER); dayBox.addView(week); }
            if (dn == days) { TextView month = text("Мес. " + compactMoney(totalForMonth(ym)), 8, GREEN, true); month.setGravity(Gravity.CENTER); dayBox.addView(month); }
            dayBox.setOnClickListener(v -> showEntryDialog(date)); grid.addView(dayBox, p);
        }
        ScrollView scroll = new ScrollView(this); scroll.addView(grid); content.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        TextView hint = text("Нажми на день, чтобы внести или изменить показатели.", 12, MUTED, false); hint.setPadding(0, dp(8), 0, 0); content.addView(hint);
    }

    private void showEntryDialog(LocalDate date) {
        String key = date.format(keyFormat); JSONObject old = entries.optJSONObject(key);
        String[] keys = {"glass", "pet", "km", "litre", "kegs"}; String[] labels = {"Стекло", "Пэт", "Км", "Литр", "Кеги"};
        double[] rates = getRates(); EditText[] fields = new EditText[5];
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20), dp(8), dp(20), dp(4));
        for (int i = 0; i < fields.length; i++) {
            form.addView(text(labels[i] + " · " + formatMoney(rates[i]) + " ₽ за единицу", 14, DARK, true));
            EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            double value = old == null ? 0 : old.optDouble(keys[i], 0); input.setText(value == 0 ? "" : numberForInput(value)); input.setHint("Количество");
            form.addView(input, new LinearLayout.LayoutParams(-1, dp(48))); fields[i] = input;
        }
        form.addView(text("Фиксированная сумма при сохранении: " + formatMoney(rates[5]) + " ₽", 13, GREEN, true));
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("ru", "RU"))))
                .setView(scroll).setPositiveButton("Сохранить", null).setNegativeButton("Отмена", null)
                .setNeutralButton(old == null ? " " : "Удалить запись", null).create();
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
                    .setMessage("Заработок за этот день будет удалён.").setNegativeButton("Отмена", null).setPositiveButton("Удалить", (dd, ww) -> { entries.remove(key); saveEntries(); dialog.dismiss(); showCalendar(); }).show());
        }); dialog.show();
    }

    private void showSettings() {
        content.removeAllViews(); content.addView(text("Настройки расчёта", 21, DARK, true));
        TextView desc = text("Новые тарифы применяются ко всем сохранённым дням, в том числе прошлым.", 13, MUTED, false); desc.setPadding(0, dp(6), 0, dp(14)); content.addView(desc);
        double[] rates = getRates(); EditText[] fields = new EditText[rates.length]; ScrollView scroll = new ScrollView(this);
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < rates.length; i++) {
            LinearLayout row = card(); row.addView(text(rateNames[i], 15, DARK, true)); row.addView(text(rateUnits[i], 12, MUTED, false));
            EditText input = new EditText(this); input.setSingleLine(true); input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL); input.setText(numberForInput(rates[i]));
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
    private Button button(String label) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setBackground(roundRect(GREEN, GREEN, 1, 12)); return b; }
    private Button smallButton(String label) { Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setTextColor(DARK); b.setBackground(roundRect(Color.WHITE, BORDER, 1, 10)); return b; }
    private LinearLayout card() { LinearLayout b = new LinearLayout(this); b.setOrientation(LinearLayout.VERTICAL); b.setPadding(dp(12), dp(10), dp(12), dp(10)); b.setBackground(roundRect(Color.WHITE, BORDER, 1, 12)); return b; }
    private GradientDrawable roundRect(int color, int strokeColor, int strokeWidth, int radiusDp) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radiusDp)); if (strokeWidth > 0) d.setStroke(dp(strokeWidth), strokeColor); return d; }
    private int dp(float value) { return (int)(value * getResources().getDisplayMetrics().density + 0.5f); }
}
