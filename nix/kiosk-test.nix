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

  testScript = ''
      start_all()
      with subtest("the kiosk starts Zettelkasten as its unprivileged user"):
          machine.wait_for_unit("cage-tty1.service")
          machine.wait_until_succeeds("pgrep -u zettelkasten -f Zettelkasten.jar", timeout=900)
      with subtest("its window is on the screen"):
          machine.wait_for_text("Zettelkasten", timeout=1200)
          machine.screenshot("zettelkasten-kiosk")
      with subtest("no port is listening"):
          print(machine.succeed("ss -tulpnH"))
          machine.fail("ss -tlnH | grep -v '127.0.0.1\\|\\[::1\\]' | grep -q .")
    '';
}
