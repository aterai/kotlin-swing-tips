package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicToolBarUI

private const val MAX_HISTORY = 3

// private val BAR_FACTORY = BarFactory("resources.Main")
private val BAR_FACTORY = BarFactory("example.Main")
private val RECENT_FILES = mutableListOf<Path>()
private val noFile = JMenuItem("(Empty)")
private var fileHistoryMenu: JMenu? = null

private fun makeUI() = JPanel(BorderLayout()).also {
  initActions(actions.toList())
  val menuBar = BAR_FACTORY.createMenuBar()
  EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
  initHistory()
  val toolBar = BAR_FACTORY.createToolBar()
  if (toolBar != null) {
    it.add(toolBar, BorderLayout.NORTH)
  }
  it.add(JScrollPane(JTextArea()))
  it.preferredSize = Dimension(320, 240)
}

private fun initHistory() {
  val fm = BAR_FACTORY.getMenu("file") ?: return
  val fhm = fileHistoryMenu?.also {
    fileHistoryMenu?.removeAll()
  } ?: JMenu("Recent Items").also {
    it.mnemonic = KeyEvent.VK_R
    BAR_FACTORY.getMenuItem("exit")?.also { exit ->
      fm.remove(exit)
      fm.add(it)
      fm.addSeparator()
      fm.add(exit)
    }
  }
  fileHistoryMenu = fhm
  if (RECENT_FILES.isEmpty()) {
    noFile.isEnabled = false
    fhm.add(noFile)
  } else {
    fm.remove(noFile)
    for (i in RECENT_FILES.indices) {
      val mi = makeHistoryMenuItem(RECENT_FILES[i], i)
      fhm.add(mi)
    }
  }
}

fun updateHistory(path: Path) {
  fileHistoryMenu?.also {
    it.removeAll()
    RECENT_FILES.remove(path)
    RECENT_FILES.add(0, path)
    if (RECENT_FILES.size > MAX_HISTORY) {
      RECENT_FILES.removeAt(RECENT_FILES.size - 1)
    }
    for (i in RECENT_FILES.indices) {
      val mi = makeHistoryMenuItem(RECENT_FILES[i], i)
      it.add(mi, i)
    }
  }
}

private fun makeHistoryMenuItem(name: Path, idx: Int): JMenuItem {
  val num = (idx + 1).toString()
  val mi = JMenuItem(HistoryAction(name))
  mi.text = "$num: $name"
  mi.mnemonic = num.codePointAt(0)
  return mi
}

private class HistoryAction(private val path: Path) : AbstractAction() {
  override fun actionPerformed(e: ActionEvent) {
    val c = e.source as? JComponent ?: return
    val obj = arrayOf(
      "Open the file.\n",
      "This example do nothing\n",
      " and move the file to the beginning of the history."
    )
    JOptionPane.showMessageDialog(
      c.rootPane, obj, VersionAction.APP_NAME, JOptionPane.INFORMATION_MESSAGE
    )
    updateHistory(path)
  }
}

fun initActions(list: List<Action>) {
  BAR_FACTORY.initActions(list)
}

private val actions: List<Action>
  get() = listOf(NewAction(), ExitAction(), HelpAction(), VersionAction())

private class NewAction : AbstractAction("new") {
  private var counter = 0
  override fun actionPerformed(e: ActionEvent) {
    val c = e.source as? JComponent ?: return
    val obj = arrayOf(
      "Create a new file.\n",
      "This example do nothing\n",
      " and pretend to generate an appropriate file name and open it."
    )
    JOptionPane.showMessageDialog(
      c.rootPane, obj, VersionAction.APP_NAME, JOptionPane.INFORMATION_MESSAGE
    )
    val fileName = "C:/tmp/dummy.jpg.$counter~"
    updateHistory(Paths.get(fileName))
    counter++
  }
}

private class ExitAction : AbstractAction("exit") {
  override fun actionPerformed(e: ActionEvent) {
    val parent = SwingUtilities.getUnwrappedParent(e.source as? Component)
    val root = if (parent is JPopupMenu) {
      SwingUtilities.getRoot(parent.invoker)
    } else if (parent is JToolBar) {
      if ((parent.ui as? BasicToolBarUI)?.isFloating == true) {
        SwingUtilities.getWindowAncestor(parent).owner
      } else {
        SwingUtilities.getRoot(parent)
      }
    } else {
      SwingUtilities.getRoot(parent)
    }
    (root as? Window)?.also {
      it.dispatchEvent(WindowEvent(it, WindowEvent.WINDOW_CLOSING))
    }
  }
}

private class HelpAction : AbstractAction("help") {
  override fun actionPerformed(e: ActionEvent) {
    // dummy
  }
}

private class VersionAction : AbstractAction("version") {
  override fun actionPerformed(e: ActionEvent) {
    val c = e.source as? JComponent ?: return
    val obj = arrayOf("$APP_NAME - Version $VERSION.$RELEASE", COPYRIGHT)
    JOptionPane.showMessageDialog(c.rootPane, obj, APP_NAME, JOptionPane.INFORMATION_MESSAGE)
  }

  companion object {
    const val APP_NAME = "@title@"
    private const val COPYRIGHT = "Copyright(C) 2006"
    private const val VERSION = "0.0"
    private const val RELEASE = 1
  }
}

class BarFactory(base: String) {
  private val resources = ResourceBundle.getBundle(base, Utf8ResourceBundleControl())
  private val menuItems = mutableMapOf<String, JMenuItem>()
  private val toolButtons = mutableMapOf<String, JButton>()
  private val commands = mutableMapOf<Any, Action>()
  private val menus = mutableMapOf<String, JMenu>()

  fun initActions(list: List<Action>) {
    for (a in list) {
      commands[a.getValue(Action.NAME)] = a
    }
  }

  private fun getResource(key: String) = getResourceString(key)?.let { javaClass.getResource(it) }

  private fun getResourceString(nm: String) = runCatching { resources?.getString(nm) }.getOrNull()

  fun createToolBar(): JToolBar? {
    val tmp = getResourceString("toolbar") ?: return null
    val toolBar = JToolBar()
    toolBar.isRollover = true
    toolBar.isFloatable = false
    for (key in tokenize(tmp)) {
      if ("-" == key) {
        toolBar.add(Box.createHorizontalStrut(5))
        toolBar.addSeparator()
        toolBar.add(Box.createHorizontalStrut(5))
      } else {
        toolBar.add(createToolBarButton(key))
      }
    }
    toolBar.add(Box.createHorizontalGlue())
    return toolBar
  }

  // private fun createTool(key: String) = createToolBarButton(key)

  private fun createToolBarButton(key: String): JButton {
    val b = getResource(key + IMAGE_SUFFIX)
      ?.let { JButton(ImageIcon(it)) }
      ?: JButton(getResourceString(key + LABEL_SUFFIX))
    b.alignmentY = Component.CENTER_ALIGNMENT
    b.isFocusPainted = false
    b.isFocusable = false
    b.isRequestFocusEnabled = false
    b.margin = Insets(1, 1, 1, 1)
    val cmd = getResourceString(key + ACTION_SUFFIX) ?: key
    val a = commands[cmd] // getAction(cmd)
    if (a != null) {
      b.actionCommand = cmd
      b.addActionListener(a)
    } else {
      b.isEnabled = false
    }
    b.toolTipText = getResourceString(key + TIP_SUFFIX)
    toolButtons[key] = b
    return b
  }

  // fun getToolButton(key: String) = toolButtons[key]

  fun createMenuBar(): JMenuBar {
    val mb = JMenuBar()
    for (key in tokenize(getResourceString("menubar"))) {
      mb.add(createMenu(key))
    }
    return mb
  }

  private fun createMenu(key: String): JMenu? {
    val miText = getResourceString(key + LABEL_SUFFIX) ?: return null
    val menu = JMenu(miText)
    getResourceString(key + MNE_SUFFIX)?.uppercase(Locale.ENGLISH)?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?.also {
        if (!miText.contains(it)) {
          menu.text = "%s (%s)".format(miText, it)
        }
        menu.mnemonic = it.codePointAt(0)
      }
    for (m in tokenize(getResourceString(key))) {
      if ("-" == m) {
        menu.addSeparator()
      } else {
        menu.add(createMenuItem(m))
      }
    }
    menus[key] = menu
    return menu
  }

  private fun createMenuItem(cmd: String): JMenuItem? {
    val miText = getResourceString(cmd + LABEL_SUFFIX) ?: return null
    val mi = JMenuItem(miText)
    getResource(cmd + IMAGE_SUFFIX)?.also {
      mi.horizontalTextPosition = SwingConstants.RIGHT
      mi.icon = ImageIcon(it)
    }
    getResourceString(cmd + MNE_SUFFIX)?.uppercase(Locale.ENGLISH)?.trim()
      ?.takeIf { it.isNotEmpty() }
      ?.also {
        if (!miText.contains(it)) {
          mi.text = "%s (%s)".format(miText, it)
        }
        mi.mnemonic = it.codePointAt(0)
      }
    val cmd1 = getResourceString(cmd + ACTION_SUFFIX) ?: cmd
    mi.actionCommand = cmd1
    val a = commands[cmd1] // getAction(cmd1)
    if (a != null) {
      mi.addActionListener(a)
      mi.isEnabled = a.isEnabled
    } else {
      mi.isEnabled = false
    }
    menuItems[cmd1] = mi
    return mi
  }

  fun getMenuItem(cmd: String) = menuItems[cmd]

  fun getMenu(cmd: String) = menus[cmd]

  // private fun getAction(cmd: String) = commands[cmd]

  companion object {
    private const val IMAGE_SUFFIX = "Image"
    private const val LABEL_SUFFIX = "Label"
    private const val ACTION_SUFFIX = "Action"
    private const val TIP_SUFFIX = "Tooltip"
    private const val MNE_SUFFIX = "Mnemonic"

    private fun tokenize(input: String?) = input?.split("\\s".toRegex()) ?: emptyList()
  }
}

private class Utf8ResourceBundleControl : ResourceBundle.Control() {
  override fun getFormats(baseName: String?): List<String> {
    requireNotNull(baseName) { "baseName must not be null" }
    return listOf("properties")
  }

  @Throws(IllegalAccessException::class, InstantiationException::class, IOException::class)
  override fun newBundle(
    baseName: String?,
    locale: Locale?,
    format: String?,
    loader: ClassLoader?,
    reload: Boolean
  ): ResourceBundle? {
    var bundle: ResourceBundle? = null
    if ("properties" == format) {
      val bundleName = toBundleName(
        requireNotNull(baseName) { "baseName must not be null" },
        requireNotNull(locale) { "locale must not be null" }
      )
      val resourceName = toResourceName(bundleName, format)
      var stream: InputStream? = null
      val cl = requireNotNull(loader) { "loader must not be null" }
      if (reload) {
        cl.getResource(resourceName)?.openConnection()?.also {
          it.useCaches = false
          stream = it.getInputStream()
        }
      } else {
        stream = cl.getResourceAsStream(resourceName)
      }
      stream?.also {
        BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).use { r ->
          bundle = PropertyResourceBundle(r)
        }
      }
    }
    return bundle
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
