{
  description = "BPMN Robot Testing Environment";

  nixConfig = {
    extra-trusted-public-keys = "datakurre.cachix.org-1:ayZJTy5BDd8K4PW9uc9LHV+WCsdi/fu1ETIYZMooK78=";
    extra-substituters = "https://datakurre.cachix.org";
  };

  inputs = {
    flake-utils.url = "github:numtide/flake-utils";
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-24.05";
    nixpkgs-unstable.url = "github:NixOS/nixpkgs/master";
    gitignore = {
      url = "github:hercules-ci/gitignore.nix";
      inputs.nixpkgs.follows = "nixpkgs";
    };
    nix-vscode-extensions = {
      url = "github:nix-community/nix-vscode-extensions";
      inputs.nixpkgs.follows = "nixpkgs";
      inputs.flake-utils.follows = "flake-utils";
    };
    poetry2nix = {
      url = "github:nix-community/poetry2nix";
      inputs.nixpkgs.follows = "nixpkgs";
      inputs.flake-utils.follows = "flake-utils";
    };
  };

  outputs =
    { self, ... }@inputs:
    inputs.flake-utils.lib.eachDefaultSystem (
      system:
      let
        pkgs = import inputs.nixpkgs {
          inherit system;
          overlays = [
            inputs.poetry2nix.overlays.default
            (final: prev: {
              gitignoreSource = inputs.gitignore.lib.gitignoreSource;
              inherit (pkgs-unstable)
                graalvm-ce
                graalvmCEPackages
                maven
                ruff
                vscode
                vscode-extensions
                vscode-with-extensions
                vscodium
                ;
              inherit (inputs.nix-vscode-extensions.extensions.${system})
                vscode-marketplace
                vscode-marketplace-release
                ;
            })
          ];
        };
        pkgs-unstable = import inputs.nixpkgs-unstable { inherit system; };
        python = "python311";
      in
      {

        apps = {
          code = {
            type = "app";
            program = self.packages.${system}.code + "/bin/code";
          };
          code-vim = {
            type = "app";
            program = self.packages.${system}.code-vim + "/bin/code";
          };
          codium = {
            type = "app";
            program = self.packages.${system}.code + "/bin/codium";
          };
          codium-vim = {
            type = "app";
            program = self.packages.${system}.code-vim + "/bin/codium";
          };
        };

        packages = {
          robot-env = pkgs.callPackage ./.nix/robot-env { python3 = builtins.getAttr python pkgs; };
          code = pkgs.callPackage ./.nix/code { };
          code-vim = pkgs.callPackage ./.nix/code { enableVim = true; };
          codium = pkgs.callPackage ./.nix/codium { };
          codium-vim = pkgs.callPackage ./.nix/codium { enableVim = true; };
        };

        devShells.default = pkgs.mkShell {
          buildInputs = [
            pkgs.maven
            pkgs.graalvm-ce
            pkgs.graalvmCEPackages.graalpy
          ];
        };

        devShells.code = pkgs.mkShell {
          buildInputs = [
            self.packages.${system}.code
            self.packages.${system}.robot-env
            (pkgs.poetry.override { python3 = builtins.getAttr python pkgs; })
            pkgs.maven
            pkgs.graalvm-ce
            pkgs.graalvmCEPackages.graalpy
          ];
        };

        devShells.code-with-vim = pkgs.mkShell {
          buildInputs = [
            self.packages.${system}.code-vim
            self.packages.${system}.robot-env
            (pkgs.poetry.override { python3 = builtins.getAttr python pkgs; })
            pkgs.maven
            pkgs.graalvm-ce
            pkgs.graalvmCEPackages.graalpy
          ];
        };

        devShells.codium = pkgs.mkShell {
          buildInputs = [
            self.packages.${system}.codium
            self.packages.${system}.robot-env
            (pkgs.poetry.override { python3 = builtins.getAttr python pkgs; })
            pkgs.maven
            pkgs.graalvm-ce
            pkgs.graalvmCEPackages.graalpy
          ];
        };

        devShells.codium-with-vim = pkgs.mkShell {
          buildInputs = [
            self.packages.${system}.codium-vim
            self.packages.${system}.robot-env
            (pkgs.poetry.override { python3 = builtins.getAttr python pkgs; })
            pkgs.maven
            pkgs.graalvm-ce
            pkgs.graalvmCEPackages.graalpy
          ];
        };

        devShells.poetry = pkgs.mkShell {
          buildInputs = [ (pkgs.poetry.override { python3 = builtins.getAttr python pkgs; }) ];
        };

        formatter = pkgs.nixfmt-rfc-style;

      }
    );
}
