# NixOS VM test: the kiosk boots, Zettelkasten opens its window, and a screenshot records it.
{ pkgs, kioskModule, zettelkasten }:
pkgs.testers.runNixOSTest {
  name = "zettelkasten-kiosk";

  nodes.machine = { ... }: {
    imports = [ kioskModule ];
    services.zettelkasten-kiosk = {
      enable = true;
      package = zettelkasten;
    };
    virtualisation.memorySize = 2048;
    # cage needs a virtio GPU instead of QEMU's default VGA.
    virtualisation.qemu.options = [ "-vga none -device virtio-gpu-pci" ];
  };

  enableOCR = true;

  testScript = { nodes, ... }:
    let uid = toString nodes.machine.users.users.zettelkasten.uid;
    in ''
      start_all()
      with subtest("the kiosk starts Zettelkasten"):
          machine.wait_for_file("/run/user/${uid}/wayland-0.lock")
          machine.wait_until_succeeds("pgrep -f Zettelkasten.jar", timeout=600)
      with subtest("its window is on the screen"):
          machine.wait_for_text("Zettelkasten", timeout=1200)
          machine.screenshot("zettelkasten-kiosk")
      with subtest("no port is listening"):
          print(machine.succeed("ss -tulpnH"))
          machine.fail("ss -tlnH | grep -v '127.0.0.1\\|\\[::1\\]' | grep -q .")
    '';
}
