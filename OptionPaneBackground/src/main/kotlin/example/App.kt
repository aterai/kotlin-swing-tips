package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  UIManager.put("OptionPane.background", Color.LIGHT_GRAY)
  val txt = "<html>JOptionPane:<br><li>messageArea<li>realBody<li>separator<li>body<li>buttonArea"
  val title = "Title"
  val type = JOptionPane.WARNING_MESSAGE

  val b1 = JButton("default")
  b1.addActionListener { JOptionPane.showMessageDialog(b1.rootPane, JLabel(txt), title, type) }

  val label = JLabel(txt)
  label.addHierarchyListener { e ->
    val c = e.component
    if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && c.isShowing) {
      descendants(SwingUtilities.getAncestorOfClass(JOptionPane::class.java, c))
        .filterIsInstance<JPanel>().forEach { it.isOpaque = false }
    }
  }
  val b2 = JButton("background")
  b2.addActionListener { JOptionPane.showMessageDialog(b2.rootPane, label, title, type) }

  val b3 = JButton("override")
  b3.addActionListener { showMessageDialog(b3.rootPane, JLabel(txt), title, type) }

  return JPanel().also {
    it.add(b1)
    it.add(b2)
    it.add(b3)
    it.preferredSize = Dimension(320, 240)
  }
}

fun showMessageDialog(parent: Component?, message: Any, title: String, messageType: Int) {
  val pane = object : JOptionPane(message, messageType, DEFAULT_OPTION, null, null, null) {
    @Transient private var texture: Paint? = null
    override fun updateUI() {
      super.updateUI()
      texture = TextureUtils.createCheckerTexture(16, Color(0x64AAAAAA, true))
    }

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = texture
      g2.fillRect(0, 0, width, height)
      g2.dispose()
    }
  }
  pane.componentOrientation = (parent ?: JOptionPane.getRootFrame()).componentOrientation
  descendants(pane).filterIsInstance<JPanel>().forEach { it.isOpaque = false }

  val dialog = pane.createDialog(title)
  dialog.isVisible = true
  dialog.dispose()
}

private fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

// private fun descendants(parent: Container) = parent.components
//   .filterIsInstance<Container>()
//   .fold(listOf<Component>(parent)) { a, b -> a + b }

private object TextureUtils {
  fun createCheckerTexture(cs: Int, color: Color): TexturePaint {
    val size = cs * cs
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.paint = color
    g2.fillRect(0, 0, size, size)
    var i = 0
    while (i * cs < size) {
      var j = 0
      while (j * cs < size) {
        if ((i + j) % 2 == 0) {
          g2.fillRect(i * cs, j * cs, cs, cs)
        }
        j++
      }
      i++
    }
    g2.dispose()
    return TexturePaint(img, Rectangle(size, size))
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
