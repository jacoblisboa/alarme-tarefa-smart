import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    // Nativos do Compose para Windows e Linux: o mesmo uber jar roda nos dois sistemas
    implementation(compose.desktop.windows_x64)
    implementation(compose.desktop.linux_x64)
    implementation(libs.kotlinx.coroutinesSwing)
    implementation(libs.quartz)
    implementation(libs.vlcj)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "br.com.yaakovbarabraham.ats.DesktopMainKt"
        jvmArgs += "-Djna.library.path=C:\\Program Files\\VideoLAN\\VLC"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "br.com.yaakovbarabraham.ats"
            packageVersion = "1.0.0"
        }
    }
}