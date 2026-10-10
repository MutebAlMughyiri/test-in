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

package org.testin.model.bug;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.testng.Assert.assertEquals;

public class BugIssueUrlTest {

    @Test
    public void anIssueReadsAsGitHubWritesAReference() {
        assertEquals(BugIssueUrl.reference("https://github.com/mtb550/product/issues/123"), "mtb550/product#123");
        assertEquals(BugIssueUrl.reference("https://ghe.acme.com/qa/product/issues/7"), "qa/product#7");
    }

    @Test
    public void anIssueReadsAsItsNumberWhereRoomIsShort() {
        assertEquals(BugIssueUrl.shortReference("https://github.com/mtb550/test-01/issues/12"), "#12");
        assertEquals(BugIssueUrl.shortReference(""), "", "no link is no text");
        assertEquals(BugIssueUrl.shortReference("https://github.com/mtb550/product/pull/5"), "https://github.com/mtb550/product/pull/5");
    }

    @Test
    public void anIssueIsReadAsItsHostRepositoryAndNumber() {
        assertEquals(BugIssueUrl.issue("https://github.example.com/qa/product/issues/14"), Optional.of(new BugIssue("github.example.com", "qa", "product", 14)));
        assertEquals(BugIssueUrl.issue("https://github.com/qa/product/pull/14"), Optional.empty());
    }

    @Test
    public void somethingElseReadsAsItself() {
        assertEquals(BugIssueUrl.reference(""), "", "no link is no text");
        assertEquals(BugIssueUrl.reference("https://github.com/mtb550/product/pull/5"), "https://github.com/mtb550/product/pull/5");
    }

    @Test
    public void theAddressIsFoundInWhatGhPrinted() {
        assertEquals(BugIssueUrl.firstIn("""
                        Creating issue in mtb550/product
                        
                        https://github.com/mtb550/product/issues/42
                        """),
                Optional.of("https://github.com/mtb550/product/issues/42"));
        assertEquals(BugIssueUrl.firstIn("could not create issue: HTTP 401"), Optional.empty());
    }
}
