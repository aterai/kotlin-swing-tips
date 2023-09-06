package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicSliderUI

fun makeUI(): Component {
  val key = "Slider.paintThumbArrowShape"
  val slider0 = JSlider()
  slider0.putClientProperty(key, true)

  val slider1 = object : JSlider() {
    override fun updateUI() {
      super.updateUI()
      val tmp = if (ui is WindowsSliderUI) {
        object : WindowsSliderUI(this) {
          override fun createTrackListener(slider: JSlider) = object : TrackListener() {
            override fun shouldScroll(direction: Int) = false
          }
        }
      } else {
        object : BasicSliderUI(this) {
          override fun createTrackListener(slider: JSlider) = object : TrackListener() {
            override fun shouldScroll(direction: Int) = false
          }
        }
      }
      setUI(tmp)
    }
  }
  slider1.putClientProperty(key, true)

  // https://ateraimemo.com/Swing/OnlyLeftMouseButtonDrag.html
  UIManager.put("Slider.onlyLeftMouseButtonDrag", false)
  val slider2 = JSlider()
  slider2.putClientProperty(key, true)
  val layer = JLayer(slider2, DisableLeftPressedLayerUI<Component>())

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  box.add(makeTitledPanel("Default", slider0))
  box.add(Box.createVerticalStrut(20))
  box.add(JLabel(" disable scroll due to click in track"))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("Override TrackListener#shouldScroll(...): false", slider1))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("JLayer + Slider.onlyLeftMouseButtonDrag: false", layer))
  box.add(Box.createVerticalGlue())
  box.components.toList()
    .filterIsInstance<JComponent>()
    .forEach { it.alignmentX = Component.LEFT_ALIGNMENT }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c, BorderLayout.NORTH)
  return p
}

private class DisableLeftPressedLayerUI<V : Component> : LayerUI<V>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.MOUSE_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out V>) {
    if (e.id == MouseEvent.MOUSE_PRESSED && SwingUtilities.isLeftMouseButton(e)) {
      e.component.dispatchEvent(
        MouseEvent(
          e.component,
          e.id,
          e.getWhen(),
          InputEvent.BUTTON3_DOWN_MASK,
          e.x,
          e.y,
          e.xOnScreen,
          e.yOnScreen,
          e.clickCount,
          e.isPopupTrigger,
          MouseEvent.BUTTON3
        )
      )
      e.consume()
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
