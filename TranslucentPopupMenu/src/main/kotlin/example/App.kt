package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.BorderUIResource

class MainPanel : JPanel(BorderLayout()) {
  init {
    val tree = JTree()
    tree.setComponentPopupMenu(TranslucentPopupMenu().also {
      it.add("Undo")
      it.add("Redo")
      it.addSeparator()
      it.add("Cut")
      it.add("Copy")
      it.add("Paste")
      it.add("Delete")
    })
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }
}

class TranslucentPopupMenu : JPopupMenu() {
  override fun isOpaque() = false

  override fun updateUI() {
    super.updateUI()
    UIManager.getBorder("PopupMenu.border")?.also {
      setBorder(BorderUIResource(BorderFactory.createLineBorder(Color.GRAY)))
    }
  }

  override fun add(c: Component): Component {
    (c as? JComponent)?.setOpaque(false)
    return c
  }

  override fun add(menuItem: JMenuItem): JMenuItem {
    menuItem.setOpaque(false)
    return super.add(menuItem)
  }

  override fun show(c: Component, x: Int, y: Int) {
    EventQueue.invokeLater {
      val p = getTopLevelAncestor()
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
    g2.setPaint(POPUP_LEFT)
    g2.fillRect(0, 0, LEFT_WIDTH, getHeight())
    g2.setPaint(POPUP_BACK)
    g2.fillRect(LEFT_WIDTH, 0, getWidth(), getHeight())
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
