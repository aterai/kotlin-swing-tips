package example

import java.awt.*
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

fun createUI() = JPanel(BorderLayout()).also {
  it.add(JScrollPane(JTree()))
  it.preferredSize = Dimension(320, 240)
}

// How to add icon to JFrame's title bar - Oracle Forums
// https://forums.oracle.com/ords/apexds/post/how-to-add-icon-to-jframe-s-title-bar-5581
private fun makeButton(title: String?, icon: Icon?): JButton {
  val extraButton = object : JButton(title, icon) {
    override fun getPreferredSize(): Dimension {
      // Use the same size as the title bar buttons of the JRootPane
      val closeIcon = UIManager.getIcon("InternalFrame.closeIcon")
      return closeIcon?.let { Dimension(it.iconWidth, it.iconHeight) }
        ?: super.getPreferredSize()
    }
  }
  extraButton.setFocusPainted(false)
  extraButton.setOpaque(false)
  extraButton.setBorderPainted(false)
  extraButton.setToolTipText("Extra JButton: $title")
  extraButton.addActionListener {
    val msg = "Extra JButton was clicked!"
    JOptionPane.showMessageDialog(extraButton.rootPane, msg)
  }
  return extraButton
}

private fun makeExtraBarWindow(frame: JFrame?): JWindow {
  // JToolBar bar = JToolBar()
  // bar.setFloatable(false)
  val box = Box.createHorizontalBox()
  box.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 4))
  box.setOpaque(false)
  box.add(makeButton("...", null))
  box.add(Box.createHorizontalStrut(5))
  box.add(makeButton(null, ExtraIcon()))
  val window = JWindow(frame)
  // window.setAlwaysOnTop(true) // XXX
  // window.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE)
  window.setBackground(UIManager.getColor("activeCaption"))
  window.contentPane.setBackground(Color(0x0, true))
  window.contentPane.add(box)
  window.pack()
  return window
}

@Suppress("TooManyFunctions")
private class ExtraBarPositionHandler(
  private val extraBar: JWindow,
) : WindowAdapter(),
  ComponentListener {
  override fun windowActivated(e: WindowEvent?) {
    extraBar.setBackground(UIManager.getColor("activeCaption"))
  }

  override fun windowDeactivated(e: WindowEvent?) {
    extraBar.setBackground(UIManager.getColor("inactiveCaption"))
  }

  override fun windowOpened(e: WindowEvent) {
    relocateBar(e.window)
    extraBar.isVisible = true
  }

  override fun windowClosed(e: WindowEvent?) {
    extraBar.isVisible = false
  }

  override fun windowIconified(e: WindowEvent?) {
    extraBar.isVisible = false
  }

  override fun windowDeiconified(e: WindowEvent) {
    relocateBar(e.window)
    extraBar.isVisible = true
  }

  override fun componentResized(e: ComponentEvent) {
    // When the frame is restored from the maximized state, the title bar
    // buttons may not have been laid out yet, so defer the update once more.
    EventQueue.invokeLater { relocateBar(e.component) }
  }

  override fun componentMoved(e: ComponentEvent) {
    relocateBar(e.component)
  }

  override fun componentShown(e: ComponentEvent) {
    relocateBar(e.component)
    extraBar.isVisible = true
  }

  override fun componentHidden(e: ComponentEvent?) {
    extraBar.isVisible = false
  }

  private fun relocateBar(p: Component) {
    // Wait until the title bar of the JRootPane has been laid out
    EventQueue.invokeLater { updateBarLocation(p) }
  }

  private fun updateBarLocation(p: Component) {
    val root = SwingUtilities.getRootPane(p) ?: return
    // The JRootPane border is removed when the frame is maximized
    val maximized = p is Frame && p.extendedState == Frame.MAXIMIZED_BOTH
    val top = if (maximized) 0 else root.border?.getBorderInsets(root)?.top ?: 0
    val iconifyIcon = UIManager.getIcon("InternalFrame.iconifyIcon")
    // The location of the iconify button is relative to the title pane,
    // so convert it to the JRootPane coordinate system first
    val pt = descendants(root)
      .filterIsInstance<JButton>()
      .firstOrNull { b -> b.icon == iconifyIcon }
      ?.let { SwingUtilities.convertPoint(it.parent, it.location, root) }
      ?: Point()
    SwingUtilities.convertPointToScreen(pt, root)
    val x = pt.x - extraBar.width
    val y = p.getY() + top + 1
    extraBar.setLocation(x, y)
  }
}

private class ExtraIcon : Icon {
  private val size = UIManager.getIcon("InternalFrame.closeIcon")?.iconHeight ?: 16

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val pressed = (c as? AbstractButton)?.model?.isPressed == true
    g2.color = if (pressed) Color.RED else Color.GREEN
    g2.fillOval(x + 2, y + 2, size - 4, size - 4)
    g2.dispose()
  }

  override fun getIconWidth() = size

  override fun getIconHeight() = size
}

fun descendants(parent: Container): List<Component> = parent.components
  .filterIsInstance<Container>()
  .flatMap { listOf(it) + descendants(it) }

fun main() {
  EventQueue.invokeLater {
    JFrame.setDefaultLookAndFeelDecorated(true)
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      minimumSize = Dimension(240, 120)
      pack()
      val bar = makeExtraBarWindow(this)
      val handler = ExtraBarPositionHandler(bar)
      addComponentListener(handler)
      addWindowListener(handler)
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
