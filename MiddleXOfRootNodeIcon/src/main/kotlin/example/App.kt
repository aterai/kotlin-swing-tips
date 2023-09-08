package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val ow = UIManager.getIcon("Tree.openIcon").iconWidth
  val iw = 32
  val ih = 24
  val icon0 = ColorIcon(Color.GREEN, Dimension(iw, ih))
  val tree0 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setRowHeight(0)
      val r = getCellRenderer()
      setCellRenderer {
          tree: JTree,
          value: Any?,
          selected: Boolean,
          expanded: Boolean,
          leaf: Boolean,
          row: Int,
          hasFocus: Boolean ->
        val c = r.getTreeCellRendererComponent(
          tree,
          value,
          selected,
          expanded,
          leaf,
          row,
          hasFocus,
        )
        if (c is JLabel && value == tree.model.root) {
          c.icon = icon0
        }
        c
      }
    }
  }
  tree0.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
  val icon1 = ColorIcon(Color.GREEN, Dimension(ow, ih))
  val icon2 = ColorIcon(Color(0x55_00_00_AA, true), Dimension(iw, ih))
  val tree = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setRowHeight(0)
      val r = getCellRenderer()
      setCellRenderer {
          tree: JTree,
          value: Any?,
          selected: Boolean,
          expanded: Boolean,
          leaf: Boolean,
          row: Int,
          hasFocus: Boolean ->
        val c = r.getTreeCellRendererComponent(
          tree,
          value,
          selected,
          expanded,
          leaf,
          row,
          hasFocus,
        )
        if (c is JLabel && value == tree.model.root) {
          c.icon = icon1
          c.iconTextGap = 2 + (iw - icon1.iconWidth) / 2
        }
        c
      }
    }
  }
  tree.border = BorderFactory.createEmptyBorder(1, 1 + (iw - ow) / 2, 1, 1)
  val layerUI = object : LayerUI<JTree>() {
    override fun paint(g: Graphics, c: JComponent) {
      super.paint(g, c)
      val g2 = g.create() as? Graphics2D ?: return
      icon2.paintIcon(c, g2, 1, 1)
      g2.dispose()
    }
  }

  return JPanel(GridLayout(1, 2)).also {
    it.add(JScrollPane(tree0))
    it.add(JScrollPane(JLayer(tree, layerUI)))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColorIcon(private val color: Color, private val dim: Dimension) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.color = color
    g2.fillRect(1, 1, dim.width - 2, dim.height - 2)
    g2.dispose()
  }

  override fun getIconWidth() = dim.width

  override fun getIconHeight() = dim.height
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
