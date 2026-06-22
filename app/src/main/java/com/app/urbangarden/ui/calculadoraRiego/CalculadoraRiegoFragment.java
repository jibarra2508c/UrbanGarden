package com.app.urbangarden.ui.calculadoraRiego;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.databinding.FragmentCalculadoraRiegoBinding;
import com.app.urbangarden.model.ParametrosRiego;
import com.app.urbangarden.model.Kit;

/**
 * Calculadora de riego para un Kit concreto.
 *
 * Recibe SOLO el id del kit y observa el LiveData del repositorio para tener
 * siempre el objeto actualizado. Pregunta ubicación, horas de sol y tamaño y
 * estima el agua diaria; el cálculo vive en {@link ParametrosRiego#mlAlDia}.
 */
public class CalculadoraRiegoFragment extends Fragment {

    /** Clave del argumento (id del kit) que llega en el Bundle de Navigation. */
    private static final String ARG_KIT_ID = "kitId";

    private FragmentCalculadoraRiegoBinding binding;
    private String kitId;
    private Kit kit;
    private int mlAlDiaCalculado;

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCalculadoraRiegoBinding.inflate(inflater, container, false);
        if (getArguments() != null) {
            kitId = getArguments().getString(ARG_KIT_ID);
        }
        observarKit();
        inicializarUI();
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // Datos: observar el kit por su id

    private void observarKit() {
        if (kitId == null) return;
        KitRepository.getInstance().getKitsLiveData()
                .observe(getViewLifecycleOwner(), kits -> {
                    if (kits == null) return;
                    for (Kit k : kits) {
                        if (kitId.equals(k.getId())) {
                            kit = k;
                            break;
                        }
                    }
                });
    }

    //.........................................................................
    // Inicialización de la UI

    private void inicializarUI() {
        // Defaults: Interior + Mediano.
        binding.toggleUbicacion.check(binding.btnUbicInterior.getId());
        binding.toggleTamano.check(binding.btnTamMediano.getId());

        // El slider actualiza su etiqueta de horas en vivo.
        binding.sliderHorasSol.addOnChangeListener((slider, value, fromUser) ->
                binding.tvHorasSol.setText(((int) value) + " h"));

        binding.btnCalcular.setOnClickListener(v -> calcularRiego());
        binding.btnRegistrarRiego.setOnClickListener(v -> registrarRiego());
    }

    //.........................................................................
    // Acción: calcular el riego y mostrar el resultado

    private void calcularRiego() {
        int horasSol = (int) binding.sliderHorasSol.getValue();
        boolean esExterior = binding.toggleUbicacion.getCheckedButtonId() == binding.btnUbicExterior.getId();

        // Volumen de sustrato del kit (en litros)
        int idTam = binding.toggleTamano.getCheckedButtonId();
        double volumen;
        if (idTam == binding.btnTamPequeno.getId()) {
            volumen = 0.5;
        } else if (idTam == binding.btnTamGrande.getId()) {
            volumen = 3.0;
        } else {
            volumen = 1.5; // mediano (por defecto)
        }

        mlAlDiaCalculado = ParametrosRiego.mlAlDia(volumen, horasSol, esExterior);

        binding.tvResultadoMl.setText(String.valueOf(mlAlDiaCalculado));
        binding.tvResultadoConsejo.setText(generarConsejo(horasSol, esExterior));

        // Revelar resultado y auto-scroll
        if (binding.cardResultado.getVisibility() != View.VISIBLE) {
            binding.cardResultado.setVisibility(View.VISIBLE);
            binding.cardResultado.startAnimation(AnimationUtils.loadAnimation(requireContext(),
                    android.R.anim.fade_in));
        }
        binding.cardResultado.post(() -> ((ScrollView) binding.getRoot()).
                smoothScrollTo(0, binding.cardResultado.getTop()));
    }

    //.........................................................................
    // Acción: registrar este riego sobre el kit

    /** Riega el kit, guarda el agua diaria calculada y persiste en Firestore. */
    private void registrarRiego() {
        if (kit != null) {
            kit.regar();                      // humedad al 100% + reinicia el decaimiento
            kit.setMlAlDia(mlAlDiaCalculado); // guarda el agua/día calculada
            KitRepository.getInstance().actualizarKit(kit);
        }
        Toast.makeText(getContext(), "Riego registrado correctamente",
                Toast.LENGTH_SHORT).show();
        Navigation.findNavController(requireView()).navigateUp();
    }

    //.........................................................................
    // Consejo de cuidado (según la situación más relevante)

    private String generarConsejo(int horasSol, boolean esExterior) {
        if (horasSol >= 9) return "Mucho sol. Riega temprano por la mañana para reducir la evaporación.";
        if (horasSol <= 2) return "Poca luz: cuidado con el exceso de agua, podría pudrir las raíces.";
        return esExterior ? "Riega temprano por la mañana o al atardecer." : "Riega lentamente alrededor del tallo.";
    }
}
