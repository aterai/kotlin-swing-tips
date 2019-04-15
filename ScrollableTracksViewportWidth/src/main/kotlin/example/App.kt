package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(0, 1)) {
  init {
    val table1 = JTable(1, 3)
    val scroll1 = JScrollPane(table1)
    scroll1.setBorder(BorderFactory.createTitledBorder("AUTO_RESIZE_SUBSEQUENT_COLUMNS(Default)"))

    val table2 = JTable(1, 3)
    table2.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
    val scroll2 = JScrollPane(table2)
    scroll2.setBorder(BorderFactory.createTitledBorder("AUTO_RESIZE_OFF"))

    val table3 = object : JTable(1, 3) {
      // java - How to make JTable both AutoResize and horizontall scrollable? - Stack Overflow
      // https://stackoverflow.com/questions/6104916/how-to-make-jtable-both-autoresize-and-horizontall-scrollable
      override fun getScrollableTracksViewportWidth() = getPreferredSize().width < getParent().getWidth()
    }
    table3.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
    val scroll3 = JScrollPane(table3)
    scroll3.setBorder(BorderFactory.createTitledBorder("AUTO_RESIZE_OFF + getScrollableTracksViewportWidth()"))

    add(scroll1)
    add(scroll2)
    add(scroll3)
    setPreferredSize(Dimension(320, 240))
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
