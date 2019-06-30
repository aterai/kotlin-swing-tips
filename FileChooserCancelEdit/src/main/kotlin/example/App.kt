package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  // UIManager.put("FileChooser.readOnly", Boolean.TRUE);
  val log = JTextArea()

  val fileChooser0 = JFileChooser()
  val button0 = JButton("default")
  button0.addActionListener {
    setViewTypeDetails(fileChooser0)
    // stream(fileChooser0)
    //     .filter(Predicate<Component> { JTable::class.java!!.isInstance(it) })
    //     .map(Function<Component, JTable> { JTable::class.java!!.cast(it) })
    //     .findFirst()
    //     .ifPresent { table -> append(log, "isEditing: ${table.isEditing()}") }

    // stream(fileChooser0)
    //     .filter(JTable::class.java::isInstance)
    //     .map(JTable::class.java::cast)
    //     .findFirst()
    //     .ifPresent { append(log, "isEditing: ${it.isEditing()}") }
    children(fileChooser0).filterIsInstance(JTable::class.java)
        .firstOrNull()?.also { append(log, "isEditing: ${it.isEditing()}") }

    val retvalue = fileChooser0.showOpenDialog(log.getRootPane())
    if (retvalue == JFileChooser.APPROVE_OPTION) {
      append(log, fileChooser0.getSelectedFile().getAbsolutePath())
    }
  }

  val fileChooser1 = JFileChooser()
  val button1 = JButton("removeEditor")
  button1.addActionListener {
    setViewTypeDetails(fileChooser1)
    // stream(fileChooser1)
    //     .filter(Predicate<Component> { JTable::class.java!!.isInstance(it) })
    //     .map(Function<Component, JTable> { JTable::class.java!!.cast(it) })
    //     .peek { table -> append(log, "isEditing: ${table.isEditing()}") }
    //     .findFirst().filter(Predicate<JTable> { it.isEditing() })
    //     .ifPresent(Consumer<JTable> { it.removeEditor() })
    // stream(fileChooser1)
    //   .filter(JTable::class.java::isInstance)
    //   .map(JTable::class.java::cast)
    //   .peek { table -> append(log, "isEditing: ${table.isEditing()}") }
    //   .findFirst()
    //   // .filter { it.isEditing() }
    //   .filter(JTable::isEditing)
    //   // .ifPresent { it.removeEditor() }
    //   .ifPresent(JTable::removeEditor)

    children(fileChooser1).filterIsInstance(JTable::class.java)
        // .firstOrNull()?.apply(JTable::removeEditor)
        .firstOrNull()?.also {
          println(it)
          it.removeEditor()
        }

    val retvalue = fileChooser1.showOpenDialog(log.getRootPane())
    if (retvalue == JFileChooser.APPROVE_OPTION) {
      append(log, fileChooser1.getSelectedFile().getAbsolutePath())
    }
  }

  val p = JPanel()
  p.setBorder(BorderFactory.createTitledBorder("JFileChooser(viewTypeDetails)"))
  p.add(button0)
  p.add(button1)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.setPreferredSize(Dimension(320, 240))
  }
}

private fun setViewTypeDetails(fileChooser: JFileChooser) {
  fileChooser.getActionMap().get("viewTypeDetails")?.actionPerformed(
      ActionEvent(fileChooser, ActionEvent.ACTION_PERFORMED, "viewTypeDetails"))
}

private fun append(log: JTextArea, str: String) {
  log.append(str + "\n")
  log.setCaretPosition(log.getDocument().getLength())
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

fun children(parent: Container): List<Component> = parent.getComponents()
    .filterIsInstance(Container::class.java)
    .map { children(it) }
    .fold(listOf<Component>(parent)) { a, b -> a + b }

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
