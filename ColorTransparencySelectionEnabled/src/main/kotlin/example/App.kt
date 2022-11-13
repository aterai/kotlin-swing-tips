package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.colorchooser.AbstractColorChooserPanel

private val defaultRadio = JRadioButton("Default")
private val enabledRadio = JRadioButton("setEnabled(false)", true)

fun makeUI(): Component {
  val label = JLabel()
  label.isOpaque = true
  label.background = Color.WHITE
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder("ColorTransparencySelectionEnabled")
  val bg = ButtonGroup()
  val visibleRadio = JRadioButton("setVisible(false)")
  for (r in listOf(defaultRadio, enabledRadio, visibleRadio)) {
    bg.add(r)
    box.add(r)
  }
  val button = JButton("open JColorChooser")
  button.addActionListener {
    val rgbName = UIManager.getString("ColorChooser.rgbNameText", button.locale)
    val cc = JColorChooser()
    for (ccPanel in cc.chooserPanels) {
      // Java 9: ccPanel.setColorTransparencySelectionEnabled(colorTransparency)
      if (rgbName == ccPanel.displayName) {
        if (!defaultRadio.isSelected) {
          EventQueue.invokeLater { setTransparencySelectionEnabled(ccPanel) }
        }
      } else {
        cc.removeChooserPanel(ccPanel)
      }
    }
    val rp = button.rootPane
    val ok = ColorTracker(cc)
    val dialog = JColorChooser.createDialog(rp, "title", true, cc, ok, null)
    dialog.addComponentListener(object : ComponentAdapter() {
      override fun componentHidden(e: ComponentEvent) {
        (e.component as? Window)?.dispose()
      }
    })
    dialog.isVisible = true // blocks until user brings dialog down...
    val color = ok.color
    if (color != null) {
      EventQueue.invokeLater { label.background = color }
    }
  }

  return JPanel(BorderLayout(10, 10)).also {
    it.add(box, BorderLayout.NORTH)
    it.add(label)
    it.add(button, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setTransparencySelectionEnabled(p: AbstractColorChooserPanel) {
  val alphaName = UIManager.getString("ColorChooser.rgbAlphaText", p.locale)
  var idx0 = 0
  var idx1 = 0
  for (c in SwingUtils.descendants(p)) {
    when {
      c is JLabel && alphaName == c.text -> setEnabledOrVisible(c)
      c is JSlider && idx0++ == 3 -> setEnabledOrVisible(c)
      c is JSpinner && idx1++ == 3 -> setEnabledOrVisible(c)
    }
  }
}

private fun setEnabledOrVisible(c: Component) {
  if (enabledRadio.isSelected) {
    c.isEnabled = false
  } else {
    c.isVisible = false
  }
}

private class ColorTracker(private val chooser: JColorChooser) : ActionListener {
  var color: Color? = null
    private set

  override fun actionPerformed(e: ActionEvent) {
    color = chooser.color
  }
}

object SwingUtils {
  fun descendants(parent: Container): List<Component> = parent.components
    .filterIsInstance<Container>()
    .flatMap { listOf(it) + descendants(it) }
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
