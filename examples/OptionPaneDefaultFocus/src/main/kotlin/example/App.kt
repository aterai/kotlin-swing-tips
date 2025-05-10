package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener

private const val DUMMY = "Hello"

fun makeUI(): Component {
  val log = JTextArea()
  val p = JPanel(GridLayout(2, 2, 5, 5))
  val textField1 = JTextField(DUMMY)
  p.add(makeTitledPanel("Default", makeButton(textField1, log)))
  val textField2 = JTextField(DUMMY)
  p.add(makeTitledPanel("WindowListener", makeButton2(textField2, log)))
  val textField3 = JTextField(DUMMY)
  textField3.addHierarchyListener(FocusHierarchyListener())
  p.add(makeTitledPanel("HierarchyListener", makeButton(textField3, log)))
  val textField4 = JTextField(DUMMY)
  textField4.addAncestorListener(FocusAncestorListener())
  p.add(makeTitledPanel("AncestorListener", makeButton(textField4, log)))
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton(
  field: JTextField,
  textArea: JTextArea,
): JButton {
  val button = JButton("show")
  button.addActionListener {
    val p = textArea.rootPane
    val ret = JOptionPane.showConfirmDialog(
      p,
      field,
      "Input Text",
      JOptionPane.OK_CANCEL_OPTION,
      JOptionPane.PLAIN_MESSAGE,
    )
    if (ret == JOptionPane.OK_OPTION) {
      textArea.text = field.text
    }
  }
  return button
}

private fun makeButton2(
  textField: JTextField,
  textArea: JTextArea,
): JButton {
  val button = JButton("show")
  button.addActionListener {
    val pane = JOptionPane(
      textField,
      JOptionPane.PLAIN_MESSAGE,
      JOptionPane.OK_CANCEL_OPTION,
    )
    val dialog = pane.createDialog(textArea.rootPane, "Input Text")
    val wl = object : WindowAdapter() {
      override fun windowOpened(e: WindowEvent) {
        textField.requestFocusInWindow()
      }
    }
    dialog.addWindowListener(wl)
    dialog.isVisible = true
    val selectedValue = pane.value
    var result = JOptionPane.CLOSED_OPTION
    if (selectedValue is Int) {
      result = selectedValue
    }
    if (result == JOptionPane.OK_OPTION) {
      textArea.text = textField.text
    }
  }
  return button
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

private class FocusHierarchyListener : HierarchyListener {
  override fun hierarchyChanged(e: HierarchyEvent) {
    val c = e.component
    val flg = e.changeFlags.toInt()
    if (flg and HierarchyEvent.SHOWING_CHANGED != 0 && c.isShowing) {
      EventQueue.invokeLater { c.requestFocusInWindow() }
    }
  }
}

// https://community.oracle.com/thread/1354218 Input focus
private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    // not needed
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    // not needed
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
