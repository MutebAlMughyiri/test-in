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


package org.testin.git;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.config.TestinYml;

import java.util.List;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class GitRemotes {
    // UC-TREE-PANEL-001, Rule-TREE-PANEL-117
    private static final @NotNull Pattern CLONE_CHARACTERS = Pattern.compile("^[A-Za-z0-9._~:/?#@%+=-]+$");

    private static final @NotNull Pattern REMOTE_SCHEME = Pattern.compile("^(https?|ssh|git)://");

    // UC-SHARE-016, Rule-SHARE-122
    public static @NotNull String chooseRemote(final @NotNull List<String> remotes) {
        final @NotNull List<String> names = remotes.stream()
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .toList();
        if (names.contains("origin")) return "origin";
        return names.isEmpty() ? "" : names.getFirst();
    }

    // Rule-TREE-PANEL-117
    public static boolean isRepositoryUrl(final @NotNull String text) {
        final @NotNull String value = text.trim();
        if (!CLONE_CHARACTERS.matcher(value).matches()) return false;

        return REMOTE_SCHEME.matcher(value).find()
                || value.startsWith(TestinYml.SCP_PREFIX)
                || value.endsWith(".git");
    }

    public static @NotNull String localNameOf(final @NotNull String remoteBranchName) {
        return remoteBranchName.substring(remoteBranchName.indexOf('/') + 1);
    }

    // UC-TREE-PANEL-026, Rule-TREE-PANEL-086, Rule-SHARE-126
    public static boolean isRemoteBranch(final @NotNull String branch, final @NotNull List<String> localBranches, final @NotNull List<String> remotes) {
        return !localBranches.contains(branch)
                && remotes.stream().map(String::trim).filter(remote -> !remote.isEmpty()).anyMatch(remote -> branch.startsWith(remote + "/"));
    }
}