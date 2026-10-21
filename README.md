# 📱 Proyecto Amaranto - Aplicación Móvil para Pacientes

![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-7F52FF?style=for-the-badge&logo=kotlin)
![Android Studio](https://img.shields.io/badge/Android%20Studio-2026.1-3DDC84?style=for-the-badge&logo=android-studio)
![Retrofit](https://img.shields.io/badge/Retrofit-2.11.0-009688?style=for-the-badge)
![MongoDB](https://img.shields.io/badge/MongoDB-amaranto__db-47A248?style=for-the-badge&logo=mongodb)

Aplicación nativa de Android desarrollada en **Kotlin** para pacientes del sistema **Proyecto Amaranto**. La app se conecta a una API RESTful desarrollada en **Laravel** con base de datos **MongoDB** (`amaranto_db`), permitiendo el registro automático con rol paciente, autenticación mediante tokens JWT y la consulta en tiempo real del expediente clínico en modalidad de **SOLO LECTURA**.

---

## 🎨 Identidad Visual y Colores de Marca
La aplicación adopta la paleta de colores institucional del logotipo de **Proyecto Amaranto**:
* 🔴 **Magenta Principal (`#C2004D`):** Títulos principales, tarjetas de expedientes e inputs.
* 🟢 **Verde Hoja (`#38761D`):** Encabezados de marca, eslogan y enlaces de navegación.
* 💖 **Rojo Coral (`#FF4B60`):** Acentos y gráficos secundarios.

---

## ✨ Características Principales
* 🔐 **Asignación Fija de Rol:** En la pantalla de registro el campo `role` no se le muestra al usuario y **siempre** se envía fijado como `"paciente"`.
* 🔑 **Autenticación JWT Segura:** Validación del rol devuelto por Laravel en inicio de sesión y persistencia local del token mediante `SharedPreferences`.
* 📋 **Expediente Clínico de Solo Lectura:** Visualización del historial nutricional (diagnóstico, metas de tratamiento, hábitos de consumo de agua/notas y observaciones médicas) en sincronía con el panel web.
* 🧪 **Modo de Prueba Autónomo (Mock Interceptor):** Permite ejecutar y probar toda la UI y navegación sin necesidad de encender el servidor backend.

---

## 🛠️ Tecnologías y Arquitectura
* **Lenguaje:** Kotlin 2.2+
* **UI:** Layouts XML con ViewBinding y Material Components 3.
* **Red / HTTP:** Retrofit 2 + Gson Converter + OkHttp 4.
* **Concurrencia:** Corrutinas de Kotlin (`lifecycleScope.launch`).
* **Persistencia Local:** SharedPreferences (`SessionManager`).
* **Backend objetivo:** Laravel 10+ REST API + MongoDB (`amaranto_db`).

---

## 🚀 Requisitos Previos e Instalación

### Requisitos:
* **Android Studio** Jellyfish / Koala / Ladybug o superior.
* **JDK 17** configurado en Gradle.
* Dispositivo físico o Emulador Android con **Android 8.0 (API 26)** o superior.

### Pasos para Ejecutar el Proyecto:

1. **Clonar el Repositorio:**
   ```bash
   git clone https://github.com/AlanBAG/proyecto-amaranto-patient-app.git
   cd proyecto-amaranto-patient-app
   ```

2. **Abrir en Android Studio:**
   * Abre Android Studio ➡️ **Open...** ➡️ Selecciona la carpeta del proyecto.
   * Espera a que la Sincronización de Gradle (`Gradle Sync`) termine exitosamente.

3. **Configuración del Servidor Backend (`RetrofitClient.kt`):**
   * Abre `app/src/main/java/com/amaranto/patientapp/api/RetrofitClient.kt`.
   * **Para probar con el Backend Real:**
     ```kotlin
     const val USE_MOCK_DATA = false
     private const val BASE_URL = "http://192.168.X.X:8000/" // Reemplaza por la IP de la laptop servidora
     ```
   * **Para probar sin Servidor (Modo Mock):**
     ```kotlin
     const val USE_MOCK_DATA = true
     ```

4. **Ejecutar la App:**
   * Presiona **Run ▶️** (`Shift + F10`) en Android Studio para instalar en tu dispositivo/emulador.

---

## 📂 Estructura del Código
```text
app/src/main/java/com/amaranto/patientapp/
├── api/
│   ├── ApiService.kt        # Definición de endpoints HTTP en Retrofit
│   └── RetrofitClient.kt    # Cliente Singleton de Retrofit y Mock Interceptor
├── models/
│   ├── AuthModels.kt        # DTOs de Registro, Login y Perfil de Usuario
│   └── HistorialClinicoModel.kt # DTOs para el historial médico en MongoDB
├── utils/
│   └── SessionManager.kt    # Gestión de sesión y Token JWT en SharedPreferences
└── views/
    ├── LoginActivity.kt     # Pantalla de acceso de pacientes
    ├── RegisterActivity.kt  # Pantalla de registro de cuenta de paciente
    └── ProfileActivity.kt   # Expediente clínico de SOLO LECTURA
```

---

## 🤝 Flujo para Contribuir en Equipo

1. **Actualizar la rama principal:**
   ```bash
   git checkout main
   git pull origin main
   ```
2. **Crear una rama para tu tarea:**
   ```bash
   git checkout -b feature/nombre-de-tu-rama
   ```
3. **Guardar y subir tus cambios:**
   ```bash
   git add .
   git commit -m "Descripción clara de tu cambio"
   git push origin feature/nombre-de-tu-rama
   ```
4. **Crear un Pull Request (PR)** en GitHub para revisar y mezclar los cambios hacia `main`.

---

## 🔒 Variables de Entorno y Secrets
Por motivos de seguridad, las credenciales locales y rutas de SDK no se incluyen en el repositorio y son gestionadas mediante `.gitignore` y **GitHub Secrets** (`API_BASE_URL`).
