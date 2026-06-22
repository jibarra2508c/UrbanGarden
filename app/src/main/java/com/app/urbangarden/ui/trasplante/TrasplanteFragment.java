package com.app.urbangarden.ui.trasplante;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.databinding.FragmentTrasplanteBinding;
import com.app.urbangarden.model.Kit;

/**
 * Guía de trasplante para un Kit concreto.
 *
 * Recibe SOLO el id del kit y observa el LiveData del repositorio. Compara la
 * edad real ({@link Kit#getDiasReales()}) con {@link Kit#DIAS_TRASPLANTE} para
 * decir si ya toca. Más una guía estática.
 */
public class TrasplanteFragment extends Fragment {

    /** Clave del argumento (id del kit) que llega en el Bundle de Navigation. */
    private static final String ARG_KIT_ID = "kitId";

    private FragmentTrasplanteBinding binding;
    private String kitId;
    private Kit kit;

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentTrasplanteBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            kitId = getArguments().getString(ARG_KIT_ID);
        }
        observarKit();
        binding.btnTrasplantarAhora.setOnClickListener(v -> marcarTrasplantado());

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // Diagnóstico

    private void observarKit() {
        if (kitId == null) {
            calcularDiagnostico(); // sin kit: muestra el estado por defecto
            return;
        }
        KitRepository.getInstance().getKitsLiveData()
                .observe(getViewLifecycleOwner(), kits -> {
                    if (kits == null) return;
                    for (Kit k : kits) {
                        if (kitId.equals(k.getId())) {
                            kit = k;
                            break;
                        }
                    }
                    calcularDiagnostico();
                });
    }

    /**
     * A los {@link Kit#DIAS_TRASPLANTE} días toca trasplantar (igual para todas).
     * Dos veredictos: "no hace falta" y "sí, ya toca".
     */
    private void calcularDiagnostico() {
        int edad = kit != null ? kit.getDiasReales() : 0;
        boolean yaTrasplantado = false;
        if (kit != null) {yaTrasplantado = kit.isTrasplantado();}

        binding.tvDiagnosticoDetalle.setText(
                "El trasplante se recomienda a los " + Kit.DIAS_TRASPLANTE
                        + " días de crecimiento, cuando las raíces llenan el cepellón.");

        if (yaTrasplantado) {
            binding.tvDiagnosticoIcono.setText("✓");
            binding.tvDiagnosticoTitulo.setText("Ya trasplantada");
            binding.tvDiagnosticoSubtitulo.setText("Has marcado esta planta como trasplantada.");
            binding.btnTrasplantarAhora.setVisibility(View.GONE);
        } else if (edad >= Kit.DIAS_TRASPLANTE) {
            binding.tvDiagnosticoIcono.setText("⚠️");
            binding.tvDiagnosticoTitulo.setText("Sí, ya toca");
            binding.tvDiagnosticoSubtitulo.setText(
                    "Lleva " + edad + " días, revisa las señales de abajo");
            binding.btnTrasplantarAhora.setVisibility(View.VISIBLE);
        } else {
            int faltan = Kit.DIAS_TRASPLANTE - edad;
            binding.tvDiagnosticoIcono.setText("✓");
            binding.tvDiagnosticoTitulo.setText("No hace falta");
            binding.tvDiagnosticoSubtitulo.setText(
                    "Faltan unos " + faltan + " días (" + edad + " de " + Kit.DIAS_TRASPLANTE + ")");
            binding.btnTrasplantarAhora.setVisibility(View.GONE);
        }
    }

    //.........................................................................
    // Acción: marcar como trasplantado

    /** Marca el kit como trasplantado y lo persiste; el observer repinta solo. */
    private void marcarTrasplantado() {
        if (kit == null) return;
        kit.setTrasplantado(true);

        KitRepository.getInstance().actualizarKit(kit);
        Toast.makeText(getContext(), "Planta trasplantada con éxito ✓", Toast.LENGTH_SHORT).show();
    }
}
