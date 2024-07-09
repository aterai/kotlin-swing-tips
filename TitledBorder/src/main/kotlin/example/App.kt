package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val border = BorderFactory.createTitledBorder("Test Test")
  val panel = JPanel()
  panel.border = border

  val positionChoices = JComboBox(VerticalOrientation.entries.toTypedArray())
  positionChoices.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is VerticalOrientation) {
      border.titlePosition = item.mode
      panel.repaint()
    }
  }

  val justificationChoices = JComboBox(Justification.entries.toTypedArray())
  justificationChoices.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is Justification) {
      border.titleJustification = item.mode
      panel.repaint()
    }
  }

  val c = GridBagConstraints()
  c.gridx = 0
  c.insets = Insets(5, 5, 5, 5)
  c.anchor = GridBagConstraints.LINE_END

  val p2 = JPanel(GridBagLayout())
  p2.add(JLabel("TitlePosition:"), c)
  p2.add(JLabel("TitleJustification:"), c)
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  p2.add(positionChoices, c)
  p2.add(justificationChoices, c)

  val p = JPanel(BorderLayout(5, 5))
  p.add(p2, BorderLayout.NORTH)
  p.add(panel)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

private enum class VerticalOrientation(
  val mode: Int,
  private val description: String,
) {
  DEFAULT_POSITION(TitledBorder.DEFAULT_POSITION, "Default Position"),
  ABOVE_TOP(TitledBorder.ABOVE_TOP, "Above Top"),
  TOP(TitledBorder.TOP, "Top"),
  BELOW_TOP(TitledBorder.BELOW_TOP, "Below Top"),
  ABOVE_BOTTOM(TitledBorder.ABOVE_BOTTOM, "Above Bottom"),
  BOTTOM(TitledBorder.BOTTOM, "Bottom"),
  BELOW_BOTTOM(TitledBorder.BELOW_BOTTOM, "Below Bottom"),
  ;

  override fun toString() = description
}

private enum class Justification(
  val mode: Int,
  private val description: String,
) {
  DEFAULT_JUSTIFICATION(TitledBorder.DEFAULT_JUSTIFICATION, "Default Justification"),
  LEFT(TitledBorder.LEFT, "Left"),
  CENTER(TitledBorder.CENTER, "Center"),
  RIGHT(TitledBorder.RIGHT, "Right"),
  LEADING(TitledBorder.LEADING, "Leading"),
  TRAILING(TitledBorder.TRAILING, "Trailing"),
  ;

  override fun toString() = description
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
