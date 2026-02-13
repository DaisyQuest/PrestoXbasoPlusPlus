plugins {
    id("org.jetbrains.intellij.platform")
}

repositories {
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation(project(":plugin-core"))
    implementation(project(":plugin-ui"))
    implementation("com.google.code.gson:gson:2.10.1")

    intellijPlatform {
        intellijIdeaCommunity("2023.3.4")
        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = "241.*"
        }
    }

    instrumentCode = false
}
