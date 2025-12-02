import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.net.URI
import java.util.zip.GZIPInputStream

plugins {
  id("java")
  id("org.springframework.boot") version "4.0.0"
  id("io.freefair.lombok") version "9.1.0"
}

group = "io.poddeck"
version = "1.0.0-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_25
java.targetCompatibility = JavaVersion.VERSION_25

repositories {
  mavenCentral()
  maven {
    url = uri("https://maven.pkg.github.com/poddeck/poddeck-common")
    credentials {
      username = System.getenv("GITHUB_USERNAME") ?: findProperty("github.username") as String?
      password = System.getenv("GITHUB_TOKEN") ?: findProperty("github.token") as String?
    }
  }
}

dependencies {
  testImplementation(platform("org.junit:junit-bom:6.0.1"))
  testImplementation("org.junit.jupiter:junit-jupiter:6.0.1")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.1")

  implementation("com.google.guava:guava:33.5.0-jre")

  implementation("org.projectlombok:lombok:1.18.42")
  annotationProcessor("org.projectlombok:lombok:1.18.42")
  testImplementation("org.projectlombok:lombok:1.18.42")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.42")

  implementation("org.json:json:20250517")

  implementation("org.apache.commons:commons-configuration2:2.13.0")
  implementation("commons-beanutils:commons-beanutils:1.11.0")

  implementation("org.postgresql:postgresql:42.7.8")
  implementation("org.hibernate.orm:hibernate-core:7.1.10.Final")
  implementation("org.reflections:reflections:0.10.2")

  implementation("io.grpc:grpc-stub:1.77.0")
  implementation("io.grpc:grpc-protobuf:1.77.0")
  implementation("io.grpc:grpc-netty:1.77.0")

  implementation("org.springframework.boot:spring-boot-starter-web:4.0.0")
  implementation("org.springframework:spring-core:7.0.1")
  implementation("org.springframework.data:spring-data-jpa:4.0.0-M6")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.0.0")
  implementation("com.h2database:h2:2.4.240")

  implementation("de.mkammerer:argon2-jvm:2.12")

  implementation("io.jsonwebtoken:jjwt:0.13.0")

  implementation("com.sun.mail:javax.mail:1.6.2")

  implementation("dev.samstevens.totp:totp:1.7.1")

  implementation("com.maxmind.geoip2:geoip2:5.0.0") {
    exclude(group = "commons-logging", module = "commons-logging")
  }

  implementation("io.poddeck:common:1.0.0-SNAPSHOT")

  implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")
}

tasks.test {
  useJUnitPlatform()
}

tasks.bootJar {
  mainClass = "io.poddeck.core.CoreApplication"
}

tasks.register("downloadGeoLite2Database") {
  val licenseKey = System.getenv("GEOLITE2_LICENSE_KEY") ?:
    findProperty("geolite2.license.key") as String?
  val databaseUrl = "https://download.maxmind.com/app/geoip_download?" +
    "edition_id=GeoLite2-City&license_key=$licenseKey&suffix=tar.gz"
  val resourcesDir = File("geo")
  val downloadFile = layout.buildDirectory.file("GeoLite2-City.tar.gz").get().asFile
  doLast {
    resourcesDir.mkdirs()
    downloadFile.parentFile.mkdirs()
    if (downloadFile.exists()) {
      downloadFile.delete()
    }
    URI(databaseUrl).toURL().openStream().use { input ->
      downloadFile.outputStream().use { output ->
        input.copyTo(output)
      }
    }
    extract(downloadFile, resourcesDir)
    downloadFile.delete()
  }
}

fun extract(file: File, destination: File) {
  GZIPInputStream(file.inputStream()).use { gis ->
    TarArchiveInputStream(gis).use { tis ->
      var entry = tis.nextEntry
      while (entry != null) {
        if (!entry.isDirectory && entry.name.endsWith(".mmdb")) {
          val outputFile = File(destination, "GeoLite2-City.mmdb")
          outputFile.outputStream().use { os ->
            tis.copyTo(os)
          }
        }
        entry = tis.nextEntry
      }
    }
  }
}