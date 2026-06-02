package com.mediremind.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mediremind.R;
import com.mediremind.models.Medicine;

import java.util.List;

public class MedicineVerificationAdapter
        extends RecyclerView.Adapter<MedicineVerificationAdapter.ViewHolder> {

    private final Context context;
    private final List<Medicine> medicines;

    private static final String[] FREQUENCY_OPTIONS = {
            "Select frequency", "daily", "morning_evening", "tds",
            "night_only", "every_14_days", "sos"
    };

    private static final String[] TIMING_OPTIONS = {
            "Select timing", "morning", "afternoon", "evening", "night",
            "morning_evening", "morning_afternoon_evening"
    };

    private static final String[] MEAL_OPTIONS = {
            "Select meal relation", "before_meal", "after_meal", "empty_stomach", "none"
    };

    public MedicineVerificationAdapter(Context context, List<Medicine> medicines) {
        this.context   = context;
        this.medicines = medicines;
    }

    public List<Medicine> getMedicines() { return medicines; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_medicine_verification, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Medicine m = medicines.get(position);

        // ── Index: plain number only, no emoji ──────────────────────────
        h.tvIndex.setText(String.valueOf(position + 1));
        // Type label as text (no emoji)
        h.tvTypeEmoji.setText(m.getMedicineType() != null ? m.getMedicineType() : "");

        h.etName.setText(m.getMedicineName());
        h.etStrength.setText(m.getStrength());
        h.etDose.setText(m.getDoseQuantity() > 0 ? String.valueOf(m.getDoseQuantity()) : "");

        // Duration: only show if AI actually extracted a value — never default to 7
        String dur = m.getDurationDays();
        h.etDuration.setText((dur != null && !dur.isEmpty() && !dur.equals("0")) ? dur : "");

        // Special instruction: show raw text; if Urdu it displays as-is
        String instr = m.getSpecialInstruction();
        h.etSpecialInstruction.setText(instr != null ? instr : "");

        // ── Confidence indicator ─────────────────────────────────────────
        double conf = m.getConfidence();
        if (conf >= 0.8) {
            h.ivConfidence.setImageResource(R.drawable.ic_confidence_high);
            h.tvConfidence.setText("High confidence");
            h.tvConfidence.setTextColor(context.getResources().getColor(R.color.success, null));
        } else if (conf >= 0.5) {
            h.ivConfidence.setImageResource(R.drawable.ic_confidence_medium);
            h.tvConfidence.setText("Medium confidence");
            h.tvConfidence.setTextColor(context.getResources().getColor(R.color.warning, null));
        } else {
            h.ivConfidence.setImageResource(R.drawable.ic_warning);
            h.tvConfidence.setText("Low confidence — review needed");
            h.tvConfidence.setTextColor(context.getResources().getColor(R.color.destructive, null));
        }

        // ── Highlight card if needs user input ───────────────────────────
        if (m.isNeedsUserInput() || conf < 0.5) {
            h.cardRoot.setCardBackgroundColor(
                    context.getResources().getColor(R.color.warning_light, null));
        } else {
            h.cardRoot.setCardBackgroundColor(
                    context.getResources().getColor(R.color.card, null));
        }

        // ── Spinners ─────────────────────────────────────────────────────
        setupSpinner(context, h.spinnerFrequency, FREQUENCY_OPTIONS,
                m.getFrequencyType(), selected -> {
                    m.setFrequencyType("Select frequency".equals(selected) ? null : selected);
                    m.setNeedsUserInput(m.getFrequencyType() == null);
                });

        setupSpinner(context, h.spinnerTiming, TIMING_OPTIONS,
                m.getTimingHint(), selected ->
                    m.setTimingHint("Select timing".equals(selected) ? null : selected));

        setupSpinner(context, h.spinnerMeal, MEAL_OPTIONS,
                m.getMealRelation(), selected -> {
                    String val = ("Select meal relation".equals(selected) || "none".equals(selected))
                            ? null : selected;
                    m.setMealRelation(val);
                });

        // ── Text watchers ─────────────────────────────────────────────────
        h.etName.addTextChangedListener(new SimpleTextWatcher(m::setMedicineName));
        h.etStrength.addTextChangedListener(new SimpleTextWatcher(m::setStrength));
        h.etDose.addTextChangedListener(new SimpleTextWatcher(text -> {
            try { m.setDoseQuantity(Integer.parseInt(text)); }
            catch (NumberFormatException ignored) {}
        }));
        h.etDuration.addTextChangedListener(new SimpleTextWatcher(m::setDurationDays));
        h.etSpecialInstruction.addTextChangedListener(
                new SimpleTextWatcher(m::setSpecialInstruction));
    }

    private void setupSpinner(Context ctx, Spinner spinner, String[] options,
                               String currentValue, OnSpinnerSelected callback) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_spinner_item, options);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        if (currentValue != null) {
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(currentValue)) {
                    spinner.setSelection(i);
                    break;
                }
            }
        }

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                callback.onSelected(options[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public int getItemCount() { return medicines.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardRoot;
        TextView tvIndex, tvTypeEmoji, tvConfidence;
        ImageView ivConfidence;
        EditText etName, etStrength, etDose, etDuration, etSpecialInstruction;
        Spinner spinnerFrequency, spinnerTiming, spinnerMeal;

        ViewHolder(View v) {
            super(v);
            cardRoot             = v.findViewById(R.id.cardRoot);
            tvIndex              = v.findViewById(R.id.tvIndex);
            tvTypeEmoji          = v.findViewById(R.id.tvTypeEmoji);
            tvConfidence         = v.findViewById(R.id.tvConfidence);
            ivConfidence         = v.findViewById(R.id.ivConfidence);
            etName               = v.findViewById(R.id.etName);
            etStrength           = v.findViewById(R.id.etStrength);
            etDose               = v.findViewById(R.id.etDose);
            etDuration           = v.findViewById(R.id.etDuration);
            etSpecialInstruction = v.findViewById(R.id.etSpecialInstruction);
            spinnerFrequency     = v.findViewById(R.id.spinnerFrequency);
            spinnerTiming        = v.findViewById(R.id.spinnerTiming);
            spinnerMeal          = v.findViewById(R.id.spinnerMeal);
        }
    }

    interface OnSpinnerSelected {
        void onSelected(String value);
    }

    static class SimpleTextWatcher implements TextWatcher {
        interface OnChanged { void onChange(String text); }
        private final OnChanged listener;
        SimpleTextWatcher(OnChanged listener) { this.listener = listener; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            listener.onChange(s.toString().trim());
        }
        @Override public void afterTextChanged(Editable s) {}
    }
}
