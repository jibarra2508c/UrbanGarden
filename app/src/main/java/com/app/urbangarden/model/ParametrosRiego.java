package com.app.urbangarden.model;

/**
 * Parámetros de riego de la demo, genéricos (iguales para todas las plantas).
 * Los usan la calculadora de riego, el detalle del kit, el reloj de humedad de
 * {@link Kit} y el valor por defecto del repositorio.
 */
public final class ParametrosRiego {

    private ParametrosRiego() {} // utilidad, no se instancia

    /**
     * Agua al día por defecto (ml) para un kit mediano en condiciones neutras.
     * Valor común inicial de los kits hasta que el usuario recalcula en la
     * calculadora de riego (que lo sobrescribe con el valor ajustado).
     */
    public static final int ML_AL_DIA_DEFECTO = 150;

    /**
     * Intervalo de riego base (horas) en condiciones neutras. ÚNICA fuente de
     * verdad: la usan el reloj de humedad de {@link Kit} (decae del 100% al 0%)
     * y la calculadora de riego (de base, antes de ajustar por sol/ubicación).
     */
    public static final int INTERVALO_RIEGO_BASE_HORAS = 24;

    /** Agua (ml) por litro de sustrato. */
    private static final double ML_POR_LITRO = 100.0;

    //.........................................................................
    // Cálculo de agua diaria

    /** Agua (ml) para {@code litros} de sustrato, redondeada a múltiplos de 10 (mín. 10). */
    private static int mlPorRiego(double litros) {
        int ml = (int) Math.round(litros * ML_POR_LITRO);
        ml = ((ml + 5) / 10) * 10;
        return Math.max(ml, 10);
    }

    /**
     * Agua al día (ml) para una maceta de {@code litros}, ajustada por la
     * evaporación: más horas de sol o estar en el exterior → más agua.
     */
    public static int mlAlDia(double litros, int horasSol, boolean exterior) {
        return mlPorRiego(litros * ajusteSol(horasSol) * ajusteUbicacion(exterior));
    }

    /** Factor por horas de sol: más sol → más evaporación. 4-6 h = 1.0 (base). */
    private static double ajusteSol(int horas) {
        if (horas <= 3) return 0.7;
        if (horas <= 6) return 1.0;
        if (horas <= 9) return 1.2;
        return 1.4;
    }

    /** Factor por ubicación: el exterior pierde más agua. Interior = 1.0 (base). */
    private static double ajusteUbicacion(boolean exterior) {
        return exterior ? 1.3 : 1.0;
    }
}
