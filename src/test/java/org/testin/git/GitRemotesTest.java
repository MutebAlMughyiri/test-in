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

import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class GitRemotesTest {

    @Test
    public void everyFormGitCloneTakesIsAnAddress() {
        assertTrue(GitRemotes.isRepositoryUrl("https://github.com/acme/repo.git"));
        assertTrue(GitRemotes.isRepositoryUrl("ssh://git@github.com/acme/repo.git"));
        assertTrue(GitRemotes.isRepositoryUrl("git@host:acme/repo.git"));
        assertTrue(GitRemotes.isRepositoryUrl("http://localhost/x.git"), "plain http is a scheme git clone takes");
        assertTrue(GitRemotes.isRepositoryUrl("git://host/x"), "the file's own rule used to drop this one");
        assertTrue(GitRemotes.isRepositoryUrl("https://host/r.git?ref=main"), "a query is part of the address");
        assertTrue(GitRemotes.isRepositoryUrl("  https://github.com/acme/repo.git  "), "surrounding space is trimmed");
    }

    @Test
    public void aProjectNameIsNotAnAddress() {
        assertFalse(GitRemotes.isRepositoryUrl("NAFATH"));
        assertFalse(GitRemotes.isRepositoryUrl("Checkout Regression"));
        assertFalse(GitRemotes.isRepositoryUrl(""));
        assertFalse(GitRemotes.isRepositoryUrl("   "));
    }

    @Test
    public void textWithCharactersNoAddressHasIsRefused() {
        assertFalse(GitRemotes.isRepositoryUrl("https://x.com/r; rm -rf /"), "a semicolon is not in a clone address");
        assertFalse(GitRemotes.isRepositoryUrl("https://x.com/r a"), "nor is a space inside one");
        assertFalse(GitRemotes.isRepositoryUrl("https://x.com/\"r\""), "nor a quote");
        assertFalse(GitRemotes.isRepositoryUrl("https://x.com/r|whoami"), "nor a pipe");
    }

    // Rule-SHARE-122
    @Test
    public void originWinsWhateverOrderTheRemotesArrive() {
        assertEquals(GitRemotes.chooseRemote(List.of("upstream", "origin", "fork")), "origin");
    }

    // Rule-SHARE-122
    @Test
    public void withoutOriginTheFirstRemoteIsUsed() {
        assertEquals(GitRemotes.chooseRemote(List.of("upstream", "fork")), "upstream");
    }

    @Test
    public void remoteNamesAreTrimmedAndBlankLinesIgnored() {
        assertEquals(GitRemotes.chooseRemote(List.of("", "  ", "  upstream  ")), "upstream");
    }

    @Test
    public void noRemotesMeansNoRemoteToSyncWith() {
        assertEquals(GitRemotes.chooseRemote(List.of()), "");
        assertEquals(GitRemotes.chooseRemote(List.of("", "   ")), "");
    }

    @Test
    public void remoteBranchNamesLoseTheirRemotePrefix() {
        assertEquals(GitRemotes.localNameOf("origin/main"), "main");
    }

    @Test
    public void aLocalBranchNameIsAlreadyItsLocalName() {
        assertEquals(GitRemotes.localNameOf("main"), "main");
    }

    // Rule-SHARE-126
    @Test
    public void aLocalBranchWithASlashIsNotARemoteBranch() {
        assertFalse(GitRemotes.isRemoteBranch("feature/login", List.of("feature/login", "main"), List.of("origin")));
    }

    @Test
    public void aRemoteBranchIsNamedAfterItsRemote() {
        assertTrue(GitRemotes.isRemoteBranch("origin/main", List.of("main"), List.of("origin")));
        assertTrue(GitRemotes.isRemoteBranch("origin/feature/login", List.of("main"), List.of("origin")));
        assertEquals(GitRemotes.localNameOf("origin/feature/login"), "feature/login");
    }

    @Test
    public void aNameStartingWithNoRemoteIsNotARemoteBranch() {
        assertFalse(GitRemotes.isRemoteBranch("feature/login", List.of("main"), List.of("origin")));
        assertFalse(GitRemotes.isRemoteBranch("origin/main", List.of("main"), List.of()));
    }
}
