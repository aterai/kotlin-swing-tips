package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.Serializable
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("String", "Integer", "Boolean")
    val data = arrayOf(arrayOf("aaa", -1, true))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }
    for (i in 0 until 20) {
      model.addRow(arrayOf("Name: $i", i, i % 2 == 0))
    }
    val table = FishEyeTable(model)
    table.setRowSelectionInterval(0, 0)
    val scroll = JScrollPane(table)
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
    scroll.setPreferredSize(Dimension(320, 240))
    add(scroll, BorderLayout.NORTH)
  }
}

internal class FishEyeRowContext(val height: Int, val font: Font, val color: Color) : Serializable {
  companion object {
    private const val serialVersionUID = 1L
  }
}

internal class FishEyeTable(m: TableModel) : JTable(m) {
  private val fishEyeRowList: List<FishEyeRowContext>
  private val minFont: Font
  @Transient
  private var handler: FishEyeTableHandler? = null

  init {
    val font = getFont()
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
      FishEyeRowContext(12, font12, color12)
    )
  }

  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    getSelectionModel().removeListSelectionListener(handler)
    super.updateUI()
    setColumnSelectionAllowed(false)
    setRowSelectionAllowed(true)
    setFillsViewportHeight(true)

    handler = FishEyeTableHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
    getSelectionModel().addListSelectionListener(handler)
  }

  private inner class FishEyeTableHandler : MouseAdapter(), ListSelectionListener {
    var prevRow = -1
    var prevHeight = 0

    override fun mouseMoved(e: MouseEvent) {
      val row = rowAtPoint(e.getPoint())
      if (prevRow == row) {
        return
      }
      initRowHeight(prevHeight, row)
      prevRow = row
    }

    override fun mouseDragged(e: MouseEvent) {
      val row = rowAtPoint(e.getPoint())
      if (prevRow == row) {
        return
      }
      initRowHeight(prevHeight, row)
      prevRow = row
    }

    override fun mousePressed(e: MouseEvent) {
      repaint()
    }

    override fun valueChanged(e: ListSelectionEvent) {
      if (e.getValueIsAdjusting()) {
        return
      }
      val row = getSelectedRow()
      if (prevRow == row) {
        return
      }
      initRowHeight(prevHeight, row)
      prevRow = row
    }
  }

  override fun doLayout() {
    super.doLayout()
    val p = SwingUtilities.getAncestorOfClass(JViewport::class.java, this) as? JViewport ?: return
    val h = p.getExtentSize().height
    if (h == handler?.prevHeight) {
      return
    }
    initRowHeight(h, getSelectedRow())
    handler?.prevHeight = h
  }

  override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
    val c = super.prepareRenderer(renderer, row, column)
    val rowCount = getModel().getRowCount()
    var color = Color.WHITE
    var font = minFont
    val ccRow = handler?.prevRow ?: -1
    var index = 0
    val rd2 = (fishEyeRowList.size - 1) / 2
    for (i in -rd2 until rowCount) {
      if (ccRow - rd2 <= i && i <= ccRow + rd2) {
        if (i == row) {
          color = fishEyeRowList[index].color
          font = fishEyeRowList[index].font
          break
        }
        index++
      }
    }
    c.setFont(font)
    c.setBackground(if (isRowSelected(row)) getSelectionBackground() else color)
    return c
  }

  private fun getViewableColoredRowCount(idx: Int): Int {
    val rd2 = (fishEyeRowList.size - 1) / 2
    val rc = getModel().getRowCount()
    return if (rd2 - idx > 0 && idx < rd2) {
      rd2 + 1 + idx
    } else if (idx > rc - 1 - rd2 && idx < rc - 1 + rd2) {
      rc - idx + rd2
    } else {
      fishEyeRowList.size
    }
  }

  private fun initRowHeight(height: Int, ccRow: Int) {
    val rd2 = (fishEyeRowList.size - 1) / 2
    val rowCount = getModel().getRowCount()
    val viewRc = getViewableColoredRowCount(ccRow)
    // var viewH = 0
    // for (i in 0 until viewRc) {
    //   viewH += fishEyeRowList[i].height
    // }
    val viewH = fishEyeRowList.map { it.height }.sum()
    val restRc = rowCount - viewRc
    val restH = height - viewH
    val restRh = maxOf(1, restH / restRc)
    var restGap = restH - restRh * restRc
    var index = -1
    for (i in -rd2 until rowCount) {
      val crh: Int
      if (ccRow - rd2 <= i && i <= ccRow + rd2) {
        index++
        if (i < 0) {
          continue
        }
        crh = fishEyeRowList[index].height
      } else {
        if (i < 0) {
          continue
        }
        crh = restRh + if (restGap > 0) 1 else 0
        restGap--
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
