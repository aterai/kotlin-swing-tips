package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicSplitPaneUI
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val m1 = DefaultTableModel(100, 256)
  val table = JTable(m1)
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF

  val scroll = JScrollPane(table)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS

  val panel = JPanel(BorderLayout())
  panel.isOpaque = false
  panel.border = BorderFactory.createEmptyBorder(3, 0, 0, 0)
  panel.add(scroll.horizontalScrollBar)

  val r1 = JRadioButton("a", true)
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      table.model = m1
    }
  }

  val m2 = DefaultTableModel(50, 8)
  val r2 = JRadioButton("b")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      table.model = m2
    }
  }

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder()
  val bg = ButtonGroup()
  listOf(r1, r2).forEach {
    it.isOpaque = false
    bg.add(it)
    box.add(it)
  }
  box.add(Box.createHorizontalGlue())

  val horizontalBox = JSplitPane(JSplitPane.HORIZONTAL_SPLIT).also {
    it.background = Color.WHITE
    it.leftComponent = box
    it.rightComponent = panel
    it.isContinuousLayout = true
    it.border = BorderFactory.createEmptyBorder()
    EventQueue.invokeLater { it.setDividerLocation(.4) }
  }

  val tripleColon = JLabel("⫶").also {
    it.foreground = Color.GRAY
    it.border = BorderFactory.createEmptyBorder(3, 0, 0, 0)
  }

  (horizontalBox.ui as? BasicSplitPaneUI)?.divider?.also {
    it.layout = BorderLayout()
    it.border = BorderFactory.createEmptyBorder()
    it.background = Color.WHITE
    it.add(tripleColon)
    it.dividerSize = 8
  }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(horizontalBox, BorderLayout.SOUTH)
    it.background = Color.WHITE
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
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
