package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.EventListener
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports

private val FONT12 = Font(Font.SANS_SERIF, Font.PLAIN, 12)
private val FONT32 = Font(Font.SANS_SERIF, Font.PLAIN, 32)

private val p = object : JPanel(BorderLayout()) {
  fun addFontChangeListener(l: FontChangeListener?) {
    listenerList.add(FontChangeListener::class.java, l)
  }

  // fun removeFontChangeListener(l: FontChangeListener?) {
  //   listenerList.remove(FontChangeListener::class.java, l)
  // }

  fun fireFontChangeEvent(font: Font) {
    val listeners = listenerList.listenerList
    val e = FontChangeEvent(this, font)
    var i = listeners.size - 2
    while (i >= 0) {
      if (listeners[i] === FontChangeListener::class.java) {
        (listeners[i + 1] as? FontChangeListener)?.fontStateChanged(e)
      }
      i -= 2
    }
  }
}

fun makeUI(): Component {
  val button = JButton("JButton")
  button.font = FONT12
  p.addFontChangeListener { e -> button.font = e.font }

  val label = JLabel("JLabel")
  label.font = FONT12
  p.addFontChangeListener { e -> button.font = e.font }

  val combo = JComboBox(DefaultComboBoxModel(arrayOf("item1", "item2")))
  combo.font = FONT12
  p.addFontChangeListener { e -> button.font = e.font }

  val menu = JMenu("Font")
  menu.toolTipText = "Select font size"
  menu.add("32pt").addActionListener {
    p.fireFontChangeEvent(FONT32)
  }
  menu.add("12pt").addActionListener {
    p.fireFontChangeEvent(FONT12)
  }

  val menuBar = JMenuBar()
  menuBar.add(menu)
  EventQueue.invokeLater { p.rootPane.jMenuBar = menuBar }

  val panel = JPanel()
  panel.add(label)
  panel.add(combo)
  panel.add(button)
  p.add(panel)
  p.preferredSize = Dimension(320, 240)
  return p
}

fun interface FontChangeListener : EventListener {
  fun fontStateChanged(e: FontChangeEvent)
}

class FontChangeEvent(source: Any?, val font: Font?) : EventObject(source) {
  companion object {
    private const val serialVersionUID = 1L
  }
}

// private class MyComboBox : JComboBox<String>(), FontChangeListener {
//   override fun fontStateChanged(e: FontChangeEvent) {
//     font = e.font
//   }
// }
//
// private class MyLabel(str: String) : JLabel(str), FontChangeListener {
//   override fun fontStateChanged(e: FontChangeEvent) {
//     font = e.font
//   }
// }
//
// private class MyButton(str: String) : JButton(str), FontChangeListener {
//   override fun fontStateChanged(e: FontChangeEvent) {
//     font = e.font
//   }
// }

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
