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

package org.testin.util;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.SpellCheckingEditorCustomizationProvider;
import com.intellij.openapi.editor.actions.IncrementalFindAction;
import com.intellij.openapi.fileTypes.FileTypes;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.EditorCustomization;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.EditorTextFieldProvider;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.textCompletion.TextCompletionProvider;
import com.intellij.util.textCompletion.TextCompletionUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpellChecker {
    // Rule-INTERNAL-134
    public static @NotNull EditorTextField createField(final @NotNull Project p) {
        return createField(p, FileTypes.PLAIN_TEXT.getLanguage(), true);
    }

    // Rule-INTERNAL-134, Rule-INTERNAL-136, Rule-INTERNAL-139
    public static @NotNull EditorTextField createField(final @NotNull Project p, final @NotNull Language language, final boolean spellChecked) {
        final @NotNull List<EditorCustomization> customizations = new ArrayList<>();
        final @NotNull SpellCheckingEditorCustomizationProvider spelling = SpellCheckingEditorCustomizationProvider.getInstance();
        ContainerUtil.addIfNotNull(customizations, spellChecked ? spelling.getEnabledCustomization() : spelling.getDisabledCustomization());
        customizations.add(editor -> editor.putUserData(IncrementalFindAction.SEARCH_DISABLED, Boolean.TRUE));

        return EditorTextFieldProvider.getInstance()
                .getEditorField(language, p, customizations);
    }

    public static @NotNull EditorTextField createCompletionField(final @NotNull Project p, final @NotNull TextCompletionProvider provider, final @NotNull String text) {
        final @NotNull EditorTextField field = createField(p);

        final Optional<PsiFile> psiFile = Optional.ofNullable(ReadAction.computeBlocking(
                () -> PsiDocumentManager.getInstance(p).getPsiFile(field.getDocument())));

        psiFile.ifPresentOrElse(
                file -> TextCompletionUtil.installProvider(file, provider, true),
                () -> TestinLogger.warn("Completion not installed: the field has no PSI file"));

        field.setText(text);
        return field;
    }
}
