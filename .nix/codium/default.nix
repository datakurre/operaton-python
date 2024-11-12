{
  vscode-with-extensions,
  vscode-extensions,
  vscode-marketplace,
  vscode-utils,
  vscodium,
  ruff,
  lib,
  enableVim ? false,
}:

vscode-with-extensions.override {
  vscode = vscodium;
  vscodeExtensions = [
    vscode-extensions.bbenoist.nix
    vscode-extensions.ms-pyright.pyright
    vscode-extensions.ms-python.debugpy
    vscode-extensions.ms-python.python
    vscode-extensions.ms-vscode.makefile-tools
    vscode-extensions.redhat.java
    vscode-extensions.redhat.vscode-xml
    vscode-extensions.vscjava.vscode-java-debug
    vscode-extensions.vscjava.vscode-java-dependency
    vscode-marketplace.d-biehl.robotcode
    vscode-marketplace.miragon-gmbh.vs-code-bpmn-modeler
    (vscode-marketplace.charliermarsh.ruff.overrideAttrs (old: {
      postInstall = ''
        rm -f $out/share/vscode/extensions/charliermarsh.ruff/bundled/libs/bin/ruff
        ln -s ${ruff}/bin/ruff $out/share/vscode/extensions/charliermarsh.ruff/bundled/libs/bin/ruff
      '';
    }))
  ] ++ lib.optionals enableVim [ vscode-extensions.vscodevim.vim ];
}
