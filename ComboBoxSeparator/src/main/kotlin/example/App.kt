package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.add(makeComboBox(makeModel()))
  box.border = BorderFactory.createTitledBorder("ComboBoxSeparator")

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea("JTextArea")))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): ComboBoxModel<Any> {
  val model = object : DefaultComboBoxModel<Any>() {
    override fun setSelectedItem(anObject: Any) {
      if (anObject !is JSeparator) {
        super.setSelectedItem(anObject)
      }
    }
  }
  model.addElement("0000")
  model.addElement("0000111")
  model.addElement("000011122")
  model.addElement("00001112233333")
  model.addElement(JSeparator())
  model.addElement("bbb1")
  model.addElement("bbb12")
  model.addElement("bbb33333")
  model.addElement(JSeparator())
  model.addElement("11111")
  model.addElement("2222222")
  return model
}

private fun <E> makeComboBox(model: ComboBoxModel<E>): JComboBox<E> {
  val combo = object : JComboBox<E>(model) {
    override fun updateUI() {
      setRenderer(null)
      super.updateUI()
      val renderer = getRenderer()
      setRenderer { list, value, index, isSelected, cellHasFocus ->
        if (value is JSeparator) {
          value
        } else {
          renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        }
      }
    }
  }

  val selectPrevKey = "selectPrevious3"
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val cb = e.source as? JComboBox<*> ?: return
      val index = cb.selectedIndex
      if (index == 0) {
        return
      }
      val o = cb.getItemAt(index - 1)
      if (o is JSeparator) {
        cb.setSelectedIndex(index - 2)
      } else {
        cb.setSelectedIndex(index - 1)
      }
    }
  }
  val am = combo.actionMap
  am.put(selectPrevKey, a1)

  val selectNextKey = "selectNext3"
  val a2 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val cb = e.source as? JComboBox<*> ?: return
      val index = cb.selectedIndex
      if (index == cb.itemCount - 1) {
        return
      }
      val o = cb.getItemAt(index + 1)
      if (o is JSeparator) {
        cb.setSelectedIndex(index + 2)
      } else {
        cb.setSelectedIndex(index + 1)
      }
    }
  }
  am.put(selectNextKey, a2)

  val im = combo.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), selectPrevKey)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0), selectPrevKey)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), selectNextKey)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0), selectNextKey)
  return combo
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
