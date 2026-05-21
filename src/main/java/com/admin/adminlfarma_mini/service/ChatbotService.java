package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Producto;
import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.entity.Factura;
import com.admin.adminlfarma_mini.entity.Asistencia;
import com.admin.adminlfarma_mini.entity.Auditoria;
import com.admin.adminlfarma_mini.entity.Cliente;
import com.admin.adminlfarma_mini.entity.ConfiguracionSistema;
import com.admin.adminlfarma_mini.entity.Novedad;
import com.admin.adminlfarma_mini.entity.Proveedor;
import com.admin.adminlfarma_mini.entity.ResultadoOptimizacion;
import com.admin.adminlfarma_mini.repository.ProductoRepository;
import com.admin.adminlfarma_mini.repository.UsuarioRepository;
import com.admin.adminlfarma_mini.repository.FacturaRepository;
import com.admin.adminlfarma_mini.repository.AsistenciaRepository;
import com.admin.adminlfarma_mini.repository.AuditoriaRepository;
import com.admin.adminlfarma_mini.repository.ClienteRepository;
import com.admin.adminlfarma_mini.repository.ConfiguracionRepository;
import com.admin.adminlfarma_mini.repository.NovedadRepository;
import com.admin.adminlfarma_mini.repository.ProveedorRepository;
import com.admin.adminlfarma_mini.repository.ResultadoOptimizacionRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final ChatClient chatClient;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final FacturaRepository facturaRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final ClienteRepository clienteRepository;
    private final ConfiguracionRepository configuracionRepository;
    private final NovedadRepository novedadRepository;
    private final ProveedorRepository proveedorRepository;
    private final ResultadoOptimizacionRepository resultadoOptimizacionRepository;
    
    // Memoria de conversación por sesión acotada
    private final Map<String, List<ChatMessage>> chatHistory = new ConcurrentHashMap<>();

    public ChatbotService(ChatClient.Builder chatClientBuilder,
                          ProductoRepository productoRepository,
                          UsuarioRepository usuarioRepository,
                          FacturaRepository facturaRepository,
                          AsistenciaRepository asistenciaRepository,
                          AuditoriaRepository auditoriaRepository,
                          ClienteRepository clienteRepository,
                          ConfiguracionRepository configuracionRepository,
                          NovedadRepository novedadRepository,
                          ProveedorRepository proveedorRepository,
                          ResultadoOptimizacionRepository resultadoOptimizacionRepository) {
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.facturaRepository = facturaRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.clienteRepository = clienteRepository;
        this.configuracionRepository = configuracionRepository;
        this.novedadRepository = novedadRepository;
        this.proveedorRepository = proveedorRepository;
        this.resultadoOptimizacionRepository = resultadoOptimizacionRepository;
        this.chatClient = chatClientBuilder.build();
    }

    // Estructura interna para almacenar el historial de conversación
    public static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    /**
     * Sobrecarga de compatibilidad para llamadas antiguas sin chat session ID.
     */
    public String chat(String userMessage) {
        return chat("default-session", userMessage, "OWNER");
    }

    /**
     * Sobrecarga de compatibilidad para llamadas antiguas sin rol.
     */
    public String chat(String chatId, String userMessage) {
        return chat(chatId, userMessage, "OWNER");
    }

    /**
     * Método principal que maneja el chat inyectando contexto inteligente, memoria acotada y conciencia de roles.
     */
    public String chat(String chatId, String userMessage, String userRole) {
        String safeChatId = chatId != null ? chatId : "default-session";
        String cleanMsg = userMessage != null ? userMessage.toLowerCase().trim() : "";
        String roleStr = userRole != null ? userRole.toUpperCase() : "EMPLEADO";

        // 1. Enrutamiento de Intenciones (Intent Routing) con Fallback General
        String intention = "General"; // Fallback por defecto (LLM puro / de propósito general)
        String contextData = "";

        // Palabras clave de dominios — Orden de declaración: de más específico a más genérico
        boolean hasOfertaKeyword = cleanMsg.contains("descuento") || cleanMsg.contains("descuentos") || 
                                    cleanMsg.contains("oferta") || cleanMsg.contains("ofertas") || 
                                    cleanMsg.contains("promocion") || cleanMsg.contains("promoción") || 
                                    cleanMsg.contains("promociones") || cleanMsg.contains("rebaja") || 
                                    cleanMsg.contains("rebajas");

        boolean hasAsistenciaKeyword = cleanMsg.contains("asistencia") || cleanMsg.contains("asistencias") || 
                                       cleanMsg.contains("tardanza") || cleanMsg.contains("tardanzas") || 
                                       cleanMsg.contains("puntualidad") || cleanMsg.contains("horario") || 
                                       cleanMsg.contains("horarios") || cleanMsg.contains("entrada") || 
                                       cleanMsg.contains("salida") || cleanMsg.contains("check-in") || 
                                       cleanMsg.contains("checkin") || cleanMsg.contains("check-out") || 
                                       cleanMsg.contains("checkout") || cleanMsg.contains("horas trabajadas") || 
                                       cleanMsg.contains("jornada") || cleanMsg.contains("jornadas") || 
                                       cleanMsg.contains("ausencia") || cleanMsg.contains("ausencias") || 
                                       cleanMsg.contains("presente") || cleanMsg.contains("presentes");

        boolean hasProveedorKeyword = cleanMsg.contains("proveedor") || cleanMsg.contains("proveedores") || 
                                      cleanMsg.contains("suministro") || cleanMsg.contains("suministros") || 
                                      cleanMsg.contains("distribuidor") || cleanMsg.contains("distribuidores") || 
                                      cleanMsg.contains("abastecimiento") || cleanMsg.contains("compra") || 
                                      cleanMsg.contains("compras") || cleanMsg.contains("pedido") || 
                                      cleanMsg.contains("pedidos") || cleanMsg.contains("surtir") || 
                                      cleanMsg.contains("surtido") || cleanMsg.contains("reabastecer") || 
                                      cleanMsg.contains("reabastecimiento") || cleanMsg.contains("laboratorio") || 
                                      cleanMsg.contains("laboratorios");

        boolean hasClienteKeyword = cleanMsg.contains("cliente") || cleanMsg.contains("clientes") || 
                                     cleanMsg.contains("comprador") || cleanMsg.contains("compradores") || 
                                     cleanMsg.contains("consumidor") || cleanMsg.contains("consumidores") || 
                                     cleanMsg.contains("frecuente") || cleanMsg.contains("frecuentes") || 
                                     cleanMsg.contains("fidelidad");

        boolean hasNovedadKeyword = cleanMsg.contains("novedad") || cleanMsg.contains("novedades") || 
                                    cleanMsg.contains("incidencia") || cleanMsg.contains("incidencias") || 
                                    cleanMsg.contains("incapacidad") || cleanMsg.contains("incapacidades") || 
                                    cleanMsg.contains("calamidad") || cleanMsg.contains("permiso") || 
                                    cleanMsg.contains("permisos") || cleanMsg.contains("vacacion") || 
                                    cleanMsg.contains("vacaciones") || cleanMsg.contains("reporte") || 
                                    cleanMsg.contains("reportes") || cleanMsg.contains("pendiente") || 
                                    cleanMsg.contains("pendientes") || cleanMsg.contains("solicitud") || 
                                    cleanMsg.contains("solicitudes");

        boolean hasAuditoriaKeyword = cleanMsg.contains("auditoria") || cleanMsg.contains("auditoría") || 
                                       cleanMsg.contains("log") || cleanMsg.contains("logs") || 
                                       cleanMsg.contains("registro de actividad") || cleanMsg.contains("registros de actividad") || 
                                       cleanMsg.contains("historial de cambios") || 
                                       cleanMsg.contains("acción realizada") || cleanMsg.contains("acciones realizadas") || 
                                       cleanMsg.contains("quién hizo") || cleanMsg.contains("quien hizo") || 
                                       cleanMsg.contains("quién modificó") || cleanMsg.contains("quien modifico") || 
                                       cleanMsg.contains("quién eliminó") || cleanMsg.contains("quien elimino") || 
                                       cleanMsg.contains("trazabilidad") || cleanMsg.contains("rastro");

        boolean hasOptimizacionKeyword = cleanMsg.contains("optimización") || cleanMsg.contains("optimizacion") || 
                                          cleanMsg.contains("optimizar") || cleanMsg.contains("pedido inteligente") || 
                                          cleanMsg.contains("pedido automatico") || cleanMsg.contains("pedido automático") || 
                                          cleanMsg.contains("sugerencia de compra") || cleanMsg.contains("sugerencias de compra") || 
                                          cleanMsg.contains("restock") || cleanMsg.contains("plan de compra") || 
                                          cleanMsg.contains("solver") || cleanMsg.contains("timefold");

        boolean hasConfiguracionKeyword = cleanMsg.contains("configuración") || cleanMsg.contains("configuracion") || 
                                           cleanMsg.contains("config") || cleanMsg.contains("ajuste") || 
                                           cleanMsg.contains("ajustes") || cleanMsg.contains("nombre de la farmacia") || 
                                           cleanMsg.contains("nombre farmacia") || cleanMsg.contains("nit") || 
                                           cleanMsg.contains("umbral") || cleanMsg.contains("stock bajo");

        boolean hasUsuarioKeyword = cleanMsg.contains("usuario") || cleanMsg.contains("usuarios") || 
                                     cleanMsg.contains("empleado") || cleanMsg.contains("empleados") || 
                                     cleanMsg.contains("vendedor") || cleanMsg.contains("vendedores") ||
                                     cleanMsg.contains("rol") || cleanMsg.contains("roles") ||
                                     cleanMsg.contains("personal");

        boolean hasVentaKeyword = cleanMsg.contains("venta") || cleanMsg.contains("ventas") || 
                                   cleanMsg.contains("factura") || cleanMsg.contains("facturas") || 
                                   cleanMsg.contains("vendido") || cleanMsg.contains("vender") || 
                                   cleanMsg.contains("ingreso") || cleanMsg.contains("ingresos") || 
                                   cleanMsg.contains("recaudado") || cleanMsg.contains("ganancia") || 
                                   cleanMsg.contains("ganancias") || cleanMsg.contains("facturado");

        boolean hasProductoKeyword = cleanMsg.contains("producto") || cleanMsg.contains("productos") || 
                                      cleanMsg.contains("stock") || cleanMsg.contains("inventario") || 
                                      cleanMsg.contains("medicamento") || cleanMsg.contains("medicamentos") || 
                                      cleanMsg.contains("bajo stock") || cleanMsg.contains("pocas unidades") || 
                                      cleanMsg.contains("por agotarse") || cleanMsg.contains("sin stock") || 
                                      cleanMsg.contains("agotado") || cleanMsg.contains("stock minimo") || 
                                      cleanMsg.contains("stock mínimo") || cleanMsg.contains("faltantes") || 
                                      cleanMsg.contains("precio") || cleanMsg.contains("precios") || 
                                      cleanMsg.contains("disponible") || cleanMsg.contains("disponibles") || 
                                      cleanMsg.contains("cuesta") || cleanMsg.contains("cuestan") || 
                                      cleanMsg.contains("código") || cleanMsg.contains("codigo") || 
                                      cleanMsg.contains("pastilla") || cleanMsg.contains("pastillas") || 
                                      cleanMsg.contains("jarabe") || cleanMsg.contains("tableta") || 
                                      cleanMsg.contains("tabletas") || cleanMsg.contains("cápsula") || 
                                      cleanMsg.contains("capsulas");

        boolean isDomainQuery = hasOfertaKeyword || hasAsistenciaKeyword || hasProveedorKeyword || 
                                hasClienteKeyword || hasNovedadKeyword || hasAuditoriaKeyword || 
                                hasOptimizacionKeyword || hasConfiguracionKeyword || 
                                hasUsuarioKeyword || hasVentaKeyword || hasProductoKeyword;

        // Si no tiene palabras clave del dominio, pero es una consulta sumamente corta (1 o 2 palabras) 
        // de términos de búsqueda que no son stopWords, asumimos que es una búsqueda directa de medicamento (ej: "Aspirina").
        if (!isDomainQuery) {
            String[] tokens = cleanMsg.split("\\s+");
            if (tokens.length > 0 && tokens.length <= 2) {
                Set<String> stopWords = getStopWordsList();
                boolean allSearchTerms = true;
                for (String t : tokens) {
                    if (t.length() < 3 || stopWords.contains(t)) {
                        allSearchTerms = false;
                        break;
                    }
                }
                if (allSearchTerms) {
                    isDomainQuery = true;
                }
            }
        }

        // Determinar la intención — Orden de prioridad: más específico primero
        if (isDomainQuery) {
            if (hasOfertaKeyword) {
                intention = "Ofertas";
            } else if (hasAsistenciaKeyword) {
                intention = "Asistencias";
            } else if (hasOptimizacionKeyword) {
                intention = "Optimizacion";
            } else if (hasProveedorKeyword) {
                intention = "Proveedores";
            } else if (hasClienteKeyword) {
                intention = "Clientes";
            } else if (hasNovedadKeyword) {
                intention = "Novedades";
            } else if (hasAuditoriaKeyword) {
                intention = "Auditoria";
            } else if (hasConfiguracionKeyword) {
                intention = "Configuracion";
            } else if (hasUsuarioKeyword) {
                intention = "Usuarios";
            } else if (hasVentaKeyword) {
                intention = "Ventas";
            } else {
                intention = "Productos";
            }
        }

        // 2. Procesar contexto según la intención detectada
        // --- OFERTAS ---
        if (intention.equals("Ofertas")) {
            List<Producto> productosOferta = productoRepository.findByPrecioOriginalNotNullAndActivoTrue();
            if (productosOferta.isEmpty()) {
                contextData = "Los siguientes productos están en descuento: [No hay productos en descuento en este momento]\n";
            } else {
                StringBuilder sb = new StringBuilder();
                sb.append("Los siguientes productos están en descuento:\n");
                for (Producto p : productosOferta) {
                    double actual = p.getPrecio() != null ? p.getPrecio() : 0.0;
                    double original = p.getPrecioOriginal() != null ? p.getPrecioOriginal() : actual;
                    double ahorroPct = 0.0;
                    if (original > 0) {
                        ahorroPct = ((original - actual) / original) * 100;
                    }
                    sb.append(String.format("- %s (Código: %s) | Precio Oferta: $%.2f | Precio Original: $%.2f (Ahorro: %.0f%%) | Stock: %d unidades | Categoría: %s\n",
                            p.getNombre() != null ? p.getNombre() : "N/A",
                            p.getCodigo() != null ? p.getCodigo() : "N/A",
                            actual,
                            original,
                            ahorroPct,
                            p.getCantidad() != null ? p.getCantidad() : 0,
                            p.getCategoria() != null ? p.getCategoria() : "N/A"
                    ));
                }
                contextData = sb.toString();
            }

        // --- ASISTENCIAS ---
        } else if (intention.equals("Asistencias")) {
            if (!roleStr.contains("OWNER") && !roleStr.contains("ADMIN")) {
                contextData = "[No tienes autorización para consultar el módulo de asistencias. Contacta al administrador.]\n";
            } else {
                boolean askingForCount = cleanMsg.contains("cuantos") || cleanMsg.contains("cuántos") || 
                                         cleanMsg.contains("cuantas") || cleanMsg.contains("cuántas") || 
                                         cleanMsg.contains("total");
                StringBuilder sb = new StringBuilder();
                if (askingForCount) {
                    long totalAsistencias = asistenciaRepository.count();
                    sb.append("El total de registros de asistencia en el sistema es: ").append(totalAsistencias).append("\n");
                }

                List<Asistencia> asistenciasHoy = asistenciaRepository.findByFechaOrderByHoraEntradaAsc(LocalDate.now());
                if (asistenciasHoy.isEmpty()) {
                    sb.append("\nASISTENCIAS DE HOY: [No hay registros de asistencia para el día de hoy]\n");
                } else {
                    sb.append("\nASISTENCIAS REGISTRADAS HOY (").append(LocalDate.now()).append("):\n");
                    int limit = Math.min(asistenciasHoy.size(), 10);
                    for (int i = 0; i < limit; i++) {
                        Asistencia a = asistenciasHoy.get(i);
                        String nombreEmpleado = "N/A";
                        if (a.getUsuario() != null) {
                            nombreEmpleado = (a.getUsuario().getNombre() != null ? a.getUsuario().getNombre() : "") + " " +
                                             (a.getUsuario().getApellido() != null ? a.getUsuario().getApellido() : "");
                            nombreEmpleado = nombreEmpleado.trim().isEmpty() ? a.getUsuario().getUsername() : nombreEmpleado.trim();
                        }
                        DateTimeFormatter hFmt = DateTimeFormatter.ofPattern("HH:mm");
                        String entrada = a.getHoraEntrada() != null ? a.getHoraEntrada().format(hFmt) : "N/A";
                        String salida = a.getHoraSalida() != null ? a.getHoraSalida().format(hFmt) : "En curso";
                        sb.append(String.format("- Empleado: %s | Entrada: %s | Salida: %s | Horas: %s\n",
                                nombreEmpleado, entrada, salida, a.getHorasTrabajadas()));
                    }
                }
                contextData = sb.toString();
            }

        // --- PROVEEDORES ---
        } else if (intention.equals("Proveedores")) {
            if (!roleStr.contains("OWNER") && !roleStr.contains("ADMIN")) {
                contextData = "[No tienes autorización para consultar el módulo de proveedores. Contacta al administrador.]\n";
            } else {
                boolean askingForCount = cleanMsg.contains("cuantos") || cleanMsg.contains("cuántos") || 
                                         cleanMsg.contains("cuantas") || cleanMsg.contains("cuántas") || 
                                         cleanMsg.contains("total");
                StringBuilder sb = new StringBuilder();

                List<Proveedor> proveedoresActivos = proveedorRepository.findByActivoTrue();
                if (askingForCount) {
                    sb.append("El total de proveedores activos registrados es: ").append(proveedoresActivos.size()).append("\n");
                }

                // Buscar si el usuario mencionó un proveedor específico por nombre
                List<Proveedor> matching = new ArrayList<>();
                String[] tokens = cleanMsg.split("\\s+");
                Set<String> stopWords = getStopWordsList();
                for (String token : tokens) {
                    if (token.length() >= 3 && !stopWords.contains(token)) {
                        List<Proveedor> found = proveedorRepository.findByNombreContainingIgnoreCaseAndActivoTrue(token);
                        for (Proveedor p : found) {
                            if (!matching.contains(p) && matching.size() < 10) {
                                matching.add(p);
                            }
                        }
                    }
                }

                List<Proveedor> listaFinal = matching.isEmpty() ? proveedoresActivos : matching;
                int limit = Math.min(listaFinal.size(), 10);

                if (listaFinal.isEmpty()) {
                    sb.append("\nPROVEEDORES: [No hay proveedores activos registrados]\n");
                } else {
                    sb.append(matching.isEmpty() ? "\nLISTA DE PROVEEDORES ACTIVOS:\n" : "\nPROVEEDORES COINCIDENTES:\n");
                    for (int i = 0; i < limit; i++) {
                        Proveedor p = listaFinal.get(i);
                        sb.append(String.format("- %s | Empresa: %s | Teléfono: %s | Email: %s | Tipo Productos: %s\n",
                                p.getNombre() != null ? p.getNombre() : "N/A",
                                p.getEmpresa() != null ? p.getEmpresa() : "N/A",
                                p.getTelefono() != null ? p.getTelefono() : "N/A",
                                p.getEmail() != null ? p.getEmail() : "N/A",
                                p.getTipoProductos() != null ? p.getTipoProductos() : "N/A"
                        ));
                    }
                }
                contextData = sb.toString();
            }

        // --- CLIENTES ---
        } else if (intention.equals("Clientes")) {
            if (!roleStr.contains("OWNER") && !roleStr.contains("ADMIN")) {
                contextData = "[No tienes autorización para consultar el módulo de clientes. Contacta al administrador.]\n";
            } else {
                boolean askingForCount = cleanMsg.contains("cuantos") || cleanMsg.contains("cuántos") || 
                                         cleanMsg.contains("cuantas") || cleanMsg.contains("cuántas") || 
                                         cleanMsg.contains("total");
                StringBuilder sb = new StringBuilder();
                if (askingForCount) {
                    long totalClientes = clienteRepository.count();
                    sb.append("El total de clientes registrados es: ").append(totalClientes).append("\n");
                }

                List<Cliente> clientes = clienteRepository.findAllByOrderByNombreAsc();
                int limit = Math.min(clientes.size(), 10);

                if (clientes.isEmpty()) {
                    sb.append("\nCLIENTES: [No hay clientes registrados]\n");
                } else {
                    sb.append("\nLISTA DE CLIENTES REGISTRADOS:\n");
                    for (int i = 0; i < limit; i++) {
                        Cliente c = clientes.get(i);
                        if (Boolean.TRUE.equals(c.getEsConsumidorFinal())) continue; // Omitir consumidor final genérico
                        if (roleStr.contains("OWNER")) {
                            // OWNER ve todos los campos incluyendo identificación
                            sb.append(String.format("- %s | Código: %s | Teléfono: %s | Email: %s | Identificación: %s\n",
                                    c.getNombre() != null ? c.getNombre() : "N/A",
                                    c.getCodigo() != null ? c.getCodigo() : "N/A",
                                    c.getTelefono() != null ? c.getTelefono() : "N/A",
                                    c.getEmail() != null ? c.getEmail() : "N/A",
                                    c.getIdentificacion() != null ? c.getIdentificacion() : "N/A"
                            ));
                        } else {
                            // ADMIN NO ve la identificación por seguridad de datos
                            sb.append(String.format("- %s | Código: %s | Teléfono: %s | Email: %s\n",
                                    c.getNombre() != null ? c.getNombre() : "N/A",
                                    c.getCodigo() != null ? c.getCodigo() : "N/A",
                                    c.getTelefono() != null ? c.getTelefono() : "N/A",
                                    c.getEmail() != null ? c.getEmail() : "N/A"
                            ));
                        }
                    }
                }
                contextData = sb.toString();
            }

        // --- NOVEDADES ---
        } else if (intention.equals("Novedades")) {
            if (!roleStr.contains("OWNER") && !roleStr.contains("ADMIN")) {
                contextData = "[No tienes autorización para consultar el módulo de novedades. Contacta al administrador.]\n";
            } else {
                StringBuilder sb = new StringBuilder();
                List<Novedad> pendientes = novedadRepository.findByEstadoOrderByFechaRegistroAsc(Novedad.Estado.PENDIENTE);
                sb.append("Novedades PENDIENTES de revisión: ").append(pendientes.size()).append("\n");

                if (!pendientes.isEmpty()) {
                    sb.append("\nNOVEDADES PENDIENTES:\n");
                    int limit = Math.min(pendientes.size(), 8);
                    for (int i = 0; i < limit; i++) {
                        Novedad n = pendientes.get(i);
                        String solicitante = "N/A";
                        if (n.getUsuario() != null) {
                            solicitante = (n.getUsuario().getNombre() != null ? n.getUsuario().getNombre() : "") + " " +
                                          (n.getUsuario().getApellido() != null ? n.getUsuario().getApellido() : "");
                            solicitante = solicitante.trim().isEmpty() ? n.getUsuario().getUsername() : solicitante.trim();
                        }
                        String motivo = n.getMotivo() != null ? n.getMotivo() : "N/A";
                        if (motivo.length() > 60) motivo = motivo.substring(0, 60) + "...";

                        sb.append(String.format("- Tipo: %s | Solicitante: %s | Desde: %s Hasta: %s | Motivo: %s | Registrada: %s\n",
                                n.getTipo() != null ? n.getTipo().name() : "N/A",
                                solicitante,
                                n.getFechaInicio() != null ? n.getFechaInicio().toString() : "N/A",
                                n.getFechaFin() != null ? n.getFechaFin().toString() : "N/A",
                                motivo,
                                n.getFechaRegistro() != null ? n.getFechaRegistro().toLocalDate().toString() : "N/A"
                        ));
                    }
                }
                contextData = sb.toString();
            }

        // --- AUDITORÍA (Solo OWNER) ---
        } else if (intention.equals("Auditoria")) {
            if (!roleStr.contains("OWNER")) {
                contextData = "[No tienes autorización para consultar el módulo de auditoría. Esta información es exclusiva del Owner.]\n";
            } else {
                StringBuilder sb = new StringBuilder();
                boolean askingForCount = cleanMsg.contains("cuantos") || cleanMsg.contains("cuántos") || 
                                         cleanMsg.contains("cuantas") || cleanMsg.contains("cuántas") || 
                                         cleanMsg.contains("total");
                if (askingForCount) {
                    long totalAuditoria = auditoriaRepository.count();
                    sb.append("El total de registros de auditoría es: ").append(totalAuditoria).append("\n");
                }

                List<Auditoria> logs = auditoriaRepository.findAllByOrderByFechaHoraDesc();
                int limit = Math.min(logs.size(), 10);

                if (logs.isEmpty()) {
                    sb.append("\nAUDITORÍA: [No hay registros de auditoría]\n");
                } else {
                    sb.append("\nÚLTIMOS REGISTROS DE AUDITORÍA DEL SISTEMA:\n");
                    DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    for (int i = 0; i < limit; i++) {
                        Auditoria a = logs.get(i);
                        String usuario = "N/A";
                        if (a.getUsuario() != null) {
                            usuario = a.getUsuario().getUsername() != null ? a.getUsuario().getUsername() : "N/A";
                        }
                        String detalle = a.getDetalle() != null ? a.getDetalle() : "";
                        if (detalle.length() > 80) detalle = detalle.substring(0, 80) + "...";

                        sb.append(String.format("- [%s] Usuario: %s | Módulo: %s | Acción: %s | Detalle: %s\n",
                                a.getFechaHora() != null ? a.getFechaHora().format(dtFmt) : "N/A",
                                usuario,
                                a.getModulo() != null ? a.getModulo() : "N/A",
                                a.getAccion() != null ? a.getAccion() : "N/A",
                                detalle
                        ));
                    }
                }
                contextData = sb.toString();
            }

        // --- OPTIMIZACIÓN (Solo OWNER) ---
        } else if (intention.equals("Optimizacion")) {
            if (!roleStr.contains("OWNER")) {
                contextData = "[No tienes autorización para consultar el motor de optimización. Esta información es exclusiva del Owner.]\n";
            } else {
                StringBuilder sb = new StringBuilder();
                Optional<ResultadoOptimizacion> ultimoOpt = resultadoOptimizacionRepository.findTopByOrderByFechaCalculoDesc();

                if (ultimoOpt.isEmpty()) {
                    sb.append("MOTOR DE OPTIMIZACIÓN: [No se ha ejecutado ningún plan de optimización aún]\n");
                } else {
                    ResultadoOptimizacion resultado = ultimoOpt.get();
                    DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    sb.append("ÚLTIMO RESULTADO DEL MOTOR DE OPTIMIZACIÓN:\n");
                    sb.append(String.format("- Score: %s | Hard: %d | Soft: %d | Fecha: %s\n",
                            resultado.getScore() != null ? resultado.getScore() : "N/A",
                            resultado.getHardScore() != null ? resultado.getHardScore() : 0,
                            resultado.getSoftScore() != null ? resultado.getSoftScore() : 0,
                            resultado.getFechaCalculo() != null ? resultado.getFechaCalculo().format(dtFmt) : "N/A"
                    ));

                    // Resumen general si existe
                    if (resultado.getResumen() != null && !resultado.getResumen().isEmpty()) {
                        sb.append("- Resumen: ");
                        resultado.getResumen().forEach((key, value) -> sb.append(key).append(": ").append(value).append(" | "));
                        sb.append("\n");
                    }

                    // Top 5 propuestas
                    if (resultado.getPropuestas() != null && !resultado.getPropuestas().isEmpty()) {
                        sb.append("\nTOP 5 PROPUESTAS DE PEDIDO SUGERIDAS:\n");
                        int limit = Math.min(resultado.getPropuestas().size(), 5);
                        for (int i = 0; i < limit; i++) {
                            Map<String, Object> prop = resultado.getPropuestas().get(i);
                            sb.append(String.format("- Producto: %s | Cantidad Sugerida: %s | Costo Total: %s | Prioridad: %s\n",
                                    prop.getOrDefault("productoNombre", prop.getOrDefault("nombreProducto", "N/A")),
                                    prop.getOrDefault("cantidadSugerida", prop.getOrDefault("cantidadAPedir", "N/A")),
                                    prop.getOrDefault("costoTotal", "N/A"),
                                    prop.getOrDefault("prioridad", "N/A")
                            ));
                        }
                    }
                }
                contextData = sb.toString();
            }

        // --- CONFIGURACIÓN (Solo OWNER) ---
        } else if (intention.equals("Configuracion")) {
            if (!roleStr.contains("OWNER")) {
                contextData = "[No tienes autorización para consultar la configuración del sistema. Esta información es exclusiva del Owner.]\n";
            } else {
                StringBuilder sb = new StringBuilder();
                List<ConfiguracionSistema> configs = configuracionRepository.findAll();

                if (configs.isEmpty()) {
                    sb.append("CONFIGURACIÓN DEL SISTEMA: [No hay configuración registrada]\n");
                } else {
                    ConfiguracionSistema config = configs.get(0); // Singleton
                    sb.append("CONFIGURACIÓN ACTUAL DEL SISTEMA:\n");
                    sb.append(String.format("- Nombre de la Farmacia: %s\n", config.getNombreFarmacia() != null ? config.getNombreFarmacia() : "N/A"));
                    sb.append(String.format("- NIT: %s\n", config.getNit() != null ? config.getNit() : "N/A"));
                    sb.append(String.format("- Dirección: %s\n", config.getDireccion() != null ? config.getDireccion() : "N/A"));
                    sb.append(String.format("- Teléfono: %s\n", config.getTelefono() != null ? config.getTelefono() : "N/A"));
                    sb.append(String.format("- Umbral de Stock Bajo: %d unidades\n", config.getUmbralStockBajo() != null ? config.getUmbralStockBajo() : 5));
                }
                contextData = sb.toString();
            }

        // --- USUARIOS ---
        } else if (intention.equals("Usuarios")) {
            long totalUsuarios = usuarioRepository.count();
            String countLine = "";
            boolean askingForCount = cleanMsg.contains("cuantos") 
                    || cleanMsg.contains("cuántos") 
                    || cleanMsg.contains("total") 
                    || cleanMsg.contains("cantidad");
            if (askingForCount) {
                countLine = "El total de usuarios/empleados registrados es: " + totalUsuarios + "\n";
            }

            List<Usuario> activeUsers = usuarioRepository.findByActivoTrue();
            List<Usuario> matchingUsers = new ArrayList<>();
            for (Usuario u : activeUsers) {
                if ((u.getUsername() != null && cleanMsg.contains(u.getUsername().toLowerCase()))
                        || (u.getNombre() != null && cleanMsg.contains(u.getNombre().toLowerCase()))
                        || (u.getApellido() != null && cleanMsg.contains(u.getApellido().toLowerCase()))
                        || (u.getEmail() != null && cleanMsg.contains(u.getEmail().toLowerCase()))) {
                    matchingUsers.add(u);
                }
            }

            StringBuilder sb = new StringBuilder();
            sb.append(countLine);
            
            if (!matchingUsers.isEmpty()) {
                sb.append("\nDETALLES DE USUARIOS/EMPLEADOS COINCIDENTES ENCONTRADOS:\n");
                for (Usuario u : matchingUsers) {
                    sb.append(String.format("- Nombre: %s %s | Username: %s | Rol: %s | Email: %s | Teléfono: %s | Estado: %s\n",
                            u.getNombre() != null ? u.getNombre() : "N/A",
                            u.getApellido() != null ? u.getApellido() : "N/A",
                            u.getUsername() != null ? u.getUsername() : "N/A",
                            u.getRol() != null ? u.getRol() : "N/A",
                            u.getEmail() != null ? u.getEmail() : "N/A",
                            u.getTelefono() != null ? u.getTelefono() : "N/A",
                            Boolean.TRUE.equals(u.getActivo()) ? "ACTIVO" : "INACTIVO"
                    ));
                }
            } else {
                sb.append("\nLISTA DE USUARIOS/EMPLEADOS ACTIVOS EN LA FARMACIA (Muestra General):\n");
                for (Usuario u : activeUsers) {
                    sb.append(String.format("- %s %s | Username: %s | Rol: %s | Email: %s\n",
                            u.getNombre() != null ? u.getNombre() : "N/A",
                            u.getApellido() != null ? u.getApellido() : "N/A",
                            u.getUsername() != null ? u.getUsername() : "N/A",
                            u.getRol() != null ? u.getRol() : "N/A",
                            u.getEmail() != null ? u.getEmail() : "N/A"
                    ));
                }
            }
            contextData = sb.toString();

        // --- VENTAS ---
        } else if (intention.equals("Ventas")) {
            List<Factura> facturas = facturaRepository.findAll();
            double totalFacturado = facturas.stream()
                    .mapToDouble(f -> f.getTotal() != null ? f.getTotal() : 0.0)
                    .sum();
            long totalVentasCount = facturas.size();

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("El total facturado en ventas (ingresos totales acumulados) es: $%.2f\n", totalFacturado));
            sb.append(String.format("El número total de facturas emitidas en el sistema es: %d\n", totalVentasCount));

            List<Factura> sortedFacturas = facturas.stream()
                    .sorted((f1, f2) -> {
                        if (f1.getFecha() == null) return 1;
                        if (f2.getFecha() == null) return -1;
                        return f2.getFecha().compareTo(f1.getFecha());
                    })
                    .limit(10)
                    .toList();

            if (!sortedFacturas.isEmpty()) {
                sb.append("\nÚLTIMAS 10 TRANSACCIONES / FACTURAS EMITIDAS:\n");
                for (Factura f : sortedFacturas) {
                    sb.append(String.format("- Factura #%s | Cliente: %s | Total: $%.2f | Vendedor/Email: %s | Fecha: %s | Método de Pago: %s\n",
                            f.getNumeroFactura() != null ? f.getNumeroFactura() : "N/A",
                            f.getClienteNombre() != null ? f.getClienteNombre() : "N/A",
                            f.getTotal() != null ? f.getTotal() : 0.0,
                            f.getVendedorEmail() != null ? f.getVendedorEmail() : "N/A",
                            f.getFecha() != null ? f.getFecha().toString() : "N/A",
                            f.getMetodoPago() != null ? f.getMetodoPago() : "EFECTIVO"
                    ));
                }
            }
            contextData = sb.toString();

        // --- PRODUCTOS ---
        } else if (intention.equals("Productos")) {
            boolean askingForLowStock = cleanMsg.contains("bajo stock") 
                    || cleanMsg.contains("pocas unidades") 
                    || cleanMsg.contains("por agotarse") 
                    || cleanMsg.contains("sin stock") 
                    || cleanMsg.contains("agotado") 
                    || cleanMsg.contains("pocos") 
                    || cleanMsg.contains("pocas") 
                    || cleanMsg.contains("stock minimo") 
                    || cleanMsg.contains("stock mínimo") 
                    || cleanMsg.contains("faltantes");

            // Obtener todas las categorías de productos activos
            List<String> categoriasDisponibles = productoRepository.findAllForCategorias().stream()
                    .map(Producto::getCategoria)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            String detectedCategory = null;
            for (String cat : categoriasDisponibles) {
                if (cleanMsg.contains(cat.toLowerCase())) {
                    detectedCategory = cat;
                    break;
                }
            }

            String conteoContextLine = "";
            List<Producto> filteredProducts = new ArrayList<>();

            if (askingForLowStock) {
                long totalBajoStock = productoRepository.countBajoStock();
                conteoContextLine = "Total real de productos con bajo stock: " + totalBajoStock + ". Aquí una muestra: [Lista limitada]\n";
                filteredProducts = productoRepository.findBajoStock(PageRequest.of(0, 10)).getContent();
            } else if (detectedCategory != null) {
                long totalCategoria = productoRepository.countByCategoriaAndActivoTrue(detectedCategory);
                conteoContextLine = "Total real en BD para esta categoría (" + detectedCategory + "): " + totalCategoria + ". Aquí una muestra: [Lista limitada]\n";
                filteredProducts = productoRepository.findProductosActivosPorCategoria(detectedCategory, PageRequest.of(0, 10)).getContent();
            } else {
                boolean askingForCount = cleanMsg.contains("cuantos") 
                        || cleanMsg.contains("cuántos") 
                        || cleanMsg.contains("total") 
                        || cleanMsg.contains("cantidad");
                
                long totalProductos = productoRepository.countActivos();
                if (askingForCount) {
                    conteoContextLine = "Total real de productos registrados (activos) en el sistema: " + totalProductos + ". Aquí una muestra: [Lista limitada]\n";
                }
                filteredProducts = getFilteredInventory(userMessage);
            }

            String inventarioContext = "";
            if (filteredProducts.isEmpty()) {
                inventarioContext = "[No hay productos que coincidan con la búsqueda en el sistema actualmente]";
            } else {
                inventarioContext = filteredProducts.stream()
                        .map(p -> String.format(
                                "- %s (Código: %s) | Precio: $%.2f | Stock: %d unidades | Presentación: %s | Categoría: %s",
                                p.getNombre() != null ? p.getNombre() : "N/A",
                                p.getCodigo() != null ? p.getCodigo() : "N/A",
                                p.getPrecio() != null ? p.getPrecio() : 0.0,
                                p.getCantidad() != null ? p.getCantidad() : 0,
                                p.getPresentacion() != null ? p.getPresentacion() : "N/A",
                                p.getCategoria() != null ? p.getCategoria() : "N/A"
                        ))
                        .collect(Collectors.joining("\n"));
            }

            contextData = conteoContextLine + "\nINVENTARIO REAL FILTRADO DE PRODUCTOS (Medicamentos):\n" + inventarioContext;

        // --- GENERAL (Fallback Inteligente) ---
        } else {
            // Intención 'General': Proporcionar contexto mínimo del sistema para respuestas inteligentes
            long totalProductos = productoRepository.countActivos();
            long totalFacturas = facturaRepository.count();
            long totalUsuarios = usuarioRepository.count();
            StringBuilder generalCtx = new StringBuilder();
            generalCtx.append("CONTEXTO GENERAL DEL SISTEMA L-FARMA:\n");
            generalCtx.append(String.format("- Total de productos activos en inventario: %d\n", totalProductos));
            generalCtx.append(String.format("- Total de facturas emitidas: %d\n", totalFacturas));
            generalCtx.append(String.format("- Total de usuarios registrados: %d\n", totalUsuarios));
            generalCtx.append("- El usuario está haciendo una consulta de propósito general o una pregunta que no se relaciona directamente con un módulo específico del sistema.\n");
            generalCtx.append("- Puedes responder la pregunta general de forma breve y luego ofrecer ayuda con el sistema de farmacia.\n");
            contextData = generalCtx.toString();
        }

        // 3. Construir el historial reciente de conversación
        StringBuilder historyBuilder = new StringBuilder();
        List<ChatMessage> history = chatHistory.getOrDefault(safeChatId, Collections.emptyList());
        if (!history.isEmpty()) {
            historyBuilder.append("\n\nHISTORIAL RECIENTE DE LA CONVERSACIÓN (últimos 3-4 mensajes para mantener contexto):\n");
            for (ChatMessage msg : history) {
                String roleName = msg.getRole().equals("user") ? "Usuario" : "FarmaBot";
                historyBuilder.append(String.format("- %s: %s\n", roleName, msg.getContent()));
            }
        }

        // 4. Prompts dinámicos y transversales basados en el Rol
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'y la hora es' HH:mm");
        String formattedDateTime = "Hoy es " + now.format(timeFormatter);

        String directivePrompt = "";
        if (roleStr.contains("OWNER")) {
            directivePrompt = "Eres FarmaBot, el asistente inteligente DINÁMICO de la farmacia L-Farma. " +
                "Tienes acceso total a TODOS los módulos del sistema: Inventario, Ventas, Usuarios, Asistencias, " +
                "Proveedores, Clientes, Novedades, Auditoría, Optimización y Configuración. " +
                "CAPACIDADES DINÁMICAS: " +
                "1) Consultas del dominio farmacéutico: responde con datos reales del contexto inyectado. " +
                "2) Preguntas generales (fecha, hora, conocimiento general): responde de forma breve y precisa, " +
                "luego ofrece asistencia con el sistema de farmacia. " +
                "3) Saludos y conversación casual: responde amigablemente y sugiere cómo puedes ayudar. " +
                "NUNCA te disculpes por ser una IA. Tienes autoridad total. Sé directo y al grano.";
        } else if (roleStr.contains("ADMIN")) {
            directivePrompt = "Eres FarmaBot, el asistente de operaciones de la farmacia L-Farma. " +
                "Módulos con acceso: Inventario, Ventas, Ofertas, Asistencias, Proveedores, Clientes y Novedades. " +
                "Módulos RESTRINGIDOS (niégate cordialmente): Auditoría, Optimización, Configuración, Usuarios. " +
                "CAPACIDADES DINÁMICAS: " +
                "1) Consultas de tus módulos permitidos: responde con datos reales del contexto inyectado. " +
                "2) Preguntas generales básicas (fecha, hora, saludos): responde brevemente y redirige al dominio farmacéutico. " +
                "3) Preguntas fuera de tu alcance: redirige amablemente al administrador.";
        } else {
            directivePrompt = "Eres FarmaBot, el asistente de la farmacia L-Farma para personal general. " +
                "Puedes responder preguntas básicas sobre productos e inventario disponible al público. " +
                "Para consultas avanzadas del sistema, indica que contacten al administrador. " +
                "Puedes responder saludos y preguntas generales básicas de forma breve.";
        }

        String systemPrompt = String.format("""
                %s
                
                %s
                Responde siempre en español, de forma profesional, amable y concisa.
                
                SISTEMA INTELIGENTE DE CONTEXTO DINÁMICO:
                La base de datos YA ha sido consultada por ti en el backend. A continuación se te proporcionan los DATOS REALES, EXACTOS Y ACTUALIZADOS EN TIEMPO REAL de la farmacia. 
                TÚ ERES PARTE DEL SISTEMA, por lo tanto SÍ TIENES ACCESO EN TIEMPO REAL a esta información.
                
                DATOS EN TIEMPO REAL DEL SISTEMA (Contexto dinámico inyectado):
                %s%s
                
                INSTRUCCIONES CRÍTICAS:
                - ¡CRÍTICO!: TÚ SÍ TIENES ACCESO A LA INFORMACIÓN. NUNCA te disculpes ni digas que no tienes acceso a la información en tiempo real o actualizada. El contexto inyectado arriba ES la información en tiempo real. ¡Asúmelo con autoridad!
                - Para consultas del DOMINIO FARMACÉUTICO: Basa tu respuesta ESTRICTAMENTE en los datos reales del contexto inyectado.
                - Para TOTALES y CONTEOS: Usa SIEMPRE el valor de "Total real" del contexto inyectado.
                - Si el usuario pregunta "cuántos productos hay", responde con el Total Real indicado en el contexto.
                - Usa tablas Markdown si devuelves listas de productos, ventas o asistencias para que se vean bien formateadas.
                - Sé directo, veraz, conciso y profesional.
                """, formattedDateTime, directivePrompt, contextData, historyBuilder.toString());

        // 5. Ejecutar llamada al LLM
        String aiResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

        // 6. Guardar la interacción en memoria acotada (últimos 4 mensajes)
        List<ChatMessage> sessionHistory = chatHistory.computeIfAbsent(safeChatId, k -> new ArrayList<>());
        sessionHistory.add(new ChatMessage("user", userMessage));
        sessionHistory.add(new ChatMessage("assistant", aiResponse));

        while (sessionHistory.size() > 8) {
            sessionHistory.remove(0);
        }

        return aiResponse;
    }

    private String buildInventarioContext(String userMessage) {
        try {
            List<Producto> productos = getFilteredInventory(userMessage);

            if (productos.isEmpty()) {
                return "[No hay productos que coincidan con la búsqueda en el sistema actualmente]";
            }

            return productos.stream()
                    .map(p -> String.format(
                            "- %s (Código: %s) | Precio: $%.2f | Stock: %d unidades | Presentación: %s | Categoría: %s",
                            p.getNombre() != null ? p.getNombre() : "N/A",
                            p.getCodigo() != null ? p.getCodigo() : "N/A",
                            p.getPrecio() != null ? p.getPrecio() : 0.0,
                            p.getCantidad() != null ? p.getCantidad() : 0,
                            p.getPresentacion() != null ? p.getPresentacion() : "N/A",
                            p.getCategoria() != null ? p.getCategoria() : "N/A"
                    ))
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            return "[Error al cargar el inventario: " + e.getMessage() + "]";
        }
    }

    private List<Producto> getFilteredInventory(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return productoRepository.findProductosActivosPaginado(
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "cantidad"))
            ).getContent();
        }

        String cleanMsg = userMessage.toLowerCase()
                .replaceAll("[.,;:!?()\"']", " ")
                .trim();

        boolean askingForLowStock = cleanMsg.contains("bajo stock") 
                || cleanMsg.contains("pocas unidades") 
                || cleanMsg.contains("por agotarse") 
                || cleanMsg.contains("sin stock") 
                || cleanMsg.contains("agotado") 
                || cleanMsg.contains("pocos") 
                || cleanMsg.contains("pocas") 
                || cleanMsg.contains("stock minimo") 
                || cleanMsg.contains("stock mínimo") 
                || cleanMsg.contains("faltantes");

        if (askingForLowStock) {
            return productoRepository.findProductosActivosPaginado(
                    PageRequest.of(0, 12, Sort.by(Sort.Direction.ASC, "cantidad"))
            ).getContent();
        }

        String[] tokens = cleanMsg.split("\\s+");
        Set<String> stopWords = getStopWordsList();
        
        // Evitar que palabras genéricas del dominio se usen para buscar el nombre de un producto específico
        Set<String> domainStopWords = new java.util.HashSet<>(stopWords);
        domainStopWords.addAll(java.util.Arrays.asList("ver", "mostrar", "inventario", "producto", "productos", "medicamento", "medicamentos", "lista", "listar"));

        List<Producto> matching = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 3 && !domainStopWords.contains(token)) {
                List<Producto> found = productoRepository.findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(token, token);
                for (Producto p : found) {
                    if (Boolean.TRUE.equals(p.getActivo()) && matching.size() < 12 && !matching.contains(p)) {
                        matching.add(p);
                    }
                }
            }
            if (matching.size() >= 12) break;
        }

        if (matching.isEmpty()) {
            return productoRepository.findProductosActivosPaginado(
                    PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "cantidad"))
            ).getContent();
        }

        return matching;
    }

    private Set<String> getStopWordsList() {
        return Set.of(
                "cuantos", "hay", "tienen", "tiene", "producto", "productos", "stock", "el", "la", "los", "las", "del", "al", "codigo",
                "un", "una", "unos", "unas", "de", "a", "en", "con", "por", "para", "y", "o", "no", "si", "que", "es", "son", "este",
                "esta", "estos", "estas", "deben", "deberia", "puedes", "puedo", "quiero", "necesito", "buscar", "dame", "muestra",
                "lista", "inventario", "medicamento", "medicamentos", "farmacia", "farmasias", "farmasis", "bot", "hola", "precio",
                "precios", "cantidad", "disponible", "disponibles", "cuanto", "cuántos", "cuesta", "cuestan", "valor", "código",
                "tienes"
        );
    }
}
