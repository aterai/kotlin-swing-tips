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

class MainPanel : JPanel(BorderLayout()) {
  protected val desktop = JDesktopPane()
  private val menuBar = JMenuBar()
  private val dummyBar = JMenuBar()

  init {

    val button = JButton(ModalInternalFrameAction3("Show"))
    button.setMnemonic(KeyEvent.VK_S)
    val internal = JInternalFrame("Button")
    internal.getContentPane().add(button)
    internal.setSize(100, 100)
    internal.setLocation(20, 20)
    internal.setVisible(true)
    desktop.add(internal)

    dummyBar.add(JMenu("Frame"))
    add(dummyBar, BorderLayout.NORTH)
    dummyBar.setVisible(false)

    val menu = JMenu("Frame")
    menu.setMnemonic(KeyEvent.VK_F)
    menuBar.add(menu)

    var menuItem = JMenuItem(object : AbstractAction("New Frame") {
      private var openFrameCount: Int = 0
      override fun actionPerformed(e: ActionEvent) {
        val iframe = JInternalFrame("title", true, true, true, true)
        iframe.setSize(130, 100)
        iframe.setLocation(30 * openFrameCount, 30 * openFrameCount)
        desktop.add(iframe)
        iframe.setVisible(true)
        openFrameCount++
      }
    })
    menuItem.setMnemonic(KeyEvent.VK_N)
    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK))
    menu.add(menuItem)

    menu.addSeparator()

    menuItem = menu.add(ModalInternalFrameAction1("InternalMessageDialog(Nomal)"))
    menuItem.setMnemonic(KeyEvent.VK_1)
    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, InputEvent.ALT_DOWN_MASK))

    menuItem = menu.add(ModalInternalFrameAction2("InternalMessageDialog"))
    menuItem.setMnemonic(KeyEvent.VK_2)
    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, InputEvent.ALT_DOWN_MASK))

    menuItem = menu.add(ModalInternalFrameAction3("InternalMessageDialog(Print)"))
    menuItem.setMnemonic(KeyEvent.VK_3)
    menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_3, InputEvent.ALT_DOWN_MASK))

    val b = JButton(DefaultEditorKit.BeepAction())
    b.setMnemonic(KeyEvent.VK_B)
    add(b, BorderLayout.SOUTH)
    add(desktop)
    EventQueue.invokeLater { getRootPane().setJMenuBar(menuBar) }
    setPreferredSize(Dimension(320, 240))
  }

  // menuItem = new JMenuItem(new ModalInternalFrameAction1("InternalMessageDialog(Nomal)"))
  // menuItem.setMnemonic(KeyEvent.VK_1)
  protected inner class ModalInternalFrameAction1(label: String) : AbstractAction(label) {
    override fun actionPerformed(e: ActionEvent) {
      setJMenuEnabled(false)
      JOptionPane.showInternalMessageDialog(desktop, "information", "modal1", JOptionPane.INFORMATION_MESSAGE)
      setJMenuEnabled(true)
    }
  }

  // menuItem = new JMenuItem(new ModalInternalFrameAction2("InternalMessageDialog"));
  // menuItem.setMnemonic(KeyEvent.VK_2);
  protected inner class ModalInternalFrameAction2(label: String) : AbstractAction(label) {
    private val glass = MyGlassPane()

    init {
      glass.setOpaque(false)
      glass.setVisible(false)
      desktop.add(glass, JLayeredPane.MODAL_LAYER)
    }

    override fun actionPerformed(e: ActionEvent) {
      setJMenuEnabled(false)
      val w = SwingUtilities.getWindowAncestor(desktop)
      val screen = w.getGraphicsConfiguration().getBounds()
      glass.setSize(screen.width, screen.height)
      glass.setVisible(true)
      JOptionPane.showInternalMessageDialog(desktop, "information", "modal2", JOptionPane.INFORMATION_MESSAGE)
      glass.setVisible(false)
      setJMenuEnabled(true)
    }
  }

  // menuItem = new JMenuItem(new ModalInternalFrameAction3("Modal"));
  // menuItem.setMnemonic(KeyEvent.VK_3);
  // Creating Modal Internal Frames -- Approach 1 and Approach 2
  // http://java.sun.com/developer/JDCTechTips/2001/tt1220.html
  protected inner class ModalInternalFrameAction3(label: String) : AbstractAction(label) {
    protected val glass: JComponent = PrintGlassPane()

    init {
      glass.setVisible(false)
    }

    override fun actionPerformed(e: ActionEvent) {
      val optionPane = JOptionPane()
      // TEST: UIManager.put("InternalFrame.titleButtonToolTipsOn", Boolean.FALSE)
      val modal = optionPane.createInternalFrame(desktop, "modal3")
      // TEST: UIManager.put("InternalFrame.titleButtonToolTipsOn", Boolean.TRUE)
      optionPane.setMessage("Hello, World")
      optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE)
      removeSystemMenuListener(modal)
      modal.addInternalFrameListener(object : InternalFrameAdapter() {
        override fun internalFrameClosed(e: InternalFrameEvent?) {
          glass.removeAll()
          glass.setVisible(false)
        }
      })
      glass.add(modal)
      modal.pack()
      // val screen = desktop.getBounds();
      // modal.setLocation(screen.x + screen.width / 2 - modal.getSize().width / 2,
      //                   screen.y + screen.height / 2 - modal.getSize().height / 2)
      getRootPane().setGlassPane(glass)
      glass.setVisible(true)
      modal.setVisible(true)
    }
  }

  protected fun setJMenuEnabled(flag: Boolean) {
    val bar = getRootPane().getJMenuBar()
    bar.setVisible(flag)
    dummyBar.setVisible(!flag)
  }

  protected fun removeSystemMenuListener(modal: JInternalFrame) {
    val ui = modal.getUI() as BasicInternalFrameUI
    ui.getNorthPane().getComponents()
        .filter { it is JLabel || "InternalFrameTitlePane.menuButton" == it.getName() }
        .forEach { removeComponentMouseListener(it) }
  }

  protected fun removeComponentMouseListener(c: Component) {
    for (ml in c.getMouseListeners()) {
      (c as JComponent).removeMouseListener(ml)
    }
  }
}

internal class MyGlassPane : JDesktopPane() {
  override fun updateUI() {
    super.updateUI()
    setFocusTraversalPolicy(object : DefaultFocusTraversalPolicy() {
      override fun accept(c: Component) = false
    })
  }

  protected override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    g2.setPaint(TEXTURE)
    g2.fillRect(0, 0, getWidth(), getHeight())
    g2.dispose()
  }

  companion object {
    private val TEXTURE = TextureUtils.createCheckerTexture(6)
  }
}

internal class PrintGlassPane : JDesktopPane() {
  override fun setVisible(isVisible: Boolean) {
    val oldVisible = isVisible()
    super.setVisible(isVisible)
    getRootPane()?.takeIf { isVisible() != oldVisible }?.getLayeredPane()?.setVisible(!isVisible)
  }

  protected override fun paintComponent(g: Graphics) {
    // http://weblogs.java.net/blog/alexfromsun/archive/2008/01/disabling_swing.html
    // it is important to call print() instead of paint() here
    // because print() doesn't affect the frame's double buffer
    getRootPane()?.getLayeredPane()?.print(g)

    val g2 = g.create() as Graphics2D
    g2.setPaint(TEXTURE)
    g2.fillRect(0, 0, getWidth(), getHeight())
    g2.dispose()
  }

  companion object {
    private val TEXTURE = TextureUtils.createCheckerTexture(4)
  }
}

internal object TextureUtils {
  private val DEFAULT_COLOR = Color(100, 100, 100, 100)

  @JvmOverloads
  fun createCheckerTexture(cs: Int, color: Color = DEFAULT_COLOR): TexturePaint {
    val size = cs * cs
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.setPaint(color)
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
} /* HideUtilityClassConstructor */

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
