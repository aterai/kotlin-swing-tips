package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.colorchooser.AbstractColorChooserPanel
import javax.swing.plaf.basic.BasicColorChooserUI

private val defaultRadio = JRadioButton("Default")
private val enabledRadio = JRadioButton("setEnabled(false)", true)

fun makeUI(): Component {
  val label = JLabel()
  label.isOpaque = true
  label.background = Color.WHITE

  val button = JButton("open JColorChooser")
  button.addActionListener {
    val color = getColor(button.rootPane)
    if (color != null) {
      label.background = color
    }
  }

  return JPanel(BorderLayout(10, 10)).also {
    it.add(makeBox(), BorderLayout.NORTH)
    it.add(label)
    it.add(button, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getColor(parent: Container): Color? {
  val rgbName = UIManager.getString("ColorChooser.rgbNameText", parent.locale)
  val cc = object : JColorChooser() {
    override fun updateUI() {
      super.updateUI()
      if ("GTK".equals(UIManager.getLookAndFeel().id)) {
        setUI(BasicColorChooserUI())
      }
    }
  }
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
  val ok = ColorTracker(cc)
  val dialog = JColorChooser.createDialog(parent, "title", true, cc, ok, null)
  dialog.addComponentListener(object : ComponentAdapter() {
    override fun componentHidden(e: ComponentEvent) {
      (e.component as? Window)?.dispose()
    }
  })
  dialog.isVisible = true // blocks until user brings dialog down...
  val color = ok.color
  return color
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

private fun makeBox(): Box {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createTitledBorder("ColorTransparencySelectionEnabled")
  val bg = ButtonGroup()
  val visibleRadio = JRadioButton("setVisible(false)")
  for (r in listOf(defaultRadio, enabledRadio, visibleRadio)) {
    bg.add(r)
    box.add(r)
  }
  return box
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
