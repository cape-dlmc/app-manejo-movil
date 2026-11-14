package com.fleetcontrol.config;

import com.fleetcontrol.entity.Conductor;
import com.fleetcontrol.entity.Usuario;
import com.fleetcontrol.entity.Vehiculo;
import com.fleetcontrol.repository.ConductorRepository;
import com.fleetcontrol.repository.UsuarioRepository;
import com.fleetcontrol.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Si la base está vacía, crea un administrador y datos de demostración. */
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final ConductorRepository conductorRepository;
    private final VehiculoRepository vehiculoRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setCorreo(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRol("ADMIN");
        admin.setEstado(true);
        usuarioRepository.save(admin);

        // Cuenta de conductor para la demostración: conductor@fleetcontrol.local / Conductor123*
        Usuario cuenta = new Usuario();
        cuenta.setNombre("Conductor Demo");
        cuenta.setCorreo("conductor@fleetcontrol.local");
        cuenta.setPasswordHash(passwordEncoder.encode("Conductor123*"));
        cuenta.setRol("CONDUCTOR");
        cuenta.setEstado(true);
        cuenta = usuarioRepository.save(cuenta);

        Conductor conductor = new Conductor();
        conductor.setUsuario(cuenta);
        conductor.setIdentificacion("1000000001");
        conductor.setTelefono("3000000000");
        conductor.setEstado("ACTIVO");
        conductorRepository.save(conductor);

        if (vehiculoRepository.count() == 0) {
            Vehiculo v = new Vehiculo();
            v.setPlaca("TLM421");
            v.setMarca("Renault");
            v.setModelo("Master");
            v.setAnio(2022);
            v.setEstado("DISPONIBLE");
            vehiculoRepository.save(v);
        }
    }
}
