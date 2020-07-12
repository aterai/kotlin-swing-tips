package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.BorderUIResource

fun makeUI(): Component {
  val popup = TranslucentPopupMenu().also {
    it.add("Undo")
    it.add("Redo")
    it.addSeparator()
    it.add("Cut")
    it.add("Copy")
    it.add("Paste")
    it.add("Delete")
  }
  val tree = JTree()
  tree.componentPopupMenu = popup

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TranslucentPopupMenu : JPopupMenu() {
  override fun isOpaque() = false

  override fun updateUI() {
    super.updateUI()
    UIManager.getBorder("PopupMenu.border")?.also {
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

  override fun show(c: Component, x: Int, y: Int) {
    EventQueue.invokeLater {
      val p = topLevelAncestor
      if (p is JWindow) {
        println("Heavy weight")
        p.setBackground(ALPHA_ZERO)
      } else {
        println("Light weight")
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
