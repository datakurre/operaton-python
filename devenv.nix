{ pkgs, ... }:
let
  shell =
    { pkgs, ... }:
    {
      packages = [
        pkgs.gnumake
        pkgs.google-java-format
        pkgs.nixfmt-rfc-style
        pkgs.prettier
        pkgs.treefmt
        pkgs.xmlformat
      ];

      enterTest = ''
        # Run all Maven tests
        mvn test
      '';
    };

  devcontainer =
    { ... }:
    {
      devcontainer.enable = true;
    };
in
{
  dotenv.enable = true;

  languages.java.enable = true;
  languages.java.jdk.package = pkgs.jdk21;

  profiles.shell.module = {
    imports = [ shell ];
  };

  profiles.devcontainer.module = {
    imports = [ devcontainer ];
  };
}
