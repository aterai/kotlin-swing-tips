package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.lang.invoke.MethodHandles
import java.util.logging.Logger
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun makeUI(): Component {
  val textArea = JTextArea("JFrame Conditional Close Test")
  val exitButton = JButton(SaveHandler.CMD_EXIT)
  val saveButton = JButton(SaveHandler.CMD_SAVE)
  EventQueue.invokeLater {
    (textArea.topLevelAncestor as? Frame)?.also { frame ->
      val handler = SaveHandler(frame)
      handler.addEnabledFlagComponent(saveButton)
      frame.addWindowListener(handler)
      textArea.document.addDocumentListener(handler)
      exitButton.addActionListener(handler)
      saveButton.addActionListener(handler)
    }
  }
  exitButton.actionCommand = SaveHandler.CMD_EXIT
  saveButton.actionCommand = SaveHandler.CMD_SAVE
  saveButton.isEnabled = false

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(exitButton)
  box.add(Box.createHorizontalStrut(5))
  box.add(saveButton)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.SOUTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class SaveHandler(
  private val frame: Frame
) : WindowAdapter(), DocumentListener, ActionListener {
  private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)
  private val title = frame.title
  private val list = mutableListOf<JComponent>()

  // WindowAdapter
  override fun windowClosing(e: WindowEvent) {
    logger.info { "windowClosing" }
    maybeExit()
  }

  // ActionListener
  override fun actionPerformed(e: ActionEvent) {
    val cmd = e.actionCommand
    if (CMD_EXIT == cmd) {
      maybeExit()
    } else if (CMD_SAVE == cmd) {
      fireUnsavedFlagChangeEvent(false)
    }
  }

  // DocumentListener
  override fun insertUpdate(e: DocumentEvent) {
    fireUnsavedFlagChangeEvent(true)
  }

  override fun removeUpdate(e: DocumentEvent) {
    fireUnsavedFlagChangeEvent(true)
  }

  override fun changedUpdate(e: DocumentEvent) {
    // not needed
  }

  private fun maybeExit() {
    if (title == frame.title) {
      logger.info { "The document has already been saved, exit without doing anything." }
      frame.dispose()
      return
    }
    Toolkit.getDefaultToolkit().beep()
    val options = arrayOf("Save", "Discard", "Cancel")
    val retValue = JOptionPane.showOptionDialog(
      frame,
      "<html>Save: Exit & Save Changes<br>Discard: Exit & Discard Changes<br>Cancel: Continue",
      "Exit Options",
      JOptionPane.YES_NO_CANCEL_OPTION,
      JOptionPane.INFORMATION_MESSAGE,
      null,
      options,
      options[0]
    )
    if (retValue != JOptionPane.CANCEL_OPTION) {
      frame.dispose()
    }
    // debug info:
    when (retValue) {
      JOptionPane.YES_OPTION -> logger.info { "Exit" }
      JOptionPane.NO_OPTION -> logger.info { "Exit without save" }
      JOptionPane.CANCEL_OPTION -> logger.info { "Cancel exit" }
    }
  }

  fun addEnabledFlagComponent(c: JComponent) {
    list.add(c)
  }

  // fun removeEnabledFlagComponent(c: JComponent) {
  //   list.remove(c)
  // }

  private fun fireUnsavedFlagChangeEvent(unsaved: Boolean) {
    val mark = if (unsaved) "* " else ""
    frame.title = "$mark$title"
    for (c in list) {
      c.isEnabled = unsaved
    }
  }

  companion object {
    // const val ASTERISK_TITLE_BAR = "unsaved"
    const val CMD_SAVE = "save"
    const val CMD_EXIT = "exit"
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
      defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
      // defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
      // defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      // defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
