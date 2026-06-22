package com.app.urbangarden.model;

/**
 * Parámetros de luz de la demo, genéricos (iguales para todas las plantas).
 * Los usa el medidor de luz.
 */
public final class ParametrosLuz {

    private ParametrosLuz() {} // utilidad, no se instancia

    /** Rango de iluminancia óptima (lux). */
    public static final int LUX_OPTIMO_MIN = 2000;
    public static final int LUX_OPTIMO_MAX = 10000;

    /** Veredicto de luz según el rango óptimo: "Necesita más luz" / "Demasiada luz" / "Luz ideal". */
    public static String veredictoLuz(int lux) {
        if (lux < LUX_OPTIMO_MIN) return "Necesita más luz";
        if (lux > LUX_OPTIMO_MAX) return "Demasiada luz";
        return "Luz ideal";
    }
}
