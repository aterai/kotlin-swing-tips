package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.basic.BasicSeparatorUI

fun makeUI(): Component {
  val p = JPanel(GridLayout(2, 1))
  p.add(makeTestPanel("JSeparator", JSeparator()))
  p.add(makeTestPanel("GradientSeparator", GradientSeparator(), 10))
  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  box.add(Box.createHorizontalStrut(10))
  box.add(JSeparator(SwingConstants.VERTICAL))
  box.add(Box.createHorizontalStrut(10))
  box.add(GradientSeparator(SwingConstants.VERTICAL))
  box.add(Box.createHorizontalStrut(10))
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.EAST)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTestPanel(
  title: String,
  sp: JSeparator,
  indent: Int = 10,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  val c = GridBagConstraints()
  c.insets = Insets(2, 2, 2, 2)
  c.gridwidth = 2
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.anchor = GridBagConstraints.LINE_START
  p.add(makeTitledPanel(title, sp), c)
  c.insets = Insets(2, 2 + indent, 2, 2)
  c.gridwidth = 1
  c.gridy = 1
  p.add(JTextField(), c)
  c.insets = Insets(2, 0, 2, 2)
  c.weightx = 0.0
  c.fill = GridBagConstraints.NONE
  p.add(JButton("JButton"), c)
  return p
}

private fun makeTitledPanel(
  title: String,
  separator: JSeparator,
): Component {
  val p = JPanel(GridBagLayout())
  val c = GridBagConstraints()
  c.insets = Insets(2, 2, 2, 2)
  p.add(JLabel(title), c)
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  p.add(separator, c)
  return p
}

private class GradientSeparator : JSeparator {
  constructor() : super()
  constructor(orientation: Int) : super(orientation)

  override fun updateUI() {
    super.updateUI()
    // setUI(GradientSeparatorUI.createUI(this))
    setUI(GradientSeparatorUI())
  }
}

private class GradientSeparatorUI : BasicSeparatorUI() {
  private var bgc: Color? = null
  private var ssc: Color? = null
  private var shc: Color? = null

  private fun updateColors(c: Component) {
    bgc = UIManager.getColor("Panel.background") as? ColorUIResource ?: c.background
    ssc = UIManager.getColor("Separator.shadow") as? ColorUIResource ?: c.background.brighter()
    shc = UIManager.getColor("Separator.highlight") as? ColorUIResource ?: c.background.darker()
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    updateColors(c)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    if (c is JSeparator) {
      val g2 = g.create() as? Graphics2D ?: return
      val s = c.getSize()
      if (c.orientation == SwingConstants.VERTICAL) {
        g2.paint = GradientPaint(0f, 0f, ssc, 0f, s.height.toFloat(), bgc, true)
        g2.fillRect(0, 0, 1, s.height)
        g2.paint = GradientPaint(0f, 0f, shc, 0f, s.height.toFloat(), bgc, true)
        g2.fillRect(1, 0, 1, s.height)
      } else {
        g2.paint = GradientPaint(0f, 0f, ssc, s.width.toFloat(), 0f, bgc, true)
        g2.fillRect(0, 0, s.width, 1)
        g2.paint = GradientPaint(0f, 0f, shc, s.width.toFloat(), 0f, bgc, true)
        g2.fillRect(0, 1, s.width, 1)
      }
      g2.dispose()
    }
  }

  // companion object {
  //   fun createUI(c: JComponent?): ComponentUI {
  //     return GradientSeparatorUI()
  //   }
  // }
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
