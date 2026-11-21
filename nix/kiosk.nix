# A computer that shows only Zettelkasten: the cage Wayland kiosk starts it for one unprivileged user
# at boot, without a desktop, login screen or terminal. The module opens no port and starts no network
# service; if the Zettelkasten window closes, cage ends and systemd starts it again.
{ config, lib, pkgs, ... }:
let
  cfg = config.services.zettelkasten-kiosk;
in {
  options.services.zettelkasten-kiosk = {
    enable = lib.mkEnableOption "a kiosk that shows only Zettelkasten";
    package = lib.mkOption {
      type = lib.types.package;
      description = "The Zettelkasten package to run.";
    };
    user = lib.mkOption {
      type = lib.types.str;
      default = "zettelkasten";
      description = "The unprivileged user that runs the kiosk; its home holds the Zettelkasten settings and data.";
    };
  };

  config = lib.mkIf cfg.enable {
    users.users.${cfg.user} = {
      isNormalUser = true;
      description = "Zettelkasten kiosk";
    };

    fonts.packages = [ pkgs.dejavu_fonts ];

    services.cage = {
      enable = true;
      user = cfg.user;
      program = lib.getExe cfg.package;
      # Swing runs under Xwayland; without a reparenting window manager it needs this to draw its window.
      environment._JAVA_AWT_WM_NONREPARENTING = "1";
    };
  };
}
