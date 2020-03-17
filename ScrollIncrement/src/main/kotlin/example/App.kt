package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val LF = "\n"

fun makeUI(): Component {
  val buf = StringBuilder()
  for (i in 0 until 100) {
    buf.append(i).append(LF)
  }

  val scrollPane = JScrollPane(JTextArea(buf.toString()))
  scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS

  val vsb = scrollPane.verticalScrollBar
  val model = SpinnerNumberModel(vsb.getUnitIncrement(1), 1, 100000, 1)
  model.addChangeListener {
    vsb.unitIncrement = model.number.toInt()
  }

  val spinner = JSpinner(model)
  spinner.editor = JSpinner.NumberEditor(spinner, "#####0")

  val box = Box.createHorizontalBox()
  box.add(JLabel("Unit Increment:"))
  box.add(Box.createHorizontalStrut(2))
  box.add(spinner)
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(scrollPane)
    it.preferredSize = Dimension(320, 240)
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
