package com.example.conversor;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.content.res.Resources;
import android.view.Gravity;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.conversor.ConversionEngine.ConversionType;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {
    private boolean categoryImageWarningShown;

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

        GridLayout categoryGrid = findViewById(R.id.categoryContainer);
        for (ConversionType type : ConversionType.values()) {
            addCategoryCard(categoryGrid, type);
        }
    }

    private void addCategoryCard(GridLayout grid, ConversionType type) {
        String name = getString(type.titleResource());
        MaterialCardView card = new MaterialCardView(this);
        card.setCardBackgroundColor(getColor(R.color.surface_card));
        card.setStrokeColor(getColor(R.color.card_outline));
        card.setStrokeWidth(dp(1));
        card.setRadius(dp(20));
        card.setCardElevation(dp(3));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(getString(R.string.open_converter_for, name));

        LinearLayout content = new LinearLayout(this);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));

        ImageView image = new ImageView(this);
        image.setLayoutParams(new LinearLayout.LayoutParams(dp(76), dp(76)));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setContentDescription(name);
        Bitmap categoryBitmap = loadBitmap(type.imageResource());
        if (categoryBitmap == null) {
            categoryBitmap = loadBitmap(R.drawable.ic_category_placeholder);
            if (!categoryImageWarningShown) {
                categoryImageWarningShown = true;
                Snackbar.make(findViewById(R.id.main), R.string.category_image_error_message,
                        Snackbar.LENGTH_LONG).show();
            }
        }
        image.setImageBitmap(categoryBitmap);
        content.addView(image);

        TextView label = new TextView(this);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setText(name);
        label.setTextColor(getColor(R.color.text_primary));
        label.setTextSize(15);
        label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        labelParams.setMargins(0, dp(4), 0, dp(6));
        content.addView(label, labelParams);

        card.addView(content, new MaterialCardView.LayoutParams(
                MaterialCardView.LayoutParams.MATCH_PARENT,
                MaterialCardView.LayoutParams.MATCH_PARENT));
        card.setOnClickListener(view -> openConverter(type));

        GridLayout.LayoutParams cardParams = new GridLayout.LayoutParams();
        cardParams.width = 0;
        cardParams.height = dp(156);
        cardParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
        cardParams.setMargins(dp(5), dp(5), dp(5), dp(5));
        grid.addView(card, cardParams);
    }

    private Bitmap loadBitmap(int imageResource) {
        final Drawable drawable;
        try {
            drawable = AppCompatResources.getDrawable(this, imageResource);
        } catch (Resources.NotFoundException exception) {
            return null;
        }
        if (drawable == null) return null;
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        int size = dp(96);
        Bitmap vectorBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(vectorBitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        return vectorBitmap;
    }

    private void openConverter(ConversionType type) {
        Intent intent = new Intent(this, ConversionActivity.class);
        intent.putExtra(ConversionActivity.EXTRA_CONVERSION_TYPE, type.name());
        startActivity(intent);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
