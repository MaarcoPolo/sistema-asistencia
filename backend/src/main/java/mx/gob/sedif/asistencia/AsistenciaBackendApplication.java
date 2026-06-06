package mx.gob.sedif.asistencia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AsistenciaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsistenciaBackendApplication.class, args);
	}

}
