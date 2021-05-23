package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentEvent
import java.awt.event.FocusEvent
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.beans.PropertyChangeEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  UIManager.put("Button.disabledText", Color.RED)
  val button1 = makeButton("Default")
  val button2 = makeButton("setForeground")
  val layerUI = DisableInputLayerUI<AbstractButton>()

  val check = JCheckBox("setEnabled", true)
  check.addActionListener { e ->
    val isSelected = (e.source as? JCheckBox)?.isSelected == true
    button1.isEnabled = isSelected
    button2.isEnabled = isSelected
    button2.foreground = if (isSelected) Color.BLACK else Color.RED
    layerUI.setLocked(!isSelected)
  }

  val p1 = JPanel()
  p1.border = BorderFactory.createTitledBorder("setEnabled")
  p1.add(button1)
  p1.add(button2)
  p1.add(JLayer(makeButton("JLayer"), layerUI))

  val p2 = JPanel()
  p2.border = BorderFactory.createTitledBorder("Focus test")
  p2.add(JTextField(16))
  p2.add(JButton("JButton"))

  val panel = JPanel(GridLayout(2, 1))
  panel.add(p1)
  panel.add(p2)

  val mb = JMenuBar()
  mb.add(LookAndFeelUtil.createLookAndFeelMenu())

  return JPanel().also {
    it.add(panel)
    it.add(check)
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
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
  button.toolTipText = title
  button.componentPopupMenu = pop
  return button
}

private class DisableInputLayerUI<V : AbstractButton> : LayerUI<V>() {
  @Transient private val dummyMouseListener = object : MouseAdapter() { /* to nothing */ }
  @Transient private val dummyKeyListener = object : KeyAdapter() { /* to nothing */ }
  private var isBlocking = false
  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      if (DEBUG_POPUP_BLOCK) {
        c.glassPane.addMouseListener(dummyMouseListener)
        c.glassPane.addKeyListener(dummyKeyListener)
      }
      c.layerEventMask = (
        AWTEvent.MOUSE_EVENT_MASK
          or AWTEvent.MOUSE_MOTION_EVENT_MASK
          or AWTEvent.MOUSE_WHEEL_EVENT_MASK
          or AWTEvent.KEY_EVENT_MASK
          or AWTEvent.FOCUS_EVENT_MASK
          or AWTEvent.COMPONENT_EVENT_MASK
        )
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.layerEventMask = 0
      if (DEBUG_POPUP_BLOCK) {
        c.glassPane.removeMouseListener(dummyMouseListener)
        c.glassPane.removeKeyListener(dummyKeyListener)
      }
    }
    super.uninstallUI(c)
  }

  override fun processComponentEvent(e: ComponentEvent, l: JLayer<out V>) {
    println("processComponentEvent")
  }

  override fun processKeyEvent(e: KeyEvent, l: JLayer<out V>) {
    println("processKeyEvent")
  }

  override fun processFocusEvent(e: FocusEvent, l: JLayer<out V>) {
    println("processFocusEvent")
  }

  override fun eventDispatched(e: AWTEvent, l: JLayer<out V>) {
    if (isBlocking && e is InputEvent) {
      e.consume()
    }
  }

  fun setLocked(flag: Boolean) {
    val old = isBlocking
    isBlocking = flag
    firePropertyChange(CMD_BLOCKING, old, isBlocking)
  }

  override fun applyPropertyChange(e: PropertyChangeEvent, l: JLayer<out V>) {
    if (CMD_BLOCKING == e.propertyName) {
      val b = l.view
      b.isFocusable = !isBlocking
      b.mnemonic = if (isBlocking) 0 else b.text.codePointAt(0)
      b.foreground = if (isBlocking) Color.RED else Color.BLACK
      l.glassPane.isVisible = e.newValue as? Boolean == true
    }
  }

  companion object {
    private const val CMD_BLOCKING = "lock"
    private const val DEBUG_POPUP_BLOCK = false
  }
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener { e ->
      val m = lafGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
      }
    }
    lafGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
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
