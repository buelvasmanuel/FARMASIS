# 💊 L-Farma: Sistema de Gestión de Farmacias Inteligente

L-Farma es un sistema de gestión empresarial (ERP) y de punto de venta (POS) de alta fidelidad diseñado para farmacias modernas. La plataforma incorpora Inteligencia Artificial (RAG con Llama 3) para la atención al personal y optimización matemática para sugerencias de reabastecimiento inteligente de inventario.

---

## 🚀 Arquitectura y Tecnologías

### **Backend**
*   **Lenguaje:** Java 21
*   **Framework Principal:** Spring Boot 3.x
*   **Seguridad:** Spring Security con hashing BCrypt y Autenticación de Doble Factor (2FA) por correo electrónico.
*   **Integración de IA:** Spring AI para la conexión asíncrona de alto rendimiento con el LLM.
*   **Motor de Optimización:** Timefold Solver para la Investigación de Operaciones (sugerencias de compras maximizando utilidad bajo restricciones presupuestarias).

### **Frontend**
*   **Motor de Plantillas:** Thymeleaf para la composición dinámica de vistas y fragments.
*   **Diseño Visual:** HTML5 nativo y CSS3 Premium (*glassmorphism*, diseños adaptativos, micro-animaciones HSL).
*   **Interactividad:** JavaScript nativo utilizando la **API Fetch** asíncrona para actualizar el DOM en tiempo real sin recarga de página.

### **Persistencia Híbrida**
*   **MySQL (Relacional - Puerto 3307):** Resguarda usuarios, roles de seguridad, registros de asistencias laborales e historial de auditoría de seguridad del sistema.
*   **MongoDB (No Relacional - Nube/Atlas):** Almacena el catálogo de productos (altamente mutable y flexible), el histórico de facturación/ventas y los reportes del motor de optimización matemática.

---

## 📦 Requisitos de Instalación

Asegúrate de contar con las siguientes herramientas en tu entorno de desarrollo local:
*   **Java Development Kit (JDK):** Versión 21 o superior.
*   **Apache Maven:** Versión 3.9 o superior.
*   **MySQL Server:** Configurado en el puerto `3307` (o el puerto que definas en tu properties).
*   **MongoDB:** Instancia local o URI de MongoDB Atlas activa.

---

## 🛠️ Configuración e Instalación

### **1. Clonar el repositorio**
```bash
git clone https://github.com/buelvasmanuel/FARMASIS.git
cd FARMASIS
```

### **2. Configurar Archivo de Secretos**
Por estrictas directivas de **DevSecOps**, el repositorio cuenta con un archivo de seguridad gitignorado para resguardar credenciales sensibles sin exponerlas públicamente.
Crea el archivo `application-secrets.properties` en la ruta:
`src/main/resources/application-secrets.properties`

E inyecta tus credenciales reales:
```properties
# Credenciales de Google OAuth (si aplica)
google.client.id=TU_GOOGLE_CLIENT_ID
google.client.secret=TU_GOOGLE_CLIENT_SECRET

# Credencial de Correo Emisor (Servicio SMTP de Gmail para 2FA)
spring.mail.password=TU_PASSWORD_DE_APLICACION_GMAIL

# Credenciales de Cloudinary (Imágenes de Productos)
cloudinary.cloud-name=TU_CLOUD_NAME
cloudinary.api-key=TU_API_KEY
cloudinary.api-secret=TU_API_SECRET

# Clave API del Asistente Virtual (Groq API Key para Llama 3)
spring.ai.openai.api-key=gsk_TU_GROQ_API_KEY
```

### **3. Compilar y Construir**
Utiliza el wrapper de Maven para limpiar y empaquetar la aplicación:
```bash
mvn clean install
```

### **4. Ejecutar la Aplicación**
Inicia el servidor local de desarrollo:
```bash
mvn spring-boot:run
```
La aplicación estará disponible para su uso en: `http://localhost:8092`

---

## 🔒 Higiene del Repositorio (.gitignore)

El archivo `.gitignore` del proyecto está configurado para cumplir de forma estricta con las mejores prácticas de la industria, asegurando que:
1.  **Archivos Sensibles:** El archivo `application-secrets.properties` queda completamente excluido del control de versiones.
2.  **Basura de Compilación:** Excluye las carpetas `target/`, carpetas temporales de compilación y logs de depuración locales (`*.log`).
3.  **Configuraciones de IDEs:** Ignora configuraciones específicas de entornos como `.idea/` (IntelliJ), `.vscode/` (Visual Studio Code) y `.settings/` (Eclipse/STS).
