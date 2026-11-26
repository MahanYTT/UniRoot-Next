package df.root;

import android.content.Context;
import android.net.IpSecAlgorithm;
import android.net.IpSecManager;
import android.net.IpSecTransform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.SecureRandom;

/**
 * "New method (fast)" engine — port of diabl0w/DFRoot (DirtyFrag CVE-2026-43284).
 * Public APIs only: IpSecManager allocates the socket + SPI + transform, the
 * native engine (libexp/libexpnext) patches the kernel page cache and ksud
 * is bind-mounted over logcat. No root, no Shizuku, no profile.
 *
 * NOTE: this is the EXACT 4.4.7 engine (the version whose manual Root now
 * was proven working on-device). The caller loads the native lib itself.
 */
public final class DFBridge {

    private static String loadedLib = null;

    private DFBridge() {}

    /** Loads the flavor-matching native engine (same JNI symbol in both). */
    public static synchronized void load(boolean next) {
        String want = next ? "expnext" : "exp";
        if (want.equals(loadedLib)) return;
        System.loadLibrary(want);
        loadedLib = want;
    }

    public static native int nativeRunAll(IReporter reporter, int encapPort, int spi,
                                          byte[] aesCbcKey, byte[] hmacKey, int icvLen,
                                          int senderPort, boolean softReboot);

    /** One full privileged-free root run. Returns the engine rc (0 = rooted). */
    public static int run(Context context, boolean next, boolean softReboot, IReporter reporter) {
        try {
            IpSecManager ipsec = (IpSecManager) context.getSystemService(Context.IPSEC_SERVICE);

            IpSecManager.UdpEncapsulationSocket encapSock = ipsec.openUdpEncapsulationSocket();
            int encapPort = encapSock.getPort();
            reporter.report("encap port: " + encapPort + "\n");

            InetAddress loopback = InetAddress.getByName("127.0.0.1");
            IpSecManager.SecurityParameterIndex spiObj = ipsec.allocateSecurityParameterIndex(loopback);
            int spiVal = spiObj.getSpi();
            reporter.report("spi: 0x" + Integer.toHexString(spiVal) + "\n");

            SecureRandom rng = new SecureRandom();
            byte[] aesKey = new byte[32]; rng.nextBytes(aesKey);
            byte[] hmacKey = new byte[32]; rng.nextBytes(hmacKey);

            IpSecAlgorithm enc = new IpSecAlgorithm(IpSecAlgorithm.CRYPT_AES_CBC, aesKey);
            IpSecAlgorithm auth = new IpSecAlgorithm(IpSecAlgorithm.AUTH_HMAC_SHA256, hmacKey, 128);

            DatagramSocket senderSock = new DatagramSocket();
            int senderPort = senderSock.getLocalPort();
            senderSock.close();

            IpSecTransform transform = new IpSecTransform.Builder(context)
                    .setEncryption(enc)
                    .setAuthentication(auth)
                    .setIpv4Encapsulation(encapSock, senderPort)
                    .buildTransportModeTransform(loopback, spiObj);

            stageKsud(context, next, softReboot, reporter, null);

            // The Next ksud CLI rejects --soft-reboot: soft-reboot is handled inside
            // the staged Next ksud binary itself (late-load -> emulated_soft_reboot).
            boolean sb = next ? false : softReboot;
            int rc = nativeRunAll(reporter, encapPort, spiVal, aesKey, hmacKey, 128 / 8, senderPort, sb);

            transform.close();
            spiObj.close();
            encapSock.close();
            return rc;
        } catch (Exception e) {
            reporter.report("\nexception: " + e + "\n");
            return -1;
        }
    }

    /** Stages the flavor's ksud (user-patched custom first, then bundled). */
    public static void stageKsud(Context context, boolean next, IReporter reporter) throws IOException {
        stageKsud(context, next, true, reporter, null);
    }

    /**
     * Same, with an explicit ksud Next profile asset — the id of a file in
     * assets/df/ksud-next/ picked on the home page (see KsudNextProfiles).
     * null = auto: selected profile if any, else the legacy bundled ksud.
     */
    public static void stageKsud(Context context, boolean next, IReporter reporter,
                                 String ksudNextAsset) throws IOException {
        stageKsud(context, next, true, reporter, ksudNextAsset);
    }

    public static File stagedBinaryFile(Context context, boolean next) {
        File root = context.getFilesDir().getParentFile();
        if (next) {
            File real = new File(root, "ksud-next-real");
            if (real.isFile()) return real;
        }
        return new File(root, "ksud");
    }

    public static void stageKsud(Context context, boolean next, boolean softReboot,
                                 IReporter reporter, String ksudNextAsset) throws IOException {
        File root = context.getFilesDir().getParentFile();
        File ksudDest = new File(root, "ksud");
        File ksudNextReal = new File(root, "ksud-next-real");
        // User-patched ksud (Advanced -> Patch custom ko): wins over bundled.
        File custom = new File(root, next ? "ksud-custom-next" : "ksud-custom-classic");
        if (custom.isFile() && custom.length() > 100_000) {
            if (next) {
                copyFile(custom, ksudNextReal);
                stageAsset(context, "df/ksud-wrapper-next.sh", ksudDest, reporter);
            } else {
                ksudNextReal.delete();
                copyFile(custom, ksudDest);
            }
            reporter.report("staged CUSTOM ksud (" + custom.length() + " bytes)\n");
            return;
        }
        ksudNextReal.delete();
        if (!next) {
            // Classic flavor: the SELECTED classic profile wins (a ksud
            // downloaded by DfKsudUpdater = ksud-classic-latest); the bundled
            // universal is the fallback.
            java.io.File dynClassic = com.uniroot.app.newmethod.KsudClassicProfiles.selectedFile(context);
            if (dynClassic != null && dynClassic.isFile()) {
                copyFile(dynClassic, ksudDest);
                reporter.report("staged DYNAMIC classic ksud (" + dynClassic.getName()
                        + ", " + dynClassic.length() + " bytes)\n");
                return;
            }
            stageAsset(context, "df/ksud-new-classic", ksudDest, reporter);
            return;
        }
        // Next: profile chosen on the home page (e.g. 3.3.0 / 3.4.0 / 3.4.2), else
        // the bundled one.
        // DYNAMIC profile (downloaded+patched by DfKsudUpdater): a plain file
        // in filesDir/ksud-next — staged verbatim.
        java.io.File dyn = com.uniroot.app.newmethod.KsudNextProfiles.selectedFile(context);
        if (dyn != null && dyn.isFile()) {
            copyFile(dyn, ksudDest);
            applyNextSoftRebootMode(ksudDest, softReboot);
            reporter.report("staged DYNAMIC ksud " + dyn.getName()
                    + " (" + dyn.length() + " bytes, softReboot=" + softReboot + ")\n");
            return;
        }
        String asset = ksudNextAsset != null
                ? ksudNextAsset
                : com.uniroot.app.newmethod.KsudNextProfiles.resolveAsset(context);
        if (asset == null) asset = "df/ksud-new-next";
        if (asset.contains("3.3.0")) {
            // 3.3.0-s938x-proven requires /data/local/tmp/.ksud-stage via wrapper
            stageAsset(context, asset, new File(root, "ksud-next-real"), reporter);
            stageAsset(context, "df/ksud-wrapper-next.sh", ksudDest, reporter);
            return;
        }
        stageAsset(context, asset, ksudDest, reporter);
        applyNextSoftRebootMode(ksudDest, softReboot);
    }

    /**
     * Toggles the late-load -> emulated_soft_reboot transition in ksud Next 3.4.0 / 3.4.2
     * according to the user's "Auto soft reboot" switch. When softReboot is true, late-load
     * transitions directly into emulated_soft_reboot (switch_mnt_ns(1) -> stop -> on_post_fs_data
     * -> start -> on_services -> on_boot_completed) so Zygisk (Vector), LSPosed and global
     * module mounts start cleanly.
     */
    private static void applyNextSoftRebootMode(File ksudFile, boolean softReboot) {
        try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(ksudFile, "rw")) {
            long len = raf.length();
            // ksud Next 3.4.2 @ file offset 0x521238 (vaddr 0x522238)
            if (len > 0x52123cL) {
                raf.seek(0x521238L);
                int cur = Integer.reverseBytes(raf.readInt());
                if (cur == 0x97ffd7a4 || cur == 0x17fff6ac) {
                    int desired = softReboot ? 0x17fff6ac : 0x97ffd7a4;
                    if (cur != desired) {
                        raf.seek(0x521238L);
                        raf.writeInt(Integer.reverseBytes(desired));
                    }
                    return;
                }
            }
            // ksud Next 3.4.0 @ file offset 0x3af7c8 (vaddr 0x3b07c8)
            if (len > 0x3af7ccL) {
                raf.seek(0x3af7c8L);
                int cur = Integer.reverseBytes(raf.readInt());
                if (cur == 0x97ffd858 || cur == 0x17fff6fb) {
                    int desired = softReboot ? 0x17fff6fb : 0x97ffd858;
                    if (cur != desired) {
                        raf.seek(0x3af7c8L);
                        raf.writeInt(Integer.reverseBytes(desired));
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void copyFile(File src, File dest) throws IOException {
        File tmp = new File(dest.getPath() + ".tmp");
        try (InputStream in = new java.io.FileInputStream(src);
             OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        }
        if (!tmp.renameTo(dest)) { tmp.delete(); throw new IOException("rename failed: " + dest); }
        dest.setExecutable(true, false);
    }

    private static void stageAsset(Context context, String asset, File dest, IReporter reporter) throws IOException {
        File tmp = new File(dest.getPath() + ".tmp");
        try (InputStream in = context.getAssets().open(asset);
             OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) out.write(buf, 0, n);
        }
        if (!tmp.renameTo(dest)) { tmp.delete(); throw new IOException("rename failed: " + dest); }
        dest.setExecutable(true, false);
        reporter.report("staged " + asset + " -> " + dest.getAbsolutePath() + "\n");
    }
}
