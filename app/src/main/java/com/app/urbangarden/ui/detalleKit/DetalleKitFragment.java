package com.app.urbangarden.ui.detalleKit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.app.urbangarden.R;
import com.app.urbangarden.databinding.FragmentDetalleKitBinding;
import com.app.urbangarden.model.ParametrosRiego;
import com.app.urbangarden.model.Kit;
import com.app.urbangarden.data.KitRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Pantalla de detalle de un Kit. Recibe SOLO el ID del kit por argumentos.
 * Se conecta al LiveData del repositorio para pintar los datos siempre actualizados.
 */
public class DetalleKitFragment extends Fragment {
    private static final String ARG_KIT_ID = "kitId";

    private FragmentDetalleKitBinding binding;
    private String kitId;
    private Kit kit;

    //.........................................................................
    // Ciclo de vida

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDetalleKitBinding.inflate(inflater, container, false);

        // 1. Recuperamos el id del kit que nos pasó la pantalla anterior
        if (getArguments() != null) {
            kitId = getArguments().getString(ARG_KIT_ID);
        }

        // 2. Conexion al LiveData del repositorio
        KitRepository.getInstance().getKitsLiveData().observe(getViewLifecycleOwner(), kits -> {
            if (kits == null || kitId == null) return;

            // Buscamos cuál de todos los kits de la base de datos coincide con nuestro ID
            for (Kit k : kits) {
                if (kitId.equals(k.getId())) {
                    this.kit = k; // Encontrado
                    break;
                }
            }
            // En cuanto lo encuentra (o si cambia en Firebase), se pinta solo
            pintarKit();
        });

        configurarHerramientas();
        configurarFooter();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    //.........................................................................
    // Pintar datos en la UI
    private void pintarKit() {
        if (kit == null) {
            Toast.makeText(getContext(), "No se ha podido cargar el kit",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Hero
        binding.tvDetalleEmoji.setText(kit.getEmoji());
        binding.tvDetalleNombre.setText(kit.getNombre());
        binding.tvDetalleEspecie.setText(kit.getEspecie());

        // Progreso (crece solo con los días)
        binding.pbDetalleProgreso.setProgress(kit.getProgresoReal());
        binding.tvDetallePorcentaje.setText(kit.getProgresoReal() + "%");

        int diasEdad = kit.getDiasReales();
        int diasRestantes = Math.max(Kit.DIAS_CICLO_COMPLETO - diasEdad, 0);
        binding.tvDetalleResumenDias.setText(
                diasEdad + " días desde activación · Cosecha en ~" + diasRestantes + " días");

        // Agua al día: valor guardado en el kit (defecto, o lo que calcule la calculadora).
        int ml = kit.getMlAlDia() > 0 ? kit.getMlAlDia() : ParametrosRiego.ML_AL_DIA_DEFECTO;
        binding.tvDetalleAgua.setText("~" + ml + " ml");

        // Footer
        binding.tvFooterMeta.setText(
                kit.getId() + " · Activado el " + formatearFecha(kit.getFechaActivacion()));
    }

    private String formatearFecha(Date fecha) {
        if (fecha == null) return "—";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("es", "ES"));
        return sdf.format(fecha);
    }

    //.........................................................................
    // Herramientas de cuidado

    /** Cablea las 5 herramientas: tanto la fila como el botón navegan a ellas. */
    private void configurarHerramientas() {
        View.OnClickListener abrirCalculadoraRiego = v -> abrirCalculadoraRiego();
        binding.itemHerramientaRiego.setOnClickListener(abrirCalculadoraRiego);
        binding.btnHerramientaRiego.setOnClickListener(abrirCalculadoraRiego);

        View.OnClickListener abrirMedidorLuz = v -> abrirMedidorLuz();
        binding.itemHerramientaLuz.setOnClickListener(abrirMedidorLuz);
        binding.btnHerramientaLuz.setOnClickListener(abrirMedidorLuz);

        View.OnClickListener abrirAsesor = v -> abrirAsesor();
        binding.itemHerramientaAsesor.setOnClickListener(abrirAsesor);
        binding.btnHerramientaAsesor.setOnClickListener(abrirAsesor);

        View.OnClickListener abrirPoda = v -> abrirPoda();
        binding.itemHerramientaPoda.setOnClickListener(abrirPoda);
        binding.btnHerramientaPoda.setOnClickListener(abrirPoda);

        View.OnClickListener abrirTrasplante = v -> abrirTrasplante();
        binding.itemHerramientaTrasplante.setOnClickListener(abrirTrasplante);
        binding.btnHerramientaTrasplante.setOnClickListener(abrirTrasplante);
    }

    private void abrirCalculadoraRiego() {
        if (kit == null) return;
        Bundle args = new Bundle();
        args.putString("kitId", kit.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_detalle_to_calculadora, args);
    }

    private void abrirMedidorLuz() {
        if (kit == null) return;
        Bundle args = new Bundle();
        args.putString("kitId", kit.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_detalle_to_medidorLuz, args);
    }

    private void abrirAsesor() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_detalle_to_asesor);
    }

    private void abrirPoda() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_detalle_to_poda);
    }

    private void abrirTrasplante() {
        if (kit == null) return;
        Bundle args = new Bundle();
        args.putString("kitId", kit.getId());
        Navigation.findNavController(requireView())
                .navigate(R.id.action_detalle_to_trasplante, args);
    }

    //.........................................................................
    // Footer (editar + eliminar)

    private void configurarFooter() {
        binding.btnEditarKit.setOnClickListener(v -> mostrarDialogoEditar());
        binding.btnEliminarKit.setOnClickListener(v -> mostrarDialogoEliminar());
    }

    /**
     * Diálogo para editar nombre y especie. Construye el formulario por código
     * (dos campos) para no depender de un layout extra. Al guardar, actualiza el
     * kit, lo persiste en Firestore y repinta la pantalla.
     */
    private void mostrarDialogoEditar() {
        if (kit == null) return;

        // Contenedor vertical con padding
        android.widget.LinearLayout contenedor = new android.widget.LinearLayout(requireContext());
        contenedor.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        contenedor.setPadding(pad, pad / 2, pad, 0);

        // Campo nombre
        final android.widget.EditText etNombre = new android.widget.EditText(requireContext());
        etNombre.setHint("Nombre");
        etNombre.setText(kit.getNombre());
        contenedor.addView(etNombre);

        // Campo especie
        final android.widget.EditText etEspecie = new android.widget.EditText(requireContext());
        etEspecie.setHint("Especie");
        etEspecie.setText(kit.getEspecie());
        contenedor.addView(etEspecie);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Editar kit")
                .setView(contenedor)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String nuevoNombre = etNombre.getText().toString().trim();
                    String nuevaEspecie = etEspecie.getText().toString().trim();

                    if (!nuevoNombre.isEmpty()) {kit.setNombre(nuevoNombre);}
                    if (!nuevaEspecie.isEmpty()) {kit.setEspecie(nuevaEspecie);}

                    KitRepository.getInstance().actualizarKit(kit);

                    Toast.makeText(getContext(), "Kit actualizado", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarDialogoEliminar() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar kit")
                .setMessage("¿Seguro que quieres eliminar este kit? Esta acción no se puede deshacer.")
                .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                    if (kit != null) {
                        KitRepository.getInstance().eliminarKit(kit.getId());
                    }
                    Toast.makeText(getContext(), "Kit eliminado", Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
