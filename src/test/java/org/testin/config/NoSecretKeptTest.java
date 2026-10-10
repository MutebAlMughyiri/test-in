/*
 * Copyright 2026 Muteb AlMughyiri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testin.config;

import org.jetbrains.annotations.NotNull;
import org.testin.setting.AppSettingsState;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import static org.testng.Assert.assertTrue;

public class NoSecretKeptTest {

    private static final @NotNull String TOKEN = "ghp_n0tARealToken";

    // Rule-PRODUCT-004
    @Test
    public void aRemotesCredentialsNeverReachTheFileTheRepositoryCarries() {
        final @NotNull Map<String, String> written = TestinYml.lines("NAFATH", "https://muteb:" + TOKEN + "@github.com/mtb550/qa.git");

        assertTrue(written.values().stream().noneMatch(value -> value.contains(TOKEN) || value.contains("muteb:")), "testin.yml would carry the remote's credentials: " + written);
        assertTrue(written.containsValue("https://github.com/mtb550/qa.git"), "the remote itself was dropped with its credentials: " + written);
    }

    // Rule-PRODUCT-004
    @Test
    public void theSettingsHoldNoSecret() {
        final @NotNull List<String> secretLike = Arrays.stream(AppSettingsState.class.getFields())
                .map(Field::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .filter(name -> Stream.of("password", "passphrase", "token", "secret", "credential").anyMatch(name::contains) || name.endsWith("key"))
                .toList();

        assertTrue(secretLike.isEmpty(), "Testin's settings hold a secret: " + secretLike);
    }
}
