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

package org.testin.apimodel;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.testin.notifications.Refused;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class JsonModelTest {

    private static @NotNull JsonNode json(final @NotNull String text) {
        return JsonModel.read(text).orElseThrow(() -> new AssertionError("not read as JSON: " + text));
    }

    private static @NotNull ModelType model(final @NotNull String name, final @NotNull String text) {
        return JsonModel.of(name, json(text));
    }

    private static @NotNull List<String> fields(final @NotNull ModelType type) {
        return type.fields().stream().map(field -> field.name() + ":" + field.type() + (field.isRenamed() ? "@" + field.key() : "")).toList();
    }

    private static @NotNull ModelType nested(final @NotNull ModelType type, final @NotNull String name) {
        return type.nested().stream().filter(one -> one.name().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError(type.name() + " holds no nested " + name + ": " + type.nested().stream().map(ModelType::name).toList()));
    }

    // Rule-CODEGEN-102, Rule-CODEGEN-103
    @Test
    public void theRequestSampleKeepsEveryKeyAndRenamesOnlyWhatJavaCannotUse() {
        final @NotNull ModelType request = model("UserRequest", """
                { "first-name": "Muteb", "mobile": "0501234567", "pin": "4321",
                  "address": { "city": "Riyadh", "street": "King Fahd Rd" },
                  "roles": ["customer"],
                  "cards": [ { "type": "debit" }, { "type": "credit", "limit": 5000 } ] }""");

        assertEquals(fields(request), List.of("firstName:String@first-name", "mobile:String", "pin:String", "address:Address", "roles:List<String>", "cards:List<CardsItem>"));
        assertEquals(fields(nested(request, "Address")), List.of("city:String", "street:String"));
        assertEquals(fields(nested(request, "CardsItem")), List.of("type:String", "limit:Integer"), "a key only the second card has was lost");
    }

    // Rule-CODEGEN-102, Rule-CODEGEN-103
    @Test
    public void everyKeyAndValueKindHasItsName() {
        final @NotNull ModelType edge = model("EdgeResponse", """
                { "data": { "id": 7 }, "id": 7, "ID": 8, "first_name": "Muteb", "2fa": true, "class": "gold",
                  "$type": "user", "_id": "u-7", "الاسم": "x", "%": 15,
                  "amounts": [1, 1.5], "tags": ["vip", null, "new"], "big": 92233720368547758070,
                  "createdAt": "2026-10-10T09:00:00Z", "list": { "page": 1 } }""");

        assertEquals(fields(edge), List.of(
                "data:DataItem", "id:Integer", "id2:Integer@ID", "firstName:String@first_name", "_2fa:Boolean@2fa", "classValue:String@class",
                "type:String@$type", "id3:String@_id", "field:String@الاسم", "field2:Integer@%",
                "amounts:List<Double>", "tags:List<String>", "big:BigInteger", "createdAt:String", "list:ListItem"));
        assertEquals(fields(nested(edge, "DataItem")), List.of("id:Integer"));
        assertEquals(fields(nested(edge, "ListItem")), List.of("page:Integer"));
    }

    // Rule-CODEGEN-102
    @Test
    public void aListsObjectsMergeAtEveryDepth() {
        final @NotNull ModelType root = model("Deep", """
                [ { "o": { "a": 1 }, "items": [ { "x": 1 } ] },
                  { "o": { "b": 2 }, "items": [ { "y": "z" } ] } ]""");

        assertEquals(fields(root), List.of("o:O", "items:List<ItemsItem>"));
        assertEquals(fields(nested(root, "O")), List.of("a:Integer", "b:Integer"), "a key inside the second object's object was lost");
        assertEquals(fields(nested(root, "ItemsItem")), List.of("x:Integer", "y:String"));
    }

    // Rule-CODEGEN-102
    @Test
    public void valuesThatMeetWidenOrBecomeObject() {
        final @NotNull ModelType root = model("Mixed", """
                [ { "n": 1, "v": 1, "l": 1, "late": null },
                  { "n": 2.5, "v": "x", "l": 3000000000, "late": { "k": true } } ]""");

        assertEquals(fields(root), List.of("n:Double", "v:Object", "l:Long", "late:Late"));
        assertEquals(fields(nested(root, "Late")), List.of("k:Boolean"));
    }

    // Rule-CODEGEN-102
    @Test
    public void listsOfListsAndEmptyListsKeepTheirDepth() {
        final @NotNull ModelType root = model("Lists", """
                { "grid": [[1, 2], [3]], "deep": [[[["1"]]], [[["2"]]]], "none": [], "noneInside": [[]], "nothing": null }""");

        assertEquals(fields(root), List.of("grid:List<List<Integer>>", "deep:List<List<List<List<String>>>>", "none:List<Object>", "noneInside:List<List<Object>>", "nothing:Object"));
    }

    // Rule-CODEGEN-103
    @Test
    public void aNestedTypeNeverTakesTheNameOfATypeAroundIt() {
        final @NotNull ModelType root = model("User", """
                { "user": { "user": { "id": 1 } } }""");

        final @NotNull ModelType inner = nested(root, "User2");
        assertEquals(fields(root), List.of("user:User2"));
        assertEquals(fields(inner), List.of("user:User3"), "a nested type took the name of a type around it");
    }

    // Rule-CODEGEN-103
    @Test
    public void oneLetterKeysAndObjectMethodNamesCompile() {
        final @NotNull ModelType root = model("Short", """
                { "a": 1, "A": 2, "_": 3, "getClass": "x", "toString": "y", "wait": 1 }""");

        assertEquals(fields(root), List.of("a:Integer", "a2:Integer@A", "field:Integer@_", "getClassValue:String@getClass", "toStringValue:String@toString", "waitValue:Integer@wait"));
    }

    // Rule-CODEGEN-099
    @Test
    public void jsonCopiedFromAScriptIsRead() {
        assertTrue(JsonModel.read("// a comment\n{ 'name': 'x', unquoted: 1, }").isPresent(), "comments, single quotes, unquoted keys or a trailing comma were refused");
    }

    // Rule-CODEGEN-099
    @Test
    public void whatIsNotJsonOrHasNoKeysIsRefused() {
        assertEquals(JsonModel.read("{ \"id\": 7,"), Optional.empty());
        assertEquals(JsonModel.read("{\"orderId\":1024,\"isPaid\":}"), Optional.empty(), "a key with no value was read");
        assertEquals(JsonModel.read("{\"orderId\":1024,\"isPaid\":tru}"), Optional.empty(), "a misspelled value was read");
        assertEquals(JsonModel.read("{ \"id\": 7 } and more"), Optional.empty(), "text after the JSON was ignored");
        assertEquals(JsonModel.read(""), Optional.empty());

        assertEquals(JsonModel.refusalOf(json("[1, 2, 3]")), Optional.of(Refused.NOT_A_JSON_OBJECT));
        assertEquals(JsonModel.refusalOf(json("\"ok\"")), Optional.of(Refused.NOT_A_JSON_OBJECT));
        assertEquals(JsonModel.refusalOf(json("{}")), Optional.of(Refused.NO_KEYS));
        assertEquals(JsonModel.refusalOf(json("[]")), Optional.of(Refused.NO_KEYS));
        assertEquals(JsonModel.refusalOf(json("[{}]")), Optional.of(Refused.NO_KEYS));
        assertEquals(JsonModel.refusalOf(json("[{ \"a\": 1 }, null]")), Optional.empty());
        assertEquals(JsonModel.refusalOf(json("{ \"a\": 1 }")), Optional.empty());
    }
}
