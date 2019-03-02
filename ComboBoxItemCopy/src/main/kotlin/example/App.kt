package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent

class MainPanel : JPanel(BorderLayout()) {
  init {
    val copyKey = "copy"

    val combo1 = JComboBox<String>(makeModel(5))
    val copy = object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val combo = e.getSource() as JComboBox<*>
        combo.getSelectedItem()?.also {
          val contents = StringSelection(it.toString())
          val clipboard = Toolkit.getDefaultToolkit().getSystemClipboard()
          clipboard.setContents(contents, null)
          println(it)
        }
      }
    }
    val am = combo1.getActionMap()
    am.put(copyKey, copy)
    val modifiers = InputEvent.CTRL_DOWN_MASK
    val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, modifiers)
    val im = combo1.getInputMap(JComponent.WHEN_FOCUSED)
    im.put(keyStroke, copyKey)
    val popup = JPopupMenu()
    popup.add(copyKey).addActionListener { e ->
      val o = popup.getInvoker()
      val c = o as? JComboBox<*> ?: SwingUtilities.getAncestorOfClass(JComboBox::class.java, o as Component)
      (c as? JComboBox<*>)?.also {
        val a = it.getActionMap().get(copyKey)
        a.actionPerformed(ActionEvent(it, e.getID(), e.getActionCommand()))
        // KeyEvent keyEvent = new KeyEvent(c, 0, 0, 0, 0, 'C');
        // SwingUtilities.notifyAction(a, keyStroke, keyEvent, it, modifiers);
      }
    }
    combo1.setComponentPopupMenu(popup)

    val combo2 = JComboBox<String>(makeModel(10))
    combo2.setEditable(true)
    val field = combo2.getEditor().getEditorComponent() as JTextField
    field.setComponentPopupMenu(TextFieldPopupMenu())

    val box = Box.createVerticalBox()
    box.add(makeTitledPanel("Default:", JComboBox<String>(makeModel(0))))
    box.add(Box.createVerticalStrut(5))
    box.add(makeTitledPanel("Editable: false, JPopupMenu, Ctrl+C", combo1))
    box.add(Box.createVerticalStrut(5))
    box.add(makeTitledPanel("Editable: true, JPopupMenu, Ctrl+C", combo2))
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(box, BorderLayout.NORTH)

    val textArea = JTextArea()
    textArea.setComponentPopupMenu(TextFieldPopupMenu())
    add(JScrollPane(textArea))
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).apply {
    setBorder(BorderFactory.createTitledBorder(title))
    add(c)
  }

  private fun makeModel(start: Int) = DefaultComboBoxModel<String>().apply {
    for (i in start until start + 5) {
      addElement("item: $i")
    }
  }
}

internal class TextFieldPopupMenu : JPopupMenu() {
  private val cutAction = DefaultEditorKit.CutAction()
  private val copyAction = DefaultEditorKit.CopyAction()
  private val pasteAction = DefaultEditorKit.PasteAction()
  private val deleteAction = object : AbstractAction("delete") {
    override fun actionPerformed(e: ActionEvent) {
      (getInvoker() as? JTextComponent)?.replaceSelection(null)
    }
  }

  init {
    add(cutAction)
    add(copyAction)
    add(pasteAction)
    add(deleteAction)
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTextComponent) {
      val hasSelectedText = c.getSelectedText() != null
      cutAction.setEnabled(hasSelectedText)
      copyAction.setEnabled(hasSelectedText)
      deleteAction.setEnabled(hasSelectedText)
      super.show(c, x, y)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
