package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val table1 = JTable(1, 3)
  val scroll1 = JScrollPane(table1)
  val title1 = "AUTO_RESIZE_SUBSEQUENT_COLUMNS(Default)"
  scroll1.border = BorderFactory.createTitledBorder(title1)

  val table2 = JTable(1, 3)
  table2.autoResizeMode = JTable.AUTO_RESIZE_OFF
  val scroll2 = JScrollPane(table2)
  val title2 = "AUTO_RESIZE_OFF"
  scroll2.border = BorderFactory.createTitledBorder(title2)

  val table3 = object : JTable(1, 3) {
    // java - How to make JTable both AutoResize and horizontally scrollable? - Stack Overflow
    // https://stackoverflow.com/questions/6104916/how-to-make-jtable-both-autoresize-and-horizontall-scrollable
    override fun getScrollableTracksViewportWidth() = preferredSize.width < parent.width
  }
  table3.autoResizeMode = JTable.AUTO_RESIZE_OFF
  val scroll3 = JScrollPane(table3)
  val title3 = "AUTO_RESIZE_OFF + getScrollableTracksViewportWidth()"
  scroll3.border = BorderFactory.createTitledBorder(title3)

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
