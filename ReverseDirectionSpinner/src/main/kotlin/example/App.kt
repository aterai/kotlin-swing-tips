package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createVerticalBox()

  val spinner0 = JSpinner(SpinnerNumberModel(5, 0, 10, 1))
  box.add(makeTitledPanel("stepSize: 1", spinner0))
  box.add(Box.createVerticalStrut(10))

  val spinner1 = JSpinner(SpinnerNumberModel(5, 0, 10, -1))
  box.add(makeTitledPanel("stepSize: -1", spinner1))
  box.add(Box.createVerticalStrut(10))

  val scale = arrayOf(
    "AAA",
    "AA+",
    "AA",
    "AA-",
    "A+",
    "A",
    "A-",
    "BBB+",
    "BBB",
    "BBB-",
    "BB+",
    "BB",
    "BB-",
    "B+",
    "B",
    "B-",
    "CCC+",
    "CCC",
    "CCC-",
    "CC",
    "R",
    "D"
  )
  val spinner2 = JSpinner(SpinnerListModel(scale))
  box.add(makeTitledPanel("SpinnerListModel", spinner2))
  box.add(Box.createVerticalStrut(10))

  val m3 = object : SpinnerListModel(scale) {
    override fun getNextValue() = super.getPreviousValue()

    override fun getPreviousValue() = super.getNextValue()
  }
  val spinner3 = JSpinner(m3)
  box.add(makeTitledPanel("Reverse direction SpinnerListModel", spinner3))

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
