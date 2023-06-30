package example

import java.awt.*
import javax.swing.*

private val STRUT = ColorIcon(Color(0x0, true)) // MetalLookAndFeel

fun makeUI(): Component {
  UIManager.put("MenuItem.disabledForeground", Color.BLACK)
  val item0 = JMenuItem("JMenuItem.setEnabled(false);")
  item0.isEnabled = false

  val item1 = JLabel("JLabel + EmptyBorder")
  item1.border = BorderFactory.createEmptyBorder(2, 32, 2, 2)

  val item2 = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      super.updateUI()
      isOpaque = false // NimbusLookAndFeel
    }
  }
  val mi2 = object : JMenuItem("JPanel with JMenuItem", STRUT) {
    override fun contains(x: Int, y: Int) = false // disable mouse events
  }
  item2.add(mi2)

  val item3 = JMenuItem("\u200B")
  item3.border = BorderFactory.createEmptyBorder() // NimbusLookAndFeel
  item3.isEnabled = false
  val mi3 = object : JMenuItem("JMenuItem(disabled) with JMenuItem", STRUT) {
    override fun contains(x: Int, y: Int) = false // disable mouse events
  }
  item3.add(mi3)

  val menuBar = JMenuBar().also {
    it.add(makeMenu("Test0", item0))
    it.add(makeMenu("Test1", item1))
    it.add(makeMenu("Test2", item2))
    it.add(makeMenu("Test3", item3))
  }

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(JScrollPane(JTextArea()))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMenu(title: String, item: Component) = JMenu(title).also {
  it.add(item)
  it.addSeparator()
  it.add("JMenuItem") // .addActionListener { println("actionPerformed") }
  it.add("JMenuItem + Icon").icon = ColorIcon(Color.RED)
  it.add("1234567878909758457546734564562346432")
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12
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
