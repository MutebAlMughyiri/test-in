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

package org.testin.importexport;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.testin.importexport.exports.ExportCsv;
import org.testin.importexport.exports.ExportExcel;
import org.testin.importexport.exports.ExportHtml;
import org.testin.importexport.exports.ExportJson;
import org.testin.importexport.imports.ImportCsv;
import org.testin.importexport.imports.ImportExcel;
import org.testin.importexport.imports.ImportJson;
import org.testin.model.TestCaseDto;
import org.testin.model.testrun.RunItems;
import org.testin.model.node.TestRunNode;
import org.testin.report.generators.TestRunExcelGenerator;
import org.testin.report.generators.TestRunHtmlGenerator;
import org.testin.report.generators.TestRunPdfGenerator;
import org.testin.report.generators.TestRunWordGenerator;
import org.testin.util.Bundle;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum FileFormat {
    XLS(
            "XLS",
            ".xls",
            _ -> "",
            ExportHandler.UNSUPPORTED,
            (p, importFile) -> new ImportExcel().processImport(p, importFile),
            ReportHandler.UNSUPPORTED
    ),

    XLSX(
            "Excel",
            ".xlsx",
            columns -> Bundle.message("import.hint.xlsx", columns),
            (_, destFile, sheets) -> new ExportExcel().exportToFile(destFile, sheets),
            (p, importFile) -> new ImportExcel().processImport(p, importFile),
            (p, testRunNode, runItems) -> new TestRunExcelGenerator().generate(p, testRunNode, runItems)
    ),

    JSON(
            "JSON",
            ".json",
            _ -> "",
            (p, destFile, sheets) -> new ExportJson().exportToFile(p, destFile, sheets),
            (p, importFile) -> new ImportJson().processImport(p, importFile),
            ReportHandler.UNSUPPORTED
    ),

    CSV(
            "CSV",
            ".csv",
            columns -> Bundle.message("import.hint.csv", columns),
            (_, destFile, sheets) -> new ExportCsv().exportToFile(destFile, sheets),
            (p, importFile) -> new ImportCsv().processImport(p, importFile),
            ReportHandler.UNSUPPORTED
    ),

    HTML(
            "HTML",
            ".html",
            _ -> "",
            (_, destFile, sheets) -> new ExportHtml().exportToFile(destFile, sheets),
            ImportHandler.UNSUPPORTED,
            (p, testRunNode, runItems) -> new TestRunHtmlGenerator().generate(p, testRunNode, runItems).getBytes(StandardCharsets.UTF_8)
    ),

    PDF(
            "PDF",
            ".pdf",
            _ -> "",
            ExportHandler.UNSUPPORTED,
            ImportHandler.UNSUPPORTED,
            (p, testRunNode, runItems) -> new TestRunPdfGenerator().generate(p, testRunNode, runItems)
    ),

    WORD(
            "Word",
            ".docx",
            _ -> "",
            ExportHandler.UNSUPPORTED,
            ImportHandler.UNSUPPORTED,
            (p, testRunNode, runItems) -> new TestRunWordGenerator().generate(p, testRunNode, runItems)
    );

    private final @NotNull String label;
    private final @NotNull String extension;

    private final @NotNull Function<String, String> hint;
    private final @NotNull ExportHandler exportHandler;
    private final @NotNull ImportHandler importHandler;
    private final @NotNull ReportHandler reportHandler;

    public static @NotNull Optional<FileFormat> importerFor(final @NotNull String fileName) {
        return Arrays.stream(values())
                .filter(type -> type.isImportable() && fileName.endsWith(type.getExtension()))
                .findFirst();
    }

    public static @NotNull String importableExtensions() {
        return Arrays.stream(values())
                .filter(FileFormat::isImportable)
                .map(FileFormat::getExtension)
                .collect(Collectors.joining(", "));
    }

    public static String @NotNull [] importableExtensionsForChooser() {
        return Arrays.stream(values())
                .filter(FileFormat::isImportable)
                .map(type -> type.getExtension().substring(1))
                .toArray(String[]::new);
    }

    public @NotNull String hintFor(final @NotNull String columns) {
        return hint.apply(columns);
    }

    public boolean isExportable() {
        return !exportHandler.equals(ExportHandler.UNSUPPORTED);
    }

    public boolean isImportable() {
        return !importHandler.equals(ImportHandler.UNSUPPORTED);
    }

    public boolean isReportable() {
        return !reportHandler.equals(ReportHandler.UNSUPPORTED);
    }

    public void exportToFile(final @NotNull Project p, final @NotNull File destFile, final @NotNull Map<String, List<TestCaseDto>> sheetsData) {
        if (!isExportable()) throw new IllegalStateException(label + " cannot be exported to");
        exportHandler.execute(p, destFile, sheetsData);
    }

    public @NotNull Map<String, List<TestCaseDto>> importToFile(final @NotNull Project p, final @NotNull File importFile) {
        if (!isImportable()) throw new IllegalStateException(label + " cannot be imported from");
        return importHandler.execute(p, importFile);
    }

    public byte @NotNull [] generateReport(final @NotNull Project p, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems) {
        if (!isReportable()) throw new IllegalStateException(label + " has no report generator");
        return reportHandler.execute(p, testRunNode, runItems);
    }
}
