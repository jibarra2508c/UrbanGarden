package com.app.urbangarden.data;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Servicio del tiempo: consulta Open-Meteo para saber si lloverá hoy y devuelve
 * una recomendación de riego. Ubicación FIJA (Mallorca) para no depender del GPS.
 *
 * - UD7 (APIs REST): consume Open-Meteo (gratis, sin clave) y parsea su JSON.
 * - UD9 (hilos/reactividad): la red corre en un hilo de fondo y el resultado se entrega
 *   en la UI de forma reactiva exponiendo un LiveData (evitando Handlers manuales).
 *
 * El último resultado correcto se guarda en una caché (compartida y con caducidad)
 * para no volver a descargar cada vez que se entra al Home y reducir los fallos
 * de red puntuales.
 */
public class TiempoService {

    // Ubicación fija para la demo: Mallorca (Palma).
    private static final double LATITUD  = 39.5696;
    private static final double LONGITUD = 2.6502;
    private static final String CIUDAD   = "Mallorca";

    /** Caducidad de la caché: el tiempo no cambia de un minuto a otro. */
    private static final long CACHE_TTL_MS = 30 * 60 * 1000L; // 30 min
    /** Intentos de descarga antes de darse por vencido (la red del emulador falla a veces). */
    private static final int INTENTOS = 3;

    // Caché en memoria, compartida entre instancias (el Home crea una nueva cada vez).
    private static Tiempo cache;
    private static long cacheTimestamp;

    //.........................................................................
    // Tipos públicos (resultado)

    /** Resultado ya "cocinado", listo para pintar en la tarjeta del Home. */
    public static class Tiempo {
        public final String emoji;
        public final String recomendacion;
        public final String temperatura; // p.ej. "21°"
        public final String ciudad;

        Tiempo(String emoji, String recomendacion, String temperatura, String ciudad) {
            this.emoji = emoji;
            this.recomendacion = recomendacion;
            this.temperatura = temperatura;
            this.ciudad = ciudad;
        }
    }

    // Sin interfaz Callback ni Handler: la entrega es reactiva vía LiveData (postValue).

    //.........................................................................
    // Carga (con caché y reintento)

    /**
     * Carga la previsión del tiempo y devuelve un LiveData.
     * Si hay un resultado reciente en caché, lo entrega al instante; si no,
     * descarga en un hilo de fondo (reintentando) y publica el resultado.
     */
    public LiveData<Tiempo> cargar() {
        MutableLiveData<Tiempo> tiempoLiveData = new MutableLiveData<>();

        if (cacheValida()) {
            // Como estamos en el hilo principal, asignamos el valor directamente.
            tiempoLiveData.setValue(cache);
            return tiempoLiveData;
        }

        new Thread(() -> {
            Tiempo resultado = descargar();
            if (resultado != null) {
                cache = resultado;                       // solo cacheamos el éxito
                cacheTimestamp = System.currentTimeMillis();
            } else if (cache != null) {
                resultado = cache;                       // si falla pero hay caché vieja, la reusamos
            } else {
                resultado = new Tiempo("⚠️",
                        "No se pudo cargar el tiempo. Revisa tu conexión.", "--°", CIUDAD);
            }

            // postValue salta de este hilo de fondo al hilo principal automáticamente.
            tiempoLiveData.postValue(resultado);
        }).start();

        return tiempoLiveData;
    }

    private boolean cacheValida() {
        return cache != null && System.currentTimeMillis() - cacheTimestamp < CACHE_TTL_MS;
    }

    //.........................................................................
    // Descarga y parseo

    /** Intenta descargar la previsión varias veces; devuelve null si todas fallan. */
    private Tiempo descargar() {
        String url = "https://api.open-meteo.com/v1/forecast"
                + "?latitude=" + LATITUD
                + "&longitude=" + LONGITUD
                + "&current=temperature_2m"
                + "&daily=precipitation_probability_max,precipitation_sum"
                + "&forecast_days=1&timezone=auto";

        for (int intento = 1; intento <= INTENTOS; intento++) {
            try {
                JSONObject root = new JSONObject(httpGet(url));
                double temp = root.getJSONObject("current").getDouble("temperature_2m");
                JSONObject daily = root.getJSONObject("daily");
                int prob = daily.getJSONArray("precipitation_probability_max").getInt(0);
                double mm = daily.getJSONArray("precipitation_sum").getDouble(0);
                return construir(temp, prob, mm);
            } catch (Exception e) {
                Log.e("TiempoService", "Fallo en el intento " + intento + " al descargar el tiempo", e);
                // Reintenta; si era el último intento, caemos al return null.
            }
        }
        return null;
    }

    /** Decide emoji + recomendación de riego según la lluvia prevista. */
    private Tiempo construir(double temp, int prob, double mm) {
        // Caso más común (soleado) por defecto; nos ahorramos un bloque else.
        String emoji = "☀️";
        String reco = "No se espera lluvia hoy (" + prob + "%). Revisa si tus plantas necesitan agua.";

        if (prob >= 50 || mm >= 1.0) {
            emoji = "🌧️";
            reco = "Se espera lluvia hoy (" + prob + "%). Puedes saltarte el riego de exterior.";
        } else if (prob >= 25) {
            emoji = "🌦️";
            reco = "Posible lluvia ligera (" + prob + "%). Comprueba la humedad antes de regar.";
        }

        return new Tiempo(emoji, reco, Math.round(temp) + "°", CIUDAD);
    }

    /** GET HTTP sencillo que devuelve el cuerpo de la respuesta como String. */
    private String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) {
                sb.append(linea);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
