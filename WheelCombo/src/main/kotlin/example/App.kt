package example

import java.awt.*
import java.awt.event.MouseWheelListener
import javax.swing.*

fun makeUI(): Component {
  val combo = object : JComboBox<String>(makeModel()) {
    @Transient private var handler: MouseWheelListener? = null

    override fun updateUI() {
      removeMouseWheelListener(handler)
      super.updateUI()
      handler = MouseWheelListener { e ->
        val c = e.component
        if (c is JComboBox<*> && c.hasFocus()) {
          val idx = c.selectedIndex + e.wheelRotation
          c.selectedIndex = idx.coerceIn(0 until c.itemCount)
        }
      }
      addMouseWheelListener(handler)
    }
  }
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder("JComboBox")
  val c = GridBagConstraints()
  c.gridx = 0
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.LINE_END
  p.add(JLabel("Wheel:"), c)
  p.add(JLabel("Normal:"), c)
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  p.add(combo, c)
  p.add(JComboBox(makeModel()), c)

  val panel = JPanel(BorderLayout())
  panel.add(p, BorderLayout.NORTH)
  panel.add(JScrollPane(JTextArea("JTextArea")))
  panel.preferredSize = Dimension(320, 240)
  return panel
}

private fun makeModel() = DefaultComboBoxModel<String>().also {
  it.addElement("111111")
  it.addElement("22222222")
  it.addElement("3333333333")
  it.addElement("444444444444")
  it.addElement("5555555")
  it.addElement("66666666666")
  it.addElement("77777777")
  it.addElement("88888888888")
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
