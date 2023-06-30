package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent

private val textArea = JTextArea()
private val timer = Timer(200) {
  val s = LocalDateTime.now(ZoneId.systemDefault()).toString()
  textArea.append(if (textArea.document.length > 0) "\n$s" else s)
}

fun makeUI(): Component {
  // (textArea.document as? AbstractDocument)?.documentFilter = FifoDocumentFilter()
  textArea.document.addDocumentListener(FifoDocumentListener(textArea))
  textArea.isEditable = false

  val start = JButton("Start")
  start.addActionListener {
    if (!timer.isRunning) {
      timer.start()
    }
  }

  val stop = JButton("Stop")
  stop.addActionListener { timer.stop() }

  val clear = JButton("Clear")
  clear.addActionListener { textArea.text = "" }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(start)
  box.add(stop)
  box.add(Box.createHorizontalStrut(5))
  box.add(clear)
  box.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      timer.stop()
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class FifoDocumentListener(private val textComponent: JTextComponent) : DocumentListener {
  override fun insertUpdate(e: DocumentEvent) {
    val doc = e.document
    val root = doc.defaultRootElement
    if (root.elementCount <= MAX_LINES) {
      return
    }
    EventQueue.invokeLater {
      runCatching {
        doc.remove(0, root.getElement(0).endOffset)
      }
    }
    textComponent.caretPosition = doc.length
  }

  override fun removeUpdate(e: DocumentEvent) {
    // not needed
  }

  override fun changedUpdate(e: DocumentEvent) {
    // not needed
  }

  companion object {
    private const val MAX_LINES = 10
  }
}

// private class FifoDocumentFilter : DocumentFilter() {
//   @Throws(BadLocationException::class)
//   override fun insertString(fb: FilterBypass, offset: Int, text: String, attr: AttributeSet) {
//     fb.insertString(offset, text, attr)
//     val root = fb.document.defaultRootElement
//     if (root.elementCount > MAX_LINES) {
//       fb.remove(0, root.getElement(0).endOffset)
//     }
//   }
//
//   companion object {
//     private const val MAX_LINES = 10
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
