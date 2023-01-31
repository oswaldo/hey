scalaVersion := "3.2.2"

enablePlugins(GitVersioning, GitBranchPrompt)

// Set to false or remove if you want to show stubs as linking errors
nativeLinkStubs := true

enablePlugins(ScalaNativePlugin)

libraryDependencies += "com.github.scopt" %%% "scopt" % "4.1.0"
libraryDependencies += "org.ekrich" %%% "sconfig" % "1.5.0"
libraryDependencies += "org.ekrich" %%% "sjavatime" % "1.1.9"

git.useGitDescribe := true
organizationName := "Oswaldo C. Dantas Jr"
startYear := Some(2019)
headerLicense := Some(
  HeaderLicense
    .MIT("2019", "Oswaldo C. Dantas Jr", HeaderLicenseStyle.SpdxSyntax)
)
