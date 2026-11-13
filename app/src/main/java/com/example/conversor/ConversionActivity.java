package com.example.conversor;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.conversor.ConversionEngine.ConversionType;
import com.google.android.material.snackbar.Snackbar;

import java.text.NumberFormat;
import java.util.Locale;

public class ConversionActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSION_TYPE =
            "com.example.conversor.EXTRA_CONVERSION_TYPE";

    private ConversionType selectedType;
    private Spinner spinnerConverter;
    private Spinner spinnerConverted;
    private EditText editTextConverter;
    private TextView textViewResult;
    private TextView textViewCurrencyNotice;
    private boolean negativeWarningShown;
    private boolean temperatureWarningShown;
    private boolean resultRangeWarningShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_conversion);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String typeName = getIntent().getStringExtra(EXTRA_CONVERSION_TYPE);
        try {
            selectedType = ConversionType.valueOf(typeName);
        } catch (IllegalArgumentException | NullPointerException exception) {
            finish();
            return;
        }

        TextView title = findViewById(R.id.textViewCategoryTitle);
        title.setText(selectedType.titleResource());

        findViewById(R.id.buttonBack).setOnClickListener(view -> finish());
        spinnerConverter = findViewById(R.id.spinnerConverter);
        spinnerConverted = findViewById(R.id.spinnerConverted);
        editTextConverter = findViewById(R.id.editTextConverter);
        textViewResult = findViewById(R.id.textViewResult);
        textViewCurrencyNotice = findViewById(R.id.textViewCurrencyNotice);
        setupCategory(selectedType);

        AdapterView.OnItemSelectedListener unitSelectionListener =
                new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            AdapterView<?> parent, View view, int position, long id) {
                        updateResult(false);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                };
        spinnerConverter.setOnItemSelectedListener(unitSelectionListener);
        spinnerConverted.setOnItemSelectedListener(unitSelectionListener);

        editTextConverter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable text) {
                String input = text.toString().trim();
                if (!selectedType.allowsNegative() && input.startsWith("-")) {
                    textViewResult.setText(R.string.result_placeholder);
                    if (!negativeWarningShown) {
                        negativeWarningShown = true;
                        showMessage(R.string.negative_not_allowed_message);
                    }
                    return;
                }

                negativeWarningShown = false;
                updateResult(false);
            }
        });

        findViewById(R.id.buttonConvert).setOnClickListener(view -> updateResult(true));
    }

    private void setupCategory(ConversionType type) {
        int unitsArray = type.unitsArrayResource();
        setSpinnerOptions(spinnerConverter, unitsArray, 0);
        setSpinnerOptions(spinnerConverted, unitsArray, 1);
        textViewCurrencyNotice.setVisibility(
                type == ConversionType.CURRENCY ? View.VISIBLE : View.GONE);
    }

    private void setSpinnerOptions(Spinner spinner, int unitsArray, int initialPosition) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, unitsArray, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        if (adapter.getCount() > initialPosition) {
            spinner.setSelection(initialPosition);
        }
    }

    private void updateResult(boolean showInputError) {
        String input = editTextConverter.getText().toString().trim().replace(',', '.');
        if (input.isEmpty()) {
            textViewResult.setText(R.string.result_placeholder);
            temperatureWarningShown = false;
            resultRangeWarningShown = false;
            if (showInputError) {
                showMessage(R.string.enter_number_message);
            }
            return;
        }

        if (!selectedType.allowsNegative() && input.startsWith("-")) {
            textViewResult.setText(R.string.result_placeholder);
            if (showInputError) {
                showMessage(R.string.negative_not_allowed_message);
            }
            return;
        }

        final double value;
        try {
            value = Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            textViewResult.setText(R.string.result_placeholder);
            temperatureWarningShown = false;
            resultRangeWarningShown = false;
            if (showInputError) {
                showMessage(R.string.invalid_number_message);
            }
            return;
        }

        if (!Double.isFinite(value)) {
            textViewResult.setText(R.string.result_placeholder);
            temperatureWarningShown = false;
            resultRangeWarningShown = false;
            if (showInputError) {
                showMessage(R.string.invalid_number_message);
            }
            return;
        }

        if (selectedType == ConversionType.TEMPERATURE
                && isBelowAbsoluteZero(value, spinnerConverter.getSelectedItemPosition())) {
            textViewResult.setText(R.string.result_placeholder);
            if (!temperatureWarningShown) {
                temperatureWarningShown = true;
                showMessage(R.string.below_absolute_zero_message);
            }
            return;
        }
        temperatureWarningShown = false;

        final double result;
        try {
            result = ConversionEngine.convert(
                    selectedType,
                    value,
                    spinnerConverter.getSelectedItemPosition(),
                    spinnerConverted.getSelectedItemPosition());
        } catch (IllegalArgumentException exception) {
            textViewResult.setText(R.string.result_placeholder);
            showMessage(R.string.conversion_failed_message);
            return;
        }

        if (!Double.isFinite(result)) {
            textViewResult.setText(R.string.result_placeholder);
            if (!resultRangeWarningShown) {
                resultRangeWarningShown = true;
                showMessage(R.string.result_out_of_range_message);
            }
            return;
        }
        resultRangeWarningShown = false;

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        numberFormat.setMaximumFractionDigits(6);
        textViewResult.setText(numberFormat.format(result));
    }

    private boolean isBelowAbsoluteZero(double value, int unitIndex) {
        double minimum;
        switch (unitIndex) {
            case 0:
                minimum = -273.15;
                break;
            case 1:
                minimum = -459.67;
                break;
            case 2:
                minimum = 0.0;
                break;
            default:
                minimum = Double.NEGATIVE_INFINITY;
        }
        return value < minimum;
    }

    private void showMessage(int messageResource) {
        Snackbar.make(findViewById(R.id.main), messageResource, Snackbar.LENGTH_SHORT).show();
    }
}
