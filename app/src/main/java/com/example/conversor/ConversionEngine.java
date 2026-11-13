package com.example.conversor;

import java.util.EnumMap;
import java.util.Map;

public final class ConversionEngine {
    public enum ConversionType {
        DISTANCE(R.string.category_distance, R.drawable.ruler, R.array.units_distance, false),
        SPEED(R.string.category_speed, R.drawable.speed, R.array.units_speed, false),
        PRESSURE(R.string.category_pressure, R.drawable.pressure, R.array.units_pressure, false),
        DATA_STORAGE(R.string.category_data_storage, R.drawable.data,
                R.array.units_data_storage, false),
        TEMPERATURE(R.string.category_temperature, R.drawable.thermometer,
                R.array.units_temperature, true),
        CURRENCY(R.string.category_currency, R.drawable.currency, R.array.units_currency, false),
        VOLUME(R.string.category_volume, R.drawable.volume, R.array.units_volume, false),
        MASS_WEIGHT(R.string.category_mass, R.drawable.mass, R.array.units_mass, false),
        TIME(R.string.category_time, R.drawable.time, R.array.units_time, false),
        AREA(R.string.category_area, R.drawable.area, R.array.units_area, false);

        private final int titleResource;
        private final int imageResource;
        private final int unitsArrayResource;
        private final boolean allowsNegative;

        ConversionType(
                int titleResource,
                int imageResource,
                int unitsArrayResource,
                boolean allowsNegative) {
            this.titleResource = titleResource;
            this.imageResource = imageResource;
            this.unitsArrayResource = unitsArrayResource;
            this.allowsNegative = allowsNegative;
        }

        public int titleResource() {
            return titleResource;
        }

        public int imageResource() {
            return imageResource;
        }

        public int unitsArrayResource() {
            return unitsArrayResource;
        }

        public boolean allowsNegative() {
            return allowsNegative;
        }
    }

    private interface ConversionStrategy {
        double convert(double value, int fromIndex, int toIndex);
    }

    private static final Map<ConversionType, ConversionStrategy> STRATEGIES =
            new EnumMap<>(ConversionType.class);

    private static final double[] TEMPERATURE_TO_CELSIUS_SCALE = {
            1.0, 5.0 / 9.0, 1.0
    };
    private static final double[] TEMPERATURE_TO_CELSIUS_OFFSET = {
            0.0, -32.0 * 5.0 / 9.0, -273.15
    };
    private static final double[] CELSIUS_TO_TEMPERATURE_SCALE = {
            1.0, 9.0 / 5.0, 1.0
    };
    private static final double[] CELSIUS_TO_TEMPERATURE_OFFSET = {
            0.0, 32.0, 273.15
    };

    // Fixed sample rates relative to EUR; the UI shows the reference date.
    private static final double[] CURRENCY_UNITS_PER_EUR = {
            1.0, 1.1193, 0.83430, 176.79
    };

    static {
        registerLinear(ConversionType.DISTANCE,
                new double[]{1.0, 1000.0, 0.01, 0.001, 1609.344, 0.3048, 0.0254});
        registerLinear(ConversionType.SPEED,
                new double[]{1.0, 1.0 / 3.6, 0.44704, 0.514444444444});
        registerLinear(ConversionType.PRESSURE,
                new double[]{1.0, 1000.0, 100000.0, 101325.0, 6894.757293168});
        registerLinear(ConversionType.DATA_STORAGE,
                new double[]{1.0, 8.0, 8000.0, 8000000.0, 8000000000.0});
        registerLinear(ConversionType.VOLUME,
                new double[]{1.0, 0.001, 1000.0, 3.785411784, 4.54609});
        registerLinear(ConversionType.MASS_WEIGHT,
                new double[]{1.0, 1000.0, 0.001, 453.59237, 28.349523125});
        registerLinear(ConversionType.TIME,
                new double[]{1.0, 60.0, 3600.0, 86400.0, 604800.0});
        registerLinear(ConversionType.AREA,
                new double[]{1.0, 1000000.0, 0.0001, 10000.0, 4046.8564224, 0.09290304});

        STRATEGIES.put(ConversionType.TEMPERATURE, ConversionEngine::convertTemperature);
        STRATEGIES.put(ConversionType.CURRENCY, ConversionEngine::convertCurrency);
    }

    private ConversionEngine() {
    }

    public static double convert(
            ConversionType type, double value, int fromIndex, int toIndex) {
        ConversionStrategy strategy = STRATEGIES.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("Tipo de conversión desconocido: " + type);
        }
        return strategy.convert(value, fromIndex, toIndex);
    }

    private static void registerLinear(ConversionType type, double[] factors) {
        STRATEGIES.put(type, (value, fromIndex, toIndex) -> {
            checkUnitIndex(factors.length, fromIndex, toIndex);
            double valueInBaseUnit = value * factors[fromIndex];
            return valueInBaseUnit / factors[toIndex];
        });
    }

    private static double convertTemperature(double value, int fromIndex, int toIndex) {
        checkUnitIndex(TEMPERATURE_TO_CELSIUS_SCALE.length, fromIndex, toIndex);
        double celsius = value * TEMPERATURE_TO_CELSIUS_SCALE[fromIndex]
                + TEMPERATURE_TO_CELSIUS_OFFSET[fromIndex];
        return celsius * CELSIUS_TO_TEMPERATURE_SCALE[toIndex]
                + CELSIUS_TO_TEMPERATURE_OFFSET[toIndex];
    }

    private static double convertCurrency(double value, int fromIndex, int toIndex) {
        checkUnitIndex(CURRENCY_UNITS_PER_EUR.length, fromIndex, toIndex);
        double valueInEur = value / CURRENCY_UNITS_PER_EUR[fromIndex];
        return valueInEur * CURRENCY_UNITS_PER_EUR[toIndex];
    }

    private static void checkUnitIndex(int unitCount, int fromIndex, int toIndex) {
        if (fromIndex < 0 || fromIndex >= unitCount || toIndex < 0 || toIndex >= unitCount) {
            throw new IllegalArgumentException("Índice de unidad fuera de rango");
        }
    }
}
