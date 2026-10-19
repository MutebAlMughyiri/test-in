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

package org.testin;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.jetbrains.annotations.NotNull;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.core.domain.AccessTarget.Predicates.declaredIn;
import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureTest {

    private static final @NotNull JavaClasses CLASSES = productionClasses();

    private static final String @NotNull [] ABOVE_MODEL = {
            "org.testin.indexer..", "org.testin.editor..", "org.testin.view..", "org.testin.codegen..",
            "org.testin.services..", "org.testin.creator..", "org.testin.explorer..",
            "org.testin.ui..", "org.testin.git..", "org.testin.report..",
            "org.testin.importexport..", "org.testin.testcase..", "org.testin.testrun..", "org.testin.testproject..",
            "org.testin.search..", "org.testin.undo..", "org.testin.rename..", "org.testin.remove..",
            "org.testin.open..", "org.testin.clipboard..", "org.testin.runner..", "org.testin.notifications..",
            "org.testin.setting..", "org.testin.config..", "org.testin.actions..", "org.testin.bug..",
            "org.testin.help.."
    };

    private static final @NotNull Set<String> MODEL_LEAF_EXCEPTIONS = Set.of();

    private static final String @NotNull [] FEATURES = {
            "org.testin.indexer..", "org.testin.editor..", "org.testin.view..", "org.testin.codegen..",
            "org.testin.creator..", "org.testin.explorer..", "org.testin.ui..",
            "org.testin.git..", "org.testin.report..", "org.testin.importexport..",
            "org.testin.testcase..", "org.testin.testrun..", "org.testin.testproject..", "org.testin.search..",
            "org.testin.undo..", "org.testin.rename..", "org.testin.remove..", "org.testin.open..",
            "org.testin.clipboard..", "org.testin.runner..", "org.testin.bug..", "org.testin.help.."
    };

    private static final @NotNull Set<String> UTIL_EXCEPTIONS = Set.of();

    private static final @NotNull Set<String> FILE_ACCESS_EXCEPTIONS = Set.of("org.testin.ui.framework.TextInput");

    private static final @NotNull Map<String, String> ONLY_THE_INDEXER_CALLS = Map.of(
            "org.testin.model.TestCase", "takeValuesOf",
            "org.testin.model.testrun.RunItem", "recordClock");

    private static final @NotNull Set<String> FILE_APIS = Set.of(
            "java.nio.file.Files", "java.nio.channels.FileChannel", "java.io.RandomAccessFile",
            "java.io.FileInputStream", "java.io.FileOutputStream", "java.io.FileReader", "java.io.FileWriter",
            "com.intellij.openapi.vfs.LocalFileSystem", "com.intellij.openapi.vfs.VfsUtil", "com.intellij.openapi.vfs.VfsUtilCore",
            "com.intellij.openapi.util.io.FileUtil", "com.intellij.openapi.util.io.FileUtilRt");

    private static final @NotNull String VIRTUAL_FILE_OPERATIONS = "delete|move|copy|rename|createChildData|createChildDirectory"
            + "|setBinaryContent|getOutputStream|getInputStream|contentsToByteArray";

    private static final @NotNull String FILE_ON_DISK = "exists|isFile|isDirectory|list|listFiles|length|lastModified|canRead|canWrite"
            + "|delete|deleteOnExit|mkdir|mkdirs|renameTo|createNewFile|setLastModified|setReadable|setWritable";

    private static final @NotNull Set<String> REFRESH_EXCEPTIONS = Set.of("org.testin.testproject.SaveTestinYml");

    private static @NotNull JavaClasses productionClasses() {
        final @NotNull List<Path> compiled = List.of(
                Path.of("build", "classes", "java", "main").toAbsolutePath(),
                Path.of("testin-java", "build", "classes", "java", "main").toAbsolutePath(),
                Path.of("testin-testng", "build", "classes", "java", "main").toAbsolutePath(),
                Path.of("testin-apimodel", "build", "classes", "java", "main").toAbsolutePath());

        for (final Path module : compiled) {
            if (!Files.isDirectory(module)) {
                throw new AssertionError("No compiled classes at " + module + " - the architecture rules have"
                        + " nothing to read. Run this through Gradle, which compiles every module first.");
            }
        }

        return new ClassFileImporter().importPaths(compiled);
    }

    private static @NotNull DescribedPredicate<JavaClass> notOneOf(final @NotNull Set<String> frozen) {
        return new DescribedPredicate<>("not one of the " + frozen.size() + " frozen violations") {
            @Override
            public boolean test(final @NotNull JavaClass javaClass) {
                final @NotNull String outermost = javaClass.getName().split("[$]", -1)[0];

                return !frozen.contains(outermost);
            }
        };
    }

    @Test
    public void modelImportsNothingAboveIt() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideInAPackage("org.testin.model..")
                .and(notOneOf(MODEL_LEAF_EXCEPTIONS))
                .should().dependOnClassesThat().resideInAnyPackage(ABOVE_MODEL)
                .because("model is a leaf: the DTOs, markers and enums are what every other package reads,"
                        + " so a model class reaching back into a feature makes the two impossible to move apart (#111)."
                        + " If this is a class the leaf rule should not apply to, say so on #111 rather than here");

        rule.check(CLASSES);
    }

    // Rule-PRODUCT-020, Rule-CODEGEN-061
    @Test
    public void theCoreWorksWithoutJava() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideInAPackage("org.testin..")
                .and().resideOutsideOfPackages("org.testin.java..", "org.testin.testng..", "org.testin.apimodel..")
                .should().dependOnClassesThat().resideInAnyPackage("com.intellij.psi.impl.source..", "com.intellij.java..", "com.intellij.codeInsight.daemon.impl.analysis..")
                .orShould().dependOnClassesThat().haveSimpleNameStartingWith("PsiJava")
                .orShould().dependOnClassesThat().haveSimpleNameStartingWith("JavaPsi")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("com.intellij.psi.PsiClass")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("com.intellij.psi.PsiMethod")
                .because("Java lives in the optional testin-java, testin-testng and testin-apimodel modules, so an IDE without Java still runs"
                        + " everything but the automation code");

        rule.check(CLASSES);
    }

    @Test
    public void theChangeModelReachesNoOtherPartOfGit() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideInAPackage("org.testin.git.change..")
                .should().dependOnClassesThat().resideInAnyPackage("org.testin.git.history..", "org.testin.git.conflict..", "org.testin.git.review..")
                .because("what changed between two versions is the vocabulary the review and the history share, so it sits"
                        + " below them and never reaches back (#394)");

        rule.check(CLASSES);
    }

    @Test
    public void utilImportsNoFeaturePackage() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideInAPackage("org.testin.util..")
                .and(notOneOf(UTIL_EXCEPTIONS))
                .should().dependOnClassesThat().resideInAnyPackage(FEATURES)
                .because("util is what everything else may call, so a util class that calls a feature back"
                        + " is a cycle waiting for its second edge (#112)");

        rule.check(CLASSES);
    }

    @Test
    public void onlyTheIndexerAndItsExemptListTouchFiles() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideOutsideOfPackages("org.testin.indexer..", "org.testin.codegen..",
                        "org.testin.config..", "org.testin.git..", "org.testin.importexport..",
                        "org.testin.report..", "org.testin.setting..", "org.testin.logger..",
                        "org.testin.bug..", "org.testin.java.codegen..")
                .and(notOneOf(FILE_ACCESS_EXCEPTIONS))
                .should().dependOnClassesThat(DescribedPredicate.describe("a file API", javaClass -> FILE_APIS.contains(javaClass.getName())))
                .orShould().callMethodWhere(target(declaredIn(assignableTo("com.intellij.openapi.vfs.VirtualFile")).and(nameMatching(VIRTUAL_FILE_OPERATIONS))))
                .orShould().callMethodWhere(target(declaredIn("java.io.File").and(nameMatching(FILE_ON_DISK))))
                .orShould().callMethodWhere(DescribedPredicate.describe("a method handed a java.io.File", call -> call.getTarget().getRawParameterTypes().stream().anyMatch(type -> type.getName().equals("java.io.File"))))
                .because("the indexer is the single owner of file access, so its cache stays authoritative over"
                        + " test data and every read is a fast in-memory lookup. The exempt packages are exempt"
                        + " because none of them touches test data (CLAUDE.md, #49); codegen includes the Java"
                        + " module's org.testin.java.codegen. TextInput is the one exception: it asks the VFS for the"
                        + " folder a file chooser opens at, which is never test data");

        rule.check(CLASSES);
    }

    // Rule-INTERNAL-089
    @Test
    public void onlyTestinYmlReadsTheConfigFile() {
        final @NotNull ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName("org.testin.config.TestinYml")
                .should().dependOnClassesThat().resideInAPackage("com.fasterxml.jackson.dataformat.yaml..")
                .because("testin.yml is read by one class, so a missing value means the same thing everywhere,"
                        + " and nothing else can open, parse or write the file (Rule-INTERNAL-089)");

        rule.check(CLASSES);
    }

    @Test
    public void onlyOneActionAsksTheEventForItsProject() {
        final @NotNull ArchRule rule = noClasses()
                .that().doNotHaveFullyQualifiedName("org.testin.actions.AbstractAnyProjectAction")
                .should().callMethod("com.intellij.openapi.actionSystem.AnActionEvent", "getProject")
                .because("the event's project is the one null the platform hands an action; AbstractAnyProjectAction"
                        + " checks it once and gives perform and update a project that is there (#360)");

        rule.check(CLASSES);
    }

    @Test
    public void onlyTheSurfacesRefreshThemselves() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideOutsideOfPackages("org.testin.explorer..", "org.testin.editor..")
                .and(notOneOf(REFRESH_EXCEPTIONS))
                .should().callMethod("org.testin.explorer.tree.TestProjectTree", "refresh")
                .orShould().callMethod("org.testin.editor.open.TestinEditors", "refreshOpen")
                .because("the indexer announces every change on IndexChanged and the surfaces redraw themselves from it;"
                        + " a caller that refreshes as well is how a rename redrew the tree twice (Rule-INTERNAL-114, #361)."
                        + " SaveTestinYml is the one exception: testin.yml turns code on or off, which the editors draw,"
                        + " and that is not a change to the index");

        rule.check(CLASSES);
    }

    @Test
    public void theTreeNeverWaitsForTheIndex() {
        final @NotNull ArchRule rule = noClasses()
                .that().resideInAPackage("org.testin.explorer.tree..")
                .should().callMethod("org.testin.indexer.TestProjectIndexer", "awaitIndexing")
                .because("the platform asks the tree for its nodes inside a read action, and a read action that"
                        + " waits holds up every write in the IDE, the one that finishes indexing included"
                        + " (Rule-INTERNAL-111)");

        rule.check(CLASSES);
    }

    // Rule-INTERNAL-089
    @Test
    public void onlyAnExplicitGestureWritesTheConfigFile() {
        final @NotNull ArchRule oneCaller = methods()
                .that().areDeclaredIn("org.testin.config.TestinYml")
                .and().haveName("save")
                .should().onlyBeCalled().byClassesThat().haveNameMatching("org\\.testin\\.(testproject\\.SaveTestinYml|config\\.TestinYml)")
                .because("only a tester's explicit gesture writes the file - Save to testin.yml, or Apply in the"
                        + " bugRepoUrl form - so cloning, picking a test project, creating one and renaming one leave a"
                        + " committed file alone (Decision-013). A second caller is a second gesture writing it");

        final @NotNull ArchRule oneBugRepoUrlCaller = methods()
                .that().areDeclaredIn("org.testin.config.TestinYml")
                .and().haveName("saveBugRepoUrl")
                .should().onlyBeCalled().byClassesThat().haveFullyQualifiedName("org.testin.bug.BugRepoUrlForm")
                .because("Apply in the bugRepoUrl form is the second gesture that writes testin.yml (Decision-013)");

        final @NotNull ArchRule oneWriter = noClasses()
                .that().doNotHaveFullyQualifiedName("org.testin.config.TestinYml")
                .and().doNotHaveFullyQualifiedName("org.testin.codegen.JavaSourceRoot")
                .and().doNotHaveFullyQualifiedName("org.testin.java.codegen.GeneratedSourceFiles")
                .should().callMethodWhere(target(name("createChildData")))
                .because("TestinYml holds the only write of testin.yml; generated code is written by the Java module's"
                        + " GeneratedSourceFiles, and put back after an undone removal by JavaSourceRoot; everything"
                        + " else asks the indexer, whose cache is authoritative (CLAUDE.md, Decision-013)");

        oneCaller.check(CLASSES);
        oneBugRepoUrlCaller.check(CLASSES);
        oneWriter.check(CLASSES);
    }

    // Rule-INTERNAL-117
    @Test
    public void onlyTheModelAndTheIndexerChangeAModelValue() {
        final @NotNull ArchRule noSetter = noClasses()
                .that().resideOutsideOfPackages("org.testin.model..", "org.testin.indexer..")
                .should().accessTargetWhere(JavaAccess.Predicates.target(declaredIn(resideInAPackage("org.testin.model..")).and(nameMatching("set[A-Z].*"))))
                .because("the index holds one instance of every test case, test run and marker, read by the EDT and by pooled"
                        + " threads alike; a feature changes one by asking the indexer, which writes the new values into"
                        + " the instance it holds, never by calling a setter on it (Rule-INTERNAL-117, #376)");

        noSetter.check(CLASSES);
        ONLY_THE_INDEXER_CALLS.forEach((owner, mutators) -> methods()
                .that().areDeclaredIn(owner)
                .and().haveNameMatching(mutators)
                .should().onlyBeCalled().byClassesThat().resideInAPackage("org.testin.indexer..")
                .because("it changes a value in the instance the index holds - an edited copy landing once its file is"
                        + " written, a running clock - and only the index knows when that may happen; a feature asks"
                        + " the indexer instead (Rule-INTERNAL-117, #451)")
                .check(CLASSES));
    }

    @Test
    public void theRuleThisFileDoesNotEnforce() {
        final @NotNull List<String> deliberate = CLASSES.stream()
                .filter(javaClass -> javaClass.getPackageName().equals("org.testin.model"))
                .filter(javaClass -> javaClass.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> dependency.getTargetClass().getPackageName().startsWith("javax.swing")
                                || dependency.getTargetClass().getPackageName().startsWith("java.awt")))
                .map(JavaClass::getSimpleName)
                .sorted()
                .toList();

        Assert.assertFalse(deliberate.isEmpty(),
                "If model no longer carries any Swing or AWT type, the convention has changed and the fourth"
                        + " rule is now worth writing. Read this method's javadoc before deleting it.");
    }
}
