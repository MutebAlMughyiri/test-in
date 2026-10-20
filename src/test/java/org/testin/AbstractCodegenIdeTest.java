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

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.UnknownSdkType;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.testin.codegen.CodeChangeType;
import org.testin.codegen.NodeCode;
import org.testin.config.TestinYml;
import org.testin.indexer.NodeMapper;
import org.testin.indexer.Nodes;
import org.testin.indexer.TestCases;
import org.testin.model.NodeType;
import org.testin.model.TestCase;
import org.testin.model.node.AbstractNode;
import org.testin.model.node.TestProjectNode;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractCodegenIdeTest extends AbstractTempRootIdeTest {

    private static final @NotNull String TEST_PROJECT = "NAFATH";

    private static final @NotNull String TESTNG_TEST = "org.testng.annotations.Test";

    private static final @NotNull LightProjectDescriptor JAVA_TEST_SOURCE_ROOT = new LightProjectDescriptor() {
        @Override
        protected @NotNull JpsModuleSourceRootType<?> getSourceRootType() {
            return JavaSourceRootType.TEST_SOURCE;
        }

        @Override
        protected void configureModule(final @NotNull Module module, final @NotNull ModifiableRootModel model, final @NotNull ContentEntry contentEntry) {
            final @NotNull Path testng = Path.of(PathUtil.getJarPathForClass(Test.class));
            PsiTestUtil.addLibrary(model, "TestNG", String.valueOf(testng.getParent()), String.valueOf(testng.getFileName()));
        }
    };

    private TestProjectNode testProject;

    protected static @NotNull String attributeOf(final @NotNull PsiMethod testMethod, final @NotNull String attribute) {
        return Optional.ofNullable(testMethod.getModifierList().findAnnotation(TESTNG_TEST))
                .map(annotation -> annotation.findDeclaredAttributeValue(attribute))
                .map(PsiElement::getText)
                .orElse("");
    }

    @Override
    protected @NotNull LightProjectDescriptor getProjectDescriptor() {
        return JAVA_TEST_SOURCE_ROOT;
    }

    @Override
    protected void setUp() {
        super.setUp();
        final @NotNull Path folder = TestinYml.savePath(getProject()).map(Path::getParent).orElseThrow(() -> new AssertionError("the project has no folder for " + TestinYml.fileName()));
        try {
            Files.createDirectories(folder);
        } catch (final IOException ex) {
            throw new AssertionError("could not make the project's folder " + folder + ": " + ex.getMessage(), ex);
        }
        assertTrue("could not write " + TestinYml.fileName(), TestinYml.save(getProject(), TestinYml.lines(TEST_PROJECT)));

        testProject = WriteAction.computeAndWait(() -> {
            final @NotNull TestProjectNode testProject = Services.getInstance(getProject(), NodeMapper.class).setTestProjectNode(root.resolve(TEST_PROJECT));
            nodes().addTestProject(testProject);
            return testProject;
        });
    }

    private @NotNull Nodes nodes() {
        return Services.getInstance(getProject(), Nodes.class);
    }

    private @NotNull NodeMapper mapper() {
        return Services.getInstance(getProject(), NodeMapper.class);
    }

    protected @NotNull AbstractNode theTestCasesDirectory() {
        return testProject.getTestCasesFolder();
    }

    protected @NotNull TestSetPackageNode indexedPackage(final @NotNull String name, final @NotNull AbstractNode parent) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetPackageNode created = mapper().getTestSetPackageNode(parent.getPath().resolve(name), parent);
            nodes().addTestSetPackage(created);
            return created;
        });
    }

    protected @NotNull TestSetNode indexedTestSet(final @NotNull String name, final @NotNull AbstractNode parent) {
        return WriteAction.computeAndWait(() -> {
            final @NotNull TestSetNode created = mapper().getTestSetNode(parent.getPath().resolve(name), parent);
            nodes().addTestSet(created);
            return created;
        });
    }

    protected @NotNull TestSetNode createdTestSet(final @NotNull String name) {
        return createdTestSet(name, theTestCasesDirectory());
    }

    protected @NotNull TestSetNode createdTestSet(final @NotNull String name, final @NotNull AbstractNode parent) {
        final @NotNull TestSetNode testSet = indexedTestSet(name, parent);

        NodeCode.of(NodeType.TEST_SET).getCreated().execute(getProject(), testSet);
        return testSet;
    }

    protected @NotNull TestCase indexedTestCase(final @NotNull TestSetNode testSet, final @NotNull String description, final @NotNull String order) {
        final @NotNull TestCase testCase = TestCase.builder().uuid(UUID.randomUUID()).description(description).order(order).build();
        testCase.setParent(testSet);
        Services.getInstance(getProject(), TestCases.class).putTestCaseVerbatim(testSet.getPath(), testCase);
        return testCase;
    }

    protected @NotNull TestCase createdTestCase(final @NotNull TestSetNode testSet, final @NotNull String description, final @NotNull String order) {
        final @NotNull TestCase testCase = indexedTestCase(testSet, description, order);
        whenIndexed();
        CodeChangeType.CREATE_TEST_CASE.execute(getProject(), testCase);
        return testCase;
    }

    protected void whenIndexed() {
        Await.until("the IDE never finished indexing", () -> !DumbService.isDumb(getProject()));
    }

    protected @NotNull Optional<PsiClass> generatedClass(final @NotNull String qualifiedName) {
        whenIndexed();
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        return Optional.ofNullable(JavaPsiFacade.getInstance(getProject()).findClass(qualifiedName, GlobalSearchScope.projectScope(getProject())));
    }

    protected @NotNull Optional<PsiMethod> methodOf(final @NotNull String qualifiedName, final @NotNull TestCase testCase) {
        return generatedClass(qualifiedName).stream()
                .flatMap(testClass -> Arrays.stream(testClass.getMethods()))
                .filter(testMethod -> attributeOf(testMethod, "testName").equals("\"" + testCase.getUuid() + "\""))
                .findFirst();
    }

    protected @NotNull PsiMethod writtenMethodOf(final @NotNull String qualifiedName, final @NotNull TestCase testCase) {
        Await.until("'" + testCase.getDescription() + "' has no method in " + qualifiedName, () -> methodOf(qualifiedName, testCase).isPresent());
        return methodOf(qualifiedName, testCase).orElseThrow();
    }

    protected void settled() {
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
    }

    protected void interpreterIs(final @NotNull String sdkType) {
        final @NotNull ProjectRootManager roots = ProjectRootManager.getInstance(getProject());
        final @NotNull Optional<Sdk> before = Optional.ofNullable(roots.getProjectSdk());
        final @NotNull Sdk interpreter = ProjectJdkTable.getInstance().createSdk(sdkType + " interpreter", UnknownSdkType.getInstance(sdkType));

        WriteAction.runAndWait(() -> {
            ProjectJdkTable.getInstance().addJdk(interpreter, getTestRootDisposable());
            roots.setProjectSdk(interpreter);
        });
        Disposer.register(getTestRootDisposable(), () -> WriteAction.runAndWait(() -> roots.setProjectSdk(before.orElse(null))));
    }

    protected void writtenByTheTester(final @NotNull PsiMethod testMethod, final @NotNull String statements) {
        final @NotNull PsiCodeBlock body = Optional.ofNullable(testMethod.getBody()).orElseThrow(() -> new AssertionError(testMethod.getName() + " has no body to write in"));
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            body.replace(JavaPsiFacade.getElementFactory(getProject()).createCodeBlockFromText("{ " + statements + " }", body));
        });
    }
}
