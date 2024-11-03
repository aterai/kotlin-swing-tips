package example

import java.awt.*
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.*
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeNode

fun makeUI(): Component {
  val tree = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      setCellRenderer(ButtonCellRenderer())
      setCellEditor(ButtonCellEditor())
      setRowHeight(0)
    }
  }
  tree.isEditable = true
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ButtonPanel : JPanel() {
  val renderer = DefaultTreeCellRenderer()
  val b1 = ColorButton(ColorIcon(Color.RED))
  val b2 = ColorButton(ColorIcon(Color.GREEN))
  val b3 = ColorButton(ColorIcon(Color.BLUE))

  init {
    isOpaque = false
  }

  fun remakePanel(c: Component): Component {
    removeAll()
    listOf(b1, b2, b3, c).forEach { add(it) }
    return this
  }
}

private class ButtonCellRenderer : TreeCellRenderer {
  private val panel = ButtonPanel()

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val c = panel.renderer.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
    return panel.remakePanel(c)
  }
}

private class ButtonCellEditor :
  AbstractCellEditor(),
  TreeCellEditor {
  private val panel = ButtonPanel()

  init {
    panel.b1.addActionListener { stopCellEditing() }
    panel.b2.addActionListener { stopCellEditing() }
    panel.b3.addActionListener { stopCellEditing() }
  }

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any?,
    isSelected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
  ): Component {
    val c = panel.renderer.getTreeCellRendererComponent(
      tree,
      value,
      true,
      expanded,
      leaf,
      row,
      true,
    )
    return panel.remakePanel(c)
  }

  override fun getCellEditorValue(): Any? = panel.renderer.text

  override fun isCellEditable(e: EventObject?): Boolean {
    val tree = (e as? MouseEvent)?.component as? JTree ?: return false
    val pt = e.point
    val path = tree.getPathForLocation(pt.x, pt.y)
    val r = tree.getPathBounds(path)
    val n = path?.lastPathComponent
    return n is TreeNode && r?.contains(pt) == true && isButton(tree, pt, n, r)
  }

  private fun isButton(
    tree: JTree,
    pt: Point,
    node: TreeNode,
    r: Rectangle,
  ): Boolean {
    val row = tree.getRowForLocation(pt.x, pt.y)
    val renderer = tree.cellRenderer
    val c = renderer.getTreeCellRendererComponent(
      tree,
      " ",
      true,
      true,
      node.isLeaf,
      row,
      true,
    )
    c.bounds = r
    c.setLocation(0, 0)
    // tree.doLayout()
    tree.revalidate()
    pt.translate(-r.x, -r.y)
    return SwingUtilities.getDeepestComponentAt(c, pt.x, pt.y) is JButton
  }
}

private class ColorButton(
  icon: ColorIcon,
) : JButton(icon) {
  init {
    pressedIcon = ColorIcon(icon.color.darker())
    isFocusable = false
    isFocusPainted = false
    isBorderPainted = false
    isContentAreaFilled = false
    border = BorderFactory.createEmptyBorder()
  }
}

private class ColorIcon(
  val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 8

  override fun getIconHeight() = 8
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
