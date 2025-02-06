package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

private const val MAX_HISTORY = 10
private const val ENTER_PRESSED = "enterPressed"

fun makeUI(): Component {
  val model = arrayOf("123456", "7890", "a")

  val comboBox0 = JComboBox(model)
  comboBox0.isEditable = true

  val comboBox1 = object : JComboBox<String>(model) {
    @Transient private var handler: PopupMenuListener? = null

    override fun updateUI() {
      removePopupMenuListener(handler)
      super.updateUI()
      setEditable(true)
      handler = SelectItemMenuListener()
      addPopupMenuListener(handler)
    }
  }

  val comboBox2 = object : JComboBox<String>(model) {
    @Transient private var handler: PopupMenuListener? = null

    override fun updateUI() {
      removePopupMenuListener(handler)
      actionMap.put(ENTER_PRESSED, null)
      super.updateUI()
      val defaultAction = actionMap[ENTER_PRESSED]
      val a = object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
          val isPopupVisible = isPopupVisible
          setPopupVisible(false)
          val m = getModel()
          val str = getEditor()?.item?.toString() ?: ""
          if (m is DefaultComboBoxModel<String> && m.getIndexOf(str) < 0) {
            m.removeElement(str)
            m.insertElementAt(str, 0)
            if (m.size > MAX_HISTORY) {
              m.removeElementAt(MAX_HISTORY)
            }
            selectedIndex = 0
            setPopupVisible(isPopupVisible)
          } else {
            defaultAction.actionPerformed(e)
          }
        }
      }
      actionMap.put(ENTER_PRESSED, a)
      setEditable(true)
      handler = SelectItemMenuListener()
      addPopupMenuListener(handler)
    }
  }

  val p = JPanel(GridLayout(0, 1))
  p.add(JLabel("Default:", SwingConstants.LEFT))
  p.add(comboBox0)
  p.add(Box.createVerticalStrut(15))
  p.add(JLabel("popupMenuWillBecomeVisible:", SwingConstants.LEFT))
  p.add(comboBox1)
  p.add(Box.createVerticalStrut(15))
  p.add(JLabel("+enterPressed Action:", SwingConstants.LEFT))
  p.add(comboBox2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    it.preferredSize = Dimension(320, 240)
  }
}

private class SelectItemMenuListener : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    (e.source as? JComboBox<*>)?.also {
      it.selectedItem = it.editor.item
    }
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed
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
