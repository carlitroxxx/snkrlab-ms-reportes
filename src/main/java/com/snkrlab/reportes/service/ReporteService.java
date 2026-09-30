package com.snkrlab.reportes.service;

import com.snkrlab.reportes.dto.ProductoVendidoDTO;
import com.snkrlab.reportes.dto.ResumenReporteDTO;
import com.snkrlab.reportes.dto.VentaDiariaDTO;
import com.snkrlab.reportes.model.DetalleVenta;
import com.snkrlab.reportes.model.Venta;
import com.snkrlab.reportes.rabbitmq.DetalleVentaMensaje;
import com.snkrlab.reportes.rabbitmq.VentaRegistradaMensaje;
import com.snkrlab.reportes.repository.VentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class ReporteService {

    private final VentaRepository ventaRepository;

    public ReporteService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    @Transactional
    public void registrarVenta(VentaRegistradaMensaje mensaje) {
        if (ventaRepository.existsById(mensaje.getOrdenId())) {
            return;
        }

        Venta venta = new Venta();
        venta.setOrdenId(mensaje.getOrdenId());
        venta.setFecha(mensaje.getFecha());
        venta.setTotal(mensaje.getTotal());

        for (DetalleVentaMensaje item : mensaje.getItems()) {
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProductoId(item.getProductoId());
            detalle.setNombreProducto(item.getNombreProducto());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(item.getPrecioUnitario());
            detalle.setSubtotal(item.getSubtotal());

            venta.getItems().add(detalle);
        }

        ventaRepository.save(venta);
    }

    @Transactional(readOnly = true)
    public ResumenReporteDTO obtenerResumen(
            LocalDate desde,
            LocalDate hasta) {

        List<Venta> ventas = buscarVentas(desde, hasta);

        long totalVentas = 0;
        long unidadesVendidas = 0;

        Map<LocalDate, VentaDiariaDTO> porDia = new TreeMap<>();
        Map<Long, ProductoVendidoDTO> porProducto = new HashMap<>();

        for (Venta venta : ventas) {
            totalVentas += venta.getTotal();

            LocalDate fecha = venta.getFecha().toLocalDate();

            VentaDiariaDTO dia = porDia.computeIfAbsent(
                    fecha,
                    valor -> new VentaDiariaDTO(valor, 0, 0)
            );

            dia.setCantidadOrdenes(
                    dia.getCantidadOrdenes() + 1
            );
            dia.setTotal(
                    dia.getTotal() + venta.getTotal()
            );

            for (DetalleVenta item : venta.getItems()) {
                unidadesVendidas += item.getCantidad();

                ProductoVendidoDTO producto =
                        porProducto.computeIfAbsent(
                                item.getProductoId(),
                                id -> new ProductoVendidoDTO(
                                        id,
                                        item.getNombreProducto(),
                                        0,
                                        0
                                )
                        );

                producto.setNombreProducto(
                        item.getNombreProducto()
                );

                producto.setUnidadesVendidas(
                        producto.getUnidadesVendidas()
                                + item.getCantidad()
                );

                producto.setTotalVentas(
                        producto.getTotalVentas()
                                + item.getSubtotal()
                );
            }
        }

        List<ProductoVendidoDTO> productosMasVendidos =
                porProducto.values().stream()
                        .sorted(
                                Comparator.comparingLong(
                                                ProductoVendidoDTO::getUnidadesVendidas
                                        ).reversed()
                                        .thenComparing(
                                                ProductoVendidoDTO::getProductoId
                                        )
                        )
                        .limit(10)
                        .toList();

        return new ResumenReporteDTO(
                totalVentas,
                ventas.size(),
                unidadesVendidas,
                new ArrayList<>(porDia.values()),
                productosMasVendidos
        );
    }

    @Transactional(readOnly = true)
    public List<Venta> obtenerVentas(
            LocalDate desde,
            LocalDate hasta) {

        List<Venta> ventas =
                new ArrayList<>(buscarVentas(desde, hasta));

        ventas.sort(
                Comparator.comparing(Venta::getFecha).reversed()
                        .thenComparing(
                                Venta::getOrdenId,
                                Comparator.reverseOrder()
                        )
        );

        return ventas;
    }

    private List<Venta> buscarVentas(
            LocalDate desde,
            LocalDate hasta) {

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La fecha desde no puede ser posterior a hasta."
            );
        }

        LocalDateTime inicio =
                desde != null ? desde.atStartOfDay() : null;

        LocalDateTime finExclusivo =
                hasta != null ? hasta.plusDays(1).atStartOfDay() : null;

        return ventaRepository.buscarPorFechas(
                inicio,
                finExclusivo
        );
    }
}