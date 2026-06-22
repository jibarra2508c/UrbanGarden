package com.app.urbangarden.ui.kits;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.app.urbangarden.R;
import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.databinding.FragmentKitsBinding;

/**
 * Lista completa de kits. Cada tarjeta tiene un botón "Regar ahora" que persiste
 * el cambio vía KitRepository (Firestore); la lista se refresca sola porque
 * observamos su LiveData (sin onResume ni notify manual).
 */
public class KitsFragment extends Fragment {

    private FragmentKitsBinding binding;
    private KitAdapter adapter;

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentKitsBinding.inflate(inflater, container, false);

        setupRecyclerView();
        setupFab();
        observarRepositorio();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // Configuración

    private void setupRecyclerView() {
        // Al regar, el repositorio persiste el cambio; el observer refresca la lista.
        KitAdapter.OnKitRegadoListener listenerRiego = kit -> {
            KitRepository.getInstance().actualizarKit(kit);
            Toast.makeText(getContext(),
                    kit.getNombre() + " regado correctamente",
                    Toast.LENGTH_SHORT).show();
        };

        adapter = new KitAdapter(new java.util.ArrayList<>(), listenerRiego);
        binding.rvListaKits.setAdapter(adapter);
    }

    private void setupFab() {
        binding.fabNuevoKit.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.navigation_qr));
    }

    private void observarRepositorio() {
        KitRepository.getInstance().getKitsLiveData()
                .observe(getViewLifecycleOwner(), kits -> {
                    if (kits != null && adapter != null) {
                        adapter.actualizarLista(kits);
                    }
                });
    }
}
