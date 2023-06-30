package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = JScrollPane(JTable(model))
  sp.bottomComponent = JScrollPane(JTree())

  val r0 = JRadioButton("0.0", true)
  r0.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      sp.resizeWeight = 0.0
    }
  }

  val r1 = JRadioButton("0.5")
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      sp.resizeWeight = .5
    }
  }

  val r2 = JRadioButton("1.0")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      sp.resizeWeight = 1.0
    }
  }

  val bg = ButtonGroup()
  val p = JPanel()
  p.add(JLabel("JSplitPane#setResizeWeight: "))
  listOf(r0, r1, r2).forEach {
    bg.add(it)
    p.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
    EventQueue.invokeLater { sp.setDividerLocation(.5) }
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
