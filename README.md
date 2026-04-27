# UrbanGarden — Estructura del Proyecto Android

**Proyecto Final de Grado Superior · Desarrollo de Aplicaciones Multiplataforma (DAM)**
**Stack:** Java 17 · MVVM · Room · JDBC · Material Design 3

---

## Árbol de paquetes

```
UrbanGarden/
│
├── app/
│   └── src/main/
│       │
│       ├── java/com/urbangarden/
│       │   │
│       │   ├── model/                        ← CAPA DE MODELO
│       │   │   └── Kit.java                  ✅ POJO / Entidad de dominio
│       │   │
│       │   ├── data/                         ← CAPA DE DATOS
│       │   │   ├── ConexionHelper.java        ✅ JDBC Singleton (MySQL remoto)
│       │   │   ├── local/
│       │   │   │   ├── UrbanGardenDatabase.java   ← Room Database (SQLite)
│       │   │   │   └── KitDao.java                ← Room DAO (interfaz consultas)
│       │   │   └── repository/
│       │   │       └── KitRepository.java         ← Abstrae local + remoto
│       │   │
│       │   ├── viewmodel/                    ← CAPA DE PRESENTACIÓN (MVVM)
│       │   │   └── KitViewModel.java         ✅ Lógica UI + Executor + LiveData
│       │   │
│       │   └── ui/                           ← CAPA DE INTERFAZ
│       │       ├── MainActivity.java              ← Single Activity + NavHost
│       │       ├── home/
│       │       │   └── HomeFragment.java
│       │       ├── kits/
│       │       │   ├── KitsFragment.java          ← RecyclerView de cultivos
│       │       │   └── KitAdapter.java            ← Adapter + ViewHolder pattern
│       │       ├── qr/
│       │       │   └── QrFragment.java            ← Escaneo QR + vinculación
│       │       └── perfil/
│       │           └── PerfilFragment.java
│       │
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml              ← NavHostFragment + BottomNav
│           │   ├── fragment_qr.xml
│           │   ├── fragment_kits.xml
│           │   └── item_planta.xml           ✅ CardView RecyclerView item
│           ├── navigation/
│           │   └── nav_graph.xml                  ← Navigation Component
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml                     ← Material You / MD3
│           └── font/
│               ├── nunito_bold.ttf
│               └── dm_sans_regular.ttf
│
└── database/
    └── schema_inicial.sql                    ✅ DDL MySQL + SP + seed data
```

---

## Archivos generados en este sprint

| Archivo | Capa | Estado |
|---|---|---|
| `Kit.java` | model | ✅ Completo |
| `ConexionHelper.java` | data | ✅ Completo |
| `KitViewModel.java` | viewmodel | ✅ Completo |
| `item_planta.xml` | ui/layout | ✅ Completo |
| `schema_inicial.sql` | database | ✅ Completo |

---

## Dependencias (build.gradle · módulo app)

```groovy
dependencies {
    // ── Arquitectura Jetpack ────────────────────────────────────────
    implementation 'androidx.lifecycle:lifecycle-viewmodel:2.8.0'
    implementation 'androidx.lifecycle:lifecycle-livedata:2.8.0'
    implementation 'androidx.navigation:navigation-fragment:2.7.7'
    implementation 'androidx.navigation:navigation-ui:2.7.7'

    // ── Persistencia local (Room / SQLite) ──────────────────────────
    implementation 'androidx.room:room-runtime:2.6.1'
    annotationProcessor 'androidx.room:room-compiler:2.6.1'

    // ── Conexión remota MySQL ────────────────────────────────────────
    implementation 'mysql:mysql-connector-java:8.0.33'

    // ── Material Design 3 ───────────────────────────────────────────
    implementation 'com.google.android.material:material:1.12.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // ── Lectura de códigos QR ────────────────────────────────────────
    implementation 'com.journeyapps:zxing-android-embedded:4.3.0'
}
```

---

## Flujo de vinculación QR

```
Usuario escanea QR
       │
       ▼
QrFragment.onQrDecoded(idKit)
       │ llama
       ▼
KitViewModel.procesarEscaneoQr(idKit)        ← UI Thread
       │ valida formato
       │ setState(CARGANDO)
       │ executor.execute(() -> {
       ▼
  ConexionHelper.buscarKitPorId()            ← Hilo secundario (JDBC)
       │ si existe:
  ConexionHelper.vincularKit()               ← Llama SP remoto
       │ si SP devuelve 1:
  guardarKitEnLocal(kit)                     ← Room / SQLite
       │
  mainHandler.post(() ->                     ← Vuelve al UI Thread
    _estadoVinculacion = EXITO
    _kitVinculado = kit
  })
       │
       ▼
QrFragment observa LiveData → muestra éxito / error
```

---

## Permisos requeridos (AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```
