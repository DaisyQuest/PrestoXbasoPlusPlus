plugins {
    `java-library`
}

dependencies {
    api(kotlin("stdlib"))
    implementation("org.yaml:snakeyaml:2.2")
    testImplementation("org.assertj:assertj-core:3.25.3")
}
