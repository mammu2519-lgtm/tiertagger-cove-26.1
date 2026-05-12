plugins {
    id("multiloader-base")
}

val common = project(":common")
val commonMain = common.sourceSets["main"]
val commonCompileJava = common.tasks.getByName<JavaCompile>(commonMain.compileJavaTaskName)
val commonProcessResources =
    common.tasks.getByName<ProcessResources>(commonMain.processResourcesTaskName)

dependencies {
    implementation(common)
}

sourceSets.main {
    runtimeClasspath += commonMain.runtimeClasspath
    compileClasspath += commonMain.compileClasspath
}

tasks {
    processResources {
        from(commonProcessResources.destinationDir)
        dependsOn(commonProcessResources)

        inputs.property("version", version)

        filesMatching(listOf("fabric.mod.json", "META-INF/neoforge.mods.toml")) {
            expand(mapOf("version" to inputs.properties["version"]))
        }
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.FAIL
        from(rootDir.resolve("LICENSE"))
        from(commonCompileJava.destinationDirectory)
    }
}
