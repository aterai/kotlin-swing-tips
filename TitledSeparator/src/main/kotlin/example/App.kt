package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Point2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.TitledBorder

class MainPanel : JPanel(BorderLayout()) {
  init {
    val box = Box.createVerticalBox()
    box.add(TitledSeparator("TitledBorder", 2, TitledBorder.DEFAULT_POSITION))
    box.add(JCheckBox("JCheckBox 0"))
    box.add(JCheckBox("JCheckBox 1"))
    box.add(Box.createVerticalStrut(10))
    box.add(TitledSeparator("TitledBorder ABOVE TOP", Color(0x64_B4_C8), 2, TitledBorder.ABOVE_TOP))
    box.add(JCheckBox("JCheckBox 2"))
    box.add(JCheckBox("JCheckBox 3"))
    box.add(Box.createVerticalStrut(10))
    box.add(JSeparator())
    box.add(JCheckBox("JCheckBox 4"))
    box.add(JCheckBox("JCheckBox 5"))
    // box.add(Box.createVerticalStrut(8))
    add(box, BorderLayout.NORTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }
}

class TitledSeparator(
  private val title: String,
  private val target: Color?,
  private val separatorHeight: Int,
  private val titlePosition: Int
) : JLabel() {
  constructor(title: String, height: Int, titlePosition: Int) : this(
    title,
    null,
    height,
    titlePosition
  )

  private fun updateBorder() {
    val icon: Icon = TitledSeparatorIcon()
    setBorder(BorderFactory.createTitledBorder(
      BorderFactory.createMatteBorder(separatorHeight, 0, 0, 0, icon), title,
      TitledBorder.DEFAULT_JUSTIFICATION, titlePosition
    ))
  }

  override fun getMaximumSize() = Dimension(Short.MAX_VALUE.toInt(), super.getPreferredSize().height)

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
        val ec = getBackground() ?: UIManager.getColor("Panel.background")
        val sc = target ?: ec
        painter1 = LinearGradientPaint(start, end, dist, arrayOf(sc.darker(), ec))
        painter2 = LinearGradientPaint(start, end, dist, arrayOf(sc.brighter(), ec))
      }
      val h = iconHeight / 2
      val g2 = g.create() as? Graphics2D ?: return
      // XXX: g2.translate(x, y)
      g2.setPaint(painter1)
      g2.fillRect(x, y, width, iconHeight)
      g2.setPaint(painter2)
      g2.fillRect(x, y + h, width, iconHeight - h)
      g2.dispose()
    }

    override fun getIconWidth() = 200 // dummy width

    override fun getIconHeight() = separatorHeight
  }

  init {
    updateBorder()
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
