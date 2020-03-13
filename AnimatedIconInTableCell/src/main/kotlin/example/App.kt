package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.ImageObserver
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

fun makeImageIcon(url: URL?, table: JTable, row: Int, col: Int): ImageIcon {
  val icon = ImageIcon(url)
  icon.imageObserver = ImageObserver { _, infoFlags, _, _, _, _ ->
    if (!table.isShowing) {
      false
    } else {
      if (infoFlags and (ImageObserver.FRAMEBITS or ImageObserver.ALLBITS) != 0) {
        val vr = table.convertRowIndexToView(row) // JDK 1.6.0
        val vc = table.convertColumnIndexToView(col)
        table.repaint(table.getCellRect(vr, vc, false))
      }
      infoFlags and (ImageObserver.ALLBITS or ImageObserver.ABORT) == 0
    }
  }
  return icon
}

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/restore_to_background_color.gif")
  val table = JTable()
  val data = arrayOf(
    arrayOf("Default ImageIcon", ImageIcon(url)),
    arrayOf("ImageIcon#setImageObserver", makeImageIcon(url, table, 1, 1))
  )
  val columnNames = arrayOf("String", "ImageIcon")
  table.model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, column: Int) = column == 0
  }
  table.autoCreateRowSorter = true
  table.rowHeight = 20

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
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
