package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private val model = SpinnerNumberModel(9, 0, 100, 1)

private fun makeLabel(): JLabel {
  val l = object : JLabel("abc") {
    override fun getPreferredSize() = Dimension(50, 50)

    override fun getMinimumSize() = super.getMinimumSize()?.also {
      val i = model.number.toInt()
      it.setSize(i, i)
    }
  }
  l.isOpaque = true
  l.background = Color.ORANGE
  l.font = l.font.deriveFont(Font.PLAIN)
  l.alignmentX = Component.CENTER_ALIGNMENT
  l.alignmentY = Component.CENTER_ALIGNMENT
  l.verticalAlignment = SwingConstants.CENTER
  l.verticalTextPosition = SwingConstants.CENTER
  l.horizontalAlignment = SwingConstants.CENTER
  l.horizontalTextPosition = SwingConstants.CENTER
  return l
}

fun makeUI(): Component {
  val p1: JPanel = TestPanel()
  p1.layout = BoxLayout(p1, BoxLayout.X_AXIS)
  p1.add(Box.createHorizontalGlue())
  p1.add(makeLabel())
  p1.add(Box.createHorizontalGlue())

  val p2: JPanel = TestPanel()
  p2.layout = BoxLayout(p2, BoxLayout.Y_AXIS)
  p2.add(Box.createVerticalGlue())
  p2.add(makeLabel())
  p2.add(Box.createVerticalGlue())

  val panel = JPanel(GridLayout(1, 2, 5, 5))
  val list = listOf(p1, p2)
  list.forEach {
    it.background = Color.WHITE
    panel.add(it)
  }
  model.addChangeListener {
    list.forEach { it.revalidate() }
  }

  val np = JPanel(GridLayout(1, 2))
  np.add(JLabel("BoxLayout.X_AXIS", SwingConstants.CENTER))
  np.add(JLabel("BoxLayout.Y_AXIS", SwingConstants.CENTER))

  val sp = JPanel(BorderLayout())
  sp.add(JLabel("MinimumSize: "), BorderLayout.WEST)
  sp.add(JSpinner(model))

  return JPanel(BorderLayout(5, 5)).also {
    it.add(np, BorderLayout.NORTH)
    it.add(panel)
    it.add(sp, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TestPanel : JPanel() {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    g.drawLine(0, height / 2, width, height / 2)
    g.drawLine(width / 2, 0, width / 2, height)
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
