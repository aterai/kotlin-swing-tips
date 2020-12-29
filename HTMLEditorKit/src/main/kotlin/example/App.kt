package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

fun makeUI(): Component {
  val textPane = JTextPane()
  textPane.componentPopupMenu = HtmlColorPopupMenu()
  textPane.contentType = "text/html"

  val textArea = JTextArea()
  textArea.text = textPane.text

  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("JTextPane", JScrollPane(textPane))
  tabbedPane.addTab("JTextArea", JScrollPane(textArea))
  tabbedPane.addChangeListener { e ->
    (e.source as? JTabbedPane)?.also {
      val isHtmlMode = it.selectedIndex == 0
      if (isHtmlMode) {
        textPane.text = textArea.text
      } else {
        val str = textPane.text
        textArea.text = str
      }
      it.revalidate()
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HtmlColorPopupMenu : JPopupMenu() {
  init {
    val red = SimpleAttributeSet()
    StyleConstants.setForeground(red, Color.RED)
    val green = SimpleAttributeSet()
    StyleConstants.setForeground(green, Color.GREEN)
    val blue = SimpleAttributeSet()
    StyleConstants.setForeground(blue, Color.BLUE)
    add("Red").addActionListener {
      (invoker as? JTextPane)?.also {
        val doc = it.styledDocument
        val start = it.selectionStart
        val end = it.selectionEnd
        doc.setCharacterAttributes(start, end - start, red, false)
      }
    }
    add("Green").addActionListener {
      (invoker as? JTextPane)?.also {
        val doc = it.styledDocument
        val start = it.selectionStart
        val end = it.selectionEnd
        doc.setCharacterAttributes(start, end - start, green, false)
      }
    }
    add("Blue").addActionListener {
      (invoker as? JTextPane)?.also {
        val doc = it.styledDocument
        val start = it.selectionStart
        val end = it.selectionEnd
        doc.setCharacterAttributes(start, end - start, blue, false)
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTextPane)?.also {
      val start = it.selectionStart
      val end = it.selectionEnd
      val flag = end - start > 0
      for (me in subElements) {
        me.component.isEnabled = flag
      }
      super.show(c, x, y)
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
