package com.app.urbangarden.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.app.urbangarden.R;
import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.data.SesionManager;
import com.app.urbangarden.data.TiempoService;
import com.app.urbangarden.databinding.FragmentHomeBinding;
import com.app.urbangarden.model.Kit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pantalla de inicio: cabecera con saludo, tarjeta "Mi huerto", "El tiempo hoy"
 * (Open-Meteo) y carrusel "Mis kits".
 *
 * Los kits no se leen de forma puntual: se OBSERVA el LiveData del
 * KitRepository, así cualquier cambio refresca la pantalla automáticamente.
 */
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private KitHomeAdapter adapter;
    private final TiempoService tiempoService = new TiempoService();

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        setupCabecera();
        setupRecyclerView();
        observarRepositorio();
        cargarTiempo();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // Cabecera y lista de kits

    private void setupCabecera() {
        // El saludo es texto fijo del layout; aquí solo ponemos el nombre.
        String nombre = new SesionManager(requireContext()).getNombre();
        binding.tvNombreUsuario.setText(nombre);
    }

    private void setupRecyclerView() {
        // El adapter arranca vacío; el observer lo rellena cuando llegan datos.
        adapter = new KitHomeAdapter(new java.util.ArrayList<>());
        binding.rvKits.setAdapter(adapter);
    }

    /** Se suscribe al LiveData: la lista y "Mi huerto" se refrescan con cada cambio. */
    private void observarRepositorio() {
        KitRepository.getInstance().getKitsLiveData()
                .observe(getViewLifecycleOwner(), kits -> {
                    if (kits == null) return;
                    adapter.actualizarLista(kits);
                    actualizarMiHuerto(kits);
                });
    }

    //.........................................................................
    // Mi huerto (esquema visual + resumen)

    /** Rellena la tarjeta "Mi huerto": mini-esquema (un emoji por kit) y resumen. */
    private void actualizarMiHuerto(List<Kit> kits) {
        binding.contenedorEsquema.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (Kit kit : kits) {
            View celda = inflater.inflate(R.layout.item_huerto_celda,
                    binding.contenedorEsquema, false);
            TextView tvEmoji = celda.findViewById(R.id.tvCeldaEmoji);
            tvEmoji.setText(kit.getEmoji());
            binding.contenedorEsquema.addView(celda);
        }

        binding.tvHuertoResumen.setText(generarResumenComposicion(kits));
    }

    /** Resumen por categoría, tipo "2 aromaticas · 1 hortaliza · 1 verdura". */
    private String generarResumenComposicion(List<Kit> kits) {
        if (kits.isEmpty()) {
            return "Aun no tienes kits en tu huerto";
        }

        Map<String, Integer> contadores = new HashMap<>();
        for (Kit kit : kits) {
            String tipo = (kit.getTipo() != null && !kit.getTipo().isEmpty()) ? kit.getTipo() : "otro";
            contadores.merge(tipo, 1, Integer::sum);
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : contadores.entrySet()) {
            if (sb.length() > 0) sb.append(" · ");
            int n = e.getValue();
            sb.append(n).append(" ").append(pluralizar(e.getKey(), n));
        }
        return sb.toString();
    }

    private String pluralizar(String categoria, int n) {
        if (n == 1) return categoria;
        return categoria + "s";
    }

    //.........................................................................
    // El tiempo hoy (delegado en TiempoService)

    /** Observa la previsión y la pinta cuando llega; el estado de carga es del layout. */
    private void cargarTiempo() {
        tiempoService.cargar().observe(getViewLifecycleOwner(), t -> {
            if (t == null) return;
            pintarTiempo(t.emoji, t.recomendacion, t.temperatura, t.ciudad);
        });
    }

    /** Vuelca el estado del tiempo en la tarjeta (siempre en el hilo UI). */
    private void pintarTiempo(String emoji, String reco, String temp, String ciudad) {
        if (binding == null) return; // la vista pudo destruirse durante la llamada async
        binding.tvTiempoEmoji.setText(emoji);
        binding.tvTiempoRecomendacion.setText(reco);
        binding.tvTiempoTemp.setText(temp);
        if (ciudad != null) binding.tvTiempoUbicacion.setText(ciudad);
    }
}
