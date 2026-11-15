package example

import java.awt.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit
import javax.swing.text.html.StyleSheet
import kotlin.streams.toList

const val GNOME_THEME_NAME = "gnome.Net/ThemeName"

fun createUI(): Component {
  val sys = JRadioButtonMenuItem("System", true)
  val editor = object : JEditorPane() {
    override fun updateUI() {
      super.updateUI()
      if (sys.isSelected) {
        applyTheme(this, isDarkMode())
      }
      isEditable = false
      selectedTextColor = null
      selectionColor = Color(0x64_88_AA_AA, true)
    }
  }
  sys.addActionListener { applyTheme(editor, isDarkMode()) }
  val light = JRadioButtonMenuItem("Light")
  light.addActionListener { applyTheme(editor, false) }
  val dark = JRadioButtonMenuItem("Dark")
  dark.addActionListener { applyTheme(editor, true) }
  val group = ButtonGroup()
  group.add(sys)
  group.add(light)
  group.add(dark)
  val theme = JMenu("Theme")
  theme.add(sys)
  theme.add(light)
  theme.add(dark)
  val key = GNOME_THEME_NAME
  Toolkit.getDefaultToolkit().addPropertyChangeListener(key) {
    val isDark = isDarkThemeName(it.newValue)
    // This listener may be notified on the AWT toolkit thread, not on the EDT
    EventQueue.invokeLater {
      if (sys.isSelected) {
        applyTheme(editor, isDark)
      }
    }
  }
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(theme)
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(editor))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun applyTheme(editor: JEditorPane, isDark: Boolean) {
  updateTheme(editor, isDark)
  // Reload the page since setEditorKit(...) replaces the document with an empty one
  loadHtml(editor)
}

private fun loadHtml(editor: JEditorPane) {
  val cl = Thread.currentThread().contextClassLoader
  cl.getResource("example/test.html")?.also { url ->
    runCatching {
      editor.page = url
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(editor)
      editor.text = it.message
    }
  }
}

fun updateTheme(editor: JEditorPane, isDark: Boolean) {
  val kit = editor.editorKit
  val htmlEditorKit = kit as? HTMLEditorKit ?: HTMLEditorKit()
  if (isDark) {
    htmlEditorKit.styleSheet = createDarkStyleSheet()
    editor.background = Color(0x1E_1F_22)
  } else {
    htmlEditorKit.styleSheet = createLightStyleSheet()
    editor.background = Color(0xEE_EE_EE)
  }
  editor.editorKit = htmlEditorKit
}

fun isDarkMode(): Boolean {
  val os = System.getProperty("os.name").lowercase()
  return when {
    os.contains("windows") -> isWindowsDarkMode()

    os.contains("linux") -> isLinuxDarkMode()

    // os.contains("mac") -> false
    else -> false
  }
}

@Throws(IOException::class)
fun getProcessOutput(cmd: List<String>): String {
  val builder = ProcessBuilder(cmd)
  builder.redirectErrorStream(true)
  val process = builder.start()
  // Reading until EOF also waits for the process to finish writing its output
  val input = InputStreamReader(process.inputStream, StandardCharsets.UTF_8)
  return BufferedReader(input).use { reader ->
    reader.lines().toList().joinToString(System.lineSeparator())
  }
}

fun isWindowsDarkMode(): Boolean {
  // reg.exe starts much faster than powershell.exe
  val cmd = listOf(
    "reg",
    "query",
    """HKCU\SOFTWARE\Microsoft\Windows\CurrentVersion\Themes\Personalize""",
    "/v",
    "AppsUseLightTheme",
  )
  // The output looks like: "    AppsUseLightTheme    REG_DWORD    0x0"
  return runCatching {
    getProcessOutput(cmd).trim().endsWith("0x0")
  }.getOrNull() ?: false
}

fun isLinuxDarkMode(): Boolean {
  val tk = Toolkit.getDefaultToolkit()
  return isDarkThemeName(tk.getDesktopProperty(GNOME_THEME_NAME))
}

// e.g. Yaru-dark, Adwaita-dark, HighContrastInverse is not supported
fun isDarkThemeName(themeName: Any?) =
  themeName?.toString()?.lowercase(Locale.ENGLISH)?.contains("dark") == true

fun createLightStyleSheet() = StyleSheet().also {
  it.addRule("pre{background:#eeeeee}")
  it.addRule(".str{color:#008800}")
  it.addRule(".kwd{color:#000088}")
  it.addRule(".com{color:#880000}")
  it.addRule(".typ{color:#660066}")
  it.addRule(".lit{color:#006666}")
  it.addRule(".pun{color:#666600}")
  it.addRule(".pln{color:#000000}")
  it.addRule(".tag{color:#000088}")
  it.addRule(".atn{color:#660066}")
  it.addRule(".atv{color:#008800}")
  it.addRule(".dec{color:#660066}")
}

fun createDarkStyleSheet() = StyleSheet().also {
  it.addRule("pre{background:#1e1f22}")
  it.addRule(".str{color:#ffa0a0}")
  it.addRule(".kwd{color:#f0e68c;font-weight:bold}")
  it.addRule(".com{color:#87ceeb}")
  it.addRule(".typ{color:#98fb98}")
  it.addRule(".lit{color:#cd5c5c}")
  it.addRule(".pun{color:#ffffff}")
  it.addRule(".pln{color:#ffffff}")
  it.addRule(".tag{color:#f0e68c;font-weight:bold}")
  it.addRule(".atn{color:#bdb76b;font-weight:bold}")
  it.addRule(".atv{color:#ffa0a0}")
  it.addRule(".dec{color:#98fb98}")
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
