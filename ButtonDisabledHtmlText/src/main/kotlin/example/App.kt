package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.MouseAdapter
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  UIManager.put("Button.disabledText", Color.RED)
  val html = "<html>html <span style='color:#0000ff'>tag"
  val button0 = makeButton(html)
  val layer1 = DisableInputLayerUI<AbstractButton>()
  val button2 = makeButton(html)

  val p = JPanel(GridLayout(0, 1, 8, 8))
  p.add(makeTitledPanel(button0, "Default"))
  p.add(makeTitledPanel(JLayer(makeButton(html), layer1), "JLayer1"))
  p.add(makeTitledPanel(JLayer(button2, DisabledHtmlTextLayerUI<AbstractButton>()), "JLayer2"))

  val check = JCheckBox("setEnabled", true)
  check.addActionListener { e ->
    val isSelected = (e.source as? JCheckBox)?.isSelected == true
    button0.isEnabled = isSelected
    layer1.setLocked(!isSelected)
    button2.isEnabled = isSelected
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(check)

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p, BorderLayout.NORTH)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton(title: String): JButton {
  val pop = JPopupMenu()
  pop.add(title)
  val button = JButton(title)
  if (title.isNotEmpty()) {
    button.mnemonic = title.codePointAt(0)
  }
  button.icon = UIManager.getIcon("FileView.directoryIcon")
  button.toolTipText = title
  button.componentPopupMenu = pop
  return button
}

private fun makeTitledPanel(
  c: Component,
  title: String,
): Container {
  val box = Box.createHorizontalBox()
  box.add(JLabel("$title: "))
  box.add(c)
  box.add(Box.createHorizontalGlue())
  return box
}

private class DisableInputLayerUI<V : AbstractButton> : LayerUI<V>() {
  private val mouseBlocker = object : MouseAdapter() { /* Do nothing */ }
  private val keyBlocker = object : KeyAdapter() { /* Do nothing */ }
  private var isBlocking = false
  private var buf: BufferedImage? = null

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      if (DEBUG_POPUP_BLOCK) {
        c.glassPane.addMouseListener(mouseBlocker)
        c.glassPane.addKeyListener(keyBlocker)
      }
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK or
        AWTEvent.MOUSE_WHEEL_EVENT_MASK or AWTEvent.KEY_EVENT_MASK or
        AWTEvent.FOCUS_EVENT_MASK or AWTEvent.COMPONENT_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.layerEventMask = 0
      if (DEBUG_POPUP_BLOCK) {
        c.glassPane.removeMouseListener(mouseBlocker)
        c.glassPane.removeKeyListener(keyBlocker)
      }
    }
    super.uninstallUI(c)
  }

  override fun eventDispatched(
    e: AWTEvent,
    l: JLayer<out V>,
  ) {
    if (isBlocking && e is InputEvent) {
      e.consume()
    }
  }

  fun setLocked(flag: Boolean) {
    val old = isBlocking
    isBlocking = flag
    firePropertyChange(CMD_BLOCKING, old, isBlocking)
  }

  override fun applyPropertyChange(
    e: PropertyChangeEvent,
    l: JLayer<out V>,
  ) {
    if (CMD_BLOCKING == e.propertyName) {
      val b = l.view
      b.isFocusable = !isBlocking
      b.mnemonic = if (isBlocking) 0 else b.text.codePointAt(0)
      b.foreground = if (isBlocking) Color.RED else Color.BLACK
      l.glassPane.isVisible = e.newValue as? Boolean == true
    }
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    if (c is JLayer<*>) {
      val view = c.view
      if (isBlocking) {
        val d = view.size
        val img = buf?.takeIf { it.width == d.width && it.height == d.height }
          ?: BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .25f)
        view.paint(g2)
        g2.dispose()
        g.drawImage(img, 0, 0, c)
        buf = img
      } else {
        // super.paint(g, view as? JComponent)
        view.paint(g)
      }
    }
  }

  companion object {
    private const val CMD_BLOCKING = "lock"
    private const val DEBUG_POPUP_BLOCK = false
  }
}

private class DisabledHtmlTextLayerUI<V : AbstractButton> : LayerUI<V>() {
  private val label = JLabel()
  private val canvas = JPanel()

  override fun updateUI(l: JLayer<out V>) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(label)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    if (c is JLayer<*>) {
      val view = c.view
      view.paint(g)
      if (!view.isEnabled()) {
        paintDisabledText(g, view)
      }
    }
  }

  private fun paintDisabledText(
    g: Graphics,
    c: Component,
  ) {
    if (c is AbstractButton) {
      val b = c
      label.font = b.font
      label.text = b.text
      label.icon = b.icon
      label.verticalAlignment = b.verticalAlignment
      label.horizontalAlignment = b.horizontalAlignment
      label.verticalTextPosition = b.verticalTextPosition
      label.horizontalTextPosition = b.horizontalTextPosition
      label.foreground = UIManager.getColor("Button.disabledText")
      label.isOpaque = false
      val g2 = g.create() as? Graphics2D ?: return
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .25f)
      val r = b.bounds
      SwingUtilities.paintComponent(g2, label, canvas, r.x, r.y, r.width, r.height)
      g2.dispose()
    }
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
