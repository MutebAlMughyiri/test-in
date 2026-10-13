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

package org.testin.apimodel;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.testin.logger.TestinLogger;
import org.testin.notifications.Done;
import org.testin.notifications.Refused;
import org.testin.ui.framework.AbstractFrameworkDialog;
import org.testin.ui.framework.DialogPart;
import org.testin.ui.framework.FieldPair;
import org.testin.ui.framework.MultiLineField;
import org.testin.ui.framework.StatusBarShortcut;
import org.testin.ui.framework.TextInput;
import org.testin.util.Bundle;
import org.testin.util.FailureText;
import org.testin.util.Json;
import org.testin.util.Shortcuts;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;

final class ApiModelDialog extends AbstractFrameworkDialog {
    private static final @NotNull String REMEMBERED_LAYOUT = "testin.apiModel.sideBySide";
    private static final @NotNull String REQUEST = "Request";
    private static final @NotNull String RESPONSE = "Response";
    private static final int EMPTY_LINES = 3;

    private final @NotNull PsiDirectory directory;
    private final @NotNull String packageName;
    private final @NotNull TextInput nameInput;
    private final @NotNull MultiLineField request;
    private final @NotNull MultiLineField response;
    private final @NotNull JBLabel requestTitle = new JBLabel(Bundle.message("dialog.api.model.request.caption"));
    private final @NotNull JBLabel responseTitle = new JBLabel(Bundle.message("dialog.api.model.response.caption"));
    private final @NotNull FieldPair pair;

    // UC-CODEGEN-022, Rule-CODEGEN-099, Rule-CODEGEN-106, Rule-CODEGEN-107, Rule-CODEGEN-109
    ApiModelDialog(final @NotNull Project project, final @NotNull PsiDirectory directory) {
        super(project);
        this.directory = directory;
        this.packageName = Optional.ofNullable(JavaDirectoryService.getInstance().getPackage(directory)).map(PsiPackage::getQualifiedName).orElse("");

        title = Bundle.message("dialog.api.model.title");
        resizable = true;

        nameInput = DialogPart.textInput()
                .caption(Bundle.message("dialog.api.model.name.caption"))
                .placeholder(Bundle.message("dialog.api.model.name.placeholder"))
                .accepting("[A-Za-z][A-Za-z0-9]*")
                .build()
                .getComponent();
        request = DialogPart.multiLineField(project, "", Bundle.message("dialog.api.model.request.placeholder"), EMPTY_LINES, MultiLineField.NO_LIMIT, Json.language(), false).getComponent();
        response = DialogPart.multiLineField(project, "", Bundle.message("dialog.api.model.response.placeholder"), EMPTY_LINES, MultiLineField.NO_LIMIT, Json.language(), false).getComponent();
        pair = DialogPart.fieldPair(project, REMEMBERED_LAYOUT, requestTitle, request, responseTitle, response).getComponent();

        components = List.of(
                DialogPart.details().row(Bundle.message("dialog.api.model.path"), packageName.isEmpty() ? Bundle.message("dialog.api.model.path.root") : packageName).build(),
                DialogPart.of(nameInput),
                DialogPart.of(pair),
                DialogPart.button(Bundle.message("dialog.api.model.create")));

        shortcuts = List.of(
                StatusBarShortcut.build(Shortcuts.Enter, Bundle.message("dialog.api.model.create"), this::submit),
                StatusBarShortcut.cancel(this::closeCancel),
                StatusBarShortcut.paste(),
                StatusBarShortcut.navigate());

        nameInput.onTextChanged(this::showFileNames);
    }

    // Rule-CODEGEN-099
    private static @NotNull String className(final @NotNull String typed) {
        final @NotNull String name = typed.trim();
        return name.isEmpty() ? "" : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    // Rule-CODEGEN-106, Rule-INTERNAL-138
    @Override
    protected @NotNull List<JComponent> titleButtons() {
        return List.of(pair.layoutButton());
    }

    // Rule-CODEGEN-106
    private void showFileNames() {
        final @NotNull String name = className(nameInput.getText());
        requestTitle.setText(titleOf(Bundle.message("dialog.api.model.request.caption"), name, REQUEST));
        responseTitle.setText(titleOf(Bundle.message("dialog.api.model.response.caption"), name, RESPONSE));
    }

    private static @NotNull String titleOf(final @NotNull String caption, final @NotNull String name, final @NotNull String suffix) {
        return name.isEmpty() ? caption : caption + "   " + name + suffix + ".java";
    }

    // UC-CODEGEN-022, Rule-CODEGEN-099, Rule-CODEGEN-100, Rule-CODEGEN-105, Rule-CODEGEN-108
    @Override
    protected void submit() {
        final @NotNull String name = className(accepted(nameInput));
        if (name.isEmpty()) return;

        final @NotNull String requestBody = request.getText();
        final @NotNull String responseBody = response.getText();
        if (requestBody.isBlank() && responseBody.isBlank()) {
            notifier.softRefuse(project, Refused.NOTHING_PASTED, "");
            return;
        }

        final @NotNull Map<String, String> files = new LinkedHashMap<>();
        if (refused(files, name + REQUEST, requestBody, Bundle.message("dialog.api.model.request"), JavaModelText::requestClass))
            return;
        if (refused(files, name + RESPONSE, responseBody, Bundle.message("dialog.api.model.response"), JavaModelText::responseRecord))
            return;

        final @NotNull List<PsiFile> written = write(files);
        if (written.isEmpty()) return;

        closeOk();
        written.forEach(file -> {
            TestinLogger.debug("API model written as " + file.getName() + ":\n" + file.getText());
            file.navigate(true);
        });
        TestinLogger.info("API model written: " + String.join(", ", files.keySet()));
        notifier.softShow(project, Done.CREATED);
    }

    // Rule-CODEGEN-099, Rule-CODEGEN-105, Rule-CODEGEN-108
    private boolean refused(final @NotNull Map<String, String> files, final @NotNull String typeName, final @NotNull String body, final @NotNull String kind, final @NotNull BiFunction<String, ModelType, String> text) {
        if (body.isBlank()) return false;

        TestinLogger.debug("API model, " + kind + " as pasted:\n" + body);
        final @NotNull Optional<JsonNode> json = JsonModel.read(body);
        final @NotNull Optional<Refused> refusal = json.map(JsonModel::refusalOf).orElse(Optional.of(Refused.NOT_JSON));
        if (refusal.isPresent()) {
            refuse(refusal.orElseThrow(), kind);
            return true;
        }

        final @NotNull String fileName = typeName + ".java";
        if (Optional.ofNullable(directory.findFile(fileName)).isPresent()) {
            refuse(Refused.ALREADY_EXISTS, fileName);
            return true;
        }

        files.put(fileName, text.apply(packageName, JsonModel.of(typeName, json.orElseThrow())));
        return false;
    }

    // Rule-CODEGEN-099, Rule-CODEGEN-108
    private void refuse(final @NotNull Refused refusal, final @NotNull String about) {
        TestinLogger.debug("API model refused: " + refusal.about(about));
        notifier.softRefuse(project, refusal, about);
    }

    // Rule-CODEGEN-006, Rule-CODEGEN-105
    private @NotNull List<PsiFile> write(final @NotNull Map<String, String> files) {
        final @NotNull List<PsiFile> written = new ArrayList<>();
        try {
            WriteCommandAction.writeCommandAction(project).withName(title).run(() -> files.forEach((fileName, text) -> {
                final @NotNull PsiFile created = PsiFileFactory.getInstance(project).createFileFromText(fileName, JavaFileType.INSTANCE, text);
                if (directory.add(created) instanceof PsiFile added) {
                    written.add((PsiFile) CodeStyleManager.getInstance(project).reformat(added));
                }
            }));
        } catch (final IncorrectOperationException ex) {
            TestinLogger.error("The API model could not be written into " + directory.getVirtualFile().getPath() + ": " + FailureText.of(ex));
            return List.of();
        }
        return written;
    }
}
