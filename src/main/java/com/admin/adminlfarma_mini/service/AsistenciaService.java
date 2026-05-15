package com.admin.adminlfarma_mini.service;

import com.admin.adminlfarma_mini.entity.Asistencia;
import com.admin.adminlfarma_mini.entity.Usuario;
import com.admin.adminlfarma_mini.repository.AsistenciaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AsistenciaService {

    private final AsistenciaRepository asistenciaRepository;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Empleado registra su entrada */
    @Transactional
    public Asistencia registrarEntrada(Usuario usuario) {
        LocalDate hoy = LocalDate.now();
        Optional<Asistencia> abierta = asistenciaRepository
                .findByUsuarioAndFechaAndHoraSalidaIsNull(usuario, hoy);
        if (abierta.isPresent()) {
            throw new IllegalStateException("Ya tienes una entrada registrada hoy. Registra tu salida primero.");
        }
        Asistencia a = new Asistencia();
        a.setUsuario(usuario);
        a.setHoraEntrada(LocalDateTime.now());
        return asistenciaRepository.save(a);
    }

    /** Empleado registra su salida */
    @Transactional
    public Asistencia registrarSalida(Usuario usuario) {
        LocalDate hoy = LocalDate.now();
        Asistencia abierta = asistenciaRepository
                .findByUsuarioAndFechaAndHoraSalidaIsNull(usuario, hoy)
                .orElseThrow(() -> new IllegalStateException("No hay entrada registrada hoy. Registra tu entrada primero."));
        abierta.setHoraSalida(LocalDateTime.now());
        return asistenciaRepository.save(abierta);
    }

    /** Vista semanal del usuario */
    public List<Asistencia> getSemana(Usuario usuario, LocalDate inicio, LocalDate fin) {
        return asistenciaRepository.findByUsuarioAndFechaBetweenOrderByFechaAsc(usuario, inicio, fin);
    }

    /** "Asistencia de Hoy" para el panel Admin */
    public List<Asistencia> getHoy() {
        return asistenciaRepository.findByFechaOrderByHoraEntradaAsc(LocalDate.now());
    }

    /** Historial completo de un usuario */
    public List<Asistencia> getHistorial(Usuario usuario) {
        return asistenciaRepository.findByUsuarioOrderByFechaDescHoraEntradaDesc(usuario);
    }

    /** ¿Tiene entrada abierta hoy? */
    public boolean tieneEntradaAbierta(Usuario usuario) {
        return asistenciaRepository
                .findByUsuarioAndFechaAndHoraSalidaIsNull(usuario, LocalDate.now())
                .isPresent();
    }

    /** Exportar Excel filtrado por roles */
    public byte[] exportarExcel(LocalDate desde, LocalDate hasta, List<String> roles) throws IOException {
        List<Asistencia> registros = (roles == null || roles.isEmpty())
                ? asistenciaRepository.findByFechaBetweenOrderByFechaAscHoraEntradaAsc(desde, hasta)
                : asistenciaRepository.findByUsuarioRolInAndFechaBetweenOrderByFechaAsc(roles, desde, hasta);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Asistencia");

            // Estilo de encabezado
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_GREEN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Encabezados
            Row header = sheet.createRow(0);
            String[] cols = {"Fecha", "Usuario", "Rol", "Hora Entrada", "Hora Salida", "Horas Trabajadas"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 5000);
            }

            // Datos
            int rowNum = 1;
            for (Asistencia a : registros) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(a.getFecha() != null ? a.getFecha().toString() : "");
                row.createCell(1).setCellValue(a.getUsuario() != null ? a.getUsuario().getUsername() : "");
                row.createCell(2).setCellValue(a.getUsuario() != null ?
                        a.getUsuario().getRol().replace("ROLE_", "") : "");
                row.createCell(3).setCellValue(a.getHoraEntrada() != null ? a.getHoraEntrada().format(FMT) : "");
                row.createCell(4).setCellValue(a.getHoraSalida() != null ? a.getHoraSalida().format(FMT) : "Pendiente");
                row.createCell(5).setCellValue(a.getHorasTrabajadas());
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
