plugins {
    java
}

// Precisa do desktopApp configurado para referenciar a task do uber jar
evaluationDependsOn(":desktopApp")

val appUberJar = project(":desktopApp").tasks.named("packageUberJarForCurrentOS")

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName = "alarme-tarefa-smart-installer"
    manifest {
        attributes("Main-Class" to "br.com.yaakovbarabraham.ats.installer.Installer")
    }
    // Payload extraído pelo instalador: jar da aplicação + bat de execução
    from(appUberJar) {
        into("payload")
        rename { "alarme-tarefa-smart.jar" }
    }
    from("src/main/payload") {
        into("payload")
    }
}
