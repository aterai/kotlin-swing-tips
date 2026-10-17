package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicSplitPaneUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

fun createUI(): Component {
  val model1 = DefaultTableModel(100, 256)
  val table = JTable(model1)
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF

  val scroll = JScrollPane(table)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS

  // Move the horizontal JScrollBar out of the JScrollPane,
  // it still shares the BoundedRangeModel with the viewport
  val scrollBox = JPanel(BorderLayout())
  scrollBox.isOpaque = false
  scrollBox.border = BorderFactory.createEmptyBorder(3, 0, 0, 0)
  scrollBox.add(scroll.horizontalScrollBar)

  val model2 = DefaultTableModel(50, 8)
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder()
  val group = ButtonGroup()
  val r1 = createRadioButton("a", table, model1)
  r1.isSelected = true
  val r2 = createRadioButton("b", table, model2)
  listOf(r1, r2).forEach {
    group.add(it)
    box.add(it)
  }
  box.add(Box.createHorizontalGlue())

  val horizontalSplit = createScrollSplitPane()
  horizontalSplit.leftComponent = box
  horizontalSplit.rightComponent = scrollBox

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(horizontalSplit, BorderLayout.SOUTH)
    it.background = Color.WHITE
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createRadioButton(
  text: String,
  table: JTable,
  model: TableModel,
) = JRadioButton(text).also {
  it.isOpaque = false
  it.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      table.model = model
    }
  }
}

private fun createScrollSplitPane(): JSplitPane {
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  split.background = Color.WHITE
  split.isContinuousLayout = true
  split.border = BorderFactory.createEmptyBorder()
  EventQueue.invokeLater { split.setDividerLocation(.4) }

  val tripleColon = JLabel("⫶")
  tripleColon.foreground = Color.GRAY
  tripleColon.border = BorderFactory.createEmptyBorder(3, 2, 0, 4)

  (split.ui as? BasicSplitPaneUI)?.divider?.also {
    it.layout = BorderLayout()
    it.border = BorderFactory.createEmptyBorder()
    it.background = Color.WHITE
    it.add(tripleColon)
  }
  // Set the size via JSplitPane instead of BasicSplitPaneDivider#setDividerSize(...),
  // JSplitPane#getDividerSize() is used by setDividerLocation(double)
  // and it is propagated to the divider by BasicSplitPaneUI
  split.dividerSize = tripleColon.preferredSize.width
  return split
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
