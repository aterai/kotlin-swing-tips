package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val table1 = JTable(1, 3)
  val scroll1 = JScrollPane(table1)
  scroll1.border = BorderFactory.createTitledBorder("AUTO_RESIZE_SUBSEQUENT_COLUMNS(Default)")

  val table2 = JTable(1, 3)
  table2.autoResizeMode = JTable.AUTO_RESIZE_OFF
  val scroll2 = JScrollPane(table2)
  scroll2.border = BorderFactory.createTitledBorder("AUTO_RESIZE_OFF")

  val table3 = object : JTable(1, 3) {
    // java - How to make JTable both AutoResize and horizontall scrollable? - Stack Overflow
    // https://stackoverflow.com/questions/6104916/how-to-make-jtable-both-autoresize-and-horizontall-scrollable
    override fun getScrollableTracksViewportWidth() = preferredSize.width < parent.width
  }
  table3.autoResizeMode = JTable.AUTO_RESIZE_OFF
  val scroll3 = JScrollPane(table3)
  scroll3.border = BorderFactory.createTitledBorder("AUTO_RESIZE_OFF + getScrollableTracksViewportWidth()")

  return JPanel(GridLayout(0, 1)).also {
    it.add(scroll1)
    it.add(scroll2)
    it.add(scroll3)
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
