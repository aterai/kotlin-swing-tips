package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  // val o = UIManager.get("Table.scrollPaneCornerComponent")
  val table = object : JTable(15, 3) {
    override fun getScrollableTracksViewportWidth(): Boolean {
      val c = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this)
      (c as? JScrollPane)?.also {
        it.getCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER)?.isVisible =
          preferredSize.width >= it.viewport.width
      }
      return super.getScrollableTracksViewportWidth()
    }
  }
  return JPanel(GridLayout(0, 1)).also {
    listOf(JTable(15, 3), table).forEach { t ->
      t.autoResizeMode = JTable.AUTO_RESIZE_OFF
      it.add(JScrollPane(t))
    }
    it.preferredSize = Dimension(320, 240)
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
