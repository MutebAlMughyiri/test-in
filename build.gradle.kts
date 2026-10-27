import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

plugins {
    id("java")
    id("idea")
    id("pmd")
    id("jacoco")
    id("org.jetbrains.intellij.platform")
    alias(libs.plugins.errorprone)
    alias(libs.plugins.changelog)
}

// What the IDE and the headless inspector must not index. The sandbox alone is
// 2.2 GB in 13,308 files - an IDE's own caches, logs and jcef store - and it
// sits inside the project folder because that is where the platform plugin puts
// it. Indexing it costs most of an inspection run and buys nothing: the scope
// in .idea/scopes/Inspected.xml throws every one of those findings away again.
idea {
    module {
        excludeDirs = excludeDirs + listOf(
            file(".sandbox"),
            file(".inspection"),
            file(".intellijPlatform"),
            file(".gradle"),
            file("build")
        )
    }
}

group = "org.testin"
version = "2.15.0-alpha"

/**
 * The oldest IDE branch the plugin claims to support, verified alongside the
 * one it is built against.
 *
 * intellij.version in gradle.properties is what this compiles and runs against.
 * This is the other end of the range plugin.xml declares - sinceBuild 262 and
 * no untilBuild - and it is the end that breaks: code written against the newer
 * build can call what the first release of the branch does not have, and only
 * the verifier says so. Move it when sinceBuild moves, and not before.
 *
 * 261 is gone from both ends since 22 September 2026. IntelliJ 2026.2 runs on
 * JetBrains Runtime 25 and ships Java 25 class files, which a plugin compiled
 * for 21 cannot read and a 2026.1 IDE cannot run.
 */
val oldestBranch = "2026.2"

// The core's source tree and every content module's, in the one place a module is
// named: settings.gradle.kts includes it, and everything here follows (#483).
val moduleSources = listOf("src") + subprojects.map { "${it.name}/src" }

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// #323: the compiler is one of the sources `tools/warnings.ps1` gathers, and it
// reported nothing because -Xlint was off - one `Note:` and no names. deprecation
// and removal are the two the Marketplace also reports, so the gate, the report
// and the plugin page now count the same things. -Xlint:all is deliberately not
// taken: on a tree that has never had it on it is hundreds of findings, which is
// a sweep rather than a gate. cast joined them on 8 October 2026: a redundant
// cast is a finding CI's Inspect fails on, and javac names it while working.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal", "-Xlint:cast"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

// #377, #389: NullAway fails compileJava and compileTestJava on a null reaching
// what the annotations say cannot take one, in all three modules. Error Prone's
// own checks run beside it at their default severity since 7 October 2026: an
// error fails compilation and a warning is fixed, not left standing.
// A test fills its fields in JUnit 3's setUp or TestNG's @BeforeMethod rather
// than a constructor, so both are named as initializers.
val errorprone = libs.errorprone
val nullaway = libs.nullaway

allprojects {
    pluginManager.apply("net.ltgt.errorprone")

    dependencies {
        "errorprone"(errorprone)
        "errorprone"(nullaway)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.errorprone {
            enabled.set(name == "compileJava" || name == "compileTestJava")
            allErrorsAsWarnings.set(false)
            check("NullAway", CheckSeverity.ERROR)
            // Types an enum constant holds that Testin cannot annotate @Immutable:
            // the platform's Icon, KeyStroke, Font and SimpleTextAttributes, and
            // the JDK's function interfaces and Comparator, every one of which Testin
            // fills with a method reference or a lambda capturing nothing that changes.
            option(
                "Immutable:KnownImmutable", listOf(
                    "javax.swing.Icon",
                    "java.awt.Font",
                    "javax.swing.KeyStroke",
                    "com.intellij.ui.SimpleTextAttributes",
                    "java.util.function.Function",
                    "java.util.function.BiFunction",
                    "java.util.function.Consumer",
                    "java.util.function.BiConsumer",
                    "java.util.function.Predicate",
                    "java.util.function.BiPredicate",
                    "java.util.function.ToLongFunction",
                    "java.util.function.LongFunction",
                    "java.util.Comparator"
                ).joinToString(",")
            )
            option("NullAway:AnnotatedPackages", "org.testin")
            option(
                "NullAway:KnownInitializers",
                "junit.framework.TestCase.setUp,com.intellij.testFramework.UsefulTestCase.setUp,com.intellij.testFramework.fixtures.BasePlatformTestCase.setUp"
            )
            option(
                "NullAway:CustomInitializerAnnotations",
                "org.testng.annotations.BeforeMethod,org.testng.annotations.BeforeClass"
            )
        }
    }
}

// #378: pmdMain, part of check, fails on a production method over the cognitive
// complexity or if-nesting limit in complexity-rules.xml, which exempts
// no method (#382). It also fails on an unused private field, private method,
// local or parameter, in unused-code-rules.xml (#445). The rules read
// the syntax tree only, so PMD is given no compiled classes and does not wait
// for compileJava. The core gets its library jars so PMD can resolve platform
// types; the two modules' jars would include the core's own, which would wait
// for it.
val pmdVersion = libs.versions.pmd.get()

allprojects {
    pluginManager.apply("pmd")

    pmd {
        toolVersion = pmdVersion
        ruleSets = listOf()
        ruleSetFiles = rootProject.files(".github/complexity-rules.xml", ".github/unused-code-rules.xml")
        isConsoleOutput = true
    }

    tasks.withType<Pmd>().configureEach {
        enabled = name == "pmdMain"
        classpath = if (project == rootProject) project.the<SourceSetContainer>()["main"].compileClasspath else files()
    }
}

// #325, #389: line and branch coverage, from test and ideTest together, in one
// report across the three modules, and check fails below either minimum. JaCoCo
// recorded nothing here
// once (#130) because the platform's class loader gives a class no code location
// and the agent skips such a class unless told otherwise. The report reads the
// instrumentCode output: JaCoCo matches a class by a checksum of its bytes, and
// those are the bytes the tests load, not the ones in build/classes. Each
// minimum is the whole percent under what was measured on 27 October 2026 -
// 95.08% of lines, 58.35% of branches - so it stops coverage falling, and
// raising it is a decision rather than a side effect. #425 raises it toward
// 100% of both as the tests it asks for land. The agent measures only Testin's
// own classes, and answers over JMX, so each test can save what it alone ran:
// ruleTrace reads those files, and the report reads them with the rest.
allprojects {
    pluginManager.apply("jacoco")

    tasks.withType<Test>().configureEach {
        extensions.configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            includes = listOf("org.testin.*")
            isJmx = true
        }
    }

    tasks.withType<JacocoReportBase>().configureEach {
        enabled = project == rootProject
    }
}

// The three modules' tests save what each one ran too (#425). TestNG finds
// TestCoverageListener through the service file in the root's test resources,
// so the root's test classes go on each module's test classpath, and the files
// land in the root's per-test folder, where ruleTrace and the report read them.
subprojects {
    pluginManager.withPlugin("java") {
        dependencies {
            "testRuntimeOnly"(
                files(
                    rootProject.layout.buildDirectory.dir("classes/java/test"),
                    rootProject.layout.buildDirectory.dir("resources/test")
                ).builtBy(":testClasses")
            )
        }
    }

    tasks.withType<Test>().configureEach {
        val ranByEachTest = rootProject.layout.buildDirectory.dir("jacoco/per-test/${project.name}-$name")
        systemProperty("testin.testCoverage", ranByEachTest.get().asFile.path)
        doFirst { ranByEachTest.get().asFile.deleteRecursively() }
    }
}

tasks.withType<JacocoReportBase>().configureEach {
    executionData.setFrom()
    classDirectories.setFrom()
    sourceDirectories.setFrom()

    allprojects.forEach { module ->
        dependsOn(module.tasks.withType<Test>().matching { it.name != "ruleTrace" })
        executionData.from(module.fileTree(module.layout.buildDirectory.dir("jacoco")).include("**/*.exec"))
        classDirectories.from(module.tasks.named("instrumentCode"))
        sourceDirectories.from(module.the<SourceSetContainer>()["main"].java.srcDirs)
    }
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}

// The report follows the gate rather than preceding it, so a build the gate
// fails still leaves the report that says where the coverage went.
tasks.jacocoTestCoverageVerification {
    finalizedBy(tasks.jacocoTestReport)

    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.95".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.58".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification, "ruleTrace")
}

// The IntelliJ Platform Gradle Plugin points each bundled plugin and module at
// its jar inside the IDE with a component metadata rule, and registers that rule
// only once intellijPlatformDependency has resolved. A command-line build gets
// there first through its tasks. A Gradle sync builds no task graph: it resolved
// compileClasspath first, lost 1,066 bundled jars, and IntelliJ and Qodana
// imported all three modules with no compile scope - Lombok, the platform and
// the core unresolved. Every other configuration now resolves it first.
allprojects {
    pluginManager.withPlugin("org.jetbrains.intellij.platform.base") {
        val platform = configurations.named("intellijPlatformDependency")

        configurations.matching { it.isCanBeResolved && it.name != platform.name }.configureEach {
            incoming.beforeResolve { platform.get().incoming.files.files }
        }
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))

        bundledPlugins(
            listOf(
                "com.intellij.java",
                "TestNG-J",
                "Git4Idea",
                "com.intellij.modules.json",
                "org.jetbrains.plugins.terminal"
            )
        )

        jetbrainsRuntime()
        pluginVerifier()
        zipSigner()
        testFramework(TestFrameworkType.Platform)

        // Everything that needs the IntelliJ Java plugin lives here rather than
        // in the core jar, so the verifier checks it only against IDEs that
        // have com.intellij.java (#144).
        //
        // Packaged, not compiled against: the core calls the extension points it
        // declares itself and never names a class in the module. Wrapping this
        // in implementation() would put the module on the core's compile
        // classpath and make the two projects depend on each other, since the
        // module compiles against the core.
        subprojects.forEach { module -> pluginModule(project(module.path)) }
    }

    // Lombok is a compile-time tool: annotations generate plain Java, nothing
    // references it at runtime - so it must never be packaged into the plugin
    // distribution ("implementation" would ship its 2MB jar to the Marketplace).
    listOf(
        "compileOnly", "annotationProcessor", "testCompileOnly", "testAnnotationProcessor"
    ).forEach { configName ->
        add(configName, libs.lombok)
    }

    // @Immutable on Testin's own types that enums hold, so ImmutableEnumChecker
    // can prove an enum constant never changes. Read by Error Prone at compile
    // time only, and the platform ships its own copy.
    compileOnly(libs.errorprone.annotations)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    // YAML for testin.yml, the file that binds an automation repository to the
    // test project it exercises (#6). SnakeYAML comes with it and the platform
    // ships its own copy, so the verifier run is what proves they do not clash.
    implementation(libs.jackson.dataformat.yaml)
    testImplementation(libs.testng)
    testImplementation(libs.junit)
    // RuleTraceTest reads what each test ran, at the version the agent wrote it.
    testImplementation("org.jacoco:org.jacoco.core:${project.the<JacocoPluginExtension>().toolVersion}")
    implementation(libs.iText.kernel)
    implementation(libs.iText.layout)
    implementation(libs.poi)
    implementation(libs.poi.ooxml)

    // POI 5.5.1, the latest, brings log4j-api 2.24.3, which CVE-2026-49844 covers
    // (2.13.1 to 2.25.4): it ships in the plugin, so the fixed release is pinned.
    // JaCoCo's core brings ASM 9.10.1, which CVE-2026-110260 and its siblings
    // cover (3.0 before 9.11), and 9.11 itself has CVE-2026-112046 (before
    // 9.11.1). Only RuleTraceTest reads it, so the fixed release is pinned for
    // the tests.
    constraints {
        implementation(libs.log4j.api)
        testImplementation(libs.asm)
        testImplementation(libs.asm.commons)
    }

    // The architecture rules, read off the bytecode rather than off CLAUDE.md
    // (#114). testImplementation only: it never reaches the distribution.
    testImplementation(libs.archunit)
}

// 2.5, 2.6 and 2.7 were released under two-part versions, which the plugin's
// default SemVer pattern refuses to read as a heading.
changelog {
    headerParserRegex.set("""(\d+\.\d+(?:\.\d+)?(?:-[0-9A-Za-z.]+)?)""".toRegex())
}

intellijPlatform {
    pluginConfiguration {
        val pluginVersion = project.version.toString()
        val changelog = project.changelog

        version.set(pluginVersion)

        // The change notes are written once, in CHANGELOG.md (#103). The section
        // named after this version becomes plugin.xml's <change-notes>, and
        // Unreleased stands in until `./gradlew patchChangelog` names it.
        changeNotes.set(provider {
            changelog.renderItem(
                (changelog.getOrNull(pluginVersion) ?: changelog.getUnreleased()).withHeader(false)
                    .withEmptySections(false),
                Changelog.OutputType.HTML
            )
        })

        ideaVersion {
            sinceBuild.set("262")
            untilBuild.set(null as String?)
        }
    }

    pluginVerification {
        // IntelliJ IDEA, PyCharm and Rider, because all three results are news.
        //
        // PyCharm used to be behind the flag below with GoLand and WebStorm: the
        // verifier reported every PSI and TestNG reference as unresolved - 53
        // apiece, 159 in all - because those IDEs do not ship the Java and TestNG
        // plugins, and its static analysis cannot see that OptionalPlugin guards
        // the code at run time (#41). Moving the Java and TestNG code into
        // content modules the IDE loads only where they apply took that to zero,
        // which the Marketplace confirmed on 2.9.0-alpha, so PyCharm's answer is
        // a real answer again and belongs in the gate rather than behind a flag.
        //
        // Rider joins them because it is the widest gap between what the plugin
        // claims and what is proved: it is not an IntelliJ IDEA with a plugin
        // missing, it is a different product built on the same platform, and the
        // plugin page names it as somewhere Testin runs. A claim nothing checks
        // is the kind that is true until the day it is not.
        //
        // Every other product the Marketplace names as compatible is verified by
        // verify.yml, one job per product, which names it here with
        // -PverifyIde=<IntelliJPlatformType> and, where its versions are not
        // the two below, -PverifyVersions=<a,b>. That workflow is the one list
        // of the products and their versions; this only verifies what it names.
        //
        //     gradlew verifyPlugin -PverifyIde=GoLand
        //     gradlew verifyPlugin -PverifyIde=Rider -PverifyVersions=2026.2.3
        ides {
            // Two versions, not one. sinceBuild is 262 with no untilBuild, so
            // the plugin claims every build from 262 onward - and that is what
            // the Marketplace verifies against. Pinning only the version this
            // compiles against hid a real defect: the Marketplace reported a
            // renderer scheduled for removal in 262 that our own sweep, running
            // against 261, could not see. The same trap sits inside a branch,
            // so the first release of it is verified as well as the build this
            // compiles against.
            val versions = providers.gradleProperty("verifyVersions").map { it.split(",") }
                .orElse(providers.gradleProperty("intellij.version").map { listOf(it, oldestBranch) }).get()
            val products = providers.gradleProperty("verifyIde").map { listOf(IntelliJPlatformType.valueOf(it)) }
                .getOrElse(
                    listOf(
                        IntelliJPlatformType.IntellijIdea,
                        IntelliJPlatformType.PyCharm,
                        IntelliJPlatformType.Rider
                    )
                )

            products.forEach { ide -> versions.forEach { create(ide, it) } }
        }

        // Eleven of the twelve levels the verifier offers, so the gate fails on
        // anything new rather than on the two it was narrowed to. The one level
        // that is off is named below, with what it would cost.
        //
        // It was down to COMPATIBILITY_PROBLEMS and INVALID_PLUGIN while one
        // internal-API call needed the exception - ExecutionManager
        // .getRunningDescriptors, how the stop found the process behind an execution.
        // The stop now records the handler the execution topic hands it, so the
        // plugin makes no internal call and the level goes back on (#140).
        //
        // EXPERIMENTAL_API_USAGES is on since #324: the last experimental call,
        // the tab title color override, went once the platform was found to give
        // a Testin tab the label color by itself (Decision-017).
        //
        // MISSING_DEPENDENCIES is off, and not as a decision to live with but as
        // a report of the thing working. Testin declares com.intellij.java, com.intellij.modules.java
        // and TestNG-J optional so the Java and TestNG code loads only in an IDE
        // that has them, which is what took PyCharm from 159 compatibility
        // problems to zero (#144). The verifier then lists all three as
        // Unavailable on every PyCharm target and raises this level while
        // reporting the plugin itself Compatible. Left on, the gate could never
        // be green for the IDE it had just been widened to cover: main was red
        // on every push from 2026-08-30 until this came off. A dependency that
        // is genuinely required and genuinely missing still fails the build,
        // through INVALID_PLUGIN and COMPATIBILITY_PROBLEMS, which are what a
        // plugin that will not load reports.
        failureLevel.set(
            listOf(
                FailureLevel.COMPATIBILITY_WARNINGS,
                FailureLevel.COMPATIBILITY_PROBLEMS,
                FailureLevel.DEPRECATED_API_USAGES,
                FailureLevel.EXPERIMENTAL_API_USAGES,
                FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
                FailureLevel.INTERNAL_API_USAGES,
                FailureLevel.OVERRIDE_ONLY_API_USAGES,
                FailureLevel.NON_EXTENDABLE_API_USAGES,
                FailureLevel.PLUGIN_STRUCTURE_WARNINGS,
                FailureLevel.INVALID_PLUGIN,
                FailureLevel.NOT_DYNAMIC,
            )
        )
    }

    signing {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishing {
        token.set(System.getenv("JETBRAINS_TOKEN"))
        // Two channels only: alpha while a release is being used, and default
        // for production. A release goes out to alpha first; to put the same
        // version on default, swap the two lines below and publish again - the
        // Marketplace takes one version once per channel, as 2.14.1-alpha
        // showed - then swap them back so alpha stays the default.
        channels.set(listOf("alpha"))
        //channels.set(listOf("default"))
    }

    sandboxContainer.set(layout.projectDirectory.dir(".sandbox"))
}

tasks {
    processResources {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        from(layout.projectDirectory.dir("docs")) {
            include("**/*.md")
            into("docs")
        }
    }

    // Everything both test tasks need. The runner is not here: `test` is TestNG
    // and `ideTest` is JUnit, because BasePlatformTestCase is a JUnit TestCase
    // and the TestNG runner never sees one (#108).
    withType<Test> {
        jvmArgs("--sun-misc-unsafe-memory-access=allow")

        // How many test cases IndexerBudgetTest measures against. Forwarded
        // rather than inherited: a -D on the Gradle command line reaches the
        // Gradle daemon, not the JVM the tests run in, so without this the
        // documented `-Dtestin.budget.testCases=10000` would silently measure the
        // default instead (#125).
        System.getProperty("testin.budget.testCases")?.let { systemProperty("testin.budget.testCases", it) }

        // What each test ran, saved by TestCoverage after every test method into a
        // folder of this task's own: the three IDE test shares run at once in one
        // build folder, and one emptying a shared folder would wipe the others'
        // (#425).
        val ranByEachTest = layout.buildDirectory.dir("jacoco/per-test/$name")
        systemProperty("testin.testCoverage", ranByEachTest.get().asFile.path)
        doFirst { ranByEachTest.get().asFile.deleteRecursively() }

        // The documents are an input to the tests that read them - RuleNumbersTest
        // scans every page for the numbers the code cites. Without this Gradle
        // sees only the Java, calls the task up to date after a documentation
        // change, and the check silently does not run.
        inputs.dir(layout.projectDirectory.dir("docs"))
            .withPropertyName("docs")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        // The same for the markers: a // Rule- or // UC- line compiles to
        // nothing, so an edit to one leaves the class files byte for byte the
        // same, and RuleNumbersTest would not run against the new marker.
        inputs.files(
            moduleSources.map { layout.projectDirectory.dir("$it/main/java") })
            .withPropertyName("markedSources")
            .withPathSensitivity(PathSensitivity.RELATIVE)

        testLogging {
            events("passed", "skipped", "failed")
        }
    }

    /**
     * The ordinary tests: everything that needs no IDE, which is almost all of
     * them. An IDE test is excluded by name rather than by living somewhere
     * else, so the two kinds sit beside the code they are about.
     */
    named<Test>("test") {
        // The indexer's parsing budget runs alone: ./gradlew test -Pbudget.
        //
        // It measures time, and a machine that is also compiling or running a
        // second build is slower. On a developer's machine it failed with no
        // code change and passed on the next run, which teaches everybody to
        // re-run rather than read (#66, finding 108). CI runs it as its own
        // step, on a runner doing nothing else.
        //
        // The same task rather than a second one, because this one carries the
        // platform plugin's launcher: a separately registered Test task starts
        // on the daemon's JDK, which refuses the JVM flag every test task gets.
        useTestNG {
            if (providers.gradleProperty("budget").isPresent) includeGroups("budget") else excludeGroups(
                "budget",
                "trace"
            )
        }
        exclude("**/*IdeTest.class")
    }
}

/**
 * The IDE tests run as three shares at once, split by package, and `ideTest`
 * runs all three.
 *
 * One JVM ran all 760 one after another, nine of the build's nine and a half
 * minutes. Four forks of that one task were tried and failed 175 tests: forks
 * share the task's sandbox, and the application settings saved in its config
 * folder are what the tests set and read, so each fork read the others'. A
 * registered task gets a sandbox of its own, so three registrations are three
 * IDEs that cannot see each other. Each pays about fifty seconds to start.
 *
 * The shares were balanced on the times of 4 October 2026: 194, 179 and 182
 * seconds side by side, so the IDE tests take a little over three minutes
 * rather than nine. When one share grows well past the others, move a package
 * from it. The last share names no package: it takes every IDE test the others do
 * not, so a new package is run by it rather than by nothing.
 */
val ideTestShares = mapOf(
    "ideTestGitAndCode" to listOf("git", "codegen"),
    "ideTestEditorAndTree" to listOf("editor", "explorer", "clipboard", "bug", "view", "testrun"),
    "ideTestTheRest" to emptyList(),
)

intellijPlatformTesting {
    /**
     * The tests that need a running IDE (#108).
     *
     * An indexer operation, an undo across two test sets and a card that asks a
     * project service what is running cannot be checked without one. Three
     * separate pieces of work wanted this in a single session, which is what
     * settled it.
     *
     * Registered through the plugin rather than hand-rolled: a Test task needs
     * the platform's own classpath, sandbox and JVM arguments, and this is where
     * they come from. What is added here is the runner and which classes it
     * takes, because BasePlatformTestCase is a JUnit TestCase and the TestNG
     * runner that runs everything else would never see one.
     *
     * Code Provenance is off in them: it starts chatter.exe inside a test's
     * temporary folder, and on Windows the platform then cannot delete that
     * folder, which failed a test at teardown on CI.
     */
    testIde {
        ideTestShares.keys.forEach {
            register(it) {
                plugins {
                    disablePlugin("com.intellij.code.provenance")
                }
            }
        }
    }

    runIde {
        // The PyCharm sandbox has RobotCode, with the LSP4IJ it needs, as a Robot team
        // has it. They are declared here because prepareSandbox deletes every plugin it
        // did not put in the sandbox itself: one installed from the Marketplace inside
        // the sandbox is gone after the next change to Testin.
        register("runPyCharm") {
            type = IntelliJPlatformType.PyCharm
            version = "2026.2.1"
            plugins {
                plugin("dev.robotcode.robotcode4ij", "2.7.0")
                plugin("com.redhat.devtools.lsp4ij", "0.21.0")
            }
        }
        register("runDataGrip") {
            type = IntelliJPlatformType.DataGrip
            version = "2026.2.1"
        }
        register("runRider") {
            type = IntelliJPlatformType.Rider
            version = "2026.2.1"
        }
    }
}


/**
 * The tests that need a running IDE.
 *
 * Registered just above under `intellijPlatformTesting` so that it carries the
 * platform's classpath, sandbox and JVM arguments; what it needs on top is here,
 * after the registration, because a task cannot be configured before it exists.
 *
 * An IDE test lives beside the code it is about rather than in a source set of
 * its own. So the task is pointed at the classes the ordinary tests compile to,
 * and it takes only the ones named for it.
 */
ideTestShares.forEach { (share, packages) ->
    tasks.named<Test>(share) {
        group = "verification"
        description =
            "The tests that need a running IDE, in " + packages.ifEmpty { listOf("every package the other shares leave") }
                .joinToString()

        useJUnit()

        testClassesDirs = sourceSets["test"].output.classesDirs

        // Three things, and the middle one is the trap. The test classes and their
        // own dependencies come from the test source set. The platform's test
        // framework - BasePlatformTestCase itself - is in a configuration of the
        // plugin's own. Without it the scanner cannot resolve the superclass and
        // skips every IDE test in silence, reporting that it found none.
        classpath += sourceSets["test"].runtimeClasspath
        classpath += configurations["intellijPlatformTestClasspath"]

        if (packages.isEmpty()) {
            include("**/*IdeTest.class")
            exclude(ideTestShares.values.flatten().map { "org/testin/$it/**" })
        } else {
            include(packages.map { "org/testin/$it/**/*IdeTest.class" })
        }
    }
}

tasks.register("ideTest") {
    group = "verification"
    description = "The tests that need a running IDE, as three shares at once"
    dependsOn(ideTestShares.keys)
}

/**
 * Whether every test labeled with a rule or a use case runs the code labeled
 * with it (#425). RuleTraceTest reads what each test ran, saved by every test
 * task, so it runs after them all, as a TestNG task of its own on the same JDK
 * as `test`. After, not on: a failing test must not stop the report. It
 * measures nothing itself.
 */
tasks.register<Test>("ruleTrace") {
    group = "verification"
    description = "Every test labeled with a rule or a use case runs the code labeled with it"

    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    javaLauncher.set(tasks.test.flatMap { it.javaLauncher })
    useTestNG { includeGroups("trace") }
    include("org/testin/docs/RuleTraceTest.class")

    mustRunAfter(listOf("test") + ideTestShares.keys + subprojects.map { "${it.path}:test" })
    inputs.files(layout.buildDirectory.dir("jacoco/per-test")).withPropertyName("ranByEachTest")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.files(
        moduleSources.map { layout.projectDirectory.dir(it) })
        .withPropertyName("labeledSources")
        .withPathSensitivity(PathSensitivity.RELATIVE)
    extensions.configure<JacocoTaskExtension> { isEnabled = false }
}

configurations.all {
    resolutionStrategy {
        cacheDynamicVersionsFor(7, TimeUnit.DAYS)
        cacheChangingModulesFor(7, TimeUnit.DAYS)
    }
}

// The two run tasks open the sample the same way and differ only in the folder
// their sandbox keeps its settings in. A loop rather than a function: the
// configuration cache cannot serialize a reference to a script-level function,
// and a helper failed the build with "cannot serialize Gradle script object
// references". The loop's body captures only the values it names.
mapOf("runIde" to "config", "runPyCharm" to "config_runPyCharm").forEach { (runTask, configFolder) ->
    tasks.named<RunIdeTask>(runTask) {
        jvmArgs("--sun-misc-unsafe-memory-access=allow")

        val sandboxes = layout.projectDirectory.dir(".sandbox/Testin").asFile
        val committedSample = layout.projectDirectory.dir("samples").asFile

        // Each task keeps a copy of its own. PyCharm, opening the Java sample, writes a
        // Python module into its .idea, and the Java IDE's build reads that file as part
        // of the Maven module: every test class then compiles without TestNG (#494).
        val workingSample = layout.projectDirectory.dir(if (runTask == "runPyCharm") ".sandbox/sample-pycharm" else ".sandbox/sample").asFile
        val sampleRoot = workingSample.resolve("testin-root").absolutePath
        val sampleProject = workingSample.resolve("automation").absolutePath

        // A machine can name the project runPyCharm opens instead of the Java sample, as
        // testin.pyCharmProject in its own ~/.gradle/gradle.properties - a Robot Framework
        // project, so every launch lands on it with its plugins already installed. It is a
        // path on one machine, so it never belongs in this file.
        val openedProject = if (runTask == "runPyCharm") providers.gradleProperty("testin.pyCharmProject").getOrElse(sampleProject) else sampleProject

        doFirst {
            // The sandbox opens a COPY of the sample, never the committed one. Opening
            // the committed sample writes to this repository: the plugin binds a
            // project to the root it runs under, so a sandbox aimed at anyone's own test
            // data resolves nothing for `testinProject: Demo` and rebinds it - writing
            // that root's project name straight back into a committed file. It happened
            // twice; SampleTestProjectTest is what caught it.
            //
            // Copied once and then left alone, so test runs created while testing survive
            // to the next launch. Deleting .sandbox/sample is how to get the sample back.
            if (!workingSample.exists()) {
                committedSample.copyRecursively(workingSample)

                // Everything in the sample's .idea is the IDE's rather than the
                // sample's - .gitignore says exactly that - but only vcs.xml is
                // written relative to where the project sits. It maps
                // $PROJECT_DIR$/../.. to a Git root, which is this repository from
                // samples/automation and is .sandbox from the copy, where there is
                // no repository and the IDE reports an invalid VCS root on every
                // launch. The rest of .idea is path-independent and is kept, so the
                // copy opens as an imported Maven project rather than re-importing.
                workingSample.walkTopDown()
                    .filter { it.name == "vcs.xml" && it.parentFile.name == ".idea" }
                    .toList()
                    .forEach { it.delete() }
                logger.lifecycle("Copied the sample into the sandbox, so the committed one stays clean: $workingSample")
            }

            // The sandbox directory is found rather than composed: the one under
            // .sandbox/Testin is named by the platform plugin from the IDE it
            // downloaded, so writing that name out would be a second place to update
            // on every version bump. prepareSandbox has run by now, so it exists. The
            // config directory, though, carries this task's own suffix and is passed
            // in - a custom run task gets config_<taskName>, not config.
            sandboxes.listFiles().orEmpty()
                .filter { it.resolve(configFolder).isDirectory }
                .forEach { sandbox ->
                    val options = sandbox.resolve(configFolder).resolve("options")
                    val settings = options.resolve("testinSettings.xml")

                    // Only a fresh sandbox is pointed at the sample data. One somebody
                    // has aimed at their own test root keeps it, and opens the sample
                    // copy against it - which rebinds the copy and harms nothing.
                    if (!settings.exists()) {
                        options.mkdirs()
                        settings.writeText(
                            """
                            <application>
                              <component name="testin.settings.AppSettingsState">
                                <option name="rootTestinPath" value="$sampleRoot" />
                                <option name="logLevel" value="DEBUG" />
                                <option name="testerName" value="Testin Sample" />
                                <option name="testerRole" value="QA Engineer" />
                              </component>
                            </application>
                            """.trimIndent()
                        )
                        logger.lifecycle("Pointed a fresh sandbox at the sample data: ${sandbox.name}")
                    }

                    // Every run, not only the first: the IDE reopens its last project on
                    // its own, and the argument is what makes that the sample copy rather
                    // than whatever was open before - the committed sample included.
                    args(openedProject)
                }
        }
    }
}

/**
 * Nothing from the test source root, and nothing compile-only, may reach the
 * published plugin (#171).
 *
 * CLAUDE.md has said so in prose since the beginning, and nothing checked it. It
 * holds today because Gradle's jar takes the main output and nothing has asked it
 * for more - a default, not a decision. A sourceSets edit, a from() added to
 * bundle a fixture, or a dependency moved off compileOnly would all change it
 * without a word, and the artifact is public and downloaded rather than reviewed.
 *
 * Matched on what compileTestJava actually produced, never on names. This plugin
 * is about test cases, so a check that looked for the word "test" would fail on
 * TestSetEditor, TestCaseCard and sixty others, and be switched off within a week.
 */
// compileOnly cannot be resolved - it is a declaration bucket - so the check
// resolves a configuration that extends it and can be.
val compileOnlyArtifacts: Configuration = configurations.create("compileOnlyArtifacts") {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations["compileOnly"])
}

tasks.register("verifyDistribution") {
    group = "verification"
    description = "Fails if a test class or a compile-only dependency reached the plugin distribution"

    // Locals inside the block, deliberately: a script-level val referenced from a
    // task action drags the whole build script into the configuration cache and
    // fails with "cannot serialize Gradle script object references".
    val distributionZip = tasks.named<Zip>("buildPlugin").flatMap { it.archiveFile }
    val testClassDirs: FileCollection = sourceSets["test"].output.classesDirs
    val forbiddenJars = compileOnlyArtifacts.elements.map { jars -> jars.map { it.asFile.name }.toSet() }

    inputs.file(distributionZip)
    inputs.files(testClassDirs)

    doLast {
        // Every path compileTestJava wrote, as it would appear inside a jar.
        val testEntries = testClassDirs.files
            .filter { it.isDirectory }
            .flatMap { dir ->
                dir.walkTopDown().filter { it.isFile }.map { it.relativeTo(dir).invariantSeparatorsPath }.toList()
            }
            .toSet()

        val forbidden = forbiddenJars.get()
        val offences = mutableListOf<String>()

        ZipFile(distributionZip.get().asFile).use { outer ->
            outer.entries().asSequence().filter { it.name.endsWith(".jar") }.toList().forEach { entry ->
                // A compile-only dependency is named, so its jar arriving at all
                // is the whole finding - Lombok is 2 MB of nothing anyone runs.
                if (entry.name.substringAfterLast('/') in forbidden) {
                    offences += "${entry.name} is a compileOnly dependency and must not be packaged"
                }

                // lib/ and lib/modules/ alike: the content modules are built by
                // their own composedJar tasks and are just as able to carry a
                // test class.
                ZipInputStream(outer.getInputStream(entry)).use { jar ->
                    generateSequence { jar.nextEntry }
                        .filter { it.name in testEntries }
                        .forEach { offences += "${entry.name} carries ${it.name} from the test source root" }
                }
            }
        }

        if (offences.isEmpty()) {
            logger.lifecycle("Distribution is clean: no test classes, no compile-only dependencies.")
            return@doLast
        }

        offences.forEach { logger.error("  $it") }
        throw GradleException("${offences.size} thing(s) reached the distribution that must not be published. See above.")
    }
}

/**
 * The inspection gate, as one command on any operating system (#106).
 *
 * The findings the editor shows exist only inside the IDE: `./gradlew compileJava`
 * reports zero warnings because everything the profile finds is an IntelliJ
 * inspection rather than a javac one. `tools/inspect.ps1` produces the same list
 * on the command line, and this is the way to call it that does not need to be
 * remembered - by CI, or by anyone who has not read the script.
 *
 * It still needs `pwsh`, which installs on macOS and Linux as readily as on
 * Windows and is what the script's own `#Requires -Version 7` asks for. The
 * script itself no longer assumes Windows: it finds the downloaded IDE by the
 * suffix of the platform it is running on and launches `inspect.sh` where there
 * is no `inspect.bat`.
 *
 * Costs one full indexing pass, so 10-20 minutes. A deliberate sweep, not a
 * per-commit gate.
 */
tasks.register<Exec>("inspect") {
    group = "verification"
    description = "Runs the IntelliJ inspections over the Inspected scope and writes .inspection/ (needs pwsh)"

    commandLine("pwsh", "-NoProfile", "-File", file("tools/inspect.ps1").absolutePath)

    // The script exits non-zero for DataFlowIssue, ReturnNull and
    // WrappedMethodDeclaration, and that result is the point of running it -
    // so it is the task's result too.
    isIgnoreExitValue = false
}

