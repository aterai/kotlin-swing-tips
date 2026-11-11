package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*

fun createUI() = JPanel(BorderLayout()).also {
  EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
  it.add(JScrollPane(JTextArea()))
  it.preferredSize = Dimension(320, 240)
}

private fun createMenuBar(): JMenuBar {
  // Default: the accelerator text alignment depends on the LookAndFeel
  val defaultMenu = JMenu("Default")
  defaultMenu.mnemonic = KeyEvent.VK_D

  // RightAlign: the accelerator text is always right-aligned
  val rightAlignedMenu = JMenu("RightAlign")
  rightAlignedMenu.mnemonic = KeyEvent.VK_R

  addMenuItems(defaultMenu, rightAlignedMenu, "MenuItem", KeyEvent.VK_N, "alt N")
  addMenuItems(defaultMenu, rightAlignedMenu, "aaa", KeyEvent.VK_1, "alt ESCAPE")
  addMenuItems(defaultMenu, rightAlignedMenu, "12345", KeyEvent.VK_2, "alt ctrl SPACE")
  val ks = "alt ctrl shift BACK_SPACE"
  addMenuItems(defaultMenu, rightAlignedMenu, "c", KeyEvent.VK_3, ks)

  val menuBar = JMenuBar()
  menuBar.add(defaultMenu)
  menuBar.add(rightAlignedMenu)
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())
  return menuBar
}

// Add menu items that share the same accelerator to both menus for comparison
private fun addMenuItems(
  defaultMenu: JMenu,
  rightAlignedMenu: JMenu,
  text: String,
  mnemonic: Int,
  keyStroke: String,
) {
  val accelerator = KeyStroke.getKeyStroke(keyStroke)

  val defaultItem = JMenuItem(text)
  defaultItem.mnemonic = mnemonic
  defaultItem.accelerator = accelerator
  defaultMenu.add(defaultItem)

  val rightAlignedItem = RightAlignedAcceleratorItem(text, accelerator)
  rightAlignedItem.mnemonic = mnemonic
  rightAlignedMenu.add(rightAlignedItem)
}

// A JMenuItem that paints its accelerator text right-aligned without using any
// internal API such as sun.swing.MenuItemLayoutHelper.
private class RightAlignedAcceleratorItem(
  text: String,
  private val acceleratorKey: KeyStroke?,
) : JMenuItem(text) {
  init {
    // JMenuItem#setAccelerator(...) is intentionally not called, so the
    // LookAndFeel never paints the accelerator text by itself.
    // The binding is registered in the component's own InputMap/ActionMap,
    // which survive updateUI() (a LookAndFeel only replaces their UI parent
    // maps), so registering it once here is enough.
    initAcceleratorBinding()
  }

  // JMenuBar#processKeyBinding(...) searches the whole menu hierarchy
  // recursively, so a WHEN_IN_FOCUSED_WINDOW binding also works for a
  // JMenuItem inside a JPopupMenu that is not showing.
  private fun initAcceleratorBinding() {
    val ks = acceleratorKey ?: return
    val a = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        doClick(0)
      }
    }
    actionMap.put(ACTION_KEY, a)
    getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ks, ACTION_KEY)
  }

  // @see javax.swing.plaf.basic.BasicMenuItemUI#getAcceleratorText()
  private fun getAcceleratorText(): String {
    val ks = acceleratorKey ?: return ""
    val buf = StringBuilder()
    val delimiter = UIManager.getString("MenuItem.acceleratorDelimiter") ?: "+"
    val modifiers = ks.modifiers
    if (modifiers > 0) {
      buf.append(InputEvent.getModifiersExText(modifiers)).append(delimiter)
    }
    val keyCode = ks.keyCode
    if (keyCode == 0) {
      buf.append(ks.keyChar)
    } else {
      buf.append(KeyEvent.getKeyText(keyCode))
    }
    return buf.toString()
  }

  private fun getAcceleratorFont() = UIManager.getFont("MenuItem.acceleratorFont") ?: font

  // NimbusLookAndFeel does not define MenuItem.acceleratorForeground and so on,
  // so fall back to the Synth style keys and then to the item's own foreground.
  private fun getAcceleratorColor(): Color {
    val m = getModel()
    val color = if (m.isEnabled) {
      if (m.isArmed || isSelected) {
        getUiColor(
          "MenuItem.acceleratorSelectionForeground",
          "MenuItem[MouseOver].textForeground",
        )
      } else {
        getUiColor("MenuItem.acceleratorForeground", "MenuItem[Enabled].textForeground")
      }
    } else {
      getUiColor("MenuItem.disabledForeground", "MenuItem[Disabled].textForeground")
    }
    return color ?: foreground
  }

  override fun getPreferredSize(): Dimension? {
    val d = super.getPreferredSize()
    val txt = getAcceleratorText()
    if (d != null && txt.isNotEmpty()) {
      // Reserve the room the LookAndFeel would have used for the accelerator
      d.width += iconTextGap + getFontMetrics(getAcceleratorFont()).stringWidth(txt)
    }
    return d
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val txt = getAcceleratorText()
    if (txt.isEmpty()) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    val hints = Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints")
    if (hints is Map<*, *>) {
      g2.addRenderingHints(hints)
    }
    g2.font = getAcceleratorFont()
    g2.color = getAcceleratorColor()
    val fm = g2.fontMetrics
    val i = insets
    val gap = iconTextGap
    // ComponentOrientation.RIGHT_TO_LEFT is also taken into account
    val x = if (componentOrientation.isLeftToRight) {
      width - i.right - gap - fm.stringWidth(txt)
    } else {
      i.left + gap
    }
    val y = (height - fm.height) / 2 + fm.ascent
    g2.drawString(txt, x, y)
    g2.dispose()
  }

  companion object {
    private const val ACTION_KEY = "doClick"

    private fun getUiColor(
      basicKey: String,
      synthKey: String,
    ): Color? = UIManager.getColor(basicKey) ?: UIManager.getColor(synthKey)
  }
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
