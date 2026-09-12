package example

import java.awt.*
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

fun createUI(): Component {
  val combo00 = createComboBox()
  combo00.isEditable = false
  val combo01 = createComboBox()
  combo01.isEditable = true

  val combo02 = createComboBox()
  combo02.isEditable = false
  combo02.addPopupMenuListener(WidePopupMenuListener())

  val combo03 = createComboBox()
  combo03.isEditable = true
  combo03.addPopupMenuListener(WidePopupMenuListener())

  val gap = 5
  val p = JPanel(GridLayout(4, 2, gap, gap))
  p.add(combo00)
  p.add(JLabel("<- normal"))
  p.add(combo01)
  p.add(JLabel("<- normal, editable"))
  p.add(combo02)
  p.add(JLabel("<- wide"))
  p.add(combo03)
  p.add(JLabel("<- wide, editable"))

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(gap, gap, gap, gap)
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createComboBox(): JComboBox<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("1111")
  model.addElement("22222222")
  model.addElement("3333333333")
  model.addElement("012345678901234567890123456789012345678901234567890123456789")
  model.addElement("444")
  model.addElement("55555")
  return JComboBox(model)
}

// How to widen the drop-down list in a JComboBox
// https://community.oracle.com/thread/1368300
private class WidePopupMenuListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val combo = e.source as? JComboBox<*> ?: return
    val size = combo.size
    if (size.width < POPUP_MIN_WIDTH) {
      // Temporarily widen the combo box so that BasicComboPopup#getPopupLocation()
      // sizes the popup from the widened bounds. The nested showPopup() fires this
      // listener again, but the width check above prevents infinite recursion.
      combo.setSize(POPUP_MIN_WIDTH, size.height)
      combo.showPopup()
      // // Java 8
      // combo.size = size
      // Java 21: the outer BasicComboPopup#show() still calls getPopupLocation()
      // after this listener returns, so restoring the size synchronously would
      // shrink the already visible popup back to the combo box width.
      EventQueue.invokeLater { combo.size = size }
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
  }

  companion object {
    private const val POPUP_MIN_WIDTH = 300
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
