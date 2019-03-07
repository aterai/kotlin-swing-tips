package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val p1 = JPanel(GridLayout(1, 0, 2, 2)).also {
      it.setBorder(BorderFactory.createTitledBorder("GridLayout"))
      it.add(JScrollPane(JTable(6, 3)))
      it.add(JScrollPane(JTree()))
      it.add(JScrollPane(JTextArea("JTextArea")))
    }

    val p2 = JPanel(GridBagLayout()).also {
      it.setBorder(BorderFactory.createTitledBorder("GridBagLayout"))
      val c = GridBagConstraints()
      c.insets = Insets(5, 5, 5, 0)
      c.fill = GridBagConstraints.BOTH
      c.weightx = 1.0
      c.weighty = 1.0
      // c.gridx = RELATIVE
      // c.gridx = 0
      it.add(JScrollPane(JTable(6, 3)), c)
      // c.gridx = 1
      it.add(JScrollPane(JTree()), c)
      // c.gridx = 2
      it.add(JScrollPane(JTextArea("JTextArea")), c)
    }

    val button = JButton("rotate")
    button.setFocusable(false)
    button.addActionListener {
      // p1.setComponentZOrder(p1.getComponent(p1.getComponentCount() - 1), 0)
      p1.setComponentZOrder(p1.getComponents().last(), 0)
      p2.setComponentZOrder(p2.getComponents().last(), 0)
      revalidate()
    }

    add(JPanel(GridLayout(2, 1)).also {
      it.add(p1)
      it.add(p2)
    })
    add(button, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
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
