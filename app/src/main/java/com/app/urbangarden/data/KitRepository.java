package com.app.urbangarden.data;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.app.urbangarden.model.Kit;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Capa intermedia (SINGLETON) entre la UI y los kits en Cloud Firestore.
 *
 * Usa el mapeo automático de Firestore: {@code set(kit)} para guardar y
 * {@code doc.toObject(Kit.class)} para leer. Los getters calculados de Kit
 * (getDiasReales/getProgresoReal/getHumedadReal) van con {@code @Exclude} para
 * que NO se serialicen como campos. Dos colecciones:
 *   - usuarios/{uid}/kits : los kits del usuario.
 *   - catalogo            : kits "de fábrica"; al vincular por ID se bajan de aquí.
 */
public class KitRepository {

    private static final String TAG = "KitRepository";

    /** Fallback si no hubiera sesión abierta (no debería ocurrir tras el login). */
    private static final String UID_SIN_SESION = "demo-user";
    private static KitRepository instance;

    /** Resultado de intentar vincular un kit del catálogo. */
    public interface VincularCallback {
        void onResultado(boolean encontrado);
    }

    private final CollectionReference kitsRef;
    private final CollectionReference catalogoRef;
    private final MutableLiveData<List<Kit>> kitsLiveData;

    /** Suscripción al listener de Firestore; se suelta al cerrar sesión. */
    private ListenerRegistration registro;

    private KitRepository() {
        FirebaseFirestore fs = FirebaseFirestore.getInstance();
        // Cada usuario tiene su propia "carpeta" de kits, identificada por su uid
        // de Firebase Auth (único por cuenta). Así A y B nunca ven los kits del otro.
        this.kitsRef = fs.collection("usuarios").document(uidActual()).collection("kits");
        this.catalogoRef = fs.collection("catalogo");
        this.kitsLiveData = new MutableLiveData<>(new ArrayList<>());

        escucharCambios();
    }

    /** uid del usuario logueado en Firebase Auth (o un fallback si no hay sesión). */
    private static String uidActual() {
        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();
        return usuario != null ? usuario.getUid() : UID_SIN_SESION;
    }

    //.........................................................................
    // Singleton

    /** Compat: fuerza la creación temprana del singleton para enganchar el listener. */
    public static void init(android.content.Context context) {
        getInstance();
    }

    public static synchronized KitRepository getInstance() {
        if (instance == null) {
            instance = new KitRepository();
        }
        return instance;
    }

    /**
     * Suelta el listener y descarta el singleton. Hay que llamarlo AL CERRAR
     * SESIÓN: el siguiente {@link #getInstance()} (tras el nuevo login) creará
     * un repositorio nuevo apuntando al uid del usuario que acaba de entrar.
     */
    public static synchronized void cerrar() {
        if (instance != null) {
            if (instance.registro != null) {
                instance.registro.remove();
            }
            instance = null;
        }
    }

    //.........................................................................
    // API pública (firmas estables para la UI)

    /** LiveData observable para que las pantallas se actualicen solas. */
    public LiveData<List<Kit>> getKitsLiveData() {
        return kitsLiveData;
    }

    /** Lista actual de kits (último valor recibido del listener). */
    public List<Kit> getKits() {
        List<Kit> kits = kitsLiveData.getValue();
        return kits != null ? kits : new ArrayList<>();
    }

    /** Guarda o actualiza un kit. Firebase traduce el objeto Kit a documento. */
    public void actualizarKit(Kit kit) {
        if (kit == null || kit.getId() == null) return;
        kitsRef.document(kit.getId()).set(kit)
                .addOnFailureListener(e -> Log.w(TAG, "Error al guardar " + kit.getId(), e));
    }

    /** Elimina un kit por su ID (el listener republica la lista). */
    public void eliminarKit(String idKit) {
        if (idKit == null) return;
        kitsRef.document(idKit).delete()
                .addOnFailureListener(e -> Log.w(TAG, "Error al eliminar " + idKit, e));
    }

    /** Busca un kit en el catálogo y, si existe, lo vincula al usuario. */
    public void vincularKit(String idKit, VincularCallback callback) {
        if (idKit == null) {
            if (callback != null) callback.onResultado(false);
            return;
        }

        catalogoRef.document(idKit).get()
                .addOnSuccessListener(doc -> {
                    Kit kitDeFabrica = doc.toObject(Kit.class);
                    if (doc.exists() && kitDeFabrica != null) {
                        kitDeFabrica.setId(doc.getId()); // conservar el ID del documento
                        kitDeFabrica.setFechaActivacion(new Date()); // se activa al vincularlo
                        actualizarKit(kitDeFabrica);
                        if (callback != null) callback.onResultado(true);
                    } else {
                        if (callback != null) callback.onResultado(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error al vincular " + idKit, e);
                    if (callback != null) callback.onResultado(false);
                });
    }

    //.........................................................................
    // Firestore: escucha en tiempo real

    /** Escucha cambios en tiempo real y actualiza el LiveData automáticamente. */
    private void escucharCambios() {
        registro = kitsRef.addSnapshotListener((snapshot, error) -> {
            if (error != null) {
                Log.w(TAG, "Error escuchando los kits", error);
                return;
            }

            List<Kit> kits = new ArrayList<>();
            if (snapshot != null) {
                for (QueryDocumentSnapshot doc : snapshot) {
                    Kit kit = doc.toObject(Kit.class); // mapeo automático documento → Kit
                    kit.setId(doc.getId());
                    kits.add(kit);
                }
            }
            kitsLiveData.setValue(kits);
        });
    }
}
