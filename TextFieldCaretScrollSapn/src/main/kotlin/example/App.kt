package example

import java.awt.*
import javax.swing.*
import javax.swing.text.DefaultCaret
import javax.swing.text.Position.Bias

fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1, 10, 10))
  p.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  val txt = "1234567890 ".repeat(10)
  val field0 = JTextField(32)
  field0.text = txt
  p.add(makeTitledPanel("LookAndFeel Default", field0))

  val field1 = object : JTextField(32) {
    override fun updateUI() {
      super.updateUI()
      val caret = DefaultCaret()
      caret.blinkRate = UIManager.getInt("TextField.caretBlinkRate")
      setCaret(caret)
    }
  }
  field1.text = txt
  p.add(makeTitledPanel("DefaultCaret", field1))

  val field2 = object : JTextField(32) {
    override fun updateUI() {
      super.updateUI()
      val caret = HorizontalScrollCaret()
      caret.blinkRate = UIManager.getInt("TextField.caretBlinkRate")
      setCaret(caret)
    }
  }
  field2.text = txt
  p.add(makeTitledPanel("override DefaultCaret#adjustVisibility(...)", field2))

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class HorizontalScrollCaret : DefaultCaret() {
  override fun adjustVisibility(r: Rectangle) {
    EventQueue.invokeLater {
      (component as? JTextField)?.also {
        horizontalScroll(it, r)
      }
    }
  }

  private fun horizontalScroll(field: JTextField, r: Rectangle) {
    val ui = field.ui
    val dot = dot
    val bias = Bias.Forward
    val startRect = kotlin.runCatching {
      ui.modelToView(field, dot, bias)
    }.getOrNull()
    val i = field.insets
    val vis = field.horizontalVisibility
    val x = r.x + vis.value - i.left
    val n = 8
    val span = vis.extent / n
    if (r.x < i.left) {
      vis.value = x - span
    } else if (r.x + r.width > i.left + vis.extent) {
      vis.value = x - (n - 1) * span
    }
    if (startRect != null) {
      runCatching {
        val endRect = ui.modelToView(field, dot, bias)
        if (endRect != null && endRect != startRect) {
          damage(endRect)
        }
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(field)
      }
    }
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

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
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
    UnsupportedLookAndFeelException::class,
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
