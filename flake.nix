{
  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    nixpkgs_clj_1_8_0 = {
      url = "github:nixos/nixpkgs/b459c8475c58f7c8d529ebeadcbebbfe00ddf6eb";
      flake = false;
    };
    flake-utils.url = "github:numtide/flake-utils";
  };
  outputs = { self, nixpkgs, nixpkgs_clj_1_8_0, flake-utils }:
    flake-utils.lib.eachDefaultSystem
      (system:
        let
          pkgs = import nixpkgs {
            inherit system;
          };
          pkgs_clj_1_8_0 = import nixpkgs_clj_1_8_0 {
            inherit system;
          };
        in
        with pkgs;
        {
          devShells.default = mkShell {
            buildInputs = [
              (leiningen.override { jdk = jdk8; })
              pkgs_clj_1_8_0.clojure
            ];
          };
        }
      );
}
