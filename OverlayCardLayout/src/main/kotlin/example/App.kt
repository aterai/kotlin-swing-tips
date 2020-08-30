package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private fun makePanel(color: Color): Component {
  val p = JPanel(GridBagLayout())
  p.background = color
  p.add(JButton("JButton on the %s JPanel".format(color)))
  return p
}

fun makeUI(): Component {
  val model = arrayOf("red", "green", "blue")
  val cardLayout = CardLayout()
  val cards = JPanel(cardLayout)
  cards.add(makePanel(Color.RED), model[0])
  cards.add(makePanel(Color.GREEN), model[1])
  cards.add(makePanel(Color.BLUE), model[2])

  val combo = JComboBox(model)
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      cardLayout.show(cards, e.item.toString())
    }
  }

  val pp = JPanel(BorderLayout())
  pp.isOpaque = false
  pp.border = BorderFactory.createEmptyBorder(8, 24, 0, 24)
  pp.add(combo, BorderLayout.NORTH)

  val p = object : JPanel() {
    override fun isOptimizedDrawingEnabled() = false
  }
  p.layout = OverlayLayout(p)
  p.add(pp)
  p.add(cards)
  p.preferredSize = Dimension(320, 240)
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
