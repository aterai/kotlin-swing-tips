package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val sp = JSplitPane().also {
    it.setLeftComponent(JScrollPane(JTree()))
    it.setRightComponent(JScrollPane(JTable(6, 3)))
    it.setResizeWeight(.4)
  }

  val check = JCheckBox("Keep DividerLocation", true)

  val button = JButton("swap")
  button.setFocusable(false)
  button.addActionListener {
    val left = sp.getLeftComponent()
    val right = sp.getRightComponent()

    // sp.removeAll(); // Divider is also removed
    sp.remove(left)
    sp.remove(right)
    // or:
    // sp.setLeftComponent(null);
    // sp.setRightComponent(null);

    sp.setLeftComponent(right)
    sp.setRightComponent(left)

    sp.setResizeWeight(1.0 - sp.getResizeWeight())
    if (check.isSelected()) {
      sp.setDividerLocation(sp.getDividerLocation())
    }
  }

  val box = Box.createHorizontalBox().also {
    it.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5))
    it.add(check)
    it.add(Box.createHorizontalGlue())
    it.add(button)
  }

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.add(box, BorderLayout.SOUTH)
    it.setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
