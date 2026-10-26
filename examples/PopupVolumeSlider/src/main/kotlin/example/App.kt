package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

private const val DEFAULT_VOLUME = 80

fun createUI(): Component {
  val popup = JPopupMenu()
  popup.layout = BorderLayout()
  popup.addMouseWheelListener { it.consume() }
  UIManager.put("Slider.paintValue", true)
  UIManager.put("Slider.focus", UIManager.get("Slider.background"))
  val slider = object : JSlider(VERTICAL, 0, 100, DEFAULT_VOLUME) {
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
        val showingChanged = (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong()) != 0L
        if (showingChanged && e.component.isShowing) {
          // Overlay the popup menu on top of the tooltip just shown
          showPopup()
        }
      }
      return tip
    }

    fun showPopup() {
      val d = popup.preferredSize
      popup.show(this, (width - d.width) / 2, -d.height)
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
      // A MouseListener is used instead of an ActionListener so that
      // the popup also opens when the button is disabled (muted)
      if (!button.isEnabled) {
        slider.value = DEFAULT_VOLUME
        button.isEnabled = true
      }
      button.showPopup()
    }

    override fun mouseEntered(e: MouseEvent) {
      enableToolTipIfPopupHidden()
    }

    override fun mouseExited(e: MouseEvent) {
      enableToolTipIfPopupHidden()
    }

    // Restore the tooltip disabled by PopupMenuListener#popupMenuWillBecomeVisible(...)
    private fun enableToolTipIfPopupHidden() {
      if (!popup.isVisible) {
        ToolTipManager.sharedInstance().isEnabled = true
      }
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
      // button#setEnabled(...) also switches the icon text
      button.isEnabled = it.value > it.minimum
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
