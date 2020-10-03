package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener

fun makeUI() = object : JPanel() {
  @Transient private var handler: MouseInputListener? = null

  init {
    add(JLabel("mouseDragged: Show JToolTip"))
    preferredSize = Dimension(320, 240)
  }

  override fun updateUI() {
    removeMouseMotionListener(handler)
    removeMouseListener(handler)
    super.updateUI()
    handler = ToolTipLocationHandler()
    addMouseMotionListener(handler)
    addMouseListener(handler)
  }
}

private class ToolTipLocationHandler : MouseInputAdapter() {
  private val window = JWindow()
  private val tip = JToolTip()
  private val factory = PopupFactory.getSharedInstance()
  private var popup: Popup? = null
  private var prev = ""

  private fun getToolTipLocation(e: MouseEvent): Point {
    val p = e.point
    val c = e.component
    SwingUtilities.convertPointToScreen(p, c)
    p.translate(0, -tip.preferredSize.height)
    return p
  }

  private fun updateTipText(e: MouseEvent) {
    val pt = e.point
    val txt = "Window(x, y)=(%d, %d)".format(pt.x, pt.y)
    tip.tipText = txt
    val p = getToolTipLocation(e)
    if (SwingUtilities.isLeftMouseButton(e)) {
      if (prev.length != txt.length) {
        window.pack()
      }
      window.location = p
      window.isAlwaysOnTop = true
    } else {
      popup?.hide()
      popup = factory.getPopup(e.component, tip, p.x, p.y).also {
        val c = tip.topLevelAncestor
        if (c is JWindow && c.type == Window.Type.POPUP) {
          println("Popup\$HeavyWeightWindow")
        } else {
          it.show()
        }
      }
    }
    prev = txt
  }

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      window.add(tip)
      updateTipText(e)
      window.isVisible = true
    } else {
      updateTipText(e)
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    updateTipText(e)
  }

  override fun mouseReleased(e: MouseEvent) {
    popup?.hide()
    window.isVisible = false
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
