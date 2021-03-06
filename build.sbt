import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """codacy-duplication-phpcpd"""

version := "1.0.0-SNAPSHOT"

val languageVersion = "2.11.12"

scalaVersion := languageVersion

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.4.6" withSources(),
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4" withSources(),
  "com.codacy" %% "codacy-duplication-scala-seed" % "1.0.2"
)

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

mappings.in(Universal) ++= resourceDirectory.in(Compile).map { (resourceDir: File) =>
  val src = resourceDir / "docs"
  val dest = "/docs"

  for {
    path <- (src ***).get
    if !path.isDirectory
  } yield path -> path.toString.replaceFirst(src.toString, dest)
}.value

val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "frolvlad/alpine-oraclejdk8"

val installAll =
  s"""
     |export COMPOSER_HOME=/opt/composer &&
     |mkdir -p $$COMPOSER_HOME &&
     |apk update &&
     |apk add bash curl git php5 php5-xml php5-cli php5-pdo php5-curl php5-json php5-phar php5-ctype php5-openssl php5-dom &&
     |ln -s /usr/bin/php5 /usr/bin/php &&
     |curl -sS https://getcomposer.org/installer | php -- --install-dir=/bin --filename=composer &&
     |composer global require "sebastian/phpcpd=2.0.1" &&
     |chmod -R 777 /opt &&
     |ln -s /opt/composer/vendor/bin/phpcpd /bin/phpcpd
   """.stripMargin.replaceAll(System.lineSeparator(), " ")

dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("WORKDIR", _) => List(cmd,
    Cmd("RUN", installAll)
  )

  case cmd@(Cmd("ADD", "opt /opt")) => List(cmd,
    Cmd("RUN", "adduser -u 2004 -D docker")
  )
  case other => List(other)
}
