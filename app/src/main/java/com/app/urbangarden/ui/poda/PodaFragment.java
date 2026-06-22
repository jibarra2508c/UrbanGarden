package com.app.urbangarden.ui.poda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.app.urbangarden.databinding.FragmentPodaBinding;

/**
 * Guía de poda: pantalla informativa y estática (cuándo podar y los pasos),
 * igual para cualquier especie. No necesita datos del kit.
 */
public class PodaFragment extends Fragment {

    private FragmentPodaBinding binding;

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentPodaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
