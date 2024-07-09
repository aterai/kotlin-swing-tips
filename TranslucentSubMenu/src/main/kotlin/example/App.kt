package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.BorderUIResource

fun makeUI(): Component {
  val tree = JTree()
  tree.componentPopupMenu = makePopupMenu()

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMenuBar(): JMenuBar {
  val mb = JMenuBar()
  val menuKeys = arrayOf("File", "Edit", "Help")
  for (key in menuKeys) {
    val m = createMenu(key)
    mb.add(m)
  }
  return mb
}

private fun makePopupMenu(): JPopupMenu {
  val menu = TransparentMenu("Test")
  menu.add("Undo")
  menu.add("Redo")
  val popup = TranslucentPopupMenu()
  popup.add(menu)
  popup.addSeparator()
  popup.add("Cut")
  popup.add("Copy")
  popup.add("Paste")
  popup.add("Delete")
  return popup
}

private fun createMenu(key: String): JMenu {
  val menu = TransparentMenu(key)
  menu.isOpaque = false // Motif lnf
  val sub = TransparentMenu("Submenu")
  sub.add("JMenuItem")
  sub.add("L${"o".repeat(20)}ng")
  menu.add(sub)
  menu.add("JMenuItem1")
  menu.add("JMenuItem12")
  return menu
}

private class TranslucentPopupMenu : JPopupMenu() {
  override fun isOpaque() = false

  override fun updateUI() {
    super.updateUI()
    if (UIManager.getBorder("PopupMenu.border") == null) {
      border = BorderUIResource(BorderFactory.createLineBorder(Color.GRAY))
    }
  }

  override fun add(c: Component): Component {
    (c as? JComponent)?.isOpaque = false
    return c
  }

  override fun add(menuItem: JMenuItem): JMenuItem {
    menuItem.isOpaque = false
    return super.add(menuItem)
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    EventQueue.invokeLater {
      val p = topLevelAncestor
      if (p is JWindow && p.type == Window.Type.POPUP) {
        // Heavy weight
        p.setBackground(ALPHA_ZERO)
      }
    }
    super.show(c, x, y)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = POPUP_LEFT
    g2.fillRect(0, 0, LEFT_WIDTH, height)
    g2.paint = POPUP_BACK
    g2.fillRect(LEFT_WIDTH, 0, width, height)
    g2.dispose()
  }

  companion object {
    private val ALPHA_ZERO = Color(0x0, true)
    private val POPUP_BACK = Color(250, 250, 250, 200)
    private val POPUP_LEFT = Color(230, 230, 230, 200)
    private const val LEFT_WIDTH = 24
  }
}

private class TransparentMenu(
  title: String?,
) : JMenu(title) {
  private var popupMenu: JPopupMenu? = null

  private fun ensurePopupMenuCreated2(): JPopupMenu {
    val popup = popupMenu ?: TranslucentPopupMenu().also {
      it.invoker = this
    }
    popupListener = createWinListener(popup)
    popupMenu = popup
    return popup
  }

  override fun getPopupMenu() = popupMenu ?: ensurePopupMenuCreated2()

  override fun add(menuItem: JMenuItem): JMenuItem {
    menuItem.isOpaque = false
    val popup = ensurePopupMenuCreated2()
    return popup.add(menuItem)
  }

  override fun add(c: Component): Component {
    (c as? JComponent)?.isOpaque = false
    val popup = ensurePopupMenuCreated2()
    popup.add(c)
    return c
  }

  override fun addSeparator() {
    ensurePopupMenuCreated2().addSeparator()
  }

  private fun checkIndex(pos: Int) {
    require(pos >= 0) { "index less than zero." }
  }

  override fun insert(
    s: String,
    pos: Int,
  ) {
    checkIndex(pos)
    val popup = ensurePopupMenuCreated2()
    popup.insert(JMenuItem(s), pos)
  }

  override fun insert(
    mi: JMenuItem,
    pos: Int,
  ): JMenuItem {
    checkIndex(pos)
    val popup = ensurePopupMenuCreated2()
    popup.insert(mi, pos)
    return mi
  }

  override fun insertSeparator(index: Int) {
    checkIndex(index)
    val popup = ensurePopupMenuCreated2()
    popup.insert(JPopupMenu.Separator(), index)
  }

  override fun isPopupMenuVisible() = ensurePopupMenuCreated2().isVisible
}

private class TranslucentPopupFactory : PopupFactory() {
  override fun getPopup(
    owner: Component,
    contents: Component,
    x: Int,
    y: Int,
  ) = TranslucentPopup(owner, contents, x, y)
}

private class TranslucentPopup(
  owner: Component?,
  contents: Component,
  ownerX: Int,
  ownerY: Int,
) : Popup(owner, contents, ownerX, ownerY) {
  private val popupWindow = JWindow()

  init {
    popupWindow.background = Color(0x0, true)
    popupWindow.setLocation(ownerX, ownerY)
    popupWindow.contentPane.add(contents)
    contents.invalidate()
  }

  override fun show() {
    popupWindow.isVisible = true
    popupWindow.pack()
  }

  override fun hide() {
    popupWindow.isVisible = false
    popupWindow.removeAll()
    popupWindow.dispose()
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      PopupFactory.setSharedInstance(TranslucentPopupFactory())
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
