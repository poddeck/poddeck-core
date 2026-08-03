import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.net.URI
import java.util.zip.GZIPInputStream

plugins {
  id("java")
  id("org.springframework.boot") version "4.1.0"
  id("io.freefair.lombok") version "9.5.0"
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
  // Override Spring Boot 4.0.6's managed Tomcat 11.0.21 to patch
  // CVE-2026-41293/-43512/-43515/-41284/-42498/-43513 (fixed in 11.0.22)
  constraints {
    implementation("org.apache.tomcat.embed:tomcat-embed-core:11.0.24")
    implementation("org.apache.tomcat.embed:tomcat-embed-el:11.0.24")
    implementation("org.apache.tomcat.embed:tomcat-embed-websocket:11.0.24")

    // Override Spring Boot 4.1.0's managed jackson-databind 2.21.3 to patch
    // CVE-2026-54512/-54513 (PolymorphicTypeValidator bypasses, fixed in 2.21.4).
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.1")
  }

  testImplementation(platform("org.junit:junit-bom:6.1.2"))
  testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.2")

  implementation("com.google.guava:guava:33.6.0-jre")

  implementation("org.projectlombok:lombok:1.18.46")
  annotationProcessor("org.projectlombok:lombok:1.18.46")
  testImplementation("org.projectlombok:lombok:1.18.46")
  testAnnotationProcessor("org.projectlombok:lombok:1.18.46")

  implementation("org.json:json:20260719")

  implementation("org.apache.commons:commons-configuration2:2.15.1")
  implementation("commons-beanutils:commons-beanutils:1.11.0")

  implementation("org.postgresql:postgresql:42.7.13")
  implementation("org.hibernate.orm:hibernate-core:7.4.5.Final")
  implementation("org.reflections:reflections:0.10.2")

  implementation("io.grpc:grpc-stub:1.83.1")
  implementation("io.grpc:grpc-protobuf:1.83.1")
  implementation("io.grpc:grpc-netty:1.83.1")

  implementation(platform("io.netty:netty-bom:4.2.16.Final"))
  implementation("com.google.protobuf:protobuf-java:4.35.1")
  implementation("com.google.protobuf:protobuf-java-util:4.35.1")

  implementation("org.springframework.boot:spring-boot-starter-web:4.1.0")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa:4.1.0")
  implementation("com.h2database:h2:2.4.240")

  implementation("de.mkammerer:argon2-jvm:2.12")

  implementation("io.jsonwebtoken:jjwt:0.13.0")

  implementation("com.sun.mail:javax.mail:1.6.2")

  implementation("dev.samstevens.totp:totp:1.7.1")

  implementation("com.maxmind.geoip2:geoip2:5.2.0") {
    exclude(group = "commons-logging", module = "commons-logging")
  }

  implementation("io.poddeck:common:1.0.0-SNAPSHOT")

  implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20260313.1")
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