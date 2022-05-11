package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val BLACK_CIRCLE = "Åú" // "\u25CF"
private const val WHITE_CIRCLE = "Åõ" // "\u25CB"

fun makeUI(): Component {
  val label1 = JLabel(BLACK_CIRCLE, SwingConstants.CENTER)
  val label2 = JLabel("", SwingConstants.CENTER)
  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.add(makeTitledPanel("Åõ<->Åú", label1))
  p.add(makeTitledPanel("!!!Warning!!!<->Empty", label2))
  val timer1 = Timer(600) {
    label1.text = if (BLACK_CIRCLE == label1.text) WHITE_CIRCLE else BLACK_CIRCLE
  }
  val timer2 = Timer(300) {
    label2.text = if ("" == label2.text) "!!!Warning!!!" else ""
  }
  p.addHierarchyListener { e ->
    if (e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L) {
      if (e.component.isDisplayable) {
        timer1.start()
        timer2.start()
      } else {
        timer1.stop()
        timer2.stop()
      }
    }
  }
  p.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
  p.preferredSize = Dimension(320, 240)
  return p
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
