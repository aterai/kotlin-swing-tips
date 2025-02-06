package example

import java.awt.*
import javax.swing.*

private const val TEXT = """

Trail: Creating a GUI with JFC/Swing
Lesson: Learning Swing by Example
This lesson explains the concepts you need to use Swing components in building a user interface.
First we examine the simplest Swing application you can write.
Then we present several progressively complicated examples of creating user interfaces using
 components in the javax.swing package.
We cover several Swing components, such as buttons, labels, and text areas. The handling of
 events is also discussed, as are layout management and accessibility. This lesson ends with
a set of questions and exercises so you can test yourself on what you've learned.
https://docs.oracle.com/javase/tutorial/uiswing/learn/index.html
"""

fun makeUI(): Component {
  val textArea1 = JTextArea("JTextArea#setMargin(Insets)$TEXT")
  textArea1.margin = Insets(5, 5, 5, 5)
  val scroll1 = JScrollPane(textArea1)

  val textArea2 = JTextArea("JScrollPane#setViewportBorder(...)$TEXT")
  textArea2.margin = Insets(0, 0, 0, 1)
  val scroll2 = object : JScrollPane(textArea2) {
    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        val c = getViewport().view.background
        viewportBorder = BorderFactory.createLineBorder(c, 5)
      }
    }
  }

  return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll1, scroll2).also {
    it.resizeWeight = .5
    it.preferredSize = Dimension(320, 240)
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
