package example

import java.awt.*
import javax.swing.*

private const val STEP = 5
private const val EXTENT = 20
private const val MIN = 0
private const val MAX = EXTENT * 10 // 200
private const val VALUE = 50

fun makeUI(): Component {
  val scrollbar = JScrollBar(Adjustable.HORIZONTAL, VALUE, EXTENT, MIN, MAX + EXTENT)
  val model = SpinnerNumberModel(VALUE, MIN, MAX, STEP)

  scrollbar.unitIncrement = STEP
  scrollbar.model.addChangeListener { e ->
    (e.source as? BoundedRangeModel)?.also {
      model.value = it.value
    }
  }

  model.addChangeListener { e ->
    (e.source as? SpinnerNumberModel)?.also {
      scrollbar.value = it.number.toInt()
    }
  }

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("JSpinner", JSpinner(model)))
    it.add(makeTitledPanel("JScrollBar", scrollbar))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, cmp: Component) = JPanel(GridBagLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.background = Color.WHITE
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  it.add(cmp, c)
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
