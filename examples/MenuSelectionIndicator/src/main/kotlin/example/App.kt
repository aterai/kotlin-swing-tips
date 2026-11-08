package example

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.plaf.LayerUI

fun createUI(): Component {
  val menuBar = MenuBarUtils.initMenuBar(SelectionIndicatorMenuBar())
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu(), 2)

  val desktop = JDesktopPane()
  val frame = JInternalFrame("JInternalFrame")
  frame.setJMenuBar(MenuBarUtils.initMenuBar(SelectionHighlightMenuBar()))
  frame.setBounds(50, 50, 240, 120)
  desktop.add(frame)
  frame.isVisible = true

  val bottomMenuBar = MenuBarUtils.initMenuBar(JMenuBar())
  val layer = JLayer<JMenuBar>(bottomMenuBar, MenuHighlightLayerUI())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.setJMenuBar(menuBar) }
    it.add(desktop)
    it.add(layer, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class SelectionIndicatorMenuBar : JMenuBar() {
  private val rect = Rectangle()
  private var listener: ChangeListener? = null

  override fun updateUI() {
    val manager = MenuSelectionManager.defaultManager()
    manager.removeChangeListener(listener)
    super.updateUI()
    val inside = BorderFactory.createEmptyBorder(THICKNESS + 1, 0, 0, 0)
    val outside = UIManager.getBorder("MenuBar.border")
    val border: Border = BorderFactory.createCompoundBorder(outside, inside)
    setBorder(border)
    listener = ChangeListener { updateSelectionIndicator(it) }
    manager.addChangeListener(listener)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (!rect.isEmpty) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = SELECTION_COLOR
      g2.fillRect(rect.x, rect.y - THICKNESS, rect.width, THICKNESS)
      g2.dispose()
    }
  }

  private fun updateSelectionIndicator(e: ChangeEvent) {
    rect.setSize(0, 0)
    val path = (e.source as? MenuSelectionManager)?.selectedPath ?: emptyArray()
    if (path.size > 1 && this == path[0].component) {
      // The element next to the JMenuBar in the selected path is the selected top-level menu.
      val c = path[1].component
      if (c is JMenu) {
        rect.bounds = c.bounds
      }
    }
    repaint()
  }

  companion object {
    private val SELECTION_COLOR = Color(0x00_AA_FF)
    private const val THICKNESS = 3
  }
}

private class SelectionHighlightMenuBar : JMenuBar() {
  private var listener: ChangeListener? = null
  private var selectedBorder: Border? = null
  private var unselectedBorder: Border? = null

  override fun updateUI() {
    val manager = MenuSelectionManager.defaultManager()
    manager.removeChangeListener(listener)
    super.updateUI()
    selectedBorder = makeMenuBorder(SELECTION_COLOR)
    unselectedBorder = makeMenuBorder(ALPHA_ZERO)
    listener = ChangeListener { updateTopLevelMenuHighlight() }
    manager.addChangeListener(listener)
    EventQueue.invokeLater { updateTopLevelMenuHighlight() }
  }

  private fun updateTopLevelMenuHighlight() {
    // JMenuBar#getSubElements() returns only the direct children of this JMenuBar.
    for (element in getSubElements()) {
      updateMenuBorder(element.component)
    }
  }

  private fun updateMenuBorder(c: Component) {
    if (c is JMenu) {
      val model = c.model
      val selected = model.isArmed || model.isPressed || model.isSelected
      // Reusing the same Border instance makes JComponent#setBorder(...) skip the repaint.
      c.border = if (selected) selectedBorder else unselectedBorder
    }
  }

  companion object {
    private val ALPHA_ZERO = Color(0x0, true)
    private val SELECTION_COLOR = Color(0x00_AA_FF)
    private const val THICKNESS = 3

    private fun makeMenuBorder(color: Color): Border {
      val inside = UIManager.getBorder("Menu.border")
      val outside = BorderFactory.createMatteBorder(0, 0, THICKNESS, 0, color)
      return BorderFactory.createCompoundBorder(outside, inside)
    }
  }
}

private class MenuHighlightLayerUI : LayerUI<JMenuBar>() {
  private val rect = Rectangle()

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.setLayerEventMask(
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK,
      )
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.setLayerEventMask(0)
    }
    super.uninstallUI(c)
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (c is JLayer<*> && !rect.isEmpty) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = SELECTION_COLOR
      g2.fillRect(rect.x, rect.y + rect.height - THICKNESS, rect.width, THICKNESS)
      g2.dispose()
    }
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JMenuBar>) {
    super.processMouseEvent(e, l)
    if (e.id == MouseEvent.MOUSE_EXITED) {
      updateIndicator(l, Rectangle())
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JMenuBar>) {
    super.processMouseMotionEvent(e, l)
    val c = e.component
    updateIndicator(l, if (c is JMenu) c.bounds else Rectangle())
  }

  private fun updateIndicator(l: JLayer<out JMenuBar>, r: Rectangle) {
    if (rect != r) {
      rect.bounds = r
      // Some LookAndFeels, such as Metal, do not repaint a JMenu when the mouse hovers over it.
      l.repaint()
    }
  }

  companion object {
    private val SELECTION_COLOR = Color(0x00_AA_FF)
    private const val THICKNESS = 3
  }
}

private object MenuBarUtils {
  fun initMenuBar(menuBar: JMenuBar): JMenuBar {
    menuBar.add(createFileMenu())
    menuBar.add(createEditMenu())
    menuBar.add(Box.createGlue())
    menuBar.add(createHelpMenu())
    return menuBar
  }

  private fun createFileMenu(): JMenu {
    val menu = JMenu("File")
    menu.setMnemonic(KeyEvent.VK_F)
    menu.add("New").setMnemonic(KeyEvent.VK_N)
    menu.add("Open").setMnemonic(KeyEvent.VK_O)
    return menu
  }

  private fun createEditMenu(): JMenu {
    val menu = JMenu("Edit")
    menu.setMnemonic(KeyEvent.VK_E)
    menu.add("Cut").setMnemonic(KeyEvent.VK_T)
    menu.add("Copy").setMnemonic(KeyEvent.VK_C)
    menu.add("Paste").setMnemonic(KeyEvent.VK_P)
    menu.add("Delete").setMnemonic(KeyEvent.VK_D)
    return menu
  }

  private fun createHelpMenu(): JMenu {
    val menu = JMenu("Help")
    menu.setMnemonic(KeyEvent.VK_H)
    menu.add("About").setMnemonic(KeyEvent.VK_A)
    menu.add("Version").setMnemonic(KeyEvent.VK_V)
    return menu
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
