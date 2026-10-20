[Documentation](../README.md) › Task guides

# How to generate and run automation code

> Testin writes a Java class for each test set and a TestNG method for each test
> case, keeps them in step with the tree, and runs them from the editor.

## Before you start

- The Java and TestNG plugins are on, in **Settings | Plugins**.
- The code project has a folder marked as **Test Sources Root**.
- `testin.yml` names the open test project. If not, read
  [How to link this repository to its test project](linkThisRepository.md).

## Steps

1. Create a test set. Its class appears under the test source folder.
   ([Get a class when I create a test set](../codegen/getClassForTestSet.md))
2. Create a test case. Its method appears in that class.
   ([Get a method when I create a test case](../codegen/getMethodForTestCase.md))
3. For test cases that arrived without a method, select them and press `F12`, **Automate Test Case**.
   ([Ask for the method a test case never got](../codegen/automateTestCase.md))
4. Press `Shift+F5` to open a test case's method. ([Go to the code from a test case](../codegen/goToCode.md))
5. Press `F5` to run the selected test cases. Each result is recorded on its
   run item. ([Run a test case's automation](../codegen/runAutomation.md))

## If something goes wrong

| What you see                                              | What to do                                                                                                                                          |
|-----------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| The red ? says the Java plugin is not available           | Turn it on in **Settings \| Plugins** and restart the IDE. ([Work in an IDE with no Java plugin](../codegen/noJavaPlugin.md))                       |
| The red ? says the project has no test source folder      | Mark a folder as **Test Sources Root** in **Project Structure**. ([Work in a project with no test source folder](../codegen/noTestSourceFolder.md)) |
| *TestNG Plugin Not Available*                             | Turn on TestNG in **Settings \| Plugins** and restart the IDE.                                                                                      |
| The red ? says testin.yml does not name this test project | Press **Save to testin.yml** in its hint.                                                                                                           |

[Documentation](../README.md) › Task guides
