package example

import com.sun.java.swing.plaf.windows.WindowsComboBoxUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.text.JTextComponent

fun makeUI(): Component {
  val field = JTextField()
  field.inputVerifier = LengthInputVerifier()

  val combo1 = makeComboBox(10)
  combo1.isEditable = true
  if (combo1.ui is WindowsComboBoxUI) {
    combo1.setUI(object : WindowsComboBoxUI() {
      override fun createPopup() = BasicComboPopup2(comboBox)
    })
  } else {
    combo1.setUI(object : BasicComboBoxUI() {
      override fun createPopup() = BasicComboPopup2(comboBox)
    })
  }

  val combo2 = makeComboBox(20)
  combo2.isFocusable = false

  val combo3 = makeComboBox(15)
  combo3.isEditable = true

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("InputVerifier:", field))
  box.add(Box.createVerticalStrut(4))
  box.add(makeTitledPanel("Default:", makeComboBox(5)))
  box.add(Box.createVerticalStrut(4))
  box.add(makeTitledPanel("setFocusable(false):", combo2))
  box.add(Box.createVerticalStrut(4))
  box.add(makeTitledPanel("setEditable(true):", combo3))
  box.add(Box.createVerticalStrut(4))
  box.add(makeTitledPanel("Override BasicComboPopup:", combo1))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private fun makeComboBox(size: Int): JComboBox<String> {
  val model = DefaultComboBoxModel<String>()
  for (i in 0..<size) {
    model.addElement("No.$i")
  }
  return JComboBox(model)
}

private class LengthInputVerifier : InputVerifier() {
  override fun verify(c: JComponent): Boolean {
    var verified = false
    if (c is JTextComponent) {
      val str = c.text
      verified = str.length > MIN_LEN
    }
    if (!verified) {
      UIManager.getLookAndFeel().provideErrorFeedback(c)
      val msg = "Enter at least %s characters.".format(MIN_LEN)
      JOptionPane.showMessageDialog(c.rootPane, msg, "Error", JOptionPane.ERROR_MESSAGE)
    }
    return verified
  }

  companion object {
    private const val MIN_LEN = 5
  }
}

private class BasicComboPopup2(
  combo: JComboBox<Any>,
) : BasicComboPopup(combo) {
  private var handler2: MouseListener? = null

  override fun uninstallingUI() {
    super.uninstallingUI()
    handler2 = null
  }

  override fun createMouseListener(): MouseListener {
    val handler = handler2 ?: Handler2()
    handler2 = handler
    return handler
  }

  private inner class Handler2 : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (!SwingUtilities.isLeftMouseButton(e) || !comboBox.isEnabled) {
        return
      }
      var hasFocus = true
      if (comboBox.isEditable) {
        val comp = comboBox.editor.editorComponent
        if (comp !is JComponent || comp.isRequestFocusEnabled) {
          hasFocus = comp.hasFocus() || comp.requestFocusInWindow()
        }
      } else if (comboBox.isRequestFocusEnabled) {
        hasFocus = comboBox.hasFocus() || comboBox.requestFocusInWindow()
      }
      val c = e.component
      if (hasFocus) {
        togglePopup()
      } else if (c is AbstractButton) {
        c.model.isPressed = false
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      val source = e.source as? Component ?: return
      val size = source.size
      val bounds = Rectangle(0, 0, size.width - 1, size.height - 1)
      if (!bounds.contains(e.point)) {
        val newEvent = SwingUtilities.convertMouseEvent(e.component, e, list)
        val location = newEvent.point
        val r = Rectangle()
        list.computeVisibleRect(r)
        if (r.contains(location)) {
          if (comboBox.selectedIndex == list.selectedIndex) {
            comboBox.editor.item = list.selectedValue
          }
          comboBox.selectedIndex = list.selectedIndex
        }
        comboBox.isPopupVisible = false
      }
      hasEntered = false
      stopAutoScrolling()
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
