package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.io.File
import javax.swing.*

fun makeUI(): Component {
  val button0 = JButton("Default")
  button0.addActionListener {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    chooser.showOpenDialog(button0.rootPane)
  }

  val button1 = JButton("System.getenv(\"SystemDrive\")")
  button1.addActionListener {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    // https://stackoverflow.com/questions/10524376/how-to-make-jfilechooser-default-to-computer-view-instead-of-my-documents
    val systemDrive = File(System.getenv("SystemDrive") + File.separatorChar)
    val pcDir = chooser.fileSystemView.getParentDirectory(systemDrive)
    chooser.currentDirectory = pcDir
    chooser.showOpenDialog(button1.rootPane)
  }

  val button2 = JButton("ShellFolder.get(\"fileChooserShortcutPanelFolders\")")
  button2.addActionListener {
    val chooser = JFileChooser()
    chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
    val fsv = chooser.fileSystemView
    val files = sun.awt.shell.ShellFolder
      .get("fileChooserShortcutPanelFolders") as? Array<*>
    chooser.addHierarchyListener { e ->
      val c = e.component
      val b = e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L
      if (b && c.isShowing && files != null) {
        descendants(chooser)
          .filterIsInstance<JToggleButton>()
          .firstOrNull { fsv.getSystemDisplayName(files[3] as? File) == it.text }
          ?.doClick()
      }
    }
    chooser.showOpenDialog(button2.rootPane)
  }

  return JPanel().also {
    it.add(button0)
    it.add(button1)
    it.add(button2)
    it.preferredSize = Dimension(320, 240)
  }
}

fun descendants(parent: Container): List<Component> =
  parent.components
    .filterIsInstance<Container>()
    .flatMap { listOf(it) + descendants(it) }

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
