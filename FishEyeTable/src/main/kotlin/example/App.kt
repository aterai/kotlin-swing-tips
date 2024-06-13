package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(arrayOf("aaa", -1, true))
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  for (i in 0..<20) {
    model.addRow(arrayOf("Name: $i", i, i % 2 == 0))
  }
  val table = FishEyeTable(model)
  table.setRowSelectionInterval(0, 0)

  val scroll = JScrollPane(table)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.preferredSize = Dimension(320, 240)

  return JPanel(BorderLayout()).also {
    it.add(scroll, BorderLayout.NORTH)
  }
}

private class FishEyeRowContext(
  val height: Int,
  val font: Font,
  val color: Color,
)

private class FishEyeTable(m: TableModel) : JTable(m) {
  private val fishEyeRowList: List<FishEyeRowContext>
  private val minFont: Font

  @Transient private var handler: FishEyeTableHandler? = null

  init {
    val font = font
    minFont = font.deriveFont(8f)
    val font12 = font.deriveFont(10f)
    val font18 = font.deriveFont(16f)
    val font24 = font.deriveFont(22f)
    val font32 = font.deriveFont(30f)
    val color12 = Color(250, 250, 250)
    val color18 = Color(245, 245, 245)
    val color24 = Color(240, 240, 240)
    val color32 = Color(230, 230, 250)

    fishEyeRowList = listOf(
      FishEyeRowContext(12, font12, color12),
      FishEyeRowContext(18, font18, color18),
      FishEyeRowContext(24, font24, color24),
      FishEyeRowContext(32, font32, color32),
      FishEyeRowContext(24, font24, color24),
      FishEyeRowContext(18, font18, color18),
      FishEyeRowContext(12, font12, color12),
    )
  }

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    getSelectionModel().removeListSelectionListener(handler)
    super.updateUI()
    columnSelectionAllowed = false
    setRowSelectionAllowed(true)
    fillsViewportHeight = true

    handler = FishEyeTableHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
    getSelectionModel().addListSelectionListener(handler)
  }

  private inner class FishEyeTableHandler : MouseAdapter(), ListSelectionListener {
    var prevRow = -1
    var prevHeight = 0

    override fun mouseMoved(e: MouseEvent) {
      update(rowAtPoint(e.point))
    }

    override fun mouseDragged(e: MouseEvent) {
      update(rowAtPoint(e.point))
    }

    override fun mousePressed(e: MouseEvent) {
      e.component.repaint()
    }

    override fun valueChanged(e: ListSelectionEvent) {
      if (!e.valueIsAdjusting) {
        update(selectedRow)
      }
    }

    private fun update(row: Int) {
      if (prevRow != row) {
        initRowHeight(prevHeight, row)
        prevRow = row
      }
    }
  }

  override fun doLayout() {
    super.doLayout()
    val p = SwingUtilities.getAncestorOfClass(JViewport::class.java, this)
    if (p is JViewport) {
      val h = p.extentSize.height
      if (h != handler?.prevHeight) {
        initRowHeight(h, selectedRow)
        handler?.prevHeight = h
      }
    }
  }

  override fun prepareRenderer(
    renderer: TableCellRenderer,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareRenderer(renderer, row, column)
    val rowCount = model.rowCount
    var color = Color.WHITE
    var font = minFont
    val ccRow = handler?.prevRow ?: -1
    var index = 0
    val rd2 = (fishEyeRowList.size - 1) / 2
    for (i in -rd2..<rowCount) {
      if (ccRow - rd2 <= i && i <= ccRow + rd2) {
        if (i == row) {
          color = fishEyeRowList[index].color
          font = fishEyeRowList[index].font
          break
        }
        index++
      }
    }
    c.font = font
    c.background = if (isRowSelected(row)) getSelectionBackground() else color
    return c
  }

  private fun getViewableColoredRowCount(idx: Int): Int {
    val rd2 = (fishEyeRowList.size - 1) / 2
    val rc = model.rowCount
    return if (rd2 - idx > 0) {
      rd2 + 1 + idx
    } else if (idx > rc - 1 - rd2 && idx < rc - 1 + rd2) {
      rc - idx + rd2
    } else {
      fishEyeRowList.size
    }
  }

  @Suppress("LoopWithTooManyJumpStatements")
  private fun initRowHeight(
    height: Int,
    ccRow: Int,
  ) {
    val rd2 = (fishEyeRowList.size - 1) / 2
    val rowCount = model.rowCount
    val viewRc = getViewableColoredRowCount(ccRow)
    // var viewH = 0
    // for (i in 0..<viewRc) {
    //   viewH += fishEyeRowList[i].height
    // }
    // val viewH = fishEyeRowList.filterIndexed { i, _ -> i < viewRc }.sumBy { it.height }
    val viewH = fishEyeRowList.filterIndexed { i, _ -> i < viewRc }.sumOf { it.height }
    val restRc = rowCount - viewRc
    val restH = height - viewH
    val restRh = maxOf(1, restH / restRc)
    var restGap = restH - restRh * restRc
    var index = -1
    val range = ccRow - rd2..ccRow + rd2
    for (i in -rd2..<rowCount) {
      val crh = if (range.contains(i)) {
        index++
        if (i < 0) {
          continue
        }
        fishEyeRowList[index].height
      } else {
        if (i < 0) {
          continue
        }
        restRh + if (restGap-- > 0) 1 else 0
      }
      setRowHeight(i, crh)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
