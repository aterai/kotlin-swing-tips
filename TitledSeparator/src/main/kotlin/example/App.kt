package example

import java.awt.*
import java.awt.geom.Point2D
import javax.swing.*
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.add(TitledSeparator("TitledBorder", TitledBorder.DEFAULT_POSITION))
  box.add(JCheckBox("JCheckBox 0"))
  box.add(JCheckBox("JCheckBox 1"))
  box.add(Box.createVerticalStrut(10))
  box.add(TitledSeparator("TitledBorder ABOVE TOP", Color(0x64_B4_C8), TitledBorder.ABOVE_TOP))
  box.add(JCheckBox("JCheckBox 2"))
  box.add(JCheckBox("JCheckBox 3"))
  box.add(Box.createVerticalStrut(10))
  box.add(JSeparator())
  box.add(JCheckBox("JCheckBox 4"))
  box.add(JCheckBox("JCheckBox 5"))
  // box.add(Box.createVerticalStrut(8))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TitledSeparator(
  private val title: String,
  private val target: Color?,
  private val titlePosition: Int
) : JLabel() {

  constructor(title: String, titlePosition: Int) : this(
    title,
    null,
    titlePosition
  )

  private fun updateBorder() {
    val icon = TitledSeparatorIcon()
    border = BorderFactory.createTitledBorder(
      BorderFactory.createMatteBorder(icon.iconHeight, 0, 0, 0, icon),
      title,
      TitledBorder.DEFAULT_JUSTIFICATION,
      titlePosition
    )
  }

  override fun getMaximumSize(): Dimension? {
    val d = super.getPreferredSize()
    d?.width = Short.MAX_VALUE.toInt()
    return d
  }

  override fun updateUI() {
    super.updateUI()
    updateBorder()
  }

  private inner class TitledSeparatorIcon : Icon {
    private var width = -1
    private var painter1: Paint? = null
    private var painter2: Paint? = null
    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
      val w = c.width
      if (w != width || painter1 == null || painter2 == null) {
        width = w
        val start = Point2D.Float()
        val end = Point2D.Float(width.toFloat(), 0f)
        val dist = floatArrayOf(0f, 1f)
        val ec = background ?: UIManager.getColor("Panel.background")
        val sc = target ?: ec
        painter1 = LinearGradientPaint(start, end, dist, arrayOf(sc.darker(), ec))
        painter2 = LinearGradientPaint(start, end, dist, arrayOf(sc.brighter(), ec))
      }
      val h = iconHeight / 2
      val g2 = g.create() as? Graphics2D ?: return
      // XXX: g2.translate(x, y)
      g2.paint = painter1
      g2.fillRect(x, y, width, iconHeight)
      g2.paint = painter2
      g2.fillRect(x, y + h, width, iconHeight - h)
      g2.dispose()
    }

    override fun getIconWidth() = 200 // sample width

    override fun getIconHeight() = 2
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
