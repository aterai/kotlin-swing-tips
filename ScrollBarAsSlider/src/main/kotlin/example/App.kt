package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val STEP = 5
private const val EXTENT = 20
private const val MIN = 0
private const val MAX = EXTENT * 10 // 200
private const val VALUE = 50

fun makeUI(): Component {
  val scrollbar = JScrollBar(Adjustable.HORIZONTAL, VALUE, EXTENT, MIN, MAX + EXTENT)
  val model = SpinnerNumberModel(VALUE, MIN, MAX, STEP)

  scrollbar.setUnitIncrement(STEP)
  scrollbar.getModel().addChangeListener { e ->
    val m = e.getSource() as? BoundedRangeModel ?: return@addChangeListener
    model.setValue(m.getValue())
  }

  model.addChangeListener { e ->
    val source = e.getSource() as? SpinnerNumberModel ?: return@addChangeListener
    scrollbar.setValue(source.getNumber().toInt())
  }

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("JSpinner", JSpinner(model)))
    it.add(makeTitledPanel("JScrollBar", scrollbar))
    it.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5))
    it.setPreferredSize(Dimension(320, 240))
  }
}

fun makeTitledPanel(title: String, cmp: Component) = JPanel(GridBagLayout()).also {
  it.setBorder(BorderFactory.createTitledBorder(title))
  it.setBackground(Color.WHITE)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
