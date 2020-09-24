package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val combo = JComboBox(makeModel())

  val m = SpinnerNumberModel(combo.maximumRowCount, -1, 1000, 1)
  m.addChangeListener {
    combo.setMaximumRowCount(m.number.toInt())
  }
  val spinner = JSpinner(m)

  val p = JPanel(BorderLayout()).also {
    it.border = BorderFactory.createTitledBorder("JComboBox#setMaximumRowCount:")
    it.add(spinner)
  }

  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.add(p)
    it.add(Box.createVerticalStrut(10))
    it.add(combo)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultComboBoxModel<String> {
  val model = DefaultComboBoxModel<String>()
  for (i in 0 until 100) {
    model.addElement("Item$i")
  }
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
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
