import jade.core.AID;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

class MotoristaGui extends JFrame {	

	private static final long serialVersionUID = 7748742724544470144L;

	private MotoristaAgent myAgent;
	
	private JTextField localDeAtuacao, preco;
	
	MotoristaGui(MotoristaAgent a) {
		super(a.getLocalName());
		
		myAgent = a;
		
		JPanel p = new JPanel();
		p.setLayout(new GridLayout(2, 2));
		p.add(new JLabel("Local de atuação:"));
		localDeAtuacao = new JTextField(15);
		p.add(localDeAtuacao);
		p.add(new JLabel("Preço:"));
		preco = new JTextField(15);
		p.add(preco);
		getContentPane().add(p, BorderLayout.CENTER);
		
		JButton addButton = new JButton("Add");
		addButton.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ev) {
				try {
					String local = localDeAtuacao.getText().trim();
					String precoLocal = preco.getText().trim();
					myAgent.atualizarLocaisDeAtuacao(local, Integer.parseInt(precoLocal));
					localDeAtuacao.setText("");
					preco.setText("");
				}
				catch (Exception e) {
					JOptionPane.showMessageDialog(MotoristaGui.this, "Invalid values. "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); 
				}
			}
		} );
		p = new JPanel();
		p.add(addButton);
		getContentPane().add(p, BorderLayout.SOUTH);
	
		addWindowListener(new	WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				myAgent.doDelete();
			}
		} );
		
		setResizable(false);
	}
	
	public void showGui() {
		pack();
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int centerX = (int)screenSize.getWidth() / 2;
		int centerY = (int)screenSize.getHeight() / 2;
		setLocation(centerX - getWidth() / 2, centerY - getHeight() / 2);
		super.setVisible(true);
	}	
}