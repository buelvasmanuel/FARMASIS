package com.admin.adminlfarma_mini.Controller;

import com.admin.adminlfarma_mini.entity.Asistencia;
import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.service.AsistenciaService;
import com.admin.adminlfarma_mini.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AsistenciaController {

    private final AsistenciaService asistenciaService;
    private final UsuarioService usuarioService;

    private Usuario getUsuarioActual(Authentication auth) {
        return usuarioService.buscarPorUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    // ===================== EMPLEADO =====================

    /** Calendario: 12 meses, meses futuros bloqueados */
    @GetMapping("/asistencia/calendario")
    public String calendario(Model model, Authentication auth) {
        Usuario usuario = getUsuarioActual(auth);
        LocalDate hoy = LocalDate.now();
        int anioActual = hoy.getYear();

        // Generar los 12 meses del año actual
        List<Map<String, Object>> meses = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(anioActual, m);
            meses.add(Map.of(
                    "numero", m,
                    "nombre", ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es")),
                    "anio", anioActual,
                    "bloqueado", ym.isAfter(YearMonth.of(hoy.getYear(), hoy.getMonth()))
            ));
        }

        model.addAttribute("meses", meses);
        model.addAttribute("anio", anioActual);
        model.addAttribute("tieneEntradaAbierta", asistenciaService.tieneEntradaAbierta(usuario));
        return "asistencia/calendario";
    }

    /** Vista semanal de un mes específico */
    @GetMapping("/asistencia/semana")
    public String semana(@RequestParam int mes, @RequestParam int anio,
                          Model model, Authentication auth) {
        Usuario usuario = getUsuarioActual(auth);
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.withDayOfMonth(inicio.lengthOfMonth());

        model.addAttribute("registros", asistenciaService.getSemana(usuario, inicio, fin));
        model.addAttribute("mes", YearMonth.of(anio, mes).getMonth()
                .getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es")));
        model.addAttribute("anio", anio);
        return "asistencia/semana";
    }

    /** Registrar entrada */
    @PostMapping("/asistencia/entrada")
    public String registrarEntrada(Authentication auth, RedirectAttributes redirect) {
        try {
            asistenciaService.registrarEntrada(getUsuarioActual(auth));
            redirect.addFlashAttribute("successMsg", "✅ Entrada registrada a las " +
                    java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/asistencia/calendario";
    }

    /** Registrar salida */
    @PostMapping("/asistencia/salida")
    public String registrarSalida(Authentication auth, RedirectAttributes redirect) {
        try {
            asistenciaService.registrarSalida(getUsuarioActual(auth));
            redirect.addFlashAttribute("successMsg", "✅ Salida registrada correctamente.");
        } catch (Exception e) {
            redirect.addFlashAttribute("errorMsg", e.getMessage());
        }
        return "redirect:/asistencia/calendario";
    }

    // ===================== ADMIN =====================

    @GetMapping("/admin/asistencia/hoy")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER')")
    public String asistenciaHoy(Model model) {
        var registros = asistenciaService.getHoy();
        long abiertos = registros.stream().filter(r -> r.getHoraSalida() == null).count();
        long completos = registros.stream().filter(r -> r.getHoraSalida() != null).count();
        model.addAttribute("registros", registros);
        model.addAttribute("totalRegistros", registros.size());
        model.addAttribute("totalAbiertos", abiertos);
        model.addAttribute("totalCompletos", completos);
        model.addAttribute("fecha", java.time.LocalDate.now());
        return "asistencia/admin-hoy";
    }

    // ===================== OWNER — Excel =====================

    @GetMapping("/owner/asistencia/exportar")
    @PreAuthorize("hasRole('OWNER')")
    public String exportarForm(Model model) {
        model.addAttribute("hoy", LocalDate.now());
        return "asistencia/exportar";
    }

    @PostMapping("/owner/asistencia/exportar")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) List<String> roles) throws IOException {

        byte[] excel = asistenciaService.exportarExcel(desde, hasta, roles);
        String filename = "asistencia_" + desde + "_" + hasta + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }
}
