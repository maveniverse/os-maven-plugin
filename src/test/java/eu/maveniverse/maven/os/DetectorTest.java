/*
 * Copyright 2025 Guillaume Nodet <gnodet@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.maveniverse.maven.os;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DetectorTest {

    @Mock
    private SystemPropertyOperationProvider systemPropertyProvider;

    @Mock
    private FileOperationProvider fileOperationProvider;

    private TestDetector detector;
    private Properties properties;

    static class TestDetector extends Detector {
        private StringBuilder logOutput = new StringBuilder();

        public TestDetector(SystemPropertyOperationProvider systemProvider, FileOperationProvider fileProvider) {
            super(systemProvider, fileProvider);
        }

        @Override
        protected void log(String message) {
            logOutput.append(message).append("\n");
        }

        @Override
        protected void logProperty(String name, String value) {
            logOutput.append(name).append("=").append(value).append("\n");
        }

        public String getLogOutput() {
            return logOutput.toString();
        }

        public void resetLogOutput() {
            logOutput = new StringBuilder();
        }
    }

    @BeforeEach
    void setup() throws IOException {
        detector = new TestDetector(systemPropertyProvider, fileOperationProvider);
        properties = new Properties();

        // Setup default behaviors for methods called with default values
        lenient()
                .when(systemPropertyProvider.getSystemProperty(anyString(), anyString()))
                .thenReturn("");
        lenient()
                .when(systemPropertyProvider.getSystemProperty(anyString(), anyString()))
                .thenReturn("");

        // Setup special case mocking
        lenient()
                .doThrow(new IOException("File not found"))
                .when(fileOperationProvider)
                .readFile(anyString());

        String osReleaseContent = "NAME=\"Ubuntu\"\nID=ubuntu\nVERSION_ID=\"20.04\"\nID_LIKE=debian";
        InputStream osReleaseStream = new ByteArrayInputStream(osReleaseContent.getBytes(StandardCharsets.UTF_8));
        lenient().doReturn(osReleaseStream).when(fileOperationProvider).readFile("/etc/os-release");

        String redhatReleaseContent = "CentOS Linux release 8.3.2011";
        InputStream redhatReleaseStream =
                new ByteArrayInputStream(redhatReleaseContent.getBytes(StandardCharsets.UTF_8));
        lenient().doReturn(redhatReleaseStream).when(fileOperationProvider).readFile("/etc/redhat-release");
    }

    @Test
    void testDetectWindows() {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Windows 10");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("amd64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("10.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null);

        // Act
        detector.detect(properties, List.of());

        // Assert
        assertEquals("windows", properties.getProperty(Detector.DETECTED_NAME));
        assertEquals("x86_64", properties.getProperty(Detector.DETECTED_ARCH));
        assertEquals("64", properties.getProperty(Detector.DETECTED_BITNESS));
        assertEquals("10.0", properties.getProperty(Detector.DETECTED_VERSION));
        assertEquals("10", properties.getProperty(Detector.DETECTED_VERSION_MAJOR));
        assertEquals("0", properties.getProperty(Detector.DETECTED_VERSION_MINOR));
        assertEquals("windows-x86_64", properties.getProperty(Detector.DETECTED_CLASSIFIER));

        verify(systemPropertyProvider).setSystemProperty(Detector.DETECTED_NAME, "windows");
        verify(systemPropertyProvider).setSystemProperty(Detector.DETECTED_ARCH, "x86_64");
        verify(systemPropertyProvider).setSystemProperty(Detector.DETECTED_BITNESS, "64");
    }

    @Test
    void testDetectLinux() throws IOException {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Linux");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("5.4.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null);

        // Act
        detector.detect(properties, List.of("debian"));

        // Assert
        assertEquals("linux", properties.getProperty(Detector.DETECTED_NAME));
        assertEquals("x86_64", properties.getProperty(Detector.DETECTED_ARCH));
        assertEquals("64", properties.getProperty(Detector.DETECTED_BITNESS));
        assertEquals("5.4", properties.getProperty(Detector.DETECTED_VERSION));
        assertEquals("5", properties.getProperty(Detector.DETECTED_VERSION_MAJOR));
        assertEquals("4", properties.getProperty(Detector.DETECTED_VERSION_MINOR));
        assertEquals("ubuntu", properties.getProperty(Detector.DETECTED_RELEASE));
        assertEquals("20.04", properties.getProperty(Detector.DETECTED_RELEASE_VERSION));
        assertEquals("true", properties.getProperty(Detector.DETECTED_RELEASE_LIKE_PREFIX + "debian"));
        assertEquals("linux-x86_64-debian", properties.getProperty(Detector.DETECTED_CLASSIFIER));
    }

    @Test
    void testDetectMacOS() {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Mac OS X");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("aarch64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("11.5.2");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null);

        // Act
        detector.detect(properties, List.of());

        // Assert
        assertEquals("osx", properties.getProperty(Detector.DETECTED_NAME));
        assertEquals("aarch_64", properties.getProperty(Detector.DETECTED_ARCH));
        assertEquals("64", properties.getProperty(Detector.DETECTED_BITNESS));
        assertEquals("11.5", properties.getProperty(Detector.DETECTED_VERSION));
        assertEquals("11", properties.getProperty(Detector.DETECTED_VERSION_MAJOR));
        assertEquals("5", properties.getProperty(Detector.DETECTED_VERSION_MINOR));
        assertEquals("osx-aarch_64", properties.getProperty(Detector.DETECTED_CLASSIFIER));
    }

    @Test
    void testUnknownOSWithFailEnabled() {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("FooOS");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("1.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null); // Default is true

        // Act & Assert
        DetectionException exception =
                assertThrows(DetectionException.class, () -> detector.detect(properties, List.of()));
        assertEquals("unknown os.name: FooOS", exception.getMessage());
    }

    @Test
    void testUnknownOSWithFailDisabled() {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("FooOS");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("1.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn("false");

        // Act
        detector.detect(properties, List.of());

        // Assert
        assertEquals("unknown", properties.getProperty(Detector.DETECTED_NAME));
        assertEquals("x86_64", properties.getProperty(Detector.DETECTED_ARCH));
        assertEquals("64", properties.getProperty(Detector.DETECTED_BITNESS));
        assertEquals("unknown-x86_64", properties.getProperty(Detector.DETECTED_CLASSIFIER));
    }

    @Test
    void testLinuxRedhatReleaseFile() throws IOException {
        doThrow(new IOException("File not found")).when(fileOperationProvider).readFile("/etc/os-release");
        doThrow(new IOException("File not found")).when(fileOperationProvider).readFile("/usr/lib/os-release");

        String redhatReleaseContent = "Red Hat Enterprise Linux release 8.6 (Ootpa)";
        InputStream redhatReleaseStream =
                new ByteArrayInputStream(redhatReleaseContent.getBytes(StandardCharsets.UTF_8));
        when(fileOperationProvider.readFile("/etc/redhat-release")).thenReturn(redhatReleaseStream);

        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Linux");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("4.18.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null);

        // Act
        detector.detect(properties, List.of("rhel"));

        // Assert
        assertEquals("linux", properties.getProperty(Detector.DETECTED_NAME));
        assertEquals("rhel", properties.getProperty(Detector.DETECTED_RELEASE));
        assertEquals("8", properties.getProperty(Detector.DETECTED_RELEASE_VERSION));
        assertEquals("true", properties.getProperty(Detector.DETECTED_RELEASE_LIKE_PREFIX + "rhel"));
        assertEquals("true", properties.getProperty(Detector.DETECTED_RELEASE_LIKE_PREFIX + "fedora"));
        assertEquals("linux-x86_64-rhel", properties.getProperty(Detector.DETECTED_CLASSIFIER));
    }

    @ParameterizedTest
    @CsvSource({
        "x8664, x86_64",
        "amd64, x86_64",
        "ia32e, x86_64",
        "em64t, x86_64",
        "x64, x86_64",
        "x8632, x86_32",
        "x86, x86_32",
        "i386, x86_32",
        "i486, x86_32",
        "i586, x86_32",
        "i686, x86_32",
        "ia32, x86_32",
        "x32, x86_32",
        "sparc, sparc_32",
        "sparcv9, sparc_64",
        "arm, arm_32",
        "arm32, arm_32",
        "aarch64, aarch_64",
        "ppc, ppc_32",
        "ppc64, ppc_64",
        "ppc64le, ppcle_64",
        "s390, s390_32",
        "s390x, s390_64",
        "riscv, riscv",
        "riscv64, riscv64",
        "loongarch64, loongarch_64",
        "unknown_arch, unknown"
    })
    void testNormalizeArch(String input, String expected) {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Linux");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn(input);
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("1.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn("false");

        // Act
        detector.detect(properties, List.of());

        // Assert
        assertEquals(expected, properties.getProperty(Detector.DETECTED_ARCH));
    }

    @ParameterizedTest
    @CsvSource({
        "aix, aix",
        "AIX, aix",
        "hpux, hpux",
        "HP-UX, hpux",
        "os400, os400",
        "OS/400, os400",
        "linux, linux",
        "Linux, linux",
        "mac, osx",
        "Mac OS X, osx",
        "osx, osx",
        "freebsd, freebsd",
        "FreeBSD, freebsd",
        "openbsd, openbsd",
        "OpenBSD, openbsd",
        "netbsd, netbsd",
        "NetBSD, netbsd",
        "solaris, sunos",
        "SunOS, sunos",
        "windows, windows",
        "Windows, windows",
        "zos, zos",
        "z/OS, zos",
        "unknown_os, unknown"
    })
    void testNormalizeOS(String input, String expected) {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn(input);
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("1.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn("false");

        // Act
        detector.detect(properties, List.of());

        // Assert
        assertEquals(expected, properties.getProperty(Detector.DETECTED_NAME));
    }

    @Test
    void testDetermineBitnessFromSunProperty() {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Windows");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("1.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null);

        // Act
        detector.detect(properties, List.of());

        // Assert
        assertEquals("64", properties.getProperty(Detector.DETECTED_BITNESS));
    }

    @Test
    void testDetermineBitnessFromIBMProperty() {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Linux");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("1.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("");
        when(systemPropertyProvider.getSystemProperty("com.ibm.vm.bitmode")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null);

        // Act
        detector.detect(properties, List.of());

        // Assert
        assertEquals("64", properties.getProperty(Detector.DETECTED_BITNESS));
    }

    @Test
    void testDetermineBitnessFromArchitecture() {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Linux");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("1.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("");
        when(systemPropertyProvider.getSystemProperty("com.ibm.vm.bitmode")).thenReturn("");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null);

        // Act
        detector.detect(properties, List.of());

        // Assert
        assertEquals("64", properties.getProperty(Detector.DETECTED_BITNESS));
    }

    @ParameterizedTest
    @MethodSource("provideGuessArchitectureBitness")
    void testGuessBitnessFromArchitecture(String arch, int expectedBitness) {
        // Act & Assert
        assertEquals(expectedBitness, Detector.guessBitnessFromArchitecture(arch));
    }

    private static Stream<Arguments> provideGuessArchitectureBitness() {
        return Stream.of(
                Arguments.of("x86_64", 64),
                Arguments.of("amd64", 64),
                Arguments.of("ppc64", 64),
                Arguments.of("aarch_64", 64),
                Arguments.of("x86_32", 32),
                Arguments.of("x86", 32),
                Arguments.of("arm_32", 32));
    }

    @Test
    void testLogging() throws IOException {
        // Arrange
        when(systemPropertyProvider.getSystemProperty("os.name")).thenReturn("Linux");
        when(systemPropertyProvider.getSystemProperty("os.arch")).thenReturn("x86_64");
        when(systemPropertyProvider.getSystemProperty("os.version")).thenReturn("5.4.0");
        when(systemPropertyProvider.getSystemProperty("sun.arch.data.model")).thenReturn("64");
        when(systemPropertyProvider.getSystemProperty("failOnUnknownOS")).thenReturn(null);

        // Act
        detector.detect(properties, List.of());

        // Assert
        String logs = detector.getLogOutput();
        assertTrue(logs.contains("Detecting the operating system and CPU architecture"));
        assertTrue(logs.contains(Detector.DETECTED_NAME + "=linux"));
        assertTrue(logs.contains(Detector.DETECTED_ARCH + "=x86_64"));
        assertTrue(logs.contains(Detector.DETECTED_BITNESS + "=64"));
    }
}
