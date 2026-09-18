package io.github.renatoxico.extract.logging;

import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.LoggingSystemProperties;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FileLoggingTest {
    @TempDir Path directory;

    @Test
    void productionConfigurationWritesConsoleAndFilesRotatesAndSurvivesRestart() throws Exception {
        Path file = directory.resolve("extract-api.log");
        String firstConsole = runProbe(file, "first", "1KB", "80");
        assertThat(firstConsole).contains("probe-first-0");
        assertThat(Files.readString(file)).contains("probe-first-79");
        try (var files = Files.list(directory)) {
            var archives = files.filter(path -> path.getFileName().toString().endsWith(".gz")).toList();
            assertThat(archives).isNotEmpty();
            StringBuilder archived = new StringBuilder();
            for (Path archive : archives) {
                try (var input = new GZIPInputStream(Files.newInputStream(archive))) {
                    archived.append(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            assertThat(archived.toString()).contains("probe-first-0");
        }
        String secondConsole = runProbe(file, "second", "100MB", "1");
        assertThat(secondConsole).contains("probe-second-0");
        assertThat(Files.readString(file)).contains("probe-first-79", "probe-second-0");
    }

    private String runProbe(Path file, String marker, String maxSize, String count) throws Exception {
        Path console = directory.resolve(marker + "-console.txt");
        Process process = new ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("java.class.path"), Probe.class.getName(),
            Path.of("target/classes/application.properties").toAbsolutePath().toString(),
            file.toString(), marker, maxSize, count)
            .redirectErrorStream(true).redirectOutput(console.toFile()).start();
        boolean finished = process.waitFor(Duration.ofSeconds(30).toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) process.destroyForcibly();
        assertThat(finished).as("logging probe completed").isTrue();
        String output = Files.readString(console);
        assertThat(process.exitValue()).withFailMessage(output).isZero();
        return output;
    }

    /** Separate JVMs exercise Boot's real logging initialization without resetting the test runner's loggers. */
    public static class Probe {
        public static void main(String[] args) throws Exception {
            Properties properties = new Properties();
            try (var input = Files.newInputStream(Path.of(args[0]))) {
                properties.load(input);
            }
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new PropertiesPropertySource("production", properties));
            environment.getPropertySources().addFirst(new MapPropertySource("probe", Map.of(
                "LOG_FILE", args[1], "LOG_MAX_FILE_SIZE", args[3], "LOG_LEVEL", "INFO")));
            LoggingSystem system = LoggingSystem.get(Probe.class.getClassLoader());
            system.beforeInitialize();
            LogFile logFile = LogFile.get(environment);
            new LoggingSystemProperties(environment).apply(logFile);
            system.initialize(new LoggingInitializationContext(environment), null, logFile);
            var logger = LoggerFactory.getLogger(Probe.class);
            for (int i = 0; i < Integer.parseInt(args[4]); i++) {
                logger.info("probe-{}-{} {}", args[2], i, "payload".repeat(40));
            }
            // Rapid rollover is asynchronous: wait for all records to reach their final
            // files before terminating this deliberately short-lived JVM.
            org.awaitility.Awaitility.await().atMost(Duration.ofSeconds(10)).until(() -> {
                StringBuilder persisted = new StringBuilder(Files.readString(Path.of(args[1])));
                try (var files = Files.list(Path.of(args[1]).getParent())) {
                    for (Path archive : files.filter(p -> p.toString().endsWith(".gz")).toList()) {
                        try (var input = new GZIPInputStream(Files.newInputStream(archive))) {
                            persisted.append(new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                        } catch (java.io.IOException unfinishedCompression) {
                            return false;
                        }
                    }
                }
                for (int i = 0; i < Integer.parseInt(args[4]); i++) {
                    if (persisted.indexOf("probe-" + args[2] + "-" + i + " ") < 0) return false;
                }
                return true;
            });
            ((LoggerContext) LoggerFactory.getILoggerFactory()).stop();
        }
    }
}
