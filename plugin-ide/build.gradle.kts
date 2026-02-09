plugins {
    id("org.jetbrains.intellij")
}

intellij {
    version.set("2023.3.4")
    type.set("IC")
    instrumentCode.set(false)
}

dependencies {
    implementation(project(":plugin-core"))
    implementation(project(":plugin-ui"))
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("241.*")
    }
    instrumentCode {
        enabled = false
    }
    instrumentTestCode {
        enabled = false
    }
}
