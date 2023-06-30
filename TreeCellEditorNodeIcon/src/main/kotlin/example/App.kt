package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultTreeCellEditor
import javax.swing.tree.DefaultTreeCellRenderer

fun makeUI(): Component {
  val icon = ColorIcon(Color.RED)

  val tree1 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      val r = DefaultTreeCellRenderer()
      setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
        r.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus).also {
          (it as? JLabel)?.icon = icon
        }
      }
    }
  }
  tree1.isEditable = true

  val tree2 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      val r = DefaultTreeCellRenderer()
      r.openIcon = icon
      r.closedIcon = icon
      r.leafIcon = icon
      setCellRenderer(r)
    }
  }
  tree2.isEditable = true

  val tree3 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      val r2 = DefaultTreeCellRenderer()
      r2.openIcon = icon
      r2.closedIcon = icon
      r2.leafIcon = icon
      val r3 = DefaultTreeCellRenderer()
      r3.openIcon = ColorIcon(Color.GREEN)
      r3.closedIcon = ColorIcon(Color.BLUE)
      r3.leafIcon = ColorIcon(Color.ORANGE)
      setCellRenderer(r2)
      setCellEditor(DefaultTreeCellEditor(this, r3))
    }
  }
  tree3.isEditable = true

  return JPanel(GridLayout(1, 3)).also {
    it.add(JScrollPane(tree1))
    it.add(JScrollPane(tree2))
    it.add(JScrollPane(tree3))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
