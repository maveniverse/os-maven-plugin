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

import org.junit.jupiter.api.Test;

class DetectionExceptionTest {

    @Test
    void testConstructorWithMessage() {
        // Arrange
        String errorMessage = "Test error message";

        // Act
        DetectionException exception = new DetectionException(errorMessage);

        // Assert
        assertEquals(errorMessage, exception.getMessage());
    }

    @Test
    void testConstructorWithMessageAndCause() {
        // Arrange
        String errorMessage = "Test error message";
        Throwable cause = new RuntimeException("Root cause");

        // Act
        DetectionException exception = new DetectionException(errorMessage, cause);

        // Assert
        assertEquals(errorMessage, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
}
