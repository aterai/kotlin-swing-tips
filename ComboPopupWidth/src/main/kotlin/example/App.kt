package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

fun makeUI(): Component {
  val combo00 = makeComboBox()
  combo00.isEditable = false
  val combo01 = makeComboBox()
  combo01.isEditable = true

  val combo02 = makeComboBox()
  combo02.isEditable = false
  combo02.addPopupMenuListener(WidePopupMenuListener())

  val combo03 = makeComboBox()
  combo03.isEditable = true
  combo03.addPopupMenuListener(WidePopupMenuListener())

  val g = 5
  val p = JPanel(GridLayout(4, 2, g, g))
  p.add(combo00)
  p.add(JLabel("<- normal"))
  p.add(combo01)
  p.add(JLabel("<- normal, editable"))
  p.add(combo02)
  p.add(JLabel("<- wide"))
  p.add(combo03)
  p.add(JLabel("<- wide, editable"))

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(g, g, g, g)
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeComboBox(): JComboBox<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("1111")
  model.addElement("22222222")
  model.addElement("3333333333")
  model.addElement("012345678901234567890123456789012345678901234567890123456789")
  model.addElement("444")
  model.addElement("55555")
  return JComboBox(model)
}

private class WidePopupMenuListener : PopupMenuListener {
  private var adjusting = false
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    val combo = e.source as JComboBox<*>
    val size = combo.size
    if (size.width >= POPUP_MIN_WIDTH) {
      return
    }
    if (!adjusting) {
      adjusting = true
      combo.setSize(POPUP_MIN_WIDTH, size.height)
      combo.showPopup()
    }
    combo.size = size
    adjusting = false
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    /* not needed */
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    /* not needed */
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
