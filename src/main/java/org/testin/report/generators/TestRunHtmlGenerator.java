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

package org.testin.report.generators;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.model.ReportColor;
import org.testin.model.bug.BugIssueUrl;
import org.testin.model.markers.DetailRow;
import org.testin.model.node.TestRunNode;
import org.testin.model.status.RunItemStatus;
import org.testin.model.status.TestRunStatus;
import org.testin.model.testrun.RunItem;
import org.testin.model.testrun.RunItems;
import org.testin.model.testrun.TestRunResultAnalysis;
import org.testin.model.testrun.TestRunSummary;
import org.testin.report.ReportTile;
import org.testin.services.Services;
import org.testin.testproject.BoundTestProject;
import org.testin.util.Bundle;
import org.testin.util.ReportFont;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class TestRunHtmlGenerator {

    // Rule-REPORT-025
    private static @NotNull String runItemStatusTokens(final @NotNull Function<RunItemStatus, String> shade) {
        final @NotNull StringBuilder tokens = new StringBuilder();

        for (final RunItemStatus runItemStatus : List.of(RunItemStatus.PASSED, RunItemStatus.FAILED, RunItemStatus.BLOCKED, RunItemStatus.UNTESTED, RunItemStatus.REMOVED)) {
            tokens.append(runItemStatus.getCssVariable()).append(": #").append(shade.apply(runItemStatus)).append(";");
        }

        return tokens.toString();
    }

    // UC-REPORT-001, Rule-REPORT-002, Rule-REPORT-005
    public @NotNull String generate(final @NotNull Project project, final @NotNull TestRunNode testRunNode, final @NotNull RunItems runItems) {
        final @NotNull List<RunItem> all = runItems.getAll();
        final @NotNull TestRunSummary summary = TestRunSummary.of(all);
        final @NotNull TestRunStatus testRunStatus = testRunNode.getMarker().getStatus();

        final @NotNull String testRunName = testRunNode.getName();

        final @NotNull String testProjectName = Services.getInstance(project, BoundTestProject.class).name();

        final @NotNull StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html><html lang='en'><head><meta charset='UTF-8'>")
                .append("<meta name='viewport' content='width=device-width, initial-scale=1'>")
                .append(styles())
                .append("</head><body>");

        html.append("<button class='theme-toggle' type='button' onclick='testinToggleTheme()'")
                .append(" data-light='").append(StringUtil.escapeXmlEntities(Bundle.message("report.theme.light"))).append("'")
                .append(" data-dark='").append(StringUtil.escapeXmlEntities(Bundle.message("report.theme.dark"))).append("'>")
                .append(Bundle.message("report.theme.light")).append("</button>");

        html.append("<div class='report-title'>").append(Bundle.message("report.title")).append("</div>")
                .append("<div class='report-subtitle'>")
                .append(StringUtil.escapeXmlEntities(ReportText.subtitle(testProjectName, testRunNode.getMarker()))).append("</div>")
                .append("<div class='report-test-run-name'>").append(StringUtil.escapeXmlEntities(testRunName)).append("</div>")
                .append("<div class='report-conf'>").append(Bundle.message("report.confidential")).append("</div>");

        sectionTitle(html, Bundle.message("report.heading.overview"));
        html.append("<table class='overview-table'>");
        for (final DetailRow row : ReportOverview.rowsFor(testProjectName, testRunNode, summary)) {
            overviewRow(html, row.caption(), row.value());
        }
        html.append("</table>");

        sectionTitle(html, Bundle.message("report.heading.execution"));

        html.append("<div class='summary-text'>")
                .append(ReportText.summary(testRunName, summary, value -> "<b>" + StringUtil.escapeXmlEntities(value) + "</b>"))
                .append("</div>");

        html.append("<div class='summary-cards'>");
        for (final ReportTile tile : ReportTile.shownFor(summary)) {
            summaryCard(html, tile.valueIn(summary), tile.labelIn(testRunStatus), tile.getCssToken());
        }
        html.append("</div>");

        final @NotNull Map<TestRunResultAnalysis, String> analysis = TestRunResultAnalysis.shown(testRunNode.getMarker().getResultAnalysis());
        final boolean analyzed = !analysis.isEmpty();

        if (analyzed) {
            sectionTitle(html, Bundle.message("report.heading.analysis"));

            for (final Map.Entry<TestRunResultAnalysis, String> written : analysis.entrySet()) {
                final @NotNull TestRunResultAnalysis section = written.getKey();

                html.append("<div class='analysis-heading' style='color: ")
                        .append(section.getRunItemStatus().getCssToken()).append("'>")
                        .append(section.heading(summary, testRunStatus)).append("</div>")
                        .append("<div class='analysis-text'>")
                        .append(StringUtil.escapeXmlEntities(written.getValue()))
                        .append("</div>");
            }
        }

        for (final NumberedSection numbered : ReportText.numberedSections(summary, analyzed)) {
            final @NotNull ReportSection section = numbered.section();
            appendTestCaseTable(html, numbered.number(), section, section.titleIn(testRunStatus), section.description("<b>" + numbered.count() + "</b>"), all);
        }

        html.append("<div class='footer'>")
                .append(ReportText.footerLead())
                .append("<a href='").append(ReportText.PLUGIN_URL).append("' target='_blank'>").append(ReportText.PLUGIN_NAME).append("</a>")
                .append(Bundle.message("report.footer.suffix"))
                .append("</div>");

        html.append(themeScript());
        html.append("</body></html>");
        return html.toString();
    }

    // Rule-REPORT-019
    private void appendTestCaseTable(final @NotNull StringBuilder html, final int sectionNumber, final @NotNull ReportSection section, final @NotNull String title, final @NotNull String blurb, final @NotNull List<RunItem> runItems) {
        final @NotNull String name = section.name().toLowerCase(Locale.ROOT);
        sectionTitle(html, sectionNumber + ". " + title);
        html.append("<div class='summary-text'>").append(blurb).append("</div>");
        html.append("<table class='detail-table'>")
                .append("<tr style='background: var(--section-").append(name).append(")")
                .append("; color: var(--section-").append(name).append("-ink)'>")
                .append("<th class='seq'>#</th><th>").append(Bundle.message("caption.test.case")).append("</th>");

        for (final FailureColumn column : section.getFailureDetailColumns()) {
            html.append("<th class='run-item-status'>").append(column.getField().getName()).append("</th>");
        }

        html.append("</tr>");

        final @NotNull AtomicInteger sequence = new AtomicInteger(1);
        runItems.stream()
                .filter(section::matches)
                .forEach(runItem -> {
                    ProgressManager.checkCanceled();
                    final @NotNull String description = runItem.shownTestCase().getDescription();

                    html.append("<tr>")
                            .append("<td class='seq'>").append(sequence.getAndIncrement()).append("</td>")
                            .append("<td>").append(StringUtil.escapeXmlEntities(ReportText.orEmpty(description)));

                    // Rule-REPORT-027
                    ReportText.otherExecutions(runItem).ifPresent(others -> html.append("<div class='actual'>").append(StringUtil.escapeXmlEntities(others)).append("</div>"));

                    if (section.isWithFailureDetail()) appendFailureDetail(html, section, runItem);
                    else html.append("</td>");

                    html.append("</tr>");
                });
        html.append("</table>");
    }

    // Rule-REPORT-017
    private void appendFailureDetail(final @NotNull StringBuilder html, final @NotNull ReportSection section, final @NotNull RunItem runItem) {
        final @NotNull String actual = runItem.shownExecution().getActualResult();
        html.append("<div class='actual'>")
                .append(Bundle.message("report.actual.result", StringUtil.escapeXmlEntities(ReportText.orEmpty(actual))));

        runItem.shownExecution().bugIssue().ifPresent(url -> html.append(" (<a href='").append(StringUtil.escapeXmlEntities(url)).append("' target='_blank'>")
                .append(StringUtil.escapeXmlEntities(BugIssueUrl.shortReference(url)))
                .append("</a>)"));
        html.append("</div>");

        final @NotNull String stacktrace = runItem.shownExecution().getStacktrace();
        if (!stacktrace.isBlank()) {
            html.append("<div class='stacktrace'>").append(StringUtil.escapeXmlEntities(stacktrace)).append("</div>");
        }

        html.append("</td>");
        for (final FailureColumn column : section.getFailureDetailColumns()) {
            html.append("<td class='run-item-status' style='color: ")
                    .append(column.emphasisOf(runItem).getCssToken()).append("'>")
                    .append(StringUtil.escapeXmlEntities(column.textOf(runItem))).append("</td>");
        }
    }

    // UC-REPORT-001, Rule-REPORT-023, Rule-REPORT-024
    private @NotNull String styles() {
        return "<style>"

                + "/* The skin on screen unless the reader asks for white. */"
                + ":root {" + darkTokens() + sectionTokens() + "}"
                + "/* The white skin: used only when the reader presses the button. */"
                + ":root[data-theme='light'] {" + lightTokens() + "}"

                + "* { margin: 0; padding: 0; box-sizing: border-box; }"
                + "body { font-family: " + ReportFont.CSS_FAMILY + "; color: var(--ink); background: var(--page); padding: 40px; }"

                + ".theme-toggle { position: fixed; top: 16px; right: 16px; font: inherit; font-size: " + ReportFont.SMALL.css() + "; "
                + "padding: 6px 12px; border-radius: 6px; cursor: pointer; "
                + "background: var(--panel); color: var(--ink); border: 1px solid var(--line); }"
                + ".theme-toggle:hover { border-color: var(--accent); }"

                + ".report-title { font-size: " + ReportFont.TITLE.css() + "; font-weight: bold; color: var(--heading); }"
                + ".report-subtitle { font-size: " + ReportFont.SUBTITLE.css() + "; color: var(--accent); margin-top: 4px; }"
                + ".report-test-run-name { font-size: " + ReportFont.LEAD.css() + "; color: var(--accent); margin-top: 2px; "
                + "padding-bottom: 6px; border-bottom: 2px solid var(--heading); }"
                + ".analysis-heading { font-size: " + ReportFont.LEAD.css() + "; font-weight: bold; margin-top: 10px; }"
                + ".analysis-text { font-size: " + ReportFont.BODY.css() + "; color: var(--ink); margin-bottom: 8px; white-space: pre-wrap; }"
                + ".report-conf { font-size: " + ReportFont.CAPTION.css() + "; color: var(--muted); font-style: italic; margin-top: 6px; margin-bottom: 20px; }"

                + ".section-title { font-size: " + ReportFont.SECTION.css() + "; font-weight: bold; color: var(--heading); margin-top: 28px; margin-bottom: 10px; }"
                + ".section-title-bar { border-bottom: 1px solid var(--heading); margin-bottom: 14px; }"

                + ".overview-table { border-collapse: collapse; width: 100%; max-width: 700px; }"
                + ".overview-table td { padding: 6px 12px; border: 1px solid var(--line); font-size: " + ReportFont.BODY.css() + "; }"
                + ".overview-table td.label { background: var(--panel); font-weight: bold; color: var(--heading); "
                + "width: 240px; white-space: nowrap; }"
                + ".overview-table td.value { color: var(--ink); white-space: pre-wrap; }"

                + ".summary-text { font-size: " + ReportFont.LEAD.css() + "; color: var(--ink); margin-bottom: 18px; line-height: 1.5; }"
                + ".summary-cards { display: flex; gap: 12px; flex-wrap: wrap; margin-bottom: 28px; }"
                + ".summary-card { flex: 1; min-width: 120px; text-align: center; background: var(--panel); border: 1px solid var(--line); border-radius: 6px; padding: 14px 8px; }"
                + ".summary-card .card-value { font-size: " + ReportFont.FIGURE.css() + "; font-weight: bold; }"
                + ".summary-card .card-label { font-size: " + ReportFont.SMALL.css() + "; color: var(--muted); margin-top: 4px; }"

                + ".detail-table { border-collapse: collapse; width: 100%; margin-top: 8px; }"
                + ".detail-table th { text-align: center; padding: 8px 12px; border: 1px solid var(--line); font-weight: bold; font-size: " + ReportFont.HEADING.css() + "; }"
                + ".detail-table td { padding: 8px 12px; border: 1px solid var(--line); font-size: " + ReportFont.BODY.css() + "; vertical-align: top; white-space: pre-wrap; }"
                + ".detail-table tr:nth-child(even) td { background: var(--panel); }"
                + ".detail-table tr:nth-child(odd) td { background: var(--page); }"
                + ".detail-table td.seq { text-align: center; color: var(--muted); }"
                + ".detail-table td.run-item-status, .detail-table th.run-item-status { width: 1%; white-space: nowrap; }"
                + ".detail-table td.run-item-status { text-align: center; font-weight: bold; }"
                + ".detail-table td.seq, .detail-table th.seq { width: 1%; white-space: nowrap; }"
                + ".actual { font-size: " + ReportFont.SMALL.css() + "; color: var(--muted); margin-top: 3px; white-space: pre-wrap; }"
                + ".stacktrace { font-family: " + ReportFont.CSS_MONO + "; font-size: " + ReportFont.SMALL.css() + "; color: var(--muted); margin-top: 6px; white-space: pre-wrap; word-break: break-word; }"

                + ".footer { margin-top: 30px; text-align: center; font-size: " + ReportFont.CAPTION.css() + "; color: var(--footer-ink); border-top: 1px solid var(--line); padding-top: 14px; }"
                + ".footer a { color: var(--link); text-decoration: none; }"

                + "/* Printing: the white skin again, whichever is on screen. */"
                + "@media print { :root, :root[data-theme='dark'] {" + lightTokens() + "}"
                + ".theme-toggle { display: none; } }"

                + "</style>";
    }

    private void overviewRow(final @NotNull StringBuilder html, final @NotNull String label, final @NotNull String value) {
        html.append("<tr>")
                .append("<td class='label'>").append(StringUtil.escapeXmlEntities(label)).append("</td>")
                .append("<td class='value'>").append(StringUtil.escapeXmlEntities(value)).append("</td>")
                .append("</tr>");
    }

    private @NotNull String sectionTokens() {
        final @NotNull StringBuilder tokens = new StringBuilder();

        for (final ReportSection section : ReportSection.values()) {
            final @NotNull String name = section.name().toLowerCase(Locale.ROOT);
            tokens.append("--section-").append(name).append(": #").append(section.getHexColor()).append(";")
                    .append("--section-").append(name).append("-ink: #").append(section.textHex()).append(";");
        }

        return tokens.toString();
    }

    private @NotNull String lightTokens() {
        return ReportColor.cssTokens(ReportColor::hex) + runItemStatusTokens(RunItemStatus::getReportHex);
    }

    private @NotNull String darkTokens() {
        return ReportColor.cssTokens(ReportColor::darkHex) + runItemStatusTokens(RunItemStatus::getReportDarkHex);
    }

    private @NotNull String themeScript() {
        return "<script>(function(){"
                + "var root=document.documentElement,btn=document.querySelector('.theme-toggle');"
                + "function shown(){return root.getAttribute('data-theme')||'dark';}"
                + "function label(){btn.textContent=shown()==='dark'?btn.dataset.light:btn.dataset.dark;}"
                + "try{var saved=localStorage.getItem('testin.report.theme');if(saved)root.setAttribute('data-theme',saved);}catch(e){}"
                + "label();"
                + "window.testinToggleTheme=function(){var next=shown()==='dark'?'light':'dark';"
                + "root.setAttribute('data-theme',next);"
                + "try{localStorage.setItem('testin.report.theme',next);}catch(e){}"
                + "label();};"
                + "})();</script>";
    }

    private void summaryCard(final @NotNull StringBuilder html, final @NotNull String value, final @NotNull String label, final @NotNull String color) {
        html.append("<div class='summary-card'>")
                .append("<div class='card-value' style='color: ").append(color).append(";'>")
                .append(StringUtil.escapeXmlEntities(value)).append("</div>")
                .append("<div class='card-label'>").append(StringUtil.escapeXmlEntities(label)).append("</div>")
                .append("</div>");
    }

    // UC-REPORT-001, Rule-REPORT-019
    private static void sectionTitle(final @NotNull StringBuilder html, final @NotNull String text) {
        html.append("<div class='section-title-bar'><div class='section-title'>").append(text).append("</div></div>");
    }
}
