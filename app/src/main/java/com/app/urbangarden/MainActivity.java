package com.app.urbangarden;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.app.urbangarden.data.GeneradorNotificaciones;
import com.app.urbangarden.data.KitRepository;
import com.app.urbangarden.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    /** TextView del badge de notificaciones (en el action view del toolbar). */
    private TextView tvBadgeNotif;

    //.........................................................................
    // Ciclo de vida

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Crear el repositorio ANTES de inflar: el NavHost crea HomeFragment al
        // inflar y este ya observa KitRepository. Así el listener de Firestore
        // queda enganchado cuanto antes.
        KitRepository.init(this);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Recalcular el badge cada vez que cambian los kits (no solo en onResume).
        KitRepository.getInstance().getKitsLiveData()
                .observe(this, kits -> actualizarBadgeNotificaciones());

        Toolbar toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        configurarNavegacion();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Recalcular el badge al volver al frente (por si cambió el nº de avisos).
        actualizarBadgeNotificaciones();
    }

    //.........................................................................
    // Navegación

    private void configurarNavegacion() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);

        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_kits,
                R.id.navigation_qr,
                R.id.navigation_perfil)
                .build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);

        // Tocar una pestaña SIEMPRE va a la raíz de su sección (vaciando la pila
        // hasta la raíz del grafo), en vez de restaurar la sub-pantalla anterior.
        binding.navView.setOnItemSelectedListener(item -> {
            NavOptions opciones = new NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setPopUpTo(navController.getGraph().getStartDestinationId(), false)
                    .build();
            try {
                navController.navigate(item.getItemId(), null, opciones);
                return true;
            } catch (IllegalArgumentException e) {
                return false; // el item no es un destino navegable
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    //.........................................................................
    // Menú del toolbar

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);

        // Action view personalizado del icono de notificaciones (campana + badge).
        MenuItem itemNotif = menu.findItem(R.id.action_notificaciones);
        if (itemNotif != null && itemNotif.getActionView() != null) {
            View actionView = itemNotif.getActionView();
            tvBadgeNotif = actionView.findViewById(R.id.tvBadgeNotif);

            // Con actionLayout el clic no pasa por onOptionsItemSelected, lo gestionamos aquí.
            actionView.setOnClickListener(v ->
                    navController.navigate(R.id.action_notificaciones));

            // onCreateOptionsMenu corre tras el primer onResume, así que el badge
            // se recalcula aquí, ya con la vista disponible.
            actualizarBadgeNotificaciones();
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (NavigationUI.onNavDestinationSelected(item, navController)) {
            return true;
        }

        int id = item.getItemId();

        if (id == R.id.action_configuracion) {
            Toast.makeText(this, "Configuración", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_perfil) {
            navController.navigate(R.id.navigation_perfil);
            return true;
        } else if (id == R.id.action_cerrar_sesion) {
            mostrarDialogoCerrarSesion();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //.........................................................................
    // Badge de notificaciones

    /** Cuenta las notificaciones y actualiza el globito: oculto si 0, "9+" si >9. */
    private void actualizarBadgeNotificaciones() {
        if (tvBadgeNotif == null) return;

        int total = GeneradorNotificaciones.generar(
                KitRepository.getInstance().getKits()).size();

        if (total <= 0) {
            tvBadgeNotif.setVisibility(View.GONE);
        } else {
            tvBadgeNotif.setVisibility(View.VISIBLE);
            tvBadgeNotif.setText(total > 9 ? "9+" : String.valueOf(total));
        }
    }

    //.........................................................................
    // Cerrar sesión

    /** Confirma y, si acepta, delega el logout en LoginActivity.cerrarSesion(). */
    private void mostrarDialogoCerrarSesion() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cerrar sesión")
                .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                .setPositiveButton("Sí, cerrar sesión",
                        (dialog, which) -> LoginActivity.cerrarSesion(this))
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
