package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.font.TextLayout
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Justified", "Default")
  val data = arrayOf(
    arrayOf("会社名", ""),
    arrayOf("所在地", ""),
    arrayOf("電話番号", ""),
    arrayOf("設立", ""),
    arrayOf("代表取締役", ""),
    arrayOf("事業内容", "")
  )
  val model = DefaultTableModel(data, columnNames)
  val table = object : JTable(model) {
    private val evenColor = Color(0xF5_F5_FF)
    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int) =
      super.prepareRenderer(tcr, row, column).also {
        if (isRowSelected(row)) {
          it.foreground = getSelectionForeground()
          it.background = getSelectionBackground()
        } else {
          it.foreground = foreground
          it.background = if (row % 2 == 0) evenColor else background
        }
      }
  }
  table.isFocusable = false
  table.rowSelectionAllowed = true
  table.showVerticalLines = false
  table.intercellSpacing = Dimension(0, 1)
  table.fillsViewportHeight = true
  table.rowHeight = 18
  table.setRowHeight(5, 80)
  val renderer = JustifiedLabel()
  table.columnModel.getColumn(0).also {
    it.minWidth = 100
    it.setCellRenderer { _, value, _, _, _, _ ->
      renderer.also { lbl ->
        lbl.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
        lbl.text = value?.toString() ?: ""
      }
    }
  }
  table.columnModel.getColumn(1).preferredWidth = 220

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

// private class InterIdeographJustifyCellRenderer : TableCellRenderer {
//   private val renderer = JustifiedLabel()
//   override fun getTableCellRendererComponent(
//     table: JTable,
//     value: Any?,
//     isSelected: Boolean,
//     hasFocus: Boolean,
//     row: Int,
//     column: Int
//   ): Component {
//     renderer.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
//     renderer.text = value?.toString() ?: ""
//     return renderer
//   }
// }

private class JustifiedLabel(str: String? = null) : JLabel(str ?: "") {
  @Transient
  private var layout: TextLayout? = null
  private var prevWidth = -1
  override fun setText(text: String) {
    super.setText(text)
    prevWidth = -1
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    val font = font
    val ins = insets
    val d = size
    val w = d.width - ins.left - ins.right
    if (w != prevWidth) {
      prevWidth = w
      layout = TextLayout(text, font, g2.fontRenderContext).getJustifiedLayout(w.toFloat())
    }
    g2.paint = background
    g2.fillRect(0, 0, d.width, d.height)
    g2.paint = foreground
    // int baseline = getBaseline(d.width, d.height);
    val baseline = ins.top + font.size2D
    layout?.draw(g2, ins.left.toFloat(), baseline)
    g2.dispose()
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
