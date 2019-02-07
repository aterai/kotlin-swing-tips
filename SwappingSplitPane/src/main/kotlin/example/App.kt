package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val sp = JSplitPane().apply {
      setLeftComponent(JScrollPane(JTree()))
      setRightComponent(JScrollPane(JTable(6, 3)))
      setResizeWeight(.4)
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

    val box = Box.createHorizontalBox().apply {
      setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5))
      add(check)
      add(Box.createHorizontalGlue())
      add(button)
    }

    add(sp)
    add(box, BorderLayout.SOUTH)
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
