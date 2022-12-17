package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI(): Component {
  UIManager.put("CheckBoxMenuItem.doNotCloseOnMouseClick", true)

  val popup = JPopupMenu()
  popup.addMouseWheelListener { it.consume() }
  popup.add(Box.createHorizontalStrut(200))
  addCheckBoxAndSlider(popup)
  addCheckBoxAndToggleSlider(popup)
  addCheckBoxMenuItemAndSlider(popup)

  val menu = JMenu("JSlider")
  menu.popupMenu.addMouseWheelListener { it.consume() }
  menu.add(Box.createHorizontalStrut(200))
  addCheckBoxAndSlider(menu)
  addCheckBoxAndToggleSlider(menu)
  addCheckBoxMenuItemAndSlider(menu)

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  mb.add(menu)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.componentPopupMenu = popup
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addCheckBoxAndSlider(popup: JComponent) {
  val slider = makeSlider()
  slider.isEnabled = false
  val check = makeCheckBox()
  check.addActionListener { slider.isEnabled = (it.source as? JCheckBox)?.isSelected == true }
  val mi = JMenuItem(" ")
  mi.layout = BorderLayout()
  mi.add(check, BorderLayout.WEST)
  mi.add(slider)
  popup.add(mi)
}

private fun addCheckBoxAndToggleSlider(popup: JComponent) {
  val slider = makeBorderLayoutMenuItem()
  slider.add(makeSlider())
  val check = makeCheckBox()
  check.text = "JCheckBox + JSlider"
  check.addActionListener { e ->
    val b = e.source as? AbstractButton
    slider.isVisible = b?.isSelected == true
    val p = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, b)
    if (p is JPopupMenu) {
      p.pack()
    }
  }
  val mi = JMenuItem(" ")
  mi.layout = BorderLayout()
  mi.add(check)
  popup.add(mi)
  popup.add(slider)
}

private fun addCheckBoxMenuItemAndSlider(popup: JComponent) {
  val slider = makeBorderLayoutMenuItem()
  slider.add(makeSlider())
  val mi = JCheckBoxMenuItem("JCheckBoxMenuItem + JSlider")
  mi.addActionListener { e ->
    val b = e.source as? AbstractButton
    slider.isVisible = b?.isSelected == true
    val p = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, b)
    if (p is JPopupMenu) {
      p.setVisible(true)
      p.pack()
    }
  }
  popup.add(mi)
  popup.add(slider)
}

private fun makeSlider(): JSlider {
  UIManager.put("Slider.paintValue", false) // GTKLookAndFeel
  UIManager.put("Slider.focus", UIManager.get("Slider.background"))
  val slider = JSlider()
  slider.addMouseWheelListener { e ->
    (e.component as? JSlider)?.takeIf { it.isEnabled }?.model?.also {
      it.value = it.value - e.wheelRotation
    }
    e.consume()
  }
  return slider
}

private fun makeCheckBox() = object : JCheckBox() {
  private var handler: MouseInputListener? = null
  override fun updateUI() {
    removeMouseListener(handler)
    removeMouseMotionListener(handler)
    super.updateUI()
    handler = DispatchParentHandler()
    addMouseListener(handler)
    addMouseMotionListener(handler)
    isFocusable = false
    isOpaque = false
  }
}

private fun makeBorderLayoutMenuItem() = JMenuItem(" ").also {
  it.layout = BorderLayout()
  it.isVisible = false
  val w = UIManager.getInt("MenuItem.minimumTextOffset")
  it.add(Box.createHorizontalStrut(w), BorderLayout.WEST)
}

private class DispatchParentHandler : MouseInputAdapter() {
  private fun dispatchEvent(e: MouseEvent) {
    val src = e.component
    val tgt = SwingUtilities.getUnwrappedParent(src)
    tgt.dispatchEvent(SwingUtilities.convertMouseEvent(src, e, tgt))
  }

  override fun mouseEntered(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mouseExited(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mouseMoved(e: MouseEvent) {
    dispatchEvent(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    dispatchEvent(e)
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
    UnsupportedLookAndFeelException::class
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
