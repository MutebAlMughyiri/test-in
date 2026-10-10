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

package org.testin.codegen;

import com.intellij.ide.IdeView;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUiKind;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.config.TestinYml;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class ApiModelActionIdeTest extends AbstractCodegenIdeTest {
    private static final @NotNull String ACTION = "Testin.ApiModel";

    private @NotNull AnAction action() {
        return Optional.ofNullable(ActionManager.getInstance().getAction(ACTION))
                .orElseThrow(() -> new AssertionError(ACTION + " is not registered in plugin.xml"));
    }

    private @NotNull VirtualFile sourceRoot() {
        return Arrays.stream(ModuleRootManager.getInstance(getModule()).getSourceRoots(true)).findFirst()
                .orElseThrow(() -> new AssertionError("the project has no source folder"));
    }

    private @NotNull PsiDirectory directoryOf(final @NotNull VirtualFile folder) {
        return Optional.ofNullable(PsiManager.getInstance(getProject()).findDirectory(folder)).orElseThrow(() -> new AssertionError("no directory for " + folder));
    }

    private @NotNull PsiDirectory aPackage() {
        return WriteAction.computeAndWait(() -> directoryOf(sourceRoot()).createSubdirectory("api"));
    }

    private @NotNull PsiDirectory aFolderOutsideTheSources() {
        return directoryOf(Objects.requireNonNull(sourceRoot().getParent(), "the source folder has no parent"));
    }

    private @NotNull Presentation shownOn(final @NotNull PsiDirectory directory) {
        final @NotNull AnAction action = action();
        final @NotNull IdeView view = new IdeView() {
            @Override
            public PsiDirectory @NotNull [] getDirectories() {
                return new PsiDirectory[]{directory};
            }

            @Override
            public @NotNull PsiDirectory getOrChooseDirectory() {
                return directory;
            }
        };
        final @NotNull AnActionEvent e = AnActionEvent.createEvent(action,
                SimpleDataContext.builder().add(CommonDataKeys.PROJECT, getProject()).add(LangDataKeys.IDE_VIEW, view).build(),
                action.getTemplatePresentation().clone(), ActionPlaces.PROJECT_VIEW_POPUP, ActionUiKind.POPUP, null);
        ActionUtil.updateAction(action, e);
        return e.getPresentation();
    }

    // UC-CODEGEN-022, Rule-CODEGEN-098
    public void testTheEntryCarriesItsName() {
        assertEquals(Bundle.message("action.Testin.ApiModel.text"), action().getTemplatePresentation().getText());
    }

    // UC-CODEGEN-022, Rule-CODEGEN-098
    public void testOnAJavaPackageItCanBeChosen() {
        assertTrue("the entry is gray on a Java package", shownOn(aPackage()).isEnabled());
    }

    // UC-CODEGEN-022, Rule-CODEGEN-098
    public void testOnAFolderThatIsNotAPackageItIsGrayAndSaysWhy() {
        final @NotNull Presentation shown = shownOn(aFolderOutsideTheSources());

        assertFalse("the entry works on a folder that is not a Java package", shown.isEnabled());
        assertEquals(Bundle.message("api.model.not.a.package"), shown.getDescription());
    }

    // UC-CODEGEN-022, Rule-CODEGEN-082, Rule-CODEGEN-098
    public void testWhileTestinYmlNamesAnotherTestProjectItIsGrayAndSaysItNeedsIt() {
        final @NotNull BoundTestProject bound = Services.getInstance(getProject(), BoundTestProject.class);
        try {
            bound.choose("Shop");

            final @NotNull Presentation shown = shownOn(aPackage());

            assertFalse("the entry works while testin.yml names another test project", shown.isEnabled());
            assertTrue("the entry does not say it needs testin.yml: " + shown.getText(), Objects.toString(shown.getText(), "").contains(TestinYml.fileName()));
        } finally {
            bound.choose(TestinYml.projectName(getProject()));
        }
    }
}
