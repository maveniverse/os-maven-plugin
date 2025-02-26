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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SystemPropertyAndFileProviderTest {

    @Test
    void testSimpleSystemPropertyOperations() {
        // Arrange
        SystemPropertyOperationProvider provider = new Detector.SimpleSystemPropertyOperations();
        String testKey = "test.property.key";
        String testValue = "test.property.value";

        try {
            // Act
            String oldValue = provider.setSystemProperty(testKey, testValue);
            String retrievedValue = provider.getSystemProperty(testKey);
            String defaultValue = provider.getSystemProperty("non.existent.key", "default");

            // Assert
            assertEquals(testValue, retrievedValue);
            assertEquals("default", defaultValue);
            assertNull(oldValue); // First time setting should return null

            // Clean up
            System.clearProperty(testKey);
        } finally {
            // Ensure cleanup even if test fails
            System.clearProperty(testKey);
        }
    }

    @Test
    void testSimpleFileOperations(@TempDir Path tempDir) throws IOException {
        // Arrange
        FileOperationProvider provider = new Detector.SimpleFileOperations();
        Path testFile = tempDir.resolve("test-file.txt");
        String content = "Test file content";
        Files.write(testFile, content.getBytes(StandardCharsets.UTF_8));

        // Act
        InputStream inputStream = provider.readFile(testFile.toString());
        byte[] bytes = inputStream.readAllBytes();
        String readContent = new String(bytes, StandardCharsets.UTF_8);

        // Assert
        assertEquals(content, readContent);
    }

    @Test
    void testSimpleFileOperationsFileNotFound() {
        // Arrange
        FileOperationProvider provider = new Detector.SimpleFileOperations();

        // Act & Assert
        assertThrows(IOException.class, () -> provider.readFile("/path/to/nonexistent/file"));
    }
}
