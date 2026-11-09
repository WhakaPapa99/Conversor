package com.example.conversor;

import java.util.EnumMap;
import java.util.Map;

public final class ConversionEngine {
    public enum ConversionType {
        DISTANCE,
        SPEED,
        PRESSURE,
        DATA_STORAGE,
        TEMPERATURE,
        CURRENCY
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

    // Sample ECB reference rates for 2026-11-09: units of currency per 1 EUR.
    // They are fixed demonstration values, not live exchange rates.
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
