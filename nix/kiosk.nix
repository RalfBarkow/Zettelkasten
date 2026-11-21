# A computer that shows only Zettelkasten: the cage Wayland kiosk starts it for one unprivileged user
# at boot, without a desktop, login screen or terminal. Without remoteScreen the module opens no port
# and starts no network service; if the Zettelkasten window closes, cage ends and systemd starts it again.
#
# remoteScreen adds a view-only remote screen: wayvnc, started inside the kiosk session, offers the screen
# as VNC over WebSocket. It requires authentication (RSA-AES with user name and password) and accepts no
# keyboard or mouse input. The credentials and the RSA key are not part of this configuration: an operator
# provisions them into secretsDirectory. Until all three files are there, owned by the kiosk user and
# readable by nobody else, wayvnc is not started and nothing listens.
{ config, lib, pkgs, ... }:
let
  cfg = config.services.zettelkasten-kiosk;
  rs = cfg.remoteScreen;

  session = pkgs.writeShellScript "zettelkasten-kiosk-session" ''
    set -u
    start_remote_screen() {
      dir=${lib.escapeShellArg rs.secretsDirectory}
      for f in username password rsa_key.pem; do
        if [ ! -f "$dir/$f" ]; then
          echo "remote screen refused: $dir/$f is missing" >&2
          return 1
        fi
        if [ "$(${pkgs.coreutils}/bin/stat -c %U "$dir/$f")" != "$(${pkgs.coreutils}/bin/id -un)" ]; then
          echo "remote screen refused: $dir/$f is not owned by $(${pkgs.coreutils}/bin/id -un)" >&2
          return 1
        fi
        case "$(${pkgs.coreutils}/bin/stat -c %a "$dir/$f")" in
          ?00) ;;
          *) echo "remote screen refused: $dir/$f is readable by others" >&2; return 1 ;;
        esac
      done
      user=$(${pkgs.coreutils}/bin/head -n1 "$dir/username")
      pass=$(${pkgs.coreutils}/bin/head -n1 "$dir/password")
      if [ -z "$user" ] || [ -z "$pass" ]; then
        echo "remote screen refused: empty user name or password" >&2
        return 1
      fi
      # The configuration holds the password, so it lives in the session's private runtime directory,
      # never in the Nix store.
      umask 077
      conf="$XDG_RUNTIME_DIR/zettelkasten-remote-screen.conf"
      {
        echo "address=${rs.address}"
        echo "port=${toString rs.port}"
        echo "enable_auth=true"
        echo "relax_encryption=false"
        echo "username=$user"
        echo "password=$pass"
        echo "rsa_private_key_file=$dir/rsa_key.pem"
      } > "$conf"
      ${pkgs.wayvnc}/bin/wayvnc --config="$conf" --websocket --disable-input &
      echo "remote screen: wayvnc started (view-only, authentication required) on ${rs.address}:${toString rs.port}" >&2
    }
    ${lib.optionalString rs.enable "start_remote_screen || true"}
    exec ${lib.getExe cfg.package}
  '';
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

    remoteScreen = {
      enable = lib.mkEnableOption "a view-only remote screen over WebSocket with required RSA-AES authentication";
      address = lib.mkOption {
        type = lib.types.str;
        default = "127.0.0.1";
        description = "Address wayvnc listens on. Keep it on loopback unless a VM forwards only from the host's loopback.";
      };
      port = lib.mkOption {
        type = lib.types.port;
        default = 5900;
        description = "Port wayvnc listens on (VNC over WebSocket).";
      };
      secretsDirectory = lib.mkOption {
        type = lib.types.str;
        default = "/var/lib/zettelkasten-kiosk/remote-screen";
        description = ''
          Directory with the files username, password and rsa_key.pem (PEM RSA private key), each owned by
          the kiosk user with mode 0600 or stricter. Provisioned by an operator; never in the Nix store.
        '';
      };
    };
  };

  config = lib.mkIf cfg.enable {
    users.users.${cfg.user} = {
      isNormalUser = true;
      description = "Zettelkasten kiosk";
    };

    fonts.packages = [ pkgs.dejavu_fonts ];

    systemd.tmpfiles.rules = lib.optionals rs.enable [
      "d ${rs.secretsDirectory} 0700 ${cfg.user} - -"
    ];

    services.cage = {
      enable = true;
      user = cfg.user;
      program = session;
      # Swing runs under Xwayland; without a reparenting window manager it needs this to draw its window.
      environment._JAVA_AWT_WM_NONREPARENTING = "1";
    };
  };
}
