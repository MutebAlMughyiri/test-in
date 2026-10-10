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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testin.logger.Logger;
import org.testin.util.Bundle;

import java.util.Locale;
import java.util.Objects;

// Rule-INTERNAL-089, Rule-TREE-PANEL-106
record TestinProjectConfig(@NotNull TestinLocation location, @NotNull String repoUrl, @NotNull String testinProject, @NotNull String bugRepoUrl) {
    public static final @NotNull TestinProjectConfig EMPTY = new TestinProjectConfig(
            TestinLocation.LOCAL, "", "", "");
    static final @NotNull String PROJECT_KEY = "testinProject";
    static final @NotNull String LOCATION_KEY = "location";
    static final @NotNull String REPO_URL_KEY = "repoUrl";
    static final @NotNull String BUG_REPO_URL_KEY = "bugRepoUrl";

    public TestinProjectConfig {
        // Rule-TREE-PANEL-117
        repoUrl = withoutCredentials(repoUrl);

        bugRepoUrl = withoutCredentials(bugRepoUrl);

        report(location, repoUrl);
    }

    // UC-TREE-PANEL-001, Rule-TREE-PANEL-124
    private static void report(final @NotNull TestinLocation location, final @NotNull String repoUrl) {
        if (location.isRemote() && repoUrl.isEmpty()) Logger.warn(Bundle.message("config.warn.remote.no.repo.url"));
    }

    @JsonCreator
    static @NotNull TestinProjectConfig read(@JsonProperty(LOCATION_KEY) final @Nullable String location, @JsonProperty(REPO_URL_KEY) final @Nullable String repoUrl, @JsonProperty(PROJECT_KEY) final @Nullable String testinProject, @JsonProperty(BUG_REPO_URL_KEY) final @Nullable String bugRepoUrl) {
        return new TestinProjectConfig(TestinLocation.of(strip(location)),
                strip(repoUrl),
                strip(testinProject),
                strip(bugRepoUrl));
    }

    private static @NotNull String strip(final @Nullable String value) {
        return Objects.requireNonNullElse(value, "").strip();
    }

    // Rule-SHARE-004
    static @NotNull String withoutCredentials(final @NotNull String address) {
        final @NotNull String url = address.strip();
        final int scheme = url.indexOf("://");
        if (scheme < 0) return url;

        final int start = scheme + "://".length();
        final int end = url.indexOf('/', start);
        final @NotNull String authority = end < 0 ? url.substring(start) : url.substring(start, end);

        final int at = authority.lastIndexOf('@');
        if (at < 0) return url;

        final @NotNull String protocol = url.substring(0, scheme).toLowerCase(Locale.ROOT);
        final boolean overHttp = protocol.equals("https") || protocol.equals("http");
        if (!overHttp && authority.lastIndexOf(':', at) < 0) return url;

        return url.substring(0, start) + authority.substring(at + 1) + (end < 0 ? "" : url.substring(end));
    }

    public @NotNull String projectName() {
        return testinProject;
    }

    public boolean hasRepoUrl() {
        return location.isRemote() && !repoUrl.isEmpty();
    }
}
