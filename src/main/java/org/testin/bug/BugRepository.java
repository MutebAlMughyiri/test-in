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

package org.testin.bug;

import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record BugRepository(@NotNull String host, @NotNull String owner, @NotNull String name) {
    private static final @NotNull Pattern SCHEME = Pattern.compile("^(https?|ssh)://");

    private static final @NotNull Pattern HOST = Pattern.compile("^[A-Za-z0-9.-]+$");

    private static final @NotNull Pattern PART = Pattern.compile("^[A-Za-z0-9._-]+$");

    public static @NotNull Optional<BugRepository> of(final @NotNull String address) {
        final @NotNull String value = TestinYml.addressWithoutCredentials(address);
        if (value.isEmpty()) return Optional.empty();

        if (value.startsWith(TestinYml.SCP_PREFIX)) {
            final int colon = value.indexOf(':');
            if (colon < 0) return Optional.empty();

            return fromParts(value.substring(TestinYml.SCP_PREFIX.length(), colon), value.substring(colon + 1));
        }

        final @NotNull Matcher scheme = SCHEME.matcher(value);
        if (!scheme.find()) return Optional.empty();

        final @NotNull String rest = value.substring(scheme.end());
        final int slash = rest.indexOf('/');
        if (slash < 0) return Optional.empty();

        final @NotNull String authority = rest.substring(0, slash);
        final @NotNull String hostAndPort = authority.substring(authority.lastIndexOf('@') + 1);
        final int port = hostAndPort.indexOf(':');

        return fromParts(port < 0 ? hostAndPort : hostAndPort.substring(0, port), rest.substring(slash + 1));
    }

    private static @NotNull Optional<BugRepository> fromParts(final @NotNull String host, final @NotNull String path) {
        String trimmed = path;
        while (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        while (trimmed.endsWith("/")) trimmed = trimmed.substring(0, trimmed.length() - 1);
        if (trimmed.endsWith(".git")) trimmed = trimmed.substring(0, trimmed.length() - ".git".length());

        final String @NotNull [] parts = trimmed.split("/", -1);
        if (parts.length != 2 || !HOST.matcher(host).matches()) return Optional.empty();
        if (!PART.matcher(parts[0]).matches() || !PART.matcher(parts[1]).matches()) return Optional.empty();

        return Optional.of(new BugRepository(host, parts[0], parts[1]));
    }

    public @NotNull String ghRepo() {
        return host + "/" + owner + "/" + name;
    }

    public @NotNull String displayName() {
        return owner + "/" + name;
    }
}
