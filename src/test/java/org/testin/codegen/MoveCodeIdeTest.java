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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.testin.AbstractCodegenIdeTest;
import org.testin.Said;
import org.testin.codegen.event.MovedNode;
import org.testin.codegen.event.MovedTestCase;
import org.testin.codegen.event.RenamedNode;
import org.testin.indexer.TestCases;
import org.testin.model.NodeType;
import org.testin.model.TestCase;
import org.testin.model.node.TestSetNode;
import org.testin.model.node.TestSetPackageNode;
import org.testin.services.Services;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MoveCodeIdeTest extends AbstractCodegenIdeTest {

    private static final @NotNull String WRITTEN_BY_THE_TESTER = "int signedIn = 1;";

    private void cutInto(final @NotNull TestCase testCase, final @NotNull TestSetNode from, final @NotNull TestSetNode into) {
        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);
        testCases.removeTestCase(from.getPath(), testCase.getUuid());
        testCase.setParent(into);
        testCases.putTestCaseVerbatim(into.getPath(), testCase);
        CodeChangeType.MOVE_TEST_CASE.execute(getProject(), new MovedTestCase(testCase, from));
        settled();
    }

    private void aFileWhereTheCardsFolderGoes() {
        WriteAction.runAndWait(() -> {
            try {
                TestSourceRoot.find(getProject()).map(root -> root.findChild("nafath")).orElseThrow(() -> new AssertionError("the test project has no package folder")).createChildData(this, "cards");
            } catch (final IOException ex) {
                throw new AssertionError("could not put a file where the cards folder goes: " + ex.getMessage(), ex);
            }
        });
    }

    private long methodsFor(final @NotNull String qualifiedName, final @NotNull TestCase testCase) {
        return generatedClass(qualifiedName).stream().flatMap(testClass -> Arrays.stream(testClass.getMethods())).filter(testMethod -> attributeOf(testMethod, "testName").equals("\"" + testCase.getUuid() + "\"")).count();
    }

    // Rule-CODEGEN-053
    public void testAMovedTestSetTakesItsClassAndItsPackageLineWithIt() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());

        NodeCode.of(NodeType.TEST_SET).getMoved().execute(getProject(), new MovedNode(login, checkout.getPath()));

        assertTrue("the class did not land in the package it was moved to, under that package's name", generatedClass("nafath.checkout.LoginTest").isPresent());
        assertTrue("the class was left behind in its old package", generatedClass("nafath.LoginTest").isEmpty());
    }

    // Rule-CODEGEN-053
    public void testATestSetMovedIntoAPackageWhoseFolderCannotBeMadeKeepsItsClass() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        aFileWhereTheCardsFolderGoes();
        final @NotNull TestSetPackageNode cards = indexedPackage("Cards", theTestCasesDirectory());

        NodeCode.of(NodeType.TEST_SET).getMoved().execute(getProject(), new MovedNode(login, cards.getPath()));

        assertTrue("the class left its package although no folder could take it", generatedClass("nafath.LoginTest").isPresent());
    }

    // Rule-CODEGEN-053
    public void testATestSetMovedIntoThePackageItIsAlreadyInKeepsItsClass() {
        final @NotNull TestSetNode login = createdTestSet("Login");

        NodeCode.of(NodeType.TEST_SET).getMoved().execute(getProject(), new MovedNode(login, theTestCasesDirectory().getPath()));

        assertTrue("the class left the package it was already in", generatedClass("nafath.LoginTest").isPresent());
    }

    // Rule-CODEGEN-055
    public void testAMoveIntoAPlaceTestinHasNotReadLeavesTheClassAndSaysSo() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        NodeCode.of(NodeType.TEST_SET).getMoved().execute(getProject(), new MovedNode(login, root.resolve("Never read")));

        assertTrue("the class moved into a place the tree does not know", generatedClass("nafath.LoginTest").isPresent());
        assertTrue("the tester was not told the code stayed behind, naming the class: " + said, said.stream().anyMatch(notification -> notification.getType() == NotificationType.WARNING && notification.getContent().contains("nafath.LoginTest")));
    }

    // Rule-CODEGEN-055
    public void testAPackageMovedIntoAPlaceTestinHasNotReadLeavesItsClassesAndSaysSo() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        createdTestSet("Login", checkout);
        final @NotNull List<Notification> said = Said.listening(getProject(), getTestRootDisposable()).notifications();

        NodeCode.of(NodeType.TEST_SET_PACKAGE).getMoved().execute(getProject(), new MovedNode(checkout, root.resolve("Never read")));

        assertTrue("the package's class moved into a place the tree does not know", generatedClass("nafath.checkout.LoginTest").isPresent());
        assertTrue("the tester was not told the package stayed behind, naming it: " + said, said.stream().anyMatch(notification -> notification.getType() == NotificationType.WARNING && notification.getContent().contains("nafath.checkout")));
    }

    // Rule-CODEGEN-056
    public void testRenamingAPackageRewritesThePackageLineOfEveryClassBeneathIt() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        createdTestSet("Login", checkout);
        createdTestSet("Visa", indexedPackage("Cards", checkout));

        NodeCode.of(NodeType.TEST_SET_PACKAGE).getRenamed().execute(getProject(), new RenamedNode(checkout, "Payment methods"));

        assertTrue("the class directly in the package does not carry its new name", generatedClass("nafath.paymentmethods.LoginTest").isPresent());
        assertTrue("a class deeper in the package does not carry its new name", generatedClass("nafath.paymentmethods.cards.VisaTest").isPresent());
        assertTrue("the old package still holds the class", generatedClass("nafath.checkout.LoginTest").isEmpty());
    }

    // Rule-CODEGEN-056
    public void testRenamingAPackageLeavesAFileBeneathItThatIsNotJavaAsItWas() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        createdTestSet("Login", checkout);
        final @NotNull VirtualFile notes = WriteAction.computeAndWait(() -> {
            try {
                final @NotNull VirtualFile created = TestSourceRoot.find(getProject()).map(root -> root.findFileByRelativePath("nafath/checkout")).orElseThrow(() -> new AssertionError("the package has no folder")).createChildData(this, "notes.txt");
                VfsUtil.saveText(created, "package nafath.checkout;");
                return created;
            } catch (final IOException ex) {
                throw new AssertionError("could not write a file that is not Java into the package: " + ex.getMessage(), ex);
            }
        });

        NodeCode.of(NodeType.TEST_SET_PACKAGE).getRenamed().execute(getProject(), new RenamedNode(checkout, "Payment methods"));

        assertEquals("the file that is not Java did not move with its package", generatedClass("nafath.paymentmethods.LoginTest").map(PsiElement::getContainingFile).map(PsiFile::getVirtualFile).map(VirtualFile::getParent), Optional.ofNullable(notes.getParent()));
        assertEquals("a file that is not Java was rewritten", Optional.of("package nafath.checkout;"), Optional.ofNullable(FileDocumentManager.getInstance().getDocument(notes)).map(Document::getText));
    }

    // Rule-CODEGEN-057
    public void testMovingAPackageMovesItsFolderAndRewritesThePackageLines() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        final @NotNull TestSetPackageNode cards = indexedPackage("Cards", checkout);
        createdTestSet("Visa", cards);
        final @NotNull TestSetPackageNode payment = indexedPackage("Payment", theTestCasesDirectory());

        NodeCode.of(NodeType.TEST_SET_PACKAGE).getMoved().execute(getProject(), new MovedNode(cards, payment.getPath()));

        assertTrue("the moved package's class is not in its new place under its new name", generatedClass("nafath.payment.cards.VisaTest").isPresent());
        assertTrue("the class was left in the package it was moved out of", generatedClass("nafath.checkout.cards.VisaTest").isEmpty());
    }

    // Rule-CODEGEN-053, Rule-CODEGEN-056, Rule-CODEGEN-057
    public void testMovingOrRenamingATestSetOrAPackageThatHasNoCodeWritesNone() {
        final @NotNull TestSetPackageNode checkout = indexedPackage("Checkout", theTestCasesDirectory());
        final @NotNull TestSetPackageNode payment = indexedPackage("Payment", theTestCasesDirectory());
        final @NotNull TestSetNode login = indexedTestSet("Login", theTestCasesDirectory());

        NodeCode.of(NodeType.TEST_SET).getRenamed().execute(getProject(), new RenamedNode(login, "Sign in"));
        NodeCode.of(NodeType.TEST_SET).getMoved().execute(getProject(), new MovedNode(login, checkout.getPath()));
        NodeCode.of(NodeType.TEST_SET_PACKAGE).getRenamed().execute(getProject(), new RenamedNode(checkout, "Basket"));
        NodeCode.of(NodeType.TEST_SET_PACKAGE).getMoved().execute(getProject(), new MovedNode(checkout, payment.getPath()));

        for (final String written : List.of("nafath.SignInTest", "nafath.LoginTest", "nafath.checkout.LoginTest", "nafath.basket.LoginTest", "nafath.payment.checkout.LoginTest"))
            assertTrue("a move or a rename of nodes that had no code wrote " + written, generatedClass(written).isEmpty());
    }

    // Rule-CODEGEN-077
    public void testACutAndPastedTestCaseTakesItsMethodBodyAndAllIntoAClassWrittenForIt() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase testCase = createdTestCase(login, "Log in with a valid user", "b");
        writtenByTheTester(writtenMethodOf("nafath.LoginTest", testCase), WRITTEN_BY_THE_TESTER);

        cutInto(testCase, login, indexedTestSet("Visa", indexedPackage("Cards", theTestCasesDirectory())));

        assertTrue("the method stayed in the class the test case was cut from", methodOf("nafath.LoginTest", testCase).isEmpty());
        assertTrue("the method did not arrive with the tester's body", writtenMethodOf("nafath.cards.VisaTest", testCase).getText().contains(WRITTEN_BY_THE_TESTER));
    }

    // Rule-CODEGEN-077
    public void testACutTestCasesBodyStillResolvesWhatItUsesInTheClassItJoins() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase cut = createdTestCase(login, "Log in with a valid user", "b");
        final @NotNull PsiClass loginTest = generatedClass("nafath.LoginTest").orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            final @NotNull PsiClass assertions = Optional.ofNullable(JavaPsiFacade.getInstance(getProject()).findClass("org.testng.Assert", GlobalSearchScope.allScope(getProject()))).orElseThrow(() -> new AssertionError("TestNG's Assert is not on the class path"));
            Optional.ofNullable(((PsiJavaFile) loginTest.getContainingFile()).getImportList()).orElseThrow().add(JavaPsiFacade.getElementFactory(getProject()).createImportStatement(assertions));
        });
        writtenByTheTester(writtenMethodOf("nafath.LoginTest", cut), "Assert.assertTrue(true);");

        cutInto(cut, login, indexedTestSet("Visa", indexedPackage("Cards", theTestCasesDirectory())));

        final @NotNull PsiJavaCodeReferenceElement used = PsiTreeUtil.findChildrenOfType(writtenMethodOf("nafath.cards.VisaTest", cut), PsiJavaCodeReferenceElement.class).stream()
                .filter(reference -> "Assert".equals(reference.getReferenceName())).findFirst().orElseThrow(() -> new AssertionError("the body lost its Assert"));
        assertTrue("the body's Assert does not resolve in the class it joined", used.resolve() instanceof PsiClass);
    }

    // Rule-CODEGEN-077
    public void testACutWhoseNewClassCannotBeWrittenLeavesTheMethodWhereItWas() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase testCase = createdTestCase(login, "Log in with a valid user", "b");
        writtenByTheTester(writtenMethodOf("nafath.LoginTest", testCase), WRITTEN_BY_THE_TESTER);
        aFileWhereTheCardsFolderGoes();

        cutInto(testCase, login, indexedTestSet("Visa", indexedPackage("Cards", theTestCasesDirectory())));

        assertTrue("the method left a class although no class could take it", writtenMethodOf("nafath.LoginTest", testCase).getText().contains(WRITTEN_BY_THE_TESTER));
    }

    // Rule-CODEGEN-077
    public void testACutIntoAClassThatAlreadyHoldsItsMethodLeavesTheMethodWhereItWas() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase testCase = createdTestCase(login, "Log in with a valid user", "b");
        final @NotNull TestSetNode visa = createdTestSet("Visa");
        final @NotNull PsiMethod copied = writtenMethodOf("nafath.LoginTest", testCase);
        final @NotNull PsiClass visaTest = generatedClass("nafath.VisaTest").orElseThrow();
        WriteCommandAction.runWriteCommandAction(getProject(), () -> {
            visaTest.add(copied.copy());
        });

        cutInto(testCase, login, visa);

        assertEquals("the method left its class for one that already holds it", 1, methodsFor("nafath.LoginTest", testCase));
        assertEquals("the class already holding the method got a second", 1, methodsFor("nafath.VisaTest", testCase));
    }

    // Rule-CODEGEN-077
    public void testACutFromATestSetWithNoClassWritesNoCode() {
        final @NotNull TestSetNode login = indexedTestSet("Login", theTestCasesDirectory());
        final @NotNull TestCase testCase = indexedTestCase(login, "Log in with a valid user", "b");

        cutInto(testCase, login, indexedTestSet("Visa", theTestCasesDirectory()));

        assertTrue("a class was written although there was no method to carry", generatedClass("nafath.VisaTest").isEmpty());
    }

    // Rule-CODEGEN-077
    public void testACutOfATestCaseWithNoMethodWritesNoClassWhereItGoes() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        createdTestCase(login, "Log out", "c");
        final @NotNull TestCase withoutCode = indexedTestCase(login, "Log in with a valid user", "b");

        cutInto(withoutCode, login, indexedTestSet("Visa", theTestCasesDirectory()));

        assertTrue("a class was written for a test case that had no method to carry", generatedClass("nafath.VisaTest").isEmpty());
    }

    // Rule-CODEGEN-077
    public void testACutPastedBackIntoItsOwnTestSetLeavesItsMethodWhereItIs() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase testCase = createdTestCase(login, "Log in with a valid user", "b");
        writtenByTheTester(writtenMethodOf("nafath.LoginTest", testCase), WRITTEN_BY_THE_TESTER);

        cutInto(testCase, login, login);

        assertEquals("a cut pasted back into its own test set did not leave one method", 1, methodsFor("nafath.LoginTest", testCase));
        assertTrue("the method lost the tester's body", writtenMethodOf("nafath.LoginTest", testCase).getText().contains(WRITTEN_BY_THE_TESTER));
    }

    // Rule-CODEGEN-077, Rule-CODEGEN-014
    public void testACutNumbersBothTestSetsAgain() {
        final @NotNull TestSetNode login = createdTestSet("Login");
        final @NotNull TestCase cut = createdTestCase(login, "Log in with a valid user", "b");
        final @NotNull TestCase left = createdTestCase(login, "Log out", "c");
        final @NotNull TestSetNode visa = indexedTestSet("Visa", indexedPackage("Cards", theTestCasesDirectory()));

        final @NotNull TestCases testCases = Services.getInstance(getProject(), TestCases.class);
        testCases.removeTestCase(login.getPath(), cut.getUuid());
        cut.setParent(visa);
        testCases.putTestCaseVerbatim(visa.getPath(), cut);
        CodeChangeType.MOVE_TEST_CASE.executeAllNow(getProject(), List.of(new MovedTestCase(cut, login)));
        settled();

        assertEquals("the test case left behind kept the position it had before the cut", "1", attributeOf(writtenMethodOf("nafath.LoginTest", left), "priority"));
        assertEquals("the cut test case is not first in the test set it joined", "1", attributeOf(writtenMethodOf("nafath.cards.VisaTest", cut), "priority"));
    }
}
