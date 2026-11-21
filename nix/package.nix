# Zettelkasten as a Nix package: the shaded JAR from the Maven build, started with the pinned JDK.
# REV names the source revision; it replaces the Git metadata that git-commit-id-plugin reads from .git,
# which a Nix source tree does not have. MVNFETCHEXTRAARGS goes to the dependency download only
# (e.g. a JAVA_TOOL_OPTIONS truststore behind a TLS-intercepting proxy); it does not change the output.
{ lib, maven, jdk, makeWrapper, rev ? "unknown", mvnFetchExtraArgs ? { } }:
maven.buildMavenPackage {
  pname = "zettelkasten";
  version = "3.2026.11";

  src = lib.cleanSourceWith {
    src = ../.;
    filter = path: type:
      let base = baseNameOf path; in
      !(lib.elem base [ "target" "result" ".idea" ]) && !(lib.hasSuffix ".nix" base);
  };

  inherit mvnFetchExtraArgs;
  mvnHash = "sha256-Y6VZgQo8m5ZcaSp1QoBeAafgoccR8dwQBR41VSGToQw=";
  mvnParameters = lib.escapeShellArgs [
    "-Dmaven.gitcommitid.skip=true"
    "-Dgit.commit.id=${rev}"
    "-Dgit.commit.id.abbrev=${builtins.substring 0 7 rev}"
    "-Dgit.branch=nix"
  ];

  nativeBuildInputs = [ makeWrapper ];

  installPhase = ''
    runHook preInstall
    install -Dm644 target/Zettelkasten.jar $out/share/zettelkasten/Zettelkasten.jar
    makeWrapper ${jdk}/bin/java $out/bin/zettelkasten \
      --add-flags "-jar $out/share/zettelkasten/Zettelkasten.jar"
    runHook postInstall
  '';

  meta = {
    description = "Zettelkasten nach Niklas Luhmann (Swing)";
    homepage = "https://github.com/RalfBarkow/zettelkasten";
    license = lib.licenses.gpl3Plus;
    mainProgram = "zettelkasten";
    platforms = lib.platforms.unix;
  };
}
