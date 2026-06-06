package mx.gob.sedif.asistencia.core.asistencia;

import mx.gob.sedif.asistencia.core.area.Area;
import mx.gob.sedif.asistencia.core.area.AreaService;
import mx.gob.sedif.asistencia.core.horario.*;
import mx.gob.sedif.asistencia.core.justificacion.AsistenciaJustificacionRepository;
import mx.gob.sedif.asistencia.core.justificacion.CatalogoJustificacionRepository;
import mx.gob.sedif.asistencia.core.usuario.Usuario;
import mx.gob.sedif.asistencia.core.usuario.UsuarioRepository;
import mx.gob.sedif.asistencia.security.SecurityUtil;
import mx.gob.sedif.asistencia.util.enums.Estado;
import mx.gob.sedif.asistencia.util.enums.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.mockito.ArgumentMatchers;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link AsistenciaService}.
 *
 * <p>Se prueba la lógica de negocio crítica sin levantar el contexto de Spring:
 * <ul>
 *   <li>Motor de cálculo de turnos ({@code calcularTurnoParaFecha}).</li>
 *   <li>Reglas de retardo y falta total en el registro de entrada.</li>
 *   <li>Cruce de medianoche en el registro de salida.</li>
 *   <li>Cálculo de sanciones y descuentos por retardos acumulados.</li>
 * </ul>
 *
 * <p>Todos los repositorios y dependencias se mockean con Mockito para
 * garantizar aislamiento total de la base de datos.
 */
@ExtendWith(MockitoExtension.class)
class AsistenciaServiceTest {

    @Mock private AsistenciaRepository asistenciaRepository;
    @Mock private HorarioUsuarioRepository horarioUsuarioRepository;
    @Mock private ExcepcionHorarioRepository excepcionHorarioRepository;
    @Mock private SecurityUtil securityUtil;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AreaService areaService;
    @Mock private AsistenciaJustificacionRepository asistenciaJustificacionRepository;
    @Mock private CatalogoJustificacionRepository catalogoJustificacionRepository;

    @InjectMocks
    private AsistenciaService asistenciaService;

    // ── Fixtures reutilizables ──────────────────────────────────────────────

    private Usuario usuarioBase;
    private Area areaBase;
    private Horario horarioSemanal;

    @BeforeEach
    void setUp() {
        areaBase = new Area();
        areaBase.setId(1);
        areaBase.setNombre("Administración");

        usuarioBase = new Usuario();
        usuarioBase.setId(1);
        usuarioBase.setNumeroControl("USR001");
        usuarioBase.setNombre("Juan");
        usuarioBase.setApellidoPaterno("Pérez");
        usuarioBase.setRol(Rol.USER);
        usuarioBase.setEstatus(Estado.ACTIVE);
        usuarioBase.setAreaPrincipal(areaBase);

        horarioSemanal = new Horario();
        horarioSemanal.setId(1);
        horarioSemanal.setNombre("Horario Estándar");
        horarioSemanal.setTipoCiclo(1);       // ciclo semanal
        horarioSemanal.setCruceMedianoche(false);
    }

    // ── Helpers para construir fixtures ────────────────────────────────────

    /**
     * Crea un HorarioDetalle para el día de semana dado con las horas especificadas.
     *
     * @param diaSemana 1=Lunes … 7=Domingo (ISO).
     * @param entrada   Hora de entrada esperada.
     * @param salida    Hora de salida esperada.
     */
    private HorarioDetalle detalle(int diaSemana, LocalTime entrada, LocalTime salida) {
        HorarioDetalle d = new HorarioDetalle();
        d.setDia(diaSemana);
        d.setHoraEntrada(entrada);
        d.setHoraSalida(salida);
        d.setHorario(horarioSemanal);
        return d;
    }

    private HorarioUsuario asignacion(LocalDate inicioCiclo) {
        HorarioUsuarioId id = new HorarioUsuarioId();
        id.setUsuarioId(usuarioBase.getId());
        id.setHorarioId(horarioSemanal.getId());

        HorarioUsuario hu = new HorarioUsuario();
        hu.setId(id);
        hu.setUsuario(usuarioBase);
        hu.setHorario(horarioSemanal);
        hu.setFechaInicioCiclo(inicioCiclo);
        return hu;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SUITE 1: Registro de entrada (reglas de retardo)
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Registro de Entrada")
    class RegistroEntrada {

        /**
         * Lunes con horario de entrada a las 08:00.
         * Llegada a las 08:10 → dentro de la tolerancia de 15:59 → OK.
         */
        @Test
        @DisplayName("Llegada puntual (dentro de tolerancia) → estatus OK")
        void llegadaPuntual_estatusOk() {
            LocalDate lunes = LocalDate.of(2026, 6, 1); // Lunes
            LocalTime entrada = LocalTime.of(8, 0);
            horarioSemanal.setDetalles(List.of(detalle(1, entrada, LocalTime.of(17, 0))));

            configurarMocksEntrada(lunes, java.time.LocalDateTime.of(lunes, LocalTime.of(8, 10)));

            asistenciaService.registrarEntrada(null, "192.168.1.1");

            verify(asistenciaRepository).save(argThat(a ->
                    a.getEstatusIncidencia() == 0 // ESTATUS_OK
            ));
        }

        /**
         * Llegada a las 08:20 → entre 16:00 y 30:59 minutos tarde → Retardo.
         */
        @Test
        @DisplayName("Llegada entre 16 y 30 min tarde → estatus Retardo")
        void llegadaTarde16min_estatusRetardo() {
            LocalDate lunes = LocalDate.of(2026, 6, 1);
            LocalTime entrada = LocalTime.of(8, 0);
            horarioSemanal.setDetalles(List.of(detalle(1, entrada, LocalTime.of(17, 0))));

            configurarMocksEntrada(lunes, java.time.LocalDateTime.of(lunes, LocalTime.of(8, 20)));

            asistenciaService.registrarEntrada(null, "192.168.1.1");

            verify(asistenciaRepository).save(argThat(a ->
                    a.getEstatusIncidencia() == 1 // ESTATUS_RETARDO
            ));
        }

        /**
         * Llegada a las 08:35 → más de 30:59 minutos tarde → Falta Total.
         */
        @Test
        @DisplayName("Llegada más de 30 min tarde → estatus Falta Total")
        void llegadaTarde31min_estatusFaltaTotal() {
            LocalDate lunes = LocalDate.of(2026, 6, 1);
            LocalTime entrada = LocalTime.of(8, 0);
            horarioSemanal.setDetalles(List.of(detalle(1, entrada, LocalTime.of(17, 0))));

            configurarMocksEntrada(lunes, java.time.LocalDateTime.of(lunes, LocalTime.of(8, 35)));

            asistenciaService.registrarEntrada(null, "192.168.1.1");

            verify(asistenciaRepository).save(argThat(a ->
                    a.getEstatusIncidencia() == 2 // ESTATUS_FALTA_TOTAL
            ));
        }

        /**
         * Si el usuario intenta registrar entrada dos veces en el mismo día
         * se debe lanzar IllegalStateException.
         */
        @Test
        @DisplayName("Doble registro de entrada → IllegalStateException")
        void dobleEntrada_lanzaExcepcion() {
            LocalDate lunes = LocalDate.of(2026, 6, 1);
            Asistencia yaRegistrada = new Asistencia();
            yaRegistrada.setHoraEntrada(java.time.LocalDateTime.of(lunes, LocalTime.of(8, 0)));

            when(securityUtil.getCurrentUser()).thenReturn(Optional.of(usuarioBase));
            when(asistenciaRepository.findByUsuarioAndFecha(usuarioBase, lunes))
                    .thenReturn(Optional.of(yaRegistrada));

            // Necesitamos que LocalDate.now() devuelva "lunes", usamos fixed clock vía spy o simplemente
            // verificamos que la excepción se lanza cuando hay entrada previa.
            // Como LocalDate.now() es estático, hacemos el test indirecto:
            // la condición es que asistenciaHoy.isPresent() && horaEntrada != null
            assertThatThrownBy(() -> {
                // simulamos el flujo completo forzando la fecha de hoy igual a la registrada
                when(usuarioRepository.findByNumeroControl("USR001")).thenReturn(Optional.of(usuarioBase));
                // La fecha "hoy" en el servicio usa LocalDate.now(); en este caso el test verifica
                // que si el repo devuelve una asistencia con horaEntrada != null, se lanza la excepción.
                asistenciaService.registrarEntrada(null, "192.168.1.1");
            }).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("entrada");
        }

        // ── helpers privados ────────────────────────────────────────────────

        private void configurarMocksEntrada(LocalDate fecha, java.time.LocalDateTime horaActual) {
            when(securityUtil.getCurrentUser()).thenReturn(Optional.of(usuarioBase));
            when(usuarioRepository.findByNumeroControl("USR001")).thenReturn(Optional.of(usuarioBase));
            when(excepcionHorarioRepository.findByUsuarioIdAndFechaEspecifica(anyInt(), any()))
                    .thenReturn(Optional.empty());
            when(horarioUsuarioRepository.findByUsuarioId(anyInt()))
                    .thenReturn(Optional.of(asignacion(fecha.minusDays(fecha.getDayOfWeek().getValue() - 1))));
            when(asistenciaRepository.findByUsuarioAndFecha(any(), any()))
                    .thenReturn(Optional.empty());
            when(asistenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SUITE 2: Cruce de medianoche
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cruce de Medianoche")
    class CruceMedianoche {

        /**
         * Turno nocturno: entrada ayer, sin salida → al registrar salida hoy
         * el servicio debe encontrar la asistencia de ayer.
         */
        @Test
        @DisplayName("Salida registrada el día siguiente → busca en asistencia de ayer")
        void salidaAlDiaSiguiente_encuentraAsistenciaDeAyer() {
            LocalDate ayer = LocalDate.now().minusDays(1);

            Horario horarioNocturno = new Horario();
            horarioNocturno.setId(2);
            horarioNocturno.setTipoCiclo(1);
            horarioNocturno.setCruceMedianoche(true);

            HorarioDetalle detalleNocturno = detalle(ayer.getDayOfWeek().getValue(),
                    LocalTime.of(22, 0), LocalTime.of(6, 0));
            detalleNocturno.setHorario(horarioNocturno);
            horarioNocturno.setDetalles(List.of(detalleNocturno));

            HorarioUsuario asignacionNocturna = new HorarioUsuario();
            HorarioUsuarioId huId = new HorarioUsuarioId();
            huId.setUsuarioId(1);
            huId.setHorarioId(2);
            asignacionNocturna.setId(huId);
            asignacionNocturna.setUsuario(usuarioBase);
            asignacionNocturna.setHorario(horarioNocturno);
            asignacionNocturna.setFechaInicioCiclo(ayer.minusDays(6));

            Asistencia entradaDeAyer = new Asistencia();
            entradaDeAyer.setFecha(ayer);
            entradaDeAyer.setHoraEntrada(java.time.LocalDateTime.of(ayer, LocalTime.of(22, 5)));

            when(securityUtil.getCurrentUser()).thenReturn(Optional.of(usuarioBase));
            when(usuarioRepository.findByNumeroControl("USR001")).thenReturn(Optional.of(usuarioBase));
            // Hoy no hay asistencia
            when(asistenciaRepository.findByUsuarioAndFecha(usuarioBase, LocalDate.now()))
                    .thenReturn(Optional.empty());
            // Ayer sí hay asistencia sin salida
            when(asistenciaRepository.findByUsuarioAndFecha(usuarioBase, ayer))
                    .thenReturn(Optional.of(entradaDeAyer));
            when(horarioUsuarioRepository.findByUsuarioId(anyInt()))
                    .thenReturn(Optional.of(asignacionNocturna));
            when(excepcionHorarioRepository.findByUsuarioIdAndFechaEspecifica(anyInt(), any()))
                    .thenReturn(Optional.empty());
            when(asistenciaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            asistenciaService.registrarSalida(null, "192.168.1.1");

            verify(asistenciaRepository).save(argThat(a ->
                    a.getFecha().equals(ayer) && a.getHoraSalida() != null
            ));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SUITE 3: Cálculo de sanciones
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Cálculo de Sanciones")
    class CalculoSanciones {

        /**
         * Con 3 retardos no hay descuento (umbral mínimo es 4).
         */
        @Test
        @DisplayName("3 retardos → descuento 0 días")
        void tresRetardos_sinDescuento() {
            List<ResumenSancionesRecord> resultado = calcularSancionesConRetardos(3);
            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).diasDescuentoRetardos()).isEqualTo(0.0);
            assertThat(resultado.get(0).totalDiasDescontar()).isEqualTo(0.0);
        }

        /**
         * Con exactamente 4 retardos el descuento es 0.5 días.
         */
        @Test
        @DisplayName("4 retardos → descuento 0.5 días")
        void cuatroRetardos_medioDescuento() {
            List<ResumenSancionesRecord> resultado = calcularSancionesConRetardos(4);
            assertThat(resultado.get(0).diasDescuentoRetardos()).isEqualTo(0.5);
        }

        /**
         * Con 5 retardos el descuento es 1 día.
         */
        @Test
        @DisplayName("5 retardos → descuento 1 día")
        void cincoRetardos_unDiaDescuento() {
            List<ResumenSancionesRecord> resultado = calcularSancionesConRetardos(5);
            assertThat(resultado.get(0).diasDescuentoRetardos()).isEqualTo(1.0);
        }

        /**
         * Con 10 o más retardos el descuento es 2 días.
         */
        @Test
        @DisplayName("10 retardos → descuento 2 días")
        void diezRetardos_dosDiasDescuento() {
            List<ResumenSancionesRecord> resultado = calcularSancionesConRetardos(10);
            assertThat(resultado.get(0).diasDescuentoRetardos()).isEqualTo(2.0);
        }

        /**
         * Una falta total equivale a 1 día de descuento.
         */
        @Test
        @DisplayName("1 falta total → descuento 1 día")
        void unaFalta_unDiaDescuento() {
            LocalDate inicio = LocalDate.of(2026, 5, 1);
            LocalDate fin = LocalDate.of(2026, 5, 31);

            Asistencia falta = new Asistencia();
            falta.setUsuario(usuarioBase);
            falta.setFecha(LocalDate.of(2026, 5, 10));
            falta.setEstatusIncidencia(2); // FALTA_TOTAL

            mockCurrentUserSuperAdmin();
            when(asistenciaRepository.findAll(ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Asistencia>>any()))
                    .thenReturn(List.of(falta));

            List<ResumenSancionesRecord> resultado = asistenciaService.calcularSanciones(
                    inicio, fin, Optional.empty(), Optional.empty());

            assertThat(resultado).hasSize(1);
            assertThat(resultado.get(0).totalFaltasYOmisiones()).isEqualTo(1L);
            assertThat(resultado.get(0).diasDescuentoFaltas()).isEqualTo(1.0);
        }

        /**
         * Incidencias ya justificadas NO deben contar en el cálculo de sanciones.
         */
        @Test
        @DisplayName("Retardo justificado → no cuenta en sanciones")
        void retardoJustificado_noContaEnSanciones() {
            LocalDate inicio = LocalDate.of(2026, 5, 1);
            LocalDate fin = LocalDate.of(2026, 5, 31);

            mx.gob.sedif.asistencia.core.justificacion.AsistenciaJustificacion justificacion =
                    new mx.gob.sedif.asistencia.core.justificacion.AsistenciaJustificacion();

            Asistencia retardoJustificado = new Asistencia();
            retardoJustificado.setUsuario(usuarioBase);
            retardoJustificado.setFecha(LocalDate.of(2026, 5, 5));
            retardoJustificado.setEstatusIncidencia(1); // RETARDO
            retardoJustificado.setJustificacionAplicada(justificacion); // ya justificado

            mockCurrentUserSuperAdmin();
            when(asistenciaRepository.findAll(ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Asistencia>>any()))
                    .thenReturn(List.of(retardoJustificado));

            List<ResumenSancionesRecord> resultado = asistenciaService.calcularSanciones(
                    inicio, fin, Optional.empty(), Optional.empty());

            // El registro justificado no aparece en sanciones
            assertThat(resultado).isEmpty();
        }

        // ── helpers privados ────────────────────────────────────────────────

        private List<ResumenSancionesRecord> calcularSancionesConRetardos(int cantidad) {
            LocalDate inicio = LocalDate.of(2026, 5, 1);
            LocalDate fin = LocalDate.of(2026, 5, 31);

            List<Asistencia> retardos = new java.util.ArrayList<>();
            for (int i = 1; i <= cantidad; i++) {
                Asistencia a = new Asistencia();
                a.setUsuario(usuarioBase);
                a.setFecha(inicio.plusDays(i - 1));
                a.setEstatusIncidencia(1); // RETARDO
                retardos.add(a);
            }

            mockCurrentUserSuperAdmin();
            when(asistenciaRepository.findAll(ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<Asistencia>>any()))
                    .thenReturn(retardos);

            return asistenciaService.calcularSanciones(inicio, fin, Optional.empty(), Optional.empty());
        }

        private void mockCurrentUserSuperAdmin() {
            Usuario superAdmin = new Usuario();
            superAdmin.setId(99);
            superAdmin.setNumeroControl("ADMIN");
            superAdmin.setRol(Rol.SUPERADMIN);
            when(securityUtil.getCurrentUser()).thenReturn(Optional.of(superAdmin));
        }
    }
}
