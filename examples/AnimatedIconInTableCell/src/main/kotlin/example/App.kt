package example

import java.awt.*
import java.awt.image.ImageObserver
import java.net.URL
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/restore_to_background_color.gif")
  val icon = url?.let { ImageIcon(it) } ?: UIManager.getIcon("html.missingImage")
  val table = JTable()
  val data = arrayOf(
    arrayOf("Default ImageIcon", icon),
    arrayOf("ImageIcon#setImageObserver", makeAnimatedIcon(url, table, 1, 1)),
  )
  val columnNames = arrayOf("String", "ImageIcon")
  table.model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = column == 0
  }
  table.autoCreateRowSorter = true
  table.rowHeight = 20

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeAnimatedIcon(
  url: URL?,
  table: JTable,
  row: Int,
  col: Int,
): Icon = if (url == null) {
  UIManager.getIcon("html.missingImage")
} else {
  ImageIcon(url).also {
    it.imageObserver = ImageObserver { _, infoFlags, _, _, _, _ ->
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
