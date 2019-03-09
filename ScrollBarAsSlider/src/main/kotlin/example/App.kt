package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(2, 1)) {
  init {
    val scrollbar = JScrollBar(Adjustable.HORIZONTAL, VALUE, EXTENT, MIN, MAX + EXTENT)
    val spinner = JSpinner(SpinnerNumberModel(VALUE, MIN, MAX, STEP))

    scrollbar.setUnitIncrement(STEP)
    scrollbar.getModel().addChangeListener { e ->
      spinner.setValue((e.getSource() as BoundedRangeModel).getValue())
    }

    spinner.addChangeListener { e ->
      val source = e.getSource() as JSpinner
      val iv = source.getValue() as Int
      scrollbar.setValue(iv)
    }

    add(makeTitledPanel("JSpinner", spinner))
    add(makeTitledPanel("JScrollBar", scrollbar))
    setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, cmp: Component): Component {
    val p = JPanel(GridBagLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.setBackground(Color.WHITE)
    val c = GridBagConstraints()
    c.weightx = 1.0
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = Insets(5, 5, 5, 5)
    p.add(cmp, c)
    return p
  }

  companion object {
    private const val STEP = 5
    private const val EXTENT = 20
    private const val MIN = 0
    private const val MAX = EXTENT * 10 // 200
    private const val VALUE = 50
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
