package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultTreeCellRenderer

class MainPanel : JPanel(GridLayout(1, 2, 2, 2)) {
  init {
    add(makeTranslucentScrollPane(TranslucentTree()))
    add(makeTranslucentScrollPane(TransparentTree()))
    setOpaque(false)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTranslucentScrollPane(view: Component) = JScrollPane(view).also {
    it.setOpaque(false)
    it.getViewport().setOpaque(false)
  }
}

class TranslucentTree : JTree() {
  override fun updateUI() {
    super.updateUI()
    UIManager.put("Tree.repaintWholeRow", true)
    setCellRenderer(TranslucentTreeCellRenderer())
    setOpaque(false)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  }
}

class TransparentTree : JTree() {
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(SELECTED_COLOR)
    getSelectionRows()
        ?.map { getRowBounds(it) }
        ?.forEach { g2.fillRect(0, it.y, getWidth(), it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      getLeadSelectionPath()?.also {
        val r = getRowBounds(getRowForPath(it))
        g2.setPaint(SELECTED_COLOR.darker())
        g2.drawRect(0, r.y, getWidth() - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
    super.updateUI()
    UIManager.put("Tree.repaintWholeRow", true)
    setCellRenderer(TransparentTreeCellRenderer())
    setOpaque(false)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  }

  companion object {
    private val SELECTED_COLOR = Color(0x64_64_64_FF, true)
  }
}

// https://ateraimemo.com/Swing/RootPaneBackground.html
class TransparentRootPane : JRootPane() {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(TEXTURE)
    g2.fillRect(0, 0, getWidth(), getHeight())
    g2.dispose()
  }

  override fun updateUI() {
    super.updateUI()
    setOpaque(false)
  }

  companion object {
    private val TEXTURE = makeCheckerTexture()
    private fun makeCheckerTexture(): TexturePaint {
      val cs = 6
      val sz = cs * cs
      val img = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
      val g2: Graphics2D = img.createGraphics()
      g2.setPaint(Color(0xDC_DC_DC))
      g2.fillRect(0, 0, sz, sz)
      g2.setPaint(Color(0xC8_C8_C8_C8.toInt(), true))
      var i = 0
      while (i * cs < sz) {
        var j = 0
        while (j * cs < sz) {
          if ((i + j) % 2 == 0) {
            g2.fillRect(i * cs, j * cs, cs, cs)
          }
          j++
        }
        i++
      }
      g2.dispose()
      return TexturePaint(img, Rectangle(sz, sz))
    }
  }
}

// https://ateraimemo.com/Swing/TreeBackgroundSelectionColor.html
open class TransparentTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, false)
    (c as? JComponent)?.setOpaque(false)
    return c
  }

  override fun getBackgroundNonSelectionColor() = ALPHA_OF_ZERO

  override fun getBackgroundSelectionColor() = ALPHA_OF_ZERO

  companion object {
    private val ALPHA_OF_ZERO = Color(0x0, true)
  }
}

class TranslucentTreeCellRenderer : TransparentTreeCellRenderer() {
  override fun getBackgroundSelectionColor() = Color(0x64_64_64_FF, true)
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    val frame: JFrame = object : JFrame() {
      override fun createRootPane(): JRootPane {
        return TransparentRootPane()
      }
    }
    (frame.getContentPane() as? JComponent)?.setOpaque(false)
    frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
    frame.getContentPane().add(MainPanel())
    frame.pack()
    frame.setLocationRelativeTo(null)
    frame.setVisible(true)
  }
}
