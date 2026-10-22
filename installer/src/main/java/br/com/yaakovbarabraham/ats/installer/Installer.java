package br.com.yaakovbarabraham.ats.installer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** Instalador: extrai o jar da aplicação e o script de execução (bat no Windows, sh no Linux). */
public class Installer {

    private static final String TITLE = "Instalador - Alarme Tarefa Smart";
    private static final String APP_JAR = "alarme-tarefa-smart.jar";
    private static final String APP_BAT = "alarme-tarefa-smart.bat";
    private static final String APP_SH = "alarme-tarefa-smart.sh";
    private static final Path VLC_DIR = Path.of("C:\\Program Files\\VideoLAN\\VLC");
    private static final boolean WINDOWS = System.getProperty("os.name").toLowerCase().startsWith("windows");

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        Path installDir = chooseInstallDir();
        if (installDir == null) return;

        String scriptName = WINDOWS ? APP_BAT : APP_SH;
        Path script = installDir.resolve(scriptName);
        try {
            Files.createDirectories(installDir);
            extract(APP_JAR, installDir);
            extract(scriptName, installDir);
            if (!WINDOWS) script.toFile().setExecutable(true, false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Falha na instalação:\n" + e.getMessage(),
                    TITLE, JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        String question = WINDOWS ? "Criar atalho na Área de Trabalho?" : "Criar atalho no menu de aplicativos?";
        int shortcut = JOptionPane.showConfirmDialog(null, question, TITLE, JOptionPane.YES_NO_OPTION);
        if (shortcut == JOptionPane.YES_OPTION) {
            if (WINDOWS) createDesktopShortcut(script, installDir);
            else createLinuxDesktopEntry(script, installDir);
        }

        StringBuilder msg = new StringBuilder("Instalado em:\n" + installDir
                + "\n\nPara abrir, execute:\n" + script);
        if (!vlcInstalled()) {
            msg.append(WINDOWS
                    ? "\n\nAtenção: VLC 64-bit não encontrado em " + VLC_DIR + ".\nInstale-o para que o som do alarme funcione."
                    : "\n\nAtenção: libvlc não encontrada.\nInstale o VLC (ex.: sudo apt install vlc) para que o som do alarme funcione.");
        }
        JOptionPane.showMessageDialog(null, msg.toString(), TITLE, JOptionPane.INFORMATION_MESSAGE);
    }

    private static Path chooseInstallDir() {
        Path dir;
        if (WINDOWS) {
            String base = System.getenv("LOCALAPPDATA");
            if (base == null) base = System.getProperty("user.home");
            dir = Path.of(base, "AlarmeTarefaSmart");
        } else {
            dir = Path.of(System.getProperty("user.home"), ".local", "share", "AlarmeTarefaSmart");
        }

        while (true) {
            Object[] options = {"Instalar", "Alterar pasta...", "Cancelar"};
            int choice = JOptionPane.showOptionDialog(null, "Instalar Alarme Tarefa Smart em:\n" + dir,
                    TITLE, JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            if (choice == 0) return dir;
            if (choice != 1) return null;

            JFileChooser chooser = new JFileChooser(dir.getParent().toFile());
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Escolha a pasta de instalação");
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                dir = selected.toPath().resolve("AlarmeTarefaSmart");
            }
        }
    }

    private static void extract(String name, Path targetDir) throws IOException {
        try (InputStream in = Installer.class.getResourceAsStream("/payload/" + name)) {
            if (in == null) throw new IOException("Arquivo ausente no instalador: " + name);
            Files.copy(in, targetDir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static boolean vlcInstalled() {
        if (WINDOWS) return Files.exists(VLC_DIR.resolve("libvlc.dll"));
        List<String> candidates = List.of("/usr/lib/x86_64-linux-gnu/libvlc.so.5", "/usr/lib64/libvlc.so.5",
                "/usr/lib/libvlc.so.5", "/usr/local/lib/libvlc.so.5");
        return candidates.stream().anyMatch(p -> Files.exists(Path.of(p)));
    }

    /** Cria o atalho .lnk via WScript.Shell (resolve a Área de Trabalho mesmo se redirecionada ao OneDrive). */
    private static void createDesktopShortcut(Path bat, Path workDir) {
        String script = "$s = (New-Object -ComObject WScript.Shell).CreateShortcut("
                + "[Environment]::GetFolderPath('Desktop') + '\\Alarme Tarefa Smart.lnk');"
                + "$s.TargetPath = '" + psQuote(bat.toString()) + "';"
                + "$s.WorkingDirectory = '" + psQuote(workDir.toString()) + "';"
                + "$s.WindowStyle = 7;"
                + "$s.Save()";
        try {
            Process p = new ProcessBuilder("powershell", "-NoProfile", "-NonInteractive", "-Command", script)
                    .redirectErrorStream(true).start();
            p.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());
            if (p.waitFor() != 0) throw new IOException("powershell saiu com código " + p.exitValue());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Não foi possível criar o atalho:\n" + e.getMessage(),
                    TITLE, JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Cria a entrada .desktop em ~/.local/share/applications (aparece no menu de aplicativos). */
    private static void createLinuxDesktopEntry(Path sh, Path workDir) {
        Path entry = Path.of(System.getProperty("user.home"), ".local", "share", "applications",
                "alarme-tarefa-smart.desktop");
        String content = "[Desktop Entry]\n"
                + "Type=Application\n"
                + "Name=Alarme Tarefa Smart\n"
                + "Exec=\"" + sh + "\"\n"
                + "Path=" + workDir + "\n"
                + "Terminal=false\n"
                + "Categories=Utility;\n";
        try {
            Files.createDirectories(entry.getParent());
            Files.writeString(entry, content);
            entry.toFile().setExecutable(true, false);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Não foi possível criar o atalho:\n" + e.getMessage(),
                    TITLE, JOptionPane.WARNING_MESSAGE);
        }
    }

    private static String psQuote(String s) {
        return s.replace("'", "''");
    }
}
