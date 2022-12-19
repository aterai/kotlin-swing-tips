package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.plaf.basic.BasicCheckBoxMenuItemUI

fun makeUI(): Component {
  // Java 9
  // UIManager.put("CheckBoxMenuItem.doNotCloseOnMouseClick", true)

  val button = JToggleButton("JPopupMenu Test")
  val popup = JPopupMenu()
  val handler = TogglePopupHandler(popup, button)
  popup.addPopupMenuListener(handler)
  button.addActionListener(handler)

  // Java 9
  // JCheckBoxMenuItem checkMenuItem = JCheckBoxMenuItem("doNotCloseOnMouseClick")
  // checkMenuItem.putClientProperty("CheckBoxMenuItem.doNotCloseOnMouseClick", true)
  // popup.add(checkMenuItem)

  val check = object : JCheckBox("JCheckBox") {
    override fun updateUI() {
      super.updateUI()
      isFocusPainted = false
    }

    override fun getMinimumSize(): Dimension {
      val d = preferredSize
      d.width = Short.MAX_VALUE.toInt()
      return d
    }
  }
  popup.add(check)
  popup.add(makeStayOpenCheckBoxMenuItem(JMenuItem("JMenuItem + JCheckBox")))
  popup.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  popup.add(JCheckBoxMenuItem("keeping open #1")).addActionListener { e ->
    // println("ActionListener")
    val c = SwingUtilities.getAncestorOfClass(JPopupMenu::class.java, e.source as? Component)
    (c as? JPopupMenu)?.isVisible = true
  }

  val mi = object : JCheckBoxMenuItem("keeping open #2") {
    override fun updateUI() {
      super.updateUI()
      val tmp = object : BasicCheckBoxMenuItemUI() {
        override fun doClick(msm: MenuSelectionManager) {
          // println("MenuSelectionManager: doClick")
          menuItem.doClick(0)
        }
      }
      setUI(tmp)
    }
  }
  popup.add(mi)

  return JPanel().also {
    it.isOpaque = true
    it.componentPopupMenu = popup
    it.add(button)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeStayOpenCheckBoxMenuItem(mi: JMenuItem): JMenuItem {
  val text = mi.text
  mi.text = " "
  mi.layout = BorderLayout()
  val check = object : JCheckBox(text) {
    @Transient private var handler: MouseInputListener? = null

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
  mi.add(check)
  return mi
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

private class TogglePopupHandler(
  private val popup: JPopupMenu,
  private val button: AbstractButton
) : PopupMenuListener, ActionListener {
  override fun actionPerformed(e: ActionEvent) {
    val b = e.source as? AbstractButton
    if (b?.isSelected == true) {
      val p = SwingUtilities.getUnwrappedParent(b)
      val r = b.bounds
      popup.show(p, r.x, r.y + r.height)
    } else {
      popup.isVisible = false
    }
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    /* not needed */
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    EventQueue.invokeLater {
      button.model.isArmed = false
      button.model.isSelected = false
    }
  }

  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    /* not needed */
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
