package com.example.instagram;

import io.github.cdimascio.dotenv.Dotenv;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InstagramApplication {

	public static void main(String[] args) {
		applyLocalDotenvIfPresent();
		SpringApplication.run(InstagramApplication.class, args);
	}

	/**
	 * {@code .env}는 Spring Boot/Gradle이 자동으로 읽지 않는다. 로컬에서
	 * {@code backend/.env} 및 저장소 루트 {@code .env}를 찾아, OS 환경변수에 없을 때만
	 * {@link System#setProperty}로 주입해 {@code application.yml}의 {@code ${JWT_SECRET}} 등이
	 * 해결되게 한다(배포 환경의 env는 덮어쓰지 않음).
	 * <p>
	 * {@code @SpringBootTest}는 {@code main}을 거치지 않으므로 테스트에 영향이 없다.
	 * <p>
	 * 셸에 {@code export DB_PASSWORD=}처럼 빈 값이 잡혀 있으면 기존엔 .env로 덮어쓸 수 없어서,
	 * null·공백일 때는 .env 값을 쓴다(실제 배포 환경이 비밀 env를 쓰는 경우는 권장: 값을
	 * 셸에만 설정하고 .env는 두지 않음).
	 */
	private static void applyLocalDotenvIfPresent() {
		// 먼저 루트, 다음 backend — 나중이 우선(동일 키는 ./.env가 이김)
		Path[] candidates = { Path.of("..", ".env"), Path.of(".env") };
		for (Path relative : candidates) {
			if (!Files.isRegularFile(relative)) {
				continue;
			}
			String baseDir = relative.getParent() != null ? relative.getParent().toString() : ".";
			Dotenv dotenv = Dotenv.configure()
					.directory(Path.of(baseDir).toAbsolutePath().toString())
					.ignoreIfMissing()
					.load();
			dotenv.entries().forEach(entry -> {
				if (isUnsetOrBlank(System.getenv(entry.getKey()))) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			});
		}
	}

	private static boolean isUnsetOrBlank(String value) {
		return value == null || value.isBlank();
	}
}
