package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val key = "Spinner.disableOnBoundaryValues"
  val flg = UIManager.getLookAndFeelDefaults().getBoolean(key)
  // println(flg)

  val model = SpinnerNumberModel(0, 0, 10, 1)
  val spinner1 = JSpinner(model)
  spinner1.font = spinner1.font.deriveFont(32f)

  val spinner2 = JSpinner(model)
  spinner2.font = spinner2.font.deriveFont(32f)

  val check = JCheckBox(key, flg)
  check.addActionListener { e ->
    UIManager.put(key, (e.source as? JCheckBox)?.isSelected == true)
    SwingUtilities.updateComponentTreeUI(spinner2)
  }

  val p = JPanel(GridLayout(2, 1))
  p.add(spinner2)
  p.add(check)

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("default", spinner1))
  box.add(Box.createVerticalStrut(15))
  box.add(makeTitledPanel(key, p))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
