scalaVersion := "2.11.12"

enablePlugins(GitVersioning, GitBranchPrompt)

// Set to false or remove if you want to show stubs as linking errors
nativeLinkStubs := true

enablePlugins(ScalaNativePlugin)

libraryDependencies += "com.github.scopt" %%% "scopt" % "4.0.0-RC2"

git.useGitDescribe := true
organizationName := "Oswaldo C. Dantas Jr"
startYear := Some(2019)
headerLicense := Some(
  HeaderLicense
    .MIT("2019", "Oswaldo C. Dantas Jr", HeaderLicenseStyle.SpdxSyntax)
)