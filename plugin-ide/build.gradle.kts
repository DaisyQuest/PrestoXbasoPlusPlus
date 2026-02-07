plugins {
    id("org.jetbrains.intellij")
}

intellij {
    version.set("2023.3.4")
    type.set("IC")
}

dependencies {
    implementation(project(":plugin-core"))
    implementation(project(":plugin-ui"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        untilBuild.set("241.*")
    }
}
