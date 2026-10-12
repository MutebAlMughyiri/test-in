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

package org.testin.testproject;

import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractTempRootIdeTest;
import org.testin.NodesOnDisk;
import org.testin.indexer.TestRuns;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestRunNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.testrun.RunItemExecution;
import org.testin.model.testrun.RunItem;
import org.testin.model.status.RunItemStatus;
import org.testin.services.Services;
import org.testin.setting.AppSettingsState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestProjectOnDiskIdeTest extends AbstractTempRootIdeTest {

    private final @NotNull AppSettingsState wasStored = new AppSettingsState();

    private Path chosen;

    private @NotNull TestProjectNode testProject = new TestProjectNode();

    private static @NotNull AppSettingsState settings() {
        return Services.getInstance(AppSettingsState.class);
    }

    private static @NotNull List<Path> filesUnder(final @NotNull Path folder) {
        try (Stream<Path> walk = Files.walk(folder)) {
            return walk.filter(Files::isRegularFile).toList();
        } catch (final IOException ex) {
            throw new AssertionError("could not read " + folder + ": " + ex.getMessage(), ex);
        }
    }

    @Override
    protected void setUp() {
        super.setUp();
        XmlSerializerUtil.copyBean(settings(), wasStored);
        try {
            chosen = Files.createDirectories(root.resolve("the folder the tester chose"));
            Files.createDirectories(root.resolve("somewhere else"));
        } catch (final IOException ex) {
            throw new AssertionError("could not make the folders: " + ex.getMessage(), ex);
        }
        settings().rootTestinPath = chosen.toString();
    }

    @Override
    protected void tearDown() {
        try {
            XmlSerializerUtil.copyBean(wasStored, settings());
        } finally {
            super.tearDown();
        }
    }

    private @NotNull TestCaseDto aTestProjectWithEverythingInIt() {
        final @NotNull NodesOnDisk onDisk = new NodesOnDisk(getProject());
        testProject = onDisk.testProject(chosen.resolve("NAFATH"));
        final @NotNull TestSetNode login = onDisk.testSet(onDisk.testSetPackage(testProject.getTestCasesFolder(), "Accounts"), "Login");
        final @NotNull TestCaseDto tc = onDisk.testCase(login);
        final @NotNull TestRunNode testRun = onDisk.testRun(testProject.getTestRunsFolder(), "Cycle-1");
        final @NotNull TestRuns testRuns = Services.getInstance(getProject(), TestRuns.class);
        testRuns.putRunItems(testRun.getPath(), new RunItems().setAll(new ArrayList<>(List.of(new RunItem().setId(tc.getId()).setExecutions(List.of(RunItemExecution.builder().status(RunItemStatus.PASSED).build()))))));
        testRuns.awaitWrites();
        return tc;
    }

    // Rule-PRODUCT-001
    public void testEveryTestCaseIsAFileOnDiskInTheFolderTheTesterChose() {
        final @NotNull TestCaseDto tc = aTestProjectWithEverythingInIt();

        final @NotNull List<Path> files = filesUnder(chosen);
        assertTrue("the test case is not a file in the folder the tester chose: " + files, files.stream().anyMatch(file -> file.getFileName().toString().startsWith(tc.getId().toString())));
        assertTrue("something was written outside the folder the tester chose", filesUnder(root.resolve("somewhere else")).isEmpty());
    }

    // Rule-PRODUCT-002
    public void testEverythingBelongingToATestProjectLivesBeneathItsOneFolder() {
        aTestProjectWithEverythingInIt();

        final @NotNull List<Path> outside = filesUnder(root).stream().filter(file -> !file.startsWith(testProject.getPath())).toList();
        assertTrue("the test project wrote files outside its own folder: " + outside, outside.isEmpty());
        assertTrue("the test project wrote nothing at all, so this proves nothing", filesUnder(testProject.getPath()).size() > 5);
    }
}
