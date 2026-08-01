package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*

fun createUI(): Component {
  // UIManager.put("FileChooser.readOnly", true)
  val log = JTextArea()

  val chooser0 = JFileChooser()
  val button0 = JButton("default")
  button0.addActionListener {
    setViewTypeDetails(chooser0)
    // stream(chooser0)
    //     .filter(Predicate<Component> { JTable::class.java!!.isInstance(it) })
    //     .map(Function<Component, JTable> { JTable::class.java!!.cast(it) })
    //     .findFirst()
    //     .ifPresent { table -> append(log, "isEditing: ${table.isEditing()}") }

    // stream(chooser0)
    //     .filter(JTable::class.java::isInstance)
    //     .map(JTable::class.java::cast)
    //     .findFirst()
    //     .ifPresent { append(log, "isEditing: ${it.isEditing()}") }
    descendants(chooser0)
      .filterIsInstance<JTable>()
      .firstNotNullOf { append(log, "isEditing: ${it.isEditing}") }

    val retValue = chooser0.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      append(log, chooser0.selectedFile.absolutePath)
    }
  }

  val chooser1 = JFileChooser()
  val button1 = JButton("removeEditor")
  button1.addActionListener {
    setViewTypeDetails(chooser1)
    // stream(chooser1)
    //     .filter(Predicate<Component> { JTable::class.java!!.isInstance(it) })
    //     .map(Function<Component, JTable> { JTable::class.java!!.cast(it) })
    //     .peek { table -> append(log, "isEditing: ${table.isEditing()}") }
    //     .findFirst().filter(Predicate<JTable> { it.isEditing() })
    //     .ifPresent(Consumer<JTable> { it.removeEditor() })
    // stream(chooser1)
    //   .filter(JTable::class.java::isInstance)
    //   .map(JTable::class.java::cast)
    //   .peek { table -> append(log, "isEditing: ${table.isEditing()}") }
    //   .findFirst()
    //   // .filter { it.isEditing() }
    //   .filter(JTable::isEditing)
    //   // .ifPresent { it.removeEditor() }
    //   .ifPresent(JTable::removeEditor)

    descendants(chooser1)
      .filterIsInstance<JTable>()
      .firstOrNull()
      ?.removeEditor()

    val retValue = chooser1.showOpenDialog(log.rootPane)
    if (retValue == JFileChooser.APPROVE_OPTION) {
      append(log, chooser1.selectedFile.absolutePath)
    }
  }

  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JFileChooser(viewTypeDetails)")
  p.add(button0)
  p.add(button1)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setViewTypeDetails(fileChooser: JFileChooser) {
  fileChooser.actionMap["viewTypeDetails"]?.actionPerformed(
    ActionEvent(fileChooser, ActionEvent.ACTION_PERFORMED, "viewTypeDetails"),
  )
}

private fun append(
  log: JTextArea,
  str: String,
) {
  log.append(str + "\n")
  log.caretPosition = log.document.length
}

// fun stream(parent: Container): Stream<Component> {
//   // return Stream.of(*parent.getComponents())
//   //   .filter(Predicate<Component> { Container::class.java.isInstance(it) })
//   //   .map { stream(Container::class.java.cast(it)) }
//   //   .reduce(Stream.of(parent), BinaryOperator<Stream<Component>> { a, b -> Stream.concat(a, b) })
//   // return Stream.of(*parent.getComponents())
//   return Arrays.stream(parent.getComponents())
//     .filter(Container::class.java::isInstance)
//     .map { c -> stream(Container::class.java.cast(c)) }
//     .reduce(Stream.of<Component>(parent), { a, b -> Stream.concat<Component>(a, b) }) // OK
//     // OK: .reduce(Stream.of(parent), BinaryOperator<Stream<Component>>{ a, b -> Stream.concat(a, b) })
//     // NG: .reduce(Stream.of(parent), Stream::concat)
// }

// fun stream(parent: Container): Stream<Component> = Arrays.stream(parent.getComponents())
//     .filter(Container::class.java::isInstance)
//     .map { c -> stream(Container::class.java.cast(c)) }
//     .reduce(Stream.of<Component>(parent), { a, b -> Stream.concat<Component>(a, b) })

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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
