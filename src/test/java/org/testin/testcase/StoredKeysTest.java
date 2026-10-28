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

package org.testin.testcase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.testin.model.TestCase;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;

public class StoredKeysTest {

    @Test
    public void everyFieldNamesTheKeyItsValueIsStoredUnder() {
        final Set<String> stored = Arrays.stream(TestCase.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()) && !field.isAnnotationPresent(JsonIgnore.class))
                .map(Field::getName)
                .collect(Collectors.toSet());

        final List<String> named = Arrays.stream(TestSetEditorFields.values()).map(TestSetEditorFields::getStoredKey).filter(key -> !key.isEmpty()).toList();

        assertEquals(named.stream().filter(key -> !stored.contains(key)).toList(), List.of(), "a field names a key no test case is stored under");
    }
}
