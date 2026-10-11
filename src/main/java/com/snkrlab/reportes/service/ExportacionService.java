package com.snkrlab.reportes.service;

import com.snkrlab.reportes.model.Exportacion;
import com.snkrlab.reportes.repository.ExportacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
public class ExportacionService {

    private final ExportacionRepository repository;
    private final ReporteService reporteService;

    public ExportacionService(
            ExportacionRepository repository,
            ReporteService reporteService
    ) {
        this.repository = repository;
        this.reporteService = reporteService;
    }

    @Transactional
    public Exportacion crear(
            String usuarioId,
            Exportacion.Tipo tipo,
            LocalDate desde,
            LocalDate hasta
    ) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha desde no puede ser posterior a hasta."
            );
        }

        Exportacion exportacion = new Exportacion();

        exportacion.setId(UUID.randomUUID().toString());
        exportacion.setUsuarioId(usuarioId);
        exportacion.setTipo(tipo);
        exportacion.setEstado(Exportacion.Estado.PENDIENTE);
        exportacion.setDesde(desde);
        exportacion.setHasta(hasta);
        exportacion.setCreada(LocalDateTime.now());

        return repository.save(exportacion);
    }

    @Transactional(readOnly = true)
    public Exportacion obtener(String id, String usuarioId) {
        return repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Exportación no encontrada."
                ));
    }

    @Transactional
    public void generar(String id) {
        Exportacion exportacion = repository.buscarParaActualizar(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Exportación inexistente: " + id
                ));

        // Evita generar nuevamente una solicitud ya procesada.
        if (exportacion.getEstado() != Exportacion.Estado.PENDIENTE) {
            return;
        }

        // Permite que Excel reconozca los acentos del archivo UTF-8.
        StringBuilder csv = new StringBuilder("\uFEFF");

        if (exportacion.getTipo() == Exportacion.Tipo.VENTAS) {
            generarVentas(csv, exportacion);
        } else {
            generarResumen(csv, exportacion);
        }

        String nombreArchivo =
                exportacion.getTipo().name().toLowerCase(Locale.ROOT)
                        + "-"
                        + exportacion.getId()
                        + ".csv";

        exportacion.setContenido(csv.toString());
        exportacion.setNombreArchivo(nombreArchivo);
        exportacion.setEstado(Exportacion.Estado.LISTO);
        exportacion.setFinalizada(LocalDateTime.now());
        exportacion.setError(null);

        // JPA guarda los cambios al finalizar esta transacción.
    }

    @Transactional
    public void marcarError(String id) {
        repository.buscarParaActualizar(id).ifPresent(exportacion -> {
            if (exportacion.getEstado() != Exportacion.Estado.PENDIENTE) {
                return;
            }

            exportacion.setEstado(Exportacion.Estado.ERROR);
            exportacion.setFinalizada(LocalDateTime.now());
            exportacion.setError(
                    "No se pudo generar el archivo. Crea una nueva solicitud."
            );
        });
    }

    private void generarVentas(
            StringBuilder csv,
            Exportacion exportacion
    ) {
        agregarFila(
                csv,
                "Orden",
                "Fecha",
                "Producto",
                "Cantidad",
                "Precio unitario CLP",
                "Subtotal CLP",
                "Total orden CLP"
        );

        var ventas = reporteService.obtenerVentas(
                exportacion.getDesde(),
                exportacion.getHasta()
        );

        for (var venta : ventas) {
            for (var item : venta.getItems()) {
                agregarFila(
                        csv,
                        venta.getOrdenId(),
                        venta.getFecha(),
                        item.getNombreProducto(),
                        item.getCantidad(),
                        item.getPrecioUnitario(),
                        item.getSubtotal(),
                        venta.getTotal()
                );
            }
        }
    }

    private void generarResumen(
            StringBuilder csv,
            Exportacion exportacion
    ) {
        var resumen = reporteService.obtenerResumen(
                exportacion.getDesde(),
                exportacion.getHasta()
        );

        agregarFila(csv, "Indicador", "Valor");

        agregarFila(
                csv,
                "Total vendido CLP",
                resumen.getTotalVentas()
        );

        agregarFila(
                csv,
                "Órdenes registradas",
                resumen.getCantidadOrdenes()
        );

        agregarFila(
                csv,
                "Unidades vendidas",
                resumen.getUnidadesVendidas()
        );

        agregarFila(csv);
        agregarFila(csv, "Ventas por día");
        agregarFila(csv, "Fecha", "Órdenes", "Total CLP");

        for (var dia : resumen.getVentasPorDia()) {
            agregarFila(
                    csv,
                    dia.getFecha(),
                    dia.getCantidadOrdenes(),
                    dia.getTotal()
            );
        }

        agregarFila(csv);
        agregarFila(csv, "Productos más vendidos");
        agregarFila(csv, "Producto", "Unidades", "Total CLP");

        for (var producto : resumen.getProductosMasVendidos()) {
            agregarFila(
                    csv,
                    producto.getNombreProducto(),
                    producto.getUnidadesVendidas(),
                    producto.getTotalVentas()
            );
        }
    }

    private void agregarFila(StringBuilder csv, Object... valores) {
        for (int i = 0; i < valores.length; i++) {
            if (i > 0) {
                csv.append(';');
            }

            Object valor = valores[i];
            String texto = valor == null ? "" : valor.toString();

            // Evita interpretar textos como fórmulas en Excel.
            if (valor instanceof String) {
                String inicio = texto.stripLeading();

                if (!inicio.isEmpty()
                        && "=+-@".indexOf(inicio.charAt(0)) >= 0) {
                    texto = "'" + texto;
                }
            }

            csv.append('"')
                    .append(texto.replace("\"", "\"\""))
                    .append('"');
        }

        csv.append("\r\n");
    }

    @Transactional(readOnly = true)
    public Exportacion obtenerUltima(String usuarioId) {
        return repository
                .findFirstByUsuarioIdOrderByCreadaDesc(usuarioId)
                .orElse(null);
    }
}