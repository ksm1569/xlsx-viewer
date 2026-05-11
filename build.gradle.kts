import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // Apache POI: xlsx (OOXML) 파싱.
    // POI 5.x 는 log4j-api 를 직접 사용하므로 그룹 전체 제외 시 NoClassDefFoundError 발생.
    // 실제 출력 구현인 log4j-core 만 제외 → API 는 유지, log4j2 는 SimpleLogger fallback 으로 동작.
    // (log4j-to-slf4j 브리지는 IntelliJ Platform 의 slf4j 버전과 충돌하여 사용하지 않음)
    implementation("org.apache.poi:poi-ooxml:5.3.0") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-core")
    }

    // IntelliJ Platform 의존성 — gradle.properties 값에서 주입
    intellijPlatform {
        create(
            providers.gradleProperty("platformType"),
            providers.gradleProperty("platformVersion")
        )
        testFramework(TestFrameworkType.Platform)
    }
}

intellijPlatform {
    // Microsoft 배포판 OpenJDK 21(ms-21.0.11)과 instrumentCode 태스크 충돌 회피.
    // 우리는 .form 파일을 쓰지 않고 @NotNull 런타임 주입도 불필요하므로 비활성화.
    instrumentCode = false

    pluginConfiguration {
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }
}
