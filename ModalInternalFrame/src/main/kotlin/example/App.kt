package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.InternalFrameAdapter
import javax.swing.event.InternalFrameEvent
import javax.swing.plaf.basic.BasicInternalFrameUI
import javax.swing.text.DefaultEditorKit

private val desktop = JDesktopPane()
private val menuBar = JMenuBar()
private val sampleBar = JMenuBar()
private var openFrameCount = 0

fun makeUI(): Component {
  val button = JButton(ModalInternalFrameAction3("Show"))
  button.mnemonic = KeyEvent.VK_S

  val internal = JInternalFrame("Button")
  internal.contentPane.add(button)
  internal.setSize(100, 100)
  internal.setLocation(20, 20)
  internal.isVisible = true
  desktop.add(internal)

  sampleBar.add(JMenu("Frame"))
  sampleBar.isVisible = false

  val menu = JMenu("Frame")
  menu.mnemonic = KeyEvent.VK_F
  menuBar.add(menu)

  menu.add("New Frame").also {
    it.mnemonic = KeyEvent.VK_N
    it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK)
    it.addActionListener {
      val frame = JInternalFrame("title", true, true, true, true)
      frame.setSize(130, 100)
      frame.setLocation(30 * openFrameCount, 30 * openFrameCount)
      desktop.add(frame)
      frame.isVisible = true
      openFrameCount++
    }
  }

  menu.addSeparator()

  menu.add(ModalInternalFrameAction1("InternalMessageDialog(Normal)")).also {
    it.mnemonic = KeyEvent.VK_1
    it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.ALT_DOWN_MASK)
  }

  menu.add(ModalInternalFrameAction2("InternalMessageDialog")).also {
    it.mnemonic = KeyEvent.VK_2
    it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.ALT_DOWN_MASK)
  }

  menu.add(ModalInternalFrameAction3("InternalMessageDialog(Print)")).also {
    it.mnemonic = KeyEvent.VK_3
    it.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.ALT_DOWN_MASK)
  }

  val b = JButton(DefaultEditorKit.BeepAction())
  b.mnemonic = KeyEvent.VK_B

  return JPanel(BorderLayout()).also {
    it.add(sampleBar, BorderLayout.NORTH)
    it.add(b, BorderLayout.SOUTH)
    it.add(desktop)
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.preferredSize = Dimension(320, 240)
  }
}

// menuItem = JMenuItem(ModalInternalFrameAction1("InternalMessageDialog(Normal)"))
// menuItem.setMnemonic(KeyEvent.VK_1)
private class ModalInternalFrameAction1(label: String) : AbstractAction(label) {
  override fun actionPerformed(e: ActionEvent) {
    setJMenuEnabled(false)
    JOptionPane.showInternalMessageDialog(
      desktop,
      "information",
      "modal1",
      JOptionPane.INFORMATION_MESSAGE
    )
    setJMenuEnabled(true)
  }
}

// menuItem = JMenuItem(ModalInternalFrameAction2("InternalMessageDialog"))
// menuItem.setMnemonic(KeyEvent.VK_2);
private class ModalInternalFrameAction2(label: String) : AbstractAction(label) {
  private val glass = MyGlassPane()

  init {
    glass.isOpaque = false
    glass.isVisible = false
    desktop.add(glass, JLayeredPane.MODAL_LAYER)
  }

  override fun actionPerformed(e: ActionEvent) {
    setJMenuEnabled(false)
    val w = SwingUtilities.getWindowAncestor(desktop)
    val screen = w.graphicsConfiguration.bounds
    glass.setSize(screen.width, screen.height)
    glass.isVisible = true
    JOptionPane.showInternalMessageDialog(
      desktop,
      "information",
      "modal2",
      JOptionPane.INFORMATION_MESSAGE
    )
    glass.isVisible = false
    setJMenuEnabled(true)
  }
}

// menuItem = new JMenuItem(new ModalInternalFrameAction3("Modal"));
// menuItem.setMnemonic(KeyEvent.VK_3);
// Creating Modal Internal Frames -- Approach 1 and Approach 2
// http://java.sun.com/developer/JDCTechTips/2001/tt1220.html
private class ModalInternalFrameAction3(label: String) : AbstractAction(label) {
  private val glassPane = PrintGlassPane()
  private var originalGlassPane: Component? = null

  init {
    glassPane.isVisible = false
  }

  override fun actionPerformed(e: ActionEvent) {
    val optionPane = JOptionPane()
    // TEST: UIManager.put("InternalFrame.titleButtonToolTipsOn", false)
    val modal = optionPane.createInternalFrame(desktop, "modal3")
    // TEST: UIManager.put("InternalFrame.titleButtonToolTipsOn", true)
    optionPane.message = "Hello, World"
    optionPane.messageType = JOptionPane.INFORMATION_MESSAGE
    removeSystemMenuListener(modal)
    val ifl = object : InternalFrameAdapter() {
      override fun internalFrameClosed(e: InternalFrameEvent) {
        glassPane.removeAll()
        glassPane.isVisible = false
        desktop.rootPane.glassPane = originalGlassPane
      }
    }
    modal.addInternalFrameListener(ifl)
    glassPane.add(modal)
    modal.pack()
    // val screen = desktop.getBounds();
    // modal.setLocation(screen.x + screen.width / 2 - modal.getSize().width / 2,
    //                   screen.y + screen.height / 2 - modal.getSize().height / 2)
    originalGlassPane = desktop.rootPane.glassPane
    desktop.rootPane.glassPane = glassPane
    glassPane.isVisible = true
    modal.isVisible = true
  }
}

private fun setJMenuEnabled(flag: Boolean) {
  val bar = desktop.rootPane.jMenuBar
  bar.isVisible = flag
  sampleBar.isVisible = !flag
}

private fun removeSystemMenuListener(modal: JInternalFrame) {
  val ui = modal.ui as? BasicInternalFrameUI ?: return
  ui.northPane.components
    .filter { it is JLabel || "InternalFrameTitlePane.menuButton" == it.name }
    .forEach { removeComponentMouseListener(it) }
}

private fun removeComponentMouseListener(c: Component) {
  for (ml in c.mouseListeners) {
    (c as? JComponent)?.removeMouseListener(ml)
  }
}

private class MyGlassPane : JDesktopPane() {
  override fun updateUI() {
    super.updateUI()
    focusTraversalPolicy = object : DefaultFocusTraversalPolicy() {
      override fun accept(c: Component) = false
    }
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = TEXTURE
    g2.fillRect(0, 0, width, height)
    g2.dispose()
  }

  companion object {
    private val TEXTURE = TextureUtils.createCheckerTexture(6)
  }
}

private class PrintGlassPane : JDesktopPane() {
  override fun setVisible(isVisible: Boolean) {
    val oldVisible = isVisible()
    super.setVisible(isVisible)
    rootPane?.takeIf { isVisible() != oldVisible }?.layeredPane?.isVisible = !isVisible
  }

  override fun paintComponent(g: Graphics) {
    // http://weblogs.java.net/blog/alexfromsun/archive/2008/01/disabling_swing.html
    // it is important to call print() instead of paint() here
    // because print() doesn't affect the frame's double buffer
    rootPane?.layeredPane?.print(g)

    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = TEXTURE
    g2.fillRect(0, 0, width, height)
    g2.dispose()
  }

  companion object {
    private val TEXTURE = TextureUtils.createCheckerTexture(4)
  }
}

private object TextureUtils {
  private val DEFAULT_COLOR = Color(100, 100, 100, 100)

  @JvmOverloads
  fun createCheckerTexture(cs: Int, color: Color = DEFAULT_COLOR): TexturePaint {
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
