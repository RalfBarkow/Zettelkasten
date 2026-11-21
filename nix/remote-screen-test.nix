# NixOS VM test of the view-only remote screen: the security contract, checked adversarially.
#  1. Without credentials, and with credentials readable by others, wayvnc is not started and nothing
#     listens on 5900.
#  2. With credentials provisioned at run time (never in the Nix store), wayvnc listens on 127.0.0.1 only,
#     and the password is on no command line.
#  3. A page on a foreign origin (http://127.0.0.2:6081) in headless Chromium gets no usable session:
#     no anonymous security type, no framebuffer bytes, no 'connect' without, or with wrong, credentials.
#  4. The legitimate viewer (http://127.0.0.1:6080, noVNC 1.4) verifies the server key's fingerprint, logs
#     in with RSA-AES, and its capture shows Zettelkasten (OCR).
#  5. Keystrokes sent over that authenticated session do not reach Zettelkasten; the same keystrokes typed
#     on the VM's own keyboard do (positive control, seen through the same capture path).
{ pkgs, kioskModule, zettelkasten }:
let
  webroot = name: pkgs.linkFarm "remote-screen-${name}-root" [
    { name = "novnc"; path = "${pkgs.novnc}/share/webapps/novnc"; }
    { name = "${name}.html"; path = ./remote-screen-test + "/${name}.html"; }
    { name = "foreign-guess.json"; path = ./remote-screen-test/foreign-guess.json; }
  ];
  server = ./remote-screen-test/server.py;
  decode = pkgs.writeText "decode-capture.py" ''
    import base64, json, sys
    data = json.load(open(sys.argv[1]))["png"]
    open(sys.argv[2], "wb").write(base64.b64decode(data.split(",", 1)[1]))
  '';
  fingerprint = pkgs.writeText "fingerprint.py" ''
    # SHA-256 over the server key as RA2 sends it: key length in bits (u32), n and e, each padded to
    # the key's byte length. The legitimate viewer pins this value.
    import hashlib, re, subprocess, sys
    text = subprocess.run(["openssl", "rsa", "-in", sys.argv[1], "-noout", "-text"],
                          capture_output=True, text=True, check=True).stdout
    n = int(subprocess.run(["openssl", "rsa", "-in", sys.argv[1], "-noout", "-modulus"],
                           capture_output=True, text=True, check=True).stdout.split("=")[1], 16)
    e = int(re.search(r"publicExponent: (\d+)", text).group(1))
    bits = n.bit_length(); size = (bits + 7) // 8
    blob = bits.to_bytes(4, "big") + n.to_bytes(size, "big") + e.to_bytes(size, "big")
    print(hashlib.sha256(blob).hexdigest())
  '';
in
pkgs.testers.runNixOSTest {
  name = "zettelkasten-remote-screen";

  nodes.machine = { pkgs, ... }: {
    imports = [ kioskModule ];
    services.zettelkasten-kiosk = {
      enable = true;
      package = zettelkasten;
      remoteScreen.enable = true;
    };
    environment.systemPackages = [ pkgs.chromium pkgs.python3 pkgs.openssl pkgs.tesseract ];
    virtualisation.memorySize = 4096;
    virtualisation.cores = 2;
    virtualisation.qemu.options = [ "-vga none -device virtio-gpu-pci" ];
  };

  testScript = ''
    import json

    secrets = "/var/lib/zettelkasten-kiosk/remote-screen"
    results = "/tmp/results"

    def restart_kiosk():
        machine.succeed("systemctl restart cage-tty1.service")
        machine.wait_until_succeeds("pgrep -u zettelkasten -f Zettelkasten.jar", timeout=900)

    def journal_since_restart():
        return machine.succeed("journalctl -u cage-tty1.service -b --no-pager -o cat")

    def ocr(json_name, png_name):
        machine.succeed(f"python3 ${decode} {results}/{json_name}.json /tmp/{png_name}.png")
        machine.copy_from_vm(f"/tmp/{png_name}.png", "")
        return machine.succeed(f"tesseract /tmp/{png_name}.png - 2>/dev/null")

    def chromium(url, tag):
        machine.succeed(
            f"systemd-run --unit=chromium-{tag} --setenv=HOME=/tmp/chromium-{tag} "
            f"chromium --headless=new --no-sandbox --disable-gpu --user-data-dir=/tmp/chromium-{tag} {url}")

    start_all()
    with subtest("the kiosk starts Zettelkasten"):
        machine.wait_for_unit("cage-tty1.service")
        machine.wait_until_succeeds("pgrep -u zettelkasten -f Zettelkasten.jar", timeout=900)

    with subtest("without credentials the remote screen is refused and nothing listens on 5900"):
        machine.wait_until_succeeds("journalctl -u cage-tty1.service -b -o cat | grep -q 'remote screen refused: .*/username is missing'")
        machine.fail("ss -tlnH | grep -q ':5900 '")

    with subtest("credentials readable by others are refused"):
        # Provisioning is the test's authorized operation; the secrets are made here, at run time.
        machine.succeed(
            f"printf zettelkasten > {secrets}/username",
            f"openssl rand -hex 24 > {secrets}/password",
            f"openssl genrsa -traditional -out {secrets}/rsa_key.pem 2048",
            f"chown zettelkasten {secrets}/*",
            f"chmod 0600 {secrets}/username {secrets}/rsa_key.pem",
            f"chmod 0644 {secrets}/password",
        )
        restart_kiosk()
        machine.wait_until_succeeds("journalctl -u cage-tty1.service -b -o cat | grep -q 'password is readable by others'")
        machine.fail("ss -tlnH | grep -q ':5900 '")

    with subtest("with credentials provisioned, wayvnc listens on loopback only and the password is on no command line"):
        machine.succeed(f"chmod 0600 {secrets}/password")
        restart_kiosk()
        machine.wait_until_succeeds("ss -tlnH | grep -q '127.0.0.1:5900 '", timeout=300)
        print(machine.succeed("ss -tlnpH"))
        machine.fail("ss -tlnH | grep -v '127.0.0.1:\\|\\[::1\\]:' | grep -q .")
        password = machine.succeed(f"cat {secrets}/password").strip()
        assert password not in machine.succeed("ps -eo args"), "password on a command line"
        assert "--disable-input" in machine.succeed("pgrep -a wayvnc")
        conf = machine.succeed("ls -l /run/user/*/zettelkasten-remote-screen.conf")
        assert conf.startswith("-rw-------"), conf

    with subtest("test web servers for two origins"):
        fp = machine.succeed(f"python3 ${fingerprint} {secrets}/rsa_key.pem").strip()
        machine.succeed(
            f"mkdir -p {results}",
            "install -m 0600 /dev/null /root/viewer-credentials.json",
            f"python3 -c 'import json; json.dump({{\"username\": \"zettelkasten\", \"password\": open(\"{secrets}/password\").read().strip(), \"fingerprint\": \"{fp}\"}}, open(\"/root/viewer-credentials.json\", \"w\"))'",
            f"systemd-run --unit=viewer-origin python3 ${server} 127.0.0.1 6080 ${webroot "viewer"} {results} --credentials /root/viewer-credentials.json",
            f"systemd-run --unit=foreign-origin python3 ${server} 127.0.0.2 6081 ${webroot "foreign"} {results}",
        )
        machine.wait_for_open_port(6080, "127.0.0.1")
        machine.wait_for_open_port(6081, "127.0.0.2")

    with subtest("a foreign origin gets no usable session"):
        chromium("http://127.0.0.2:6081/foreign.html", "foreign")
        machine.wait_for_file(f"{results}/foreign.json", timeout=900)
        machine.succeed("systemctl stop chromium-foreign")
        foreign = json.loads(machine.succeed(f"cat {results}/foreign.json"))
        print(json.dumps(foreign, indent=1))
        assert foreign["origin"] == "http://127.0.0.2:6081"
        raw = foreign["raw"]
        assert raw["opened"], "the WebSocket attempt itself is expected to open"
        assert raw["securityTypes"], "the server offered no security types"
        assert 1 not in raw["securityTypes"], f"anonymous access offered: {raw['securityTypes']}"
        assert 2 not in raw["securityTypes"], f"plain VNC password offered: {raw['securityTypes']}"
        assert raw["bytesAfterRequest"] < 64, f"framebuffer data reached the foreign page: {raw}"
        for case in ("nocreds", "wronguser", "wrongpass"):
            assert "connect" not in foreign[case], f"{case}: a session was established: {foreign[case]}"
        assert any("credentialsrequired" in e for e in foreign["nocreds"] if isinstance(e, dict)), foreign["nocreds"]

    with subtest("the legitimate viewer verifies the server key, logs in and sees Zettelkasten"):
        chromium("http://127.0.0.1:6080/viewer.html", "viewer")
        machine.wait_for_file(f"{results}/viewer.json", timeout=900)
        viewer = json.loads(machine.succeed(f"cat {results}/viewer.json"))
        print(json.dumps({k: v for k, v in viewer.items() if k != "png"}, indent=1))
        assert viewer["result"] == "connected", viewer
        assert {"serververification": fp} in viewer["events"], viewer["events"]
        text = ocr("viewer", "remote-screen-viewer")
        print(text)
        assert "Zettelkasten" in text, "Zettelkasten not visible through the remote screen"

    with subtest("input over the remote screen is dropped; local input is seen (positive control)"):
        machine.wait_for_file(f"{results}/viewer-after-input.json", timeout=600)
        after = ocr("viewer-after-input", "remote-screen-after-input")
        assert "probe" not in after.lower(), "keystrokes from the remote screen reached Zettelkasten"
        machine.send_chars("probe")
        machine.succeed(f"touch {results}/control-typed")
        machine.wait_for_file(f"{results}/viewer-control.json", timeout=600)
        control = ocr("viewer-control", "remote-screen-control")
        print(control)
        assert "probe" in control.lower(), "positive control failed: local keystrokes not seen, so the input check proves nothing"
  '';
}
