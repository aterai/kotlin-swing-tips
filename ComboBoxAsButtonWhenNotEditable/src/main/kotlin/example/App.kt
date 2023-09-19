package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val combo = makeComboBox(false)
  val d = UIManager.getLookAndFeelDefaults()
  d["ComboBox.buttonWhenNotEditable"] = false
  combo.putClientProperty("Nimbus.Overrides", d)
  combo.putClientProperty("Nimbus.Overrides.InheritDefaults", true)

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Default(editable):", makeComboBox(true)))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Default:", makeComboBox(false)))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("ComboBox.buttonWhenNotEditable: FALSE", combo))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private fun makeComboBox(editable: Boolean): JComboBox<String> {
  val model = DefaultComboBoxModel<String>()
  model.addElement("11111")
  model.addElement("22222222")
  model.addElement("33333333333")
  val combo = JComboBox(model)
  combo.isEditable = editable
  return combo
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
