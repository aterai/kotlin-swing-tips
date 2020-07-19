package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val popup0 = JPopupMenu()
  initPopupMenu(popup0)
  val popup1 = DropShadowPopupMenu()
  initPopupMenu(popup1)

  val cl = Thread.currentThread().contextClassLoader
  val label = JLabel(ImageIcon(cl.getResource("example/test.png")))
  label.componentPopupMenu = popup1

  val check = JCheckBox("Paint Shadow", true)
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    label.componentPopupMenu = if (b) popup1 else popup0
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(label)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initPopupMenu(p: JPopupMenu) {
  listOf("Open", "Save", "Close").forEach { p.add("$it(test)") }
  p.addSeparator()
  p.add("Exit").addActionListener { e ->
    val m = e.source as? JMenuItem
    val pop = SwingUtilities.getUnwrappedParent(m) as? JPopupMenu
    (SwingUtilities.getRoot(pop?.invoker) as? Window)?.dispose()
  }
}

private class DropShadowPopupMenu : JPopupMenu() {
  private val dim = Dimension()
  @Transient private var shadow: BufferedImage? = null

  override fun updateUI() {
    border = null
    super.updateUI()
    val inner = border
    val outer = BorderFactory.createEmptyBorder(0, 0, OFFSET, OFFSET)
    border = BorderFactory.createCompoundBorder(outer, inner)
  }

  override fun isOpaque() = false

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.drawImage(shadow, 0, 0, this)
    g2.paint = background // ??? 1.7.0_03
    g2.fillRect(0, 0, width - OFFSET, height - OFFSET)
    g2.dispose()
  }

  override fun show(c: Component, x: Int, y: Int) {
    val d = preferredSize
    val w = d.width
    val h = d.height
    if (dim.width != w || dim.height != h) {
      dim.setSize(w, h)
      shadow = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB).also {
        val g2 = it.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f)
        g2.paint = Color.BLACK
        for (i in 0 until OFFSET) {
          g2.fillRoundRect(
            OFFSET,
            OFFSET,
            w - OFFSET - OFFSET + i,
            h - OFFSET - OFFSET + i,
            4,
            4
          )
        }
        g2.dispose()
      }
    }
    EventQueue.invokeLater {
      val top = topLevelAncestor
      (top as? JWindow)?.background = Color(0x0, true)
    }
    super.show(c, x, y)
  }

  companion object {
    private const val OFFSET = 4
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
