package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val model = DefaultListModel<String>().also {
    it.addElement("Name1-comment")
    it.addElement("Name2-test")
    it.addElement("11111111111")
    it.addElement("35663456345634563456")
    it.addElement("222222222222222222")
    it.addElement("Name0-333333333")
    it.addElement("44444444444444444444")
    it.addElement("5555555555555555")
    it.addElement("66666666666666666666666")
    it.addElement("4352345123452345234523452345234534")
  }
  val list = RollOverList(model)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(list))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RollOverList<E>(model: ListModel<E>) : JList<E>(model) {
  private var rollOverHandler: RollOverCellHandler? = null
  override fun updateUI() {
    removeMouseListener(rollOverHandler)
    removeMouseMotionListener(rollOverHandler)
    selectionBackground = null // Nimbus
    super.updateUI()
    rollOverHandler = RollOverCellHandler()
    addMouseMotionListener(rollOverHandler)
    addMouseListener(rollOverHandler)
    cellRenderer = rollOverHandler
  }

  private inner class RollOverCellHandler : MouseAdapter(), ListCellRenderer<E> {
    private val rolloverBackground = Color(0xDC_F0_FF)
    private var rollOverRowIndex = -1
    private val r: ListCellRenderer<in E> = DefaultListCellRenderer()
    override fun getListCellRendererComponent(
      list: JList<out E>,
      value: E?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component = r.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus
    ).also {
      if (index == rollOverRowIndex) {
        it.background = rolloverBackground
        if (isSelected) {
          it.foreground = Color.BLACK
        }
      }
    }

    override fun mouseExited(e: MouseEvent) {
      rollOverRowIndex = -1
      repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
      val row = locationToIndex(e.point)
      if (row != rollOverRowIndex) {
        val rect = getCellBounds(row, row)
        if (rollOverRowIndex >= 0) {
          rect.add(getCellBounds(rollOverRowIndex, rollOverRowIndex))
        }
        rollOverRowIndex = row
        repaint(rect)
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
