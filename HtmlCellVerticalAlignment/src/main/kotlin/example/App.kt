package example

import java.awt.*
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.DefaultTableModel

private const val TEXT = "drag select table cells 1 22 333 4444 55555 666666 7777777 88888888"

fun makeUI(): Component {
  val columnNames = arrayOf("html")
  val data = arrayOf(
    arrayOf("<html><font color=red>font color red</font><br /> $TEXT"),
    arrayOf("<html><font color=green>font color green</font> $TEXT"),
    arrayOf("<html><font color=blue>font color blue</font> $TEXT"),
    arrayOf("<html><font color=black>font color black</font><br />  $TEXT"),
    arrayOf("<html><font color=orange>font color orange</font> $TEXT"),
    arrayOf("<html><font color=gray>font color gray</font> $TEXT"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = String::class.java

    override fun isCellEditable(row: Int, column: Int) = false
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.rowHeight = 16

  val centerRadio = JRadioButton("CENTER", true)
  val topRadio = JRadioButton("TOP")
  val bottomRadio = JRadioButton("BOTTOM")
  val al = ActionListener {
    (table.getDefaultRenderer(String::class.java) as? JLabel)?.verticalAlignment = when {
      topRadio.isSelected -> SwingConstants.TOP
      bottomRadio.isSelected -> SwingConstants.BOTTOM
      else -> SwingConstants.CENTER
    }
    table.repaint()
  }

  val bg = ButtonGroup()
  val p = JPanel()
  listOf(centerRadio, topRadio, bottomRadio).forEach {
    it.addActionListener(al)
    bg.add(it)
    p.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.add(p, BorderLayout.SOUTH)
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
