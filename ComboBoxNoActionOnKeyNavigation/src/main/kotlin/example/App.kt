package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val key1 = "ComboBox.noActionOnKeyNavigation"
  val check1 = JCheckBox(key1, UIManager.getBoolean(key1))
  check1.addActionListener { e ->
    val cb = e.source as? JCheckBox
    UIManager.put(key1, cb?.isSelected == true)
  }

  val key2 = "ComboBox.isEnterSelectablePopup"
  val check2 = JCheckBox(key2, UIManager.getBoolean(key2))
  check2.addActionListener { e ->
    val cb = e.source as? JCheckBox
    UIManager.put(key2, cb?.isSelected == true)
  }

  val combo1 = JComboBox(makeModel())
  combo1.isEditable = false

  val combo2 = JComboBox(makeModel())
  combo2.isEditable = true

  val box1 = Box.createVerticalBox()
  box1.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box1.add(check1)
  box1.add(Box.createVerticalStrut(5))
  box1.add(check2)

  val box2 = Box.createVerticalBox()
  box2.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box2.add(combo1)
  box2.add(Box.createVerticalStrut(10))
  box2.add(combo2)

  val p = JPanel(GridLayout(2, 1))
  p.add(box1)
  p.add(box2)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): ComboBoxModel<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("00000")
  model.addElement("11111")
  model.addElement("22222")
  model.addElement("33333")
  model.addElement("44444")
  model.addElement("55555")
  model.addElement("66666")
  model.addElement("77777")
  model.addElement("88888")
  model.addElement("99999")
  return model
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
      minimumSize = Dimension(256, 200)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
