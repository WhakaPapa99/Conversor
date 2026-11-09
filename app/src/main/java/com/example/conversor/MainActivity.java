package com.example.conversor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
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
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final Map<ConversionType, Integer> UNIT_ARRAYS =
            new EnumMap<>(ConversionType.class);
    private static final Map<ConversionType, Integer> CATEGORY_IMAGES =
            new EnumMap<>(ConversionType.class);

    static {
        UNIT_ARRAYS.put(ConversionType.DISTANCE, R.array.units_distance);
        UNIT_ARRAYS.put(ConversionType.SPEED, R.array.units_speed);
        UNIT_ARRAYS.put(ConversionType.PRESSURE, R.array.units_pressure);
        UNIT_ARRAYS.put(ConversionType.DATA_STORAGE, R.array.units_data_storage);
        UNIT_ARRAYS.put(ConversionType.TEMPERATURE, R.array.units_temperature);
        UNIT_ARRAYS.put(ConversionType.CURRENCY, R.array.units_currency);

        CATEGORY_IMAGES.put(ConversionType.DISTANCE, R.drawable.ruler);
        CATEGORY_IMAGES.put(ConversionType.SPEED, R.drawable.speed);
        CATEGORY_IMAGES.put(ConversionType.PRESSURE, R.drawable.pressure);
        CATEGORY_IMAGES.put(ConversionType.DATA_STORAGE, R.drawable.data);
        CATEGORY_IMAGES.put(ConversionType.TEMPERATURE, R.drawable.thermometer);
        CATEGORY_IMAGES.put(ConversionType.CURRENCY, R.drawable.currency);
    }

    private ConversionType selectedType;
    private Spinner spinnerConverter;
    private Spinner spinnerConverted;
    private EditText editTextConverter;
    private TextView textViewResult;
    private TextView textViewCurrencyNotice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Spinner spinnerType = findViewById(R.id.spinnerType);
        spinnerConverter = findViewById(R.id.spinnerConverter);
        spinnerConverted = findViewById(R.id.spinnerConverted);
        editTextConverter = findViewById(R.id.editTextConverter);
        textViewResult = findViewById(R.id.textViewResult);
        textViewCurrencyNotice = findViewById(R.id.textViewCurrencyNotice);
        ImageView imageView = findViewById(R.id.imageView);

        selectedType = ConversionType.values()[spinnerType.getSelectedItemPosition()];
        updateCategory(selectedType, imageView);

        spinnerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                ConversionType newType = ConversionType.values()[position];
                if (newType != selectedType) {
                    selectedType = newType;
                    editTextConverter.setText("");
                    textViewResult.setText(R.string.result_placeholder);
                    updateCategory(selectedType, imageView);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

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
                updateResult(false);
            }
        });

        findViewById(R.id.buttonConvert).setOnClickListener(view -> updateResult(true));
    }

    private void updateCategory(ConversionType type, ImageView imageView) {
        int unitsArray = UNIT_ARRAYS.get(type);
        setSpinnerOptions(spinnerConverter, unitsArray, 0);
        setSpinnerOptions(spinnerConverted, unitsArray, 1);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), CATEGORY_IMAGES.get(type));
        imageView.setImageBitmap(bitmap);
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
            if (showInputError) {
                showMessage(R.string.enter_number_message);
            }
            return;
        }

        final double value;
        try {
            value = Double.parseDouble(input);
        } catch (NumberFormatException exception) {
            textViewResult.setText(R.string.result_placeholder);
            if (showInputError) {
                showMessage(R.string.invalid_number_message);
            }
            return;
        }

        if (!Double.isFinite(value)) {
            textViewResult.setText(R.string.result_placeholder);
            if (showInputError) {
                showMessage(R.string.invalid_number_message);
            }
            return;
        }

        double result = ConversionEngine.convert(
                selectedType,
                value,
                spinnerConverter.getSelectedItemPosition(),
                spinnerConverted.getSelectedItemPosition());

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        numberFormat.setMaximumFractionDigits(6);
        textViewResult.setText(numberFormat.format(result));
    }

    private void showMessage(int messageResource) {
        Snackbar.make(findViewById(R.id.main), messageResource, Snackbar.LENGTH_SHORT).show();
    }
}
