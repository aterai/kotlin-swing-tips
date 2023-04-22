package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

fun makeUI(): Component {
  val popup = JPopupMenu()
  popup.layout = BorderLayout()
  popup.addMouseWheelListener { it.consume() }
  UIManager.put("Slider.paintValue", true)
  UIManager.put("Slider.focus", UIManager.get("Slider.background"))
  val slider = object : JSlider(VERTICAL, 0, 100, 80) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 120
      return d
    }
  }
  slider.addMouseWheelListener { e ->
    (e.component as? JSlider)?.also {
      if (it.isEnabled) {
        val m = it.model
        m.value = m.value - e.wheelRotation * 2
      }
    }
    e.consume()
  }
  popup.add(slider)

  val button = object : JToggleButton("🔊") {
    override fun createToolTip(): JToolTip {
      val tip = super.createToolTip()
      tip.addHierarchyListener { e ->
        val flg = e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()
        if (flg != 0L && e.component.isShowing) {
          val d = popup.preferredSize
          popup.show(this, (width - d.width) / 2, -d.height)
        }
      }
      return tip
    }

    override fun getToolTipLocation(e: MouseEvent) = Point(width / 2, -height)

    override fun setEnabled(b: Boolean) {
      super.setEnabled(b)
      text = if (b) "🔊" else "🔇"
    }
  }
  button.toolTipText = ""
  button.addMouseListener(object : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      val btn = e.component
      if (!btn.isEnabled) {
        slider.value = 80
        btn.isEnabled = true
      }
      val d = popup.preferredSize
      popup.show(btn, (btn.width - d.width) / 2, -d.height)
    }

    override fun mouseEntered(e: MouseEvent) {
      if (!popup.isVisible) {
        ToolTipManager.sharedInstance().isEnabled = true
      }
    }

    override fun mouseExited(e: MouseEvent) {
      mouseEntered(e)
    }
  })
  popup.addPopupMenuListener(object : PopupMenuListener {
    override fun popupMenuCanceled(e: PopupMenuEvent) {
      // not needed
    }

    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      EventQueue.invokeLater { ToolTipManager.sharedInstance().isEnabled = false }
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      button.isSelected = false
    }
  })
  slider.model.addChangeListener { e ->
    (e.source as? BoundedRangeModel)?.also {
      button.isEnabled = it.value > it.minimum
      button.repaint()
    }
  }

  val box = Box.createHorizontalBox()
  box.add(button)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(2, 10, 2, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
