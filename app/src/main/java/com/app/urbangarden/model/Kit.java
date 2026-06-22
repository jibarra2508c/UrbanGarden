package com.app.urbangarden.model;

import com.google.firebase.firestore.Exclude;

import java.util.Date;

/**
 * Entidad de dominio que representa un Kit físico de cultivo UrbanGarden.
 * Se persiste en Cloud Firestore vía KitRepository (mapeo automático). Entre
 * pantallas se pasa solo el id del kit, no el objeto.
 */
public class Kit {

    private String id;
    private String nombre;
    private String especie;
    private String tipo;       // categoría: "aromática" / "hortaliza" / "verdura"...
    private Date fechaActivacion;

    private String emoji;
    private int humedad;      // Humedad base 0-100 (valor en el último riego/siembra)
    private float temperatura; // Grados Celsius
    private int horasLuz;     // Horas de luz al día
    private int diasEdad;     // Días desde activación
    private Date fechaUltimoRiego; // Referencia para el decaimiento de humedad
    private boolean trasplantado;  // true cuando el usuario ya ha trasplantado
    private int mlAlDia;           // Agua diaria calculada (0 = sin calcular)

    /** Umbral de humedad (%) por debajo del cual toca regar. General para todas. */
    private static final int HUMEDAD_MINIMA_RIEGO = 50;

    /** Días de crecimiento a los que, en la demo, se recomienda trasplantar. */
    public static final int DIAS_TRASPLANTE = 35;

    /** Días estimados de ciclo completo (de la activación a la cosecha). */
    public static final int DIAS_CICLO_COMPLETO = 110;

    private static final long MS_POR_HORA = 1000L * 60 * 60;
    private static final long MS_POR_DIA  = 24 * MS_POR_HORA;

    /** Constructor vacío: lo usa el repositorio al leer de Firestore. */
    public Kit() {}

    //.........................................................................
    // Getters

    public String getId()               { return id; }
    public String getNombre()           { return nombre; }
    public String getEspecie()          { return especie; }
    public String getTipo()             { return tipo; }
    public Date getFechaActivacion()    { return fechaActivacion; }

    public String getEmoji()            { return emoji; }
    public int getHumedad()              { return humedad; }
    public float getTemperatura()        { return temperatura; }
    public int getHorasLuz()             { return horasLuz; }
    public int getDiasEdad()             { return diasEdad; }
    public Date getFechaUltimoRiego()    { return fechaUltimoRiego; }
    public boolean isTrasplantado()      { return trasplantado; }
    public int getMlAlDia()              { return mlAlDia; }

    //.........................................................................
    // Setters

    public void setId(String id)                         { this.id = id; }
    public void setNombre(String nombre)                 { this.nombre = nombre; }
    public void setEspecie(String especie)               { this.especie = especie; }
    public void setTipo(String tipo)                     { this.tipo = tipo; }
    public void setFechaActivacion(Date fechaActivacion) { this.fechaActivacion = fechaActivacion; }

    public void setEmoji(String emoji)                   { this.emoji = emoji; }
    public void setHumedad(int humedad)              { this.humedad = humedad; }
    public void setTemperatura(float temperatura)    { this.temperatura = temperatura; }
    public void setHorasLuz(int horasLuz)            { this.horasLuz = horasLuz; }
    public void setDiasEdad(int diasEdad)            { this.diasEdad = diasEdad; }
    public void setFechaUltimoRiego(Date fecha)      { this.fechaUltimoRiego = fecha; }
    public void setTrasplantado(boolean trasplantado) { this.trasplantado = trasplantado; }
    public void setMlAlDia(int mlAlDia)              { this.mlAlDia = mlAlDia; }

    //.........................................................................
    // Cálculos en vivo (crecen solos con el tiempo)

    /**
     * Edad real en días: días desde la activación + la edad base de siembra.
     * Así crece sola cada día sin tocar la BD. Sin fecha, usa {@code diasEdad}.
     */
    @Exclude
    public int getDiasReales() {
        if (fechaActivacion == null) return diasEdad;
        return (int) (msDesde(fechaActivacion) / MS_POR_DIA) + diasEdad;
    }

    /**
     * Progreso del ciclo (0-100%) calculado en vivo desde la edad real: crece
     * solo cada día hasta el 100% en {@link #DIAS_CICLO_COMPLETO} días.
     */
    @Exclude
    public int getProgresoReal() {
        int p = (int) Math.round(getDiasReales() * 100.0 / DIAS_CICLO_COMPLETO);
        return Math.max(0, Math.min(100, p));
    }

    /**
     * Humedad actual: cae desde el valor base hasta 0% en
     * {@link ParametrosRiego#INTERVALO_RIEGO_BASE_HORAS} horas. Sin fecha de
     * referencia, devuelve el valor base.
     */
    @Exclude
    public int getHumedadReal() {
        if (fechaUltimoRiego == null) return humedad;
        double horas = msDesde(fechaUltimoRiego) / (double) MS_POR_HORA;
        double perdida = (100.0 / ParametrosRiego.INTERVALO_RIEGO_BASE_HORAS) * horas;
        int actual = (int) Math.round(humedad - perdida);
        return Math.max(0, Math.min(100, actual));
    }

    /** Milisegundos desde {@code fecha} hasta ahora (nunca negativo). */
    private static long msDesde(Date fecha) {
        return Math.max(System.currentTimeMillis() - fecha.getTime(), 0);
    }

    /** Indica si toca regar: la humedad real ha bajado del umbral mínimo. */
    public boolean necesitaRiego() {
        return getHumedadReal() < HUMEDAD_MINIMA_RIEGO;
    }

    //.........................................................................
    // Acciones

    /** Riega el kit: humedad al 100% y reinicia el decaimiento de humedad. */
    public void regar() {
        this.humedad = 100;
        this.fechaUltimoRiego = new Date();
    }

    @Override
    public String toString() {
        return "Kit{" +
                "id='"          + id             + '\'' +
                ", nombre='"    + nombre         + '\'' +
                ", especie='"   + especie        + '\'' +
                ", activacion=" + fechaActivacion +
                ", progreso="   + getProgresoReal() + "%" +
                '}';
    }
}
