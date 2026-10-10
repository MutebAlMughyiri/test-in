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
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class BugRepositoryTest {

    private static @NotNull String gh(final String address) {
        return BugRepository.of(address).map(BugRepository::ghRepo).orElse("refused");
    }

    @Test
    public void aWebAddressNamesItsRepository() {
        assertEquals(gh("https://github.com/mtb550/product"), "github.com/mtb550/product");
        assertEquals(gh("http://localhost/mtb550/product"), "localhost/mtb550/product");
    }

    @Test
    public void aCloneAddressNamesTheSameRepository() {
        assertEquals(gh("https://github.com/mtb550/product.git"), "github.com/mtb550/product");
        assertEquals(gh("https://github.com/mtb550/product/"), "github.com/mtb550/product", "a trailing slash is not a segment");
        assertEquals(gh("https://github.com/mtb550/product.git/"), "github.com/mtb550/product");
    }

    @Test
    public void sshAddressesNameTheirRepository() {
        assertEquals(gh("git@github.com:mtb550/product.git"), "github.com/mtb550/product");
        assertEquals(gh("ssh://git@github.com/mtb550/product.git"), "github.com/mtb550/product");
        assertEquals(gh("ssh://git@ghe.acme.com:2222/qa/product"), "ghe.acme.com/qa/product", "the port is not the host");
    }

    @Test
    public void anEnterpriseHostIsKept() {
        assertEquals(gh("https://ghe.acme.com/qa/product"), "ghe.acme.com/qa/product");
    }

    @Test
    public void credentialsNeverReachTheRepository() {
        assertEquals(gh("https://mtb550:ghp_secret@github.com/mtb550/product"), "github.com/mtb550/product");
    }

    @Test
    public void anAddressWithMoreThanARepositoryIsRefused() {
        assertEquals(gh("https://github.com/mtb550/product/issues"), "refused",
                "copied from the issues page, it would still file somewhere a tester did not mean");
        assertEquals(gh("https://github.com/mtb550/product/tree/main"), "refused");
    }

    @Test
    public void anAddressWithLessThanARepositoryIsRefused() {
        assertEquals(gh("https://github.com/mtb550"), "refused", "an owner is not a repository");
        assertEquals(gh("https://github.com/"), "refused");
        assertEquals(gh("git@github.com"), "refused");
    }

    @Test
    public void filesAndPathsAreRefused() {
        assertEquals(gh("file:///C:/repos/product"), "refused");
        assertEquals(gh("C:\\repos\\product"), "refused");
        assertEquals(gh("/home/qa/product"), "refused");
        assertEquals(gh("github.com/mtb550/product"), "refused", "no scheme, so not an address this can be sure of");
    }

    @Test
    public void somethingThatIsNotAnAddressIsRefused() {
        assertEquals(gh("https://github.com/mtb550/pro duct"), "refused");
        assertEquals(gh("https://github.com/mtb550/product; rm -rf /"), "refused");
        assertTrue(BugRepository.of("").isEmpty());
        assertTrue(BugRepository.of("   ").isEmpty());
    }

    @Test
    public void aTesterReadsOwnerAndName() {
        assertEquals(BugRepository.of("https://github.com/mtb550/product.git").map(BugRepository::displayName).orElse(""),
                "mtb550/product");
    }
}
