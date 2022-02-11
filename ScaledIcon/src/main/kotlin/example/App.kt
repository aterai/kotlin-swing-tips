package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

private val model = object : DefaultTableModel(4, 3) {
  override fun getColumnClass(column: Int) = java.lang.Boolean::class.java
}
private val table = object : JTable(model) {
  private val iconIns = Insets(4, 4, 4, 4)
  private val checkIcon: Icon = CheckBoxIcon()

  override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
    val c = super.prepareRenderer(renderer, row, column)
    if (c is JCheckBox) {
      val s = getRowHeight(row) - iconIns.top - iconIns.bottom
      c.icon = ScaledIcon(checkIcon, s, s)
      c.isBorderPainted = false
    }
    return c
  }

  override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
    val c = super.prepareEditor(editor, row, column)
    if (c is JCheckBox) {
      val s = getRowHeight(row) - iconIns.top - iconIns.bottom
      c.icon = ScaledIcon(checkIcon, s, s)
      c.background = getSelectionBackground()
    }
    return c
  }
}

fun makeUI(): Component {
  table.rowHeight = 40
  table.selectionBackground = Color.WHITE
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ScaledIcon(
  private val icon: Icon,
  private val width: Int,
  private val height: Int
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.translate(x, y)
    val sx = width / icon.iconWidth.toDouble()
    val sy = height / icon.iconHeight.toDouble()
    g2.scale(sx, sy)
    icon.paintIcon(c, g2, 0, 0)
    g2.dispose()
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
}

private class CheckBoxIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create()
    if (g2 is Graphics2D && c is AbstractButton) {
      val model = c.model
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.translate(x, y)
      g2.paint = Color.DARK_GRAY
      val s = iconWidth.coerceAtMost(iconHeight) * .05
      val w = iconWidth - s - s
      val h = iconHeight - s - s
      val gw = w / 8.0
      val gh = h / 8.0
      g2.stroke = BasicStroke(s.toFloat())
      g2.draw(Rectangle2D.Double(s, s, w, h))
      if (model.isSelected) {
        g2.stroke = BasicStroke(3f * s.toFloat())
        val p = Path2D.Double()
        p.moveTo(x + 2f * gw, y + .5f * h)
        p.lineTo(x + .4f * w, y + h - 2f * gh)
        p.lineTo(x + w - 2f * gw, y + 2f * gh)
        g2.draw(p)
      }
      g2.dispose()
    }
  }

  override fun getIconWidth() = 1000 // 16

  override fun getIconHeight() = 1000 // 16
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
