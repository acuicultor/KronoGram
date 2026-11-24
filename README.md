<p align="center">
  <img src="docs/kronogram_banner.jpg" alt="KronoGram Banner" width="760" />
</p>

<h1 align="center">KronoGram</h1>

<p align="center">
  <strong>Tu cliente de Telegram para Android más privado, personalizable y cómodo</strong>
</p>

<p align="center">
  <strong>🇪🇸 Español</strong> &nbsp;·&nbsp; <a href="README.en.md">🇬🇧 English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Plataforma-Android%207.0%2B-00E5FF?style=for-the-badge&logo=android&logoColor=white" alt="Android 7.0+" />
  <img src="https://img.shields.io/badge/Versi%C3%B3n-13.2.2-0284C7?style=for-the-badge" alt="Versión 13.2.2" />
  <img src="https://img.shields.io/badge/Google%20Play-Pr%C3%B3ximamente-1E293B?style=for-the-badge&logo=google-play&logoColor=00E5FF" alt="Próximamente en Google Play" />
  <img src="https://img.shields.io/badge/Licencia-GPLv2-0F172A?style=for-the-badge" alt="Licencia GPLv2" />
</p>

---

## ¿Qué es KronoGram?

**KronoGram** es un cliente de mensajería para Android basado en el código fuente oficial de **Telegram**. Mantiene toda la velocidad, estabilidad y compatibilidad de Telegram, pero añade un conjunto completo de herramientas enfocadas en **privacidad avanzada, bóveda oculta, personalización visual, transcripción offline, copias de seguridad en la nube y productividad diaria**.

---

## Tabla de contenidos

- [Privacidad, Modo Fantasma y Bóveda Oculta](#privacidad-modo-fantasma-y-bóveda-oculta)
- [Cero Telemetría, Sin Anuncios y Mapas Privados](#cero-telemetría-sin-anuncios-y-mapas-privados)
- [Diseño, Apariencia y Personalización](#diseño-apariencia-y-personalización)
- [Chats, Carpetas, Gestos y Almacenamiento](#chats-carpetas-gestos-y-almacenamiento)
- [Transcripción de Voz Offline y Traducción Gratuita](#transcripción-de-voz-offline-y-traducción-gratuita)
- [Copia de Seguridad en la Nube y Local](#copia-de-seguridad-en-la-nube-y-local)
- [Notificaciones Fiables y Diagnóstico Push](#notificaciones-fiables-y-diagnóstico-push)
- [Compilación desde el código fuente](#compilación-desde-el-código-fuente)
- [Créditos y Agradecimientos](#créditos-y-agradecimientos)
- [Licencia](#licencia)

---

## Características principales

### Privacidad, Modo Fantasma y Bóveda Oculta

- **Modo Fantasma completo:** Actívalo con un solo toque (guardando y restaurando tus preferencias individuales al desactivarlo) o configura cada opción por separado:
  - **No marcar mensajes como leídos** (con control local de lectura y sin enviar confirmación de apertura en multimedia efímera).
  - **No marcar historias como vistas.**
  - **Ocultar el estado «escribiendo...».**
  - **Ocultar el estado «en línea».**
- **Bóveda de Chats y Carpetas Ocultas:**
  - Oculta chats individuales (manteniendo pulsado o con selección múltiple) y carpetas completas (manteniendo pulsada su pestaña).
  - **Acceso rápido y discreto:** Mantén pulsado el título **«KronoGram»** en la barra superior de chats o accede desde *Ajustes de KronoGram*.
  - **Protección multicapa:** Elige entre **Biometría** (huella/rostro), **PIN numérico** (4–8 dígitos almacenado con SHA-256 y sal), **Contraseña personalizada** o **Biometría + PIN/Contraseña**.
  - **Bloqueo de capturas (`FLAG_SECURE`):** Impide automáticamente capturas de pantalla y oculta el contenido en el selector de aplicaciones recientes al abrir la bóveda o un chat oculto.
  - **Control total:** Opción para silenciar notificaciones y previsualizaciones de chats/carpetas ocultos, ocultar los chats pertenecientes a carpetas ocultas en todas partes (incluyendo «Todos» y la búsqueda) y bloquear automáticamente la bóveda al salir de la aplicación.
- **Anti-borrado de mensajes:** Conserva localmente en tu dispositivo los mensajes eliminados por otras personas mostrándolos con un indicador de eliminado, con opción para excluir canales (activada por defecto).
- **Multimedia de 1 sola vista sin límite:** Vuelve a abrir fotos, vídeos, notas de voz y videomensajes efímeros tantas veces como necesites, con una opción dedicada para permitir capturas de pantalla y guardarlos en la galería.
- **Omitir restricciones en chats y canales:** Permite realizar capturas de pantalla, grabación de pantalla, copiar texto, guardar archivos y reenviar contenido en canales, chats, historias y chats secretos con protección contra reenvío.
- **Discreción en pantalla y controles adicionales:**
  - Oculta tu número de teléfono en Ajustes, Cajón lateral y Perfil.
  - Oculta nombres y avatares en la lista principal de chats cuando estés acompañado.
  - Bloquea la creación y aceptación de chats secretos.

---

### Cero Telemetría, Sin Anuncios y Mapas Privados

- **Bloqueo integral con un toque (*Cero Telemetría*)** o activación granular de cada protección:
  - **Bloquear telemetría interna de Telegram:** Detiene envíos de `appLog`, estadísticas de dispositivo y *install referrer*.
  - **Desactivar Firebase Analytics:** Deshabilita en tiempo de ejecución la analítica y la recopilación del ID de publicidad.
  - **Bloquear anuncios y métricas patrocinadas:** Oculta mensajes y vídeos patrocinados en canales y evita registrar métricas de impresiones o clics.
  - **Limpiar rastreadores en enlaces:** Elimina automáticamente parámetros de seguimiento (`utm_*`, `fbclid`, `gclid`, `igshid`, `si`, etc.) antes de abrir cualquier URL.
  - **Desactivar Google Push (FCM):** Permite prescindir del servicio Push de Google y usar el servicio propio en segundo plano.
- **Mapas privados con OpenStreetMap:** Proveedor nativo de mapas integrado mediante mosaicos de OpenStreetMap (`tile.openstreetmap.org`) con soporte de modo oscuro, marcadores y ubicación sin depender de claves de Google Maps.

---

### Diseño, Apariencia y Personalización

- **Temas Monet (Material You):** Elige entre *Monet Light*, *Monet Dark* y *Monet AMOLED* (negro puro ideal para pantallas OLED). En Android 12+ toma dinámicamente las paletas tonales del sistema (`system_accent` / `system_neutral`), con paleta armónica azul pizarra en versiones anteriores y armonización de colores en avatares.
- **Tres estilos visuales de interfaz:** Cambia al instante el aspecto general de la aplicación entre:
  - **Material You (Material 3 / Monet):** Tarjetas modernas con bordes redondeados e interruptores Material 3.
  - **Estilo iOS:** Cabeceras centradas, píldoras e interruptores inspirados en Cupertino.
  - **Estilo Clásico:** Lista plana, limpia y sin bordes.
- **Quitar efecto marquee en títulos:** Detiene el desplazamiento horizontal continuo en títulos largos de cabeceras y visor multimedia.
- **Iconos adaptativos de KronoGram:** Selecciona entre varios iconos de aplicación exclusivos (*Predeterminado*, *Vintage*, *Aqua*, *Premium*, *Turbo* y *Nox*), compatibles con iconos adaptativos y temáticos monocromáticos de Android.

---

### Chats, Carpetas, Gestos y Almacenamiento

- **Pestañas de carpetas totalmente personalizables:**
  - **Contenido:** *Solo texto*, *Icono y texto* o *Solo icono* (con selector de iconos personalizados por carpeta).
  - **Cinco estilos de indicador:** *Predeterminado*, *Redondeado*, *Píldora*, *Subrayado inferior (VKUI)* o *Solo texto (sin indicador)*.
  - Contorno sutil en la pestaña activa, opción para ocultar los contadores de no leídos y opción para mostrar el nombre de la carpeta actual en la barra superior.
  - **Scroll infinito entre carpetas:** Desliza en bucle entre la primera y la última carpeta en lugar de saltar a Contactos (o desactiva el deslizamiento horizontal entre carpetas si lo prefieres).
  - **Carpeta local «Favoritos» (⭐)** y opción para ocultar la pestaña «Todos» cuando tengas carpetas personalizadas.
  - Añade o quita chats de cualquier carpeta rápidamente manteniendo pulsado un chat en la lista.
- **Gestos y mejoras en conversaciones:**
  - **Gesto personalizable al deslizar mensajes:** *Responder*, *Editar (si es propio) / Responder*, *Guardar en Mensajes guardados*, *Traducir*, *Reenviar*, *Copiar texto* o *Desactivado*.
  - **Editar mensaje propio con gesto:** Desliza a la izquierda sobre un mensaje enviado por ti para editarlo al instante.
  - **«Borrar para todos» por defecto** al eliminar mensajes.
  - **Hora exacta con segundos** en las burbujas de mensajes.
  - **Chats anclados ilimitados** (almacenados localmente).
  - **Botón rápido «Ir al mensaje» en búsquedas** para saltar directamente desde archivos, enlaces y audios.
  - **Menú contextual en búsqueda y multimedia compartida:** Mantén pulsado un elemento para acceder rápidamente a *Ir al mensaje*, *Seleccionar* o *Reenviar*.
  - **Desactivar el salto automático al siguiente canal o tema** al hacer scroll hasta el final.
  - **Filtro de palabras bajo Spoiler:** Oculta automáticamente bajo el formato de spoiler de Telegram cualquier lista de palabras que definas (ideal para evitar adelantos de series, películas o deportes).
- **Archivos y control del caché:**
  - **Guardar archivos con su nombre original:** Evita prefijos numéricos al descargar documentos.
  - **Eliminar del caché por mensaje o álbum:** Borra del almacenamiento local el archivo multimedia de un mensaje o álbum concreto directamente desde su menú contextual o el visor multimedia, viendo el espacio liberado al instante.
  - **Limpiador rápido de caché:** Muestra el tamaño actual y limpia la caché temporal con un toque (sin tocar tus chats ni la carpeta de Descargas), más acceso directo al administrador avanzado de almacenamiento.
- **Integración con la búsqueda de Ajustes:** Todos los ajustes de KronoGram aparecen directamente en el buscador global de Ajustes de Telegram.

---

### Transcripción de Voz Offline y Traducción Gratuita

- **Transcripción gratuita de notas de voz y videomensajes (sin Premium ni API keys):**
  - Impulsada por **Vosk** y **Media3 FFmpeg/AudioDecoder**, funcionando de forma 100% local en cualquier versión de Android.
  - **Dos modos:** *Automático (Telegram gratis → Local offline)* o *Siempre local offline (sin servidores)*.
  - **Gestor de modelos offline integrado (~31–48 MB por idioma):** Descarga o limpia modelos ligeros para **8 idiomas** (*Español*, *Inglés (EE. UU.)*, *Portugués*, *Francés*, *Alemán*, *Italiano*, *Ruso* y *Catalán*), además de detección automática según el idioma de la app o del dispositivo.
- **Traducción gratuita de mensajes y chats completos:**
  - Activa la traducción de conversaciones y canales completos sin suscripción a Telegram Premium.
  - **4 motores de traducción seleccionables:** **Google Translate**, **Yandex Translate**, **Lingva Translate** y el traductor oficial de **Telegram**.

---

### Copia de Seguridad en la Nube y Local

- **Copia en la nube de Telegram (`@KronoGramCloudBot`):** Almacena tu configuración en el `CloudStorage` oficial de Telegram mediante MTProto (con compresión GZIP + Base64 fragmentado), sin servidores de terceros ni tokens en el código.
- **Cuatro modos de sincronización:** *Automático (al realizar cambios, con espera inteligente de 20 s y reintento sin conexión)*, *Diario*, *Semanal* y *Manual*.
- **Selección granular (al guardar y al restaurar):** Elige exactamente qué incluir:
  - Ajustes de KronoGram (incluyendo iconos personalizados de carpetas).
  - Ajustes principales de Telegram.
  - Tema y apariencia.
  - Notificaciones de Telegram.
  - Lista de la Bóveda Oculta *(desmarcada por defecto por privacidad)*.
- **Mensajes Guardados y archivos locales (`.kronogram`):** Envía tu copia directamente a *Mensajes Guardados* —toca cualquier archivo `.kronogram` dentro de un chat para restaurarlo al momento— o expórtalo e impórtalo desde el almacenamiento del dispositivo mediante el selector de archivos de Android (SAF).
- **Privacidad y seguridad por diseño:** Nunca exporta sesiones iniciadas, códigos de bloqueo, tokens ni el PIN/contraseña de la Bóveda Oculta.
- **Restablecimiento seguro:** Restaura únicamente los valores por defecto de KronoGram con opción para conservar intacta tu Bóveda Oculta.

---

### Notificaciones Fiables y Diagnóstico Push

- **Servicio persistente en primer plano (anti-cierre OEM):** Notificación silenciosa de baja prioridad diseñada para capas con ahorro agresivo de batería (Xiaomi/HyperOS, Samsung, Huawei, Oppo…) para evitar retrasos o cierres en segundo plano.
- **Conexión TCP MTProto en segundo plano:** Mantiene la recepción de mensajes activa cuando Google Push (FCM) falla o está desactivado.
- **Centro de diagnóstico Push integrado:** Comprueba en un solo panel Google Play Services, estado de FCM, token FCM y registro (con detalles técnicos y copia rápida), permiso de notificaciones, estado de optimización de batería (con accesos directos a los ajustes del sistema), estado del servicio anti-cierre y último push recibido; además permite **forzar el re-registro del token FCM** y enviar una **notificación local de prueba** con un toque.
- **Soporte para Android Auto:** Lee y responde mensajes desde la pantalla del coche mediante notificaciones `MessagingStyle` y la app nativa de Android Auto (`CarAppService`), reproduce notas de voz y explora la música de tus chats respetando el bloqueo por código y la Bóveda Oculta.

---

## Compilación desde el código fuente

### Requisitos
- **JDK:** Java 21
- **Android SDK:** API Level 36 (`compileSdk 36`, `targetSdk 36`, `minSdk 24`, `buildTools 36.0.0`)
- **Gradle:** 9.3.1 (incluido mediante `./gradlew`) · Android Gradle Plugin `9.1.1`
- **ID de aplicación:** `com.rcuenca.kronogram`

### 1. Versión de desarrollo (Debug)
Genera un APK (`arm64-v8a`) listo para instalar firmado con el certificado de depuración:

```bash
./gradlew assembleDebug
```

El archivo generado se guardará en `app/build/outputs/apk/debug/` con el formato `KronoGram-<versión>-<código>-arm64-v8a.apk`.

### 2. Versión de producción (Release)
Genera un APK (`arm64-v8a`) firmado con tu propio certificado (`.jks` o `.keystore`) mediante variables de entorno:

```bash
KEYSTORE_PATH="/ruta/absoluta/a/tu-keystore.jks" \
STORE_PASSWORD="tu_store_password" \
KEY_ALIAS="tu_key_alias" \
KEY_PASSWORD="tu_key_password" \
./gradlew assembleRelease
```

Para generar un Android App Bundle (`.aab`) para Google Play (sin división de ABI en el bundle):

```bash
KEYSTORE_PATH="/ruta/absoluta/a/tu-keystore.jks" \
STORE_PASSWORD="tu_store_password" \
KEY_ALIAS="tu_key_alias" \
KEY_PASSWORD="tu_key_password" \
./gradlew bundleRelease
```

Los artefactos generados se ubican en:
- **APK:** `app/build/outputs/apk/release/`
- **AAB:** `app/build/outputs/bundle/release/`

*(Si omites `KEY_ALIAS`, se utilizará `"upload"` por defecto cuando `KEYSTORE_PATH` esté definido. Si ejecutas `assembleRelease` sin definir `KEYSTORE_PATH`, se utilizará el `debug.keystore` incluido como respaldo).*

---

## Créditos y Agradecimientos

**KronoGram** no existiría sin el trabajo de la comunidad de software libre alrededor de Telegram para Android. Agradecemos especialmente a los siguientes proyectos, forks y bibliotecas por su código base, arquitectura o inspiración en funciones y diseño:

### Cliente base
- **[Telegram for Android](https://github.com/DrKLO/Telegram)** — Cliente oficial de código abierto sobre el que se construye KronoGram.

### Forks de Telegram (código, arquitectura e inspiración)
- **[Nekogram](https://github.com/Nekogram/Nekogram)** — Base de estructura modular del proyecto (Gradle, Media3 y renderizado LaTeX), además de inspiración para traductores alternativos y mejoras de privacidad y usabilidad.
- **[AyuGram](https://github.com/AyuGram/AyuGram4A)** — Referencia e inspiración clave para el Modo Fantasma, el sistema anti-borrado de mensajes, la visualización ilimitada de multimedia de 1 sola vista y la omisión de restricciones de contenido.
- **[exteraGram](https://github.com/exteraSquad/exteraGram)** — Inspiración para los temas dinámicos Monet (Material You), estilos de pestañas de carpetas (redondeado, píldora, subrayado VKUI, iconos) y detalles de interfaz moderna.
- **[Cherrygram](https://github.com/arsLan4k1390/Cherrygram)** — Inspiración para gestos en mensajes, personalización de carpetas y mejoras prácticas de búsqueda y multimedia.
- **[OctoGram](https://github.com/OctoGramApp/OctoGram)** — Inspiración en estilos visuales, organización de ajustes y conceptos de copia de seguridad y exportación de configuración.
- **[OwlGram](https://github.com/OwlGramDev/OwlGram)** — Inspiración en temas Monet y personalización estética de la interfaz y pestañas.
- **[NekoX](https://github.com/NekoX-Dev/NekoX)** — Inspiración para controles avanzados de privacidad, bloqueo de telemetría y opciones para usuarios exigentes.
- **[Telegram FOSS](https://github.com/Telegram-FOSS-Team/Telegram-FOSS)** — Inspiración en prácticas orientadas a la privacidad, modo sin dependencia obligatoria de FCM y uso de OpenStreetMap en lugar de Google Maps.
- **[Forkgram](https://github.com/Forkgram/TelegramAndroid)** — Inspiración para mejoras cotidianas limpias y útiles en chats y archivos.

### Bibliotecas y servicios de código abierto
- **[Vosk (Alpha Cephei)](https://alphacephei.com/vosk/)** — Motor de reconocimiento de voz offline en el dispositivo que hace posible la transcripción gratuita sin servidores.
- **[AndroidX Media3](https://github.com/androidx/media)** — Decodificación de audio y reproducción multimedia.
- **[OpenStreetMap](https://www.openstreetmap.org/)** — Cartografía abierta y gratuita utilizada por el proveedor de mapas integrado en KronoGram.
- **[Lingva Translate](https://github.com/thedaviddelta/lingva-translate)** — Interfaz abierta de traducción respetuosa con la privacidad.
- **[JLaTeXMath](https://github.com/opencollab/jlatexmath) & [Markwon](https://github.com/noties/Markwon)** — Soporte para renderizado de fórmulas matemáticas y texto enriquecido.

---

## Licencia

**KronoGram** es software libre publicado bajo la licencia **GNU General Public License v2.0 (GPLv2)**, en conformidad con el código fuente oficial de Telegram para Android. Consulta el archivo [LICENSE](LICENSE) para más información.
