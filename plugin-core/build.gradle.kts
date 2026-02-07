plugins {
    `java-library`
}

dependencies {
    api(kotlin("stdlib"))
    testImplementation(project(":test-framework"))
}
