package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.WindowEvent
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicToolBarUI

// private val BAR_FACTORY = BarFactory("resources.Main")
private val BAR_FACTORY = BarFactory("example.Main")

private fun makeUI(): Component {
  initActions(actions.toList())
  val menuPanel = JPanel(BorderLayout())
  val menuBar = BAR_FACTORY.createMenuBar()
  menuPanel.add(menuBar, BorderLayout.NORTH)
  BAR_FACTORY.createToolBar()?.also {
    menuPanel.add(it, BorderLayout.SOUTH)
  }
  return JPanel(BorderLayout()).also {
    it.add(menuPanel, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

fun initActions(list: List<Action>) {
  BAR_FACTORY.initActions(list)
}

private val actions: List<Action>
  get() = listOf(NewAction(), ExitAction(), HelpAction(), VersionAction())

private class NewAction : AbstractAction("new") {
  override fun actionPerformed(e: ActionEvent) {
    // new action...
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
    val toolbar = JToolBar()
    toolbar.isRollover = true
    toolbar.isFloatable = false
    for (key in tokenize(tmp)) {
      if ("-" == key) {
        toolbar.add(Box.createHorizontalStrut(5))
        toolbar.addSeparator()
        toolbar.add(Box.createHorizontalStrut(5))
      } else {
        toolbar.add(createToolBarButton(key))
      }
    }
    toolbar.add(Box.createHorizontalGlue())
    return toolbar
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
    getResourceString(key + MNE_SUFFIX)?.toUpperCase(Locale.ENGLISH)?.trim()
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
    getResourceString(cmd + MNE_SUFFIX)?.toUpperCase(Locale.ENGLISH)?.trim()
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

  // fun getMenuItem(cmd: String) = menuItems[cmd]

  // fun getMenu(cmd: String) = menus[cmd]

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
        BufferedReader(InputStreamReader(it, StandardCharsets.UTF_8)).use { r -> bundle = PropertyResourceBundle(r) }
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
