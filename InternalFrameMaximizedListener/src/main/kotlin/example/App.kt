package example

import java.awt.*
import java.util.EventObject
import javax.swing.*
import javax.swing.event.InternalFrameEvent
import javax.swing.event.InternalFrameListener

private val textArea = JTextArea()

fun makeUI(): Component {
  val frame = JInternalFrame("title", true, true, true, true)
  frame.addPropertyChangeListener { e ->
    val prop = e.propertyName
    if (JInternalFrame.IS_MAXIMUM_PROPERTY == prop) {
      val str = if (e.newValue == true) "maximized" else "minimized"
      textArea.append("* Internal frame %s: %s%n".format(str, e.source))
      textArea.caretPosition = textArea.document.length
    }
  }
  val ifl = object : InternalFrameListener {
    override fun internalFrameClosing(e: InternalFrameEvent) {
      displayMessage("Internal frame closing", e)
    }

    override fun internalFrameClosed(e: InternalFrameEvent) {
      displayMessage("Internal frame closed", e)
    }

    override fun internalFrameOpened(e: InternalFrameEvent) {
      displayMessage("Internal frame opened", e)
    }

    override fun internalFrameIconified(e: InternalFrameEvent) {
      displayMessage("Internal frame iconified", e)
    }

    override fun internalFrameDeiconified(e: InternalFrameEvent) {
      displayMessage("Internal frame deiconified", e)
    }

    override fun internalFrameActivated(e: InternalFrameEvent) {
      displayMessage("Internal frame activated", e)
    }

    override fun internalFrameDeactivated(e: InternalFrameEvent) {
      displayMessage("Internal frame deactivated", e)
    }

    private fun displayMessage(
      prefix: String,
      e: EventObject,
    ) {
      val s = prefix + ": " + e.source + "\n"
      textArea.append(s)
      textArea.caretPosition = textArea.document.length
    }
  }
  frame.addInternalFrameListener(ifl)
  frame.setBounds(10, 10, 160, 100)

  val desktop = JDesktopPane()
  desktop.add(frame)
  EventQueue.invokeLater { frame.isVisible = true }

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = desktop
  sp.bottomComponent = JScrollPane(textArea)
  sp.resizeWeight = .8

  return JPanel(BorderLayout()).also {
    it.add(sp)
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
