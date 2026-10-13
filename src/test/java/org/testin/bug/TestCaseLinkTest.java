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

import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestCaseLinkTest {

    private static final Path FILE = Path.of("Test Cases", "ts2", "07f7e754-b849-4b38-9e6e-a2cacd84e927.testcase");
    private static final String LINK = "https://github.com/mtb550/test-03/blob/master/Test%20Cases/ts2/07f7e754-b849-4b38-9e6e-a2cacd84e927.testcase";

    @Test
    public void aWebRemoteLinksToTheFileOnItsBranch() {
        assertEquals(TestCaseLink.of("https://github.com/mtb550/test-03.git", "master", FILE), Optional.of(LINK));
    }

    @Test
    public void aCloneRemoteBecomesItsWebForm() {
        assertEquals(TestCaseLink.of("git@github.com:mtb550/test-03.git", "master", FILE), Optional.of(LINK));
        assertEquals(TestCaseLink.of("ssh://git@github.com:22/mtb550/test-03", "master", FILE), Optional.of(LINK));
    }

    @Test
    public void aBranchWithASlashKeepsIt() {
        assertEquals(TestCaseLink.of("https://github.com/mtb550/test-03", "feature/login", Path.of("a.json")),
                Optional.of("https://github.com/mtb550/test-03/blob/feature/login/a.json"));
    }

    @Test
    public void charactersALinkCannotHoldAreEncoded() {
        assertEquals(TestCaseLink.of("https://github.com/mtb550/test-03", "master", Path.of("Test Cases", "Log in (web) #2", "a.json")),
                Optional.of("https://github.com/mtb550/test-03/blob/master/Test%20Cases/Log%20in%20%28web%29%20%232/a.json"));
    }

    @Test
    public void noBranchOrNoRepositoryMeansNoLink() {
        assertTrue(TestCaseLink.of("https://github.com/mtb550/test-03", "", FILE).isEmpty(), "a detached HEAD has no branch to link to");
        assertTrue(TestCaseLink.of("", "master", FILE).isEmpty(), "a repository with no remote");
        assertTrue(TestCaseLink.of("C:/repos/test-03", "master", FILE).isEmpty(), "a remote that is a folder on this machine");
    }
}
