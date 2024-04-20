import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("jvm")
    alias(libs.plugins.mavenPublish)
}

mavenPublishing {
    signAllPublications()
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    coordinates(artifactId =  "core")
}
