// The Robot Framework half of Testin: the gutter mark beside a Robot test's
// [Tags] (#2). It depends on no plugin, since it reads a .robot file as text,
// so it works with RobotCode, with another Robot plugin, or with none.
plugins {
    id("java")
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(providers.gradleProperty("intellij.version"))
    }

    // The core, which ships beside this module rather than inside it - so
    // compileOnly. The dependency runs one way only: this module calls the
    // core's AbstractTestCaseGutter and TestName, and the core never names it.
    compileOnly(project(":"))

    // Same rule as the root build: a compile-time tool, never packaged.
    listOf(
        "compileOnly",
        "annotationProcessor",
        "testAnnotationProcessor"
    ).forEach { configuration ->
        add(configuration, libs.lombok)
    }

    testImplementation(libs.testng)
}

// The tests of this module's classes live here too, and they need everything
// the classes compile against - the core above all - on their classpath, where
// it runs as well. One declaration for both, so the two cannot drift apart.
configurations.testImplementation {
    extendsFrom(configurations.compileOnly.get())
}

tasks.withType<Test> {
    useTestNG()
    jvmArgs("--sun-misc-unsafe-memory-access=allow")

    // A --tests filter is applied to every module, and a module with no matching
    // test fails rather than being skipped - so `./gradlew test --tests
    // "*DocumentClaimsTest*"` was a red build naming the filter rather than the
    // reason, which reads like the test does not exist (#66, finding 62). The
    // root module keeps the default: nearly every test lives there, so a filter
    // that matches nothing at all is still a misspelling worth failing on.
    filter {
        isFailOnNoMatchingTests = false
    }

    testLogging {
        events("passed", "skipped", "failed")
    }
}
