package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

private val log = JTextArea()

fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1))
  p.add(JLabel("ComboBox.isEnterSelectablePopup: false(default)", SwingConstants.LEFT))
  p.add(makeComboBox(false))
  p.add(JLabel("ComboBox.isEnterSelectablePopup: true", SwingConstants.LEFT))
  p.add(makeComboBox(true))

  return JPanel(BorderLayout(5, 5)).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeComboBox(isEnterSelectable: Boolean): JComboBox<String> {
  // UIManager.put("ComboBox.isEnterSelectablePopup", Boolean.TRUE)
  val combo = JComboBox(arrayOf("aaa", "bbb", "CCC", "DDD"))
  combo.isEditable = true
  val pml = object : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      UIManager.put("ComboBox.isEnterSelectablePopup", isEnterSelectable)
      val b = UIManager.getBoolean("ComboBox.isEnterSelectablePopup")
      append("isEnterSelectablePopup: $b")
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      // not needed
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      // not needed
    }
  }
  combo.addPopupMenuListener(pml)
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      append("ItemListener: " + e.item)
    }
  }
  return combo
}

fun append(text: String) {
  log.append("$text\n")
  log.caretPosition = log.document.length
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
