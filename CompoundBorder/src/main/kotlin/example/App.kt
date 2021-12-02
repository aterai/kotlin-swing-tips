package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val b0 = BorderFactory.createLineBorder(Color.GRAY)
  val b1 = BorderFactory.createTitledBorder(b0, "TitledBorder")
  b1.titleJustification = TitledBorder.CENTER
  val p1 = JPanel()
  p1.border = b1

  val raisedBevel = BorderFactory.createRaisedBevelBorder()
  val topLine = BorderFactory.createMatteBorder(10, 0, 0, 0, Color.GRAY.brighter())
  val loweredBevel = BorderFactory.createLoweredBevelBorder()
  val compound1 = BorderFactory.createCompoundBorder(raisedBevel, topLine)
  val compound2 = BorderFactory.createCompoundBorder(compound1, loweredBevel)
  val b2 = BorderFactory.createTitledBorder(compound2, "CompoundBorder")
  b2.titleJustification = TitledBorder.CENTER
  val p2 = JPanel()
  p2.border = b2

  return JPanel(GridLayout(2, 1, 5, 5)).also {
    it.add(p1)
    it.add(p2)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
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
