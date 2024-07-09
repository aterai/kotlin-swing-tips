package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.Locale
import javax.swing.*
import javax.swing.colorchooser.AbstractColorChooserPanel

fun makeUI(): Component {
  val label = object : JLabel() {
    override fun getPreferredSize() = Dimension(64, 64)
  }
  label.setOpaque(true)
  label.setBackground(Color(0xFF_FF_00_00.toInt(), true))
  val pp = JPanel()
  pp.add(label)
  val p = JPanel()
  p.setBorder(BorderFactory.createTitledBorder("JColorChooser:"))
  p.add(makeButton1(label))
  p.add(makeButton2(label))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(pp)
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton1(label: JLabel): JButton {
  val button1 = JButton("Default")
  button1.addActionListener {
    val cc = JColorChooser()
    cc.color = label.getBackground()
    val ok = ColorTracker(cc)
    val parent = button1.rootPane
    val title = "Default JColorChooser"
    val dialog = JColorChooser.createDialog(parent, title, true, cc, ok, null)
    dialog.addComponentListener(object : ComponentAdapter() {
      override fun componentHidden(e: ComponentEvent) {
        (e.component as? Window)?.dispose()
      }
    })
    dialog.isVisible = true
    ok.color?.also {
      label.setBackground(it)
    }
  }
  return button1
}

private fun makeButton2(label: JLabel): JButton {
  val button2 = JButton("Mnemonic")
  button2.addActionListener {
    val cc = JColorChooser()
    cc.color = label.background
    val rgbChooser = getRgbChooser(cc)
    if (rgbChooser != null) {
      val panels = cc.chooserPanels
      val choosers = panels.toMutableList()
      SwingUtils
        .descendants(rgbChooser)
        .filterIsInstance<JRadioButton>()
        .forEach {
          setMnemonic(it, rgbChooser.getLocale())
        }
      cc.setChooserPanels(choosers.toTypedArray())
    }
    val ok = ColorTracker(cc)
    val parent = button2.rootPane
    val title = "ColorChooser.rgbRedTextMnemonic"
    val dialog = JColorChooser.createDialog(parent, title, true, cc, ok, null)
    dialog.addComponentListener(object : ComponentAdapter() {
      override fun componentHidden(e: ComponentEvent) {
        (e.component as Window).dispose()
      }
    })
    dialog.isVisible = true
    ok.color?.also {
      label.setBackground(it)
    }
  }
  return button2
}

private fun setMnemonic(
  r: JRadioButton,
  locale: Locale,
) {
  val rgbKey = listOf("rgbRed", "rgbGreen", "rgbBlue")
  val fmt = "ColorChooser.%sText"
  val rgbList = listOf(
    UIManager.getString(fmt.format(rgbKey[0]), locale),
    UIManager.getString(fmt.format(rgbKey[1]), locale),
    UIManager.getString(fmt.format(rgbKey[2]), locale),
  )
  val txt = r.text
  val idx = rgbList.indexOf(txt)
  if (idx >= 0) {
    val key = "ColorChooser.%sMnemonic".format(rgbKey[idx])
    val mnemonic = getInteger(key, locale)
    if (mnemonic > 0) {
      r.setMnemonic(Character.toChars(mnemonic)[0])
      setDisplayedMnemonicIndex(r, key, locale)
    }
  }
}

private fun setDisplayedMnemonicIndex(
  r: JRadioButton,
  key: String,
  locale: Locale,
) {
  val mnemonic = getInteger(key + "Index", locale)
  if (mnemonic >= 0) {
    r.setDisplayedMnemonicIndex(mnemonic)
  }
}

private fun getInteger(
  key: String,
  locale: Locale,
): Int {
  val value = UIManager.get(key, locale)
  return when (value) {
    is Int -> value
    is String -> runCatching { value.toInt() }.getOrDefault(-1)
    else -> -1
  }
}

private fun getRgbChooser(colorChooser: JColorChooser): AbstractColorChooserPanel? {
  val rgbName = UIManager.getString("ColorChooser.rgbNameText", Locale.getDefault())
  var rgbChooser: AbstractColorChooserPanel? = null
  for (p in colorChooser.chooserPanels) {
    if (rgbName == p.displayName) {
      rgbChooser = p
    }
  }
  return rgbChooser
}

private class ColorTracker(
  private val chooser: JColorChooser,
) : ActionListener {
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
