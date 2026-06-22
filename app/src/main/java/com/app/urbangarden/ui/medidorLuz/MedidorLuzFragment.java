package com.app.urbangarden.ui.medidorLuz;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.app.urbangarden.R;
import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.databinding.FragmentMedidorLuzBinding;
import com.app.urbangarden.model.ParametrosLuz;
import com.app.urbangarden.model.Kit;

/**
 * Medidor de luz para un Kit concreto.
 *
 * Recibe SOLO el id del kit y observa el LiveData del repositorio (para mostrar
 * el nombre de la planta). Lee la iluminancia en lux con el sensor de luz
 * (Sensor.TYPE_LIGHT), la compara con {@link ParametrosLuz} y emite un veredicto.
 */
public class MedidorLuzFragment extends Fragment implements SensorEventListener {

    /** Clave del argumento (id del kit) que llega en el Bundle de Navigation. */
    private static final String ARG_KIT_ID = "kitId";

    /** Tope de lux usado para la barra de intensidad (escala visual). */
    private static final float LUX_MAX_BARRA = 30000f;

    private FragmentMedidorLuzBinding binding;
    private String kitId;

    private SensorManager sensorManager;
    private Sensor sensorLuz;
    private boolean midiendo = false;

    private String nombrePlanta = "tu planta";

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMedidorLuzBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            kitId = getArguments().getString(ARG_KIT_ID);
        }
        observarKit();
        inicializarSensor();

        return binding.getRoot();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (midiendo) alternarMedicion(); // no gastar batería fuera de pantalla
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        binding = null;
    }

    //.........................................................................
    // Datos: observar el kit por su id (para el nombre)

    private void observarKit() {
        if (kitId == null) return;
        KitRepository.getInstance().getKitsLiveData()
                .observe(getViewLifecycleOwner(), kits -> {
                    if (kits == null) return;
                    for (Kit k : kits) {
                        if (kitId.equals(k.getId())) {
                            nombrePlanta = k.getNombre();
                            break;
                        }
                    }
                });
    }

    //.........................................................................
    // Inicialización (sensor y botón)

    /** Obtiene el sensor de luz; si no existe, avisa y desactiva el botón. */
    private void inicializarSensor() {
        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager != null) {
            sensorLuz = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }

        if (sensorLuz == null) {
            binding.cardSinSensor.setVisibility(View.VISIBLE);
            binding.btnMedir.setEnabled(false);
            binding.btnMedir.setText("Sensor no disponible");
        } else {
            binding.btnMedir.setOnClickListener(v -> alternarMedicion());
        }
    }

    private void alternarMedicion() {
        if (midiendo) {
            sensorManager.unregisterListener(this);
            binding.btnMedir.setText("Empezar medición");
            binding.btnMedir.setIconResource(R.drawable.ic_ug_play);
        } else {
            sensorManager.registerListener(this, sensorLuz, SensorManager.SENSOR_DELAY_NORMAL);
            binding.btnMedir.setText("Detener medición");
            binding.btnMedir.setIconResource(R.drawable.ic_ug_pause);
            binding.tvNivelLuz.setText("Midiendo...");
        }
        midiendo = !midiendo; // Invierte el estado booleano de forma limpia
    }

    //.........................................................................
    // SensorEventListener

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_LIGHT) return;

        float lux = event.values[0];
        int luxRedondeado = Math.round(lux);

        binding.tvLuxValor.setText(String.valueOf(luxRedondeado));
        binding.tvNivelLuz.setText(getNivelTexto(lux));
        binding.pbIntensidadLuz.setProgress((int) Math.min(100, (lux / LUX_MAX_BARRA) * 100));

        actualizarVeredicto(luxRedondeado);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // No necesitamos reaccionar a cambios de precisión
    }

    //.........................................................................
    // Lectura y veredicto

    private String getNivelTexto(float lux) {
        if (lux < 500)   return "Muy poca luz";
        if (lux < 2000)  return "Luz baja";
        if (lux < 10000) return "Luz media (indirecta)";
        if (lux < 25000) return "Luz alta";
        return "Luz muy intensa (sol directo)";
    }

    /** Compara con el rango óptimo y actualiza la tarjeta de veredicto. */
    private void actualizarVeredicto(int lux) {
        if (binding.cardVeredicto.getVisibility() != View.VISIBLE) {
            binding.cardVeredicto.setVisibility(View.VISIBLE);
            binding.cardVeredicto.startAnimation(AnimationUtils.loadAnimation(requireContext(), android.R.anim.fade_in));
        }

        binding.tvNecesidadPlanta.setText(nombrePlanta + " necesita entre " + ParametrosLuz.LUX_OPTIMO_MIN
                        + " y " + ParametrosLuz.LUX_OPTIMO_MAX + " lux.");

        int iconoRes;
        String consejo;

        if (lux < ParametrosLuz.LUX_OPTIMO_MIN) {
            iconoRes = R.drawable.ic_notif_alerta;
            consejo  = "Acerca la planta a una ventana más luminosa o usa luz artificial de cultivo.";
        } else if (lux > ParametrosLuz.LUX_OPTIMO_MAX) {
            iconoRes = R.drawable.ic_ug_sun;
            consejo  = "Aleja la planta del sol directo o usa una cortina fina para difuminar la luz.";
        } else {
            iconoRes = R.drawable.ic_ug_check;
            consejo  = "La ubicación actual es perfecta. Mantén la planta donde está.";
        }

        binding.tvVeredictoTitulo.setText(ParametrosLuz.veredictoLuz(lux));
        binding.ivVeredictoIcono.setImageResource(iconoRes);
        binding.tvVeredictoConsejo.setText(consejo);
    }
}
