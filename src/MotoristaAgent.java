import java.util.Hashtable;

import jade.core.*; 
import jade.core.behaviours.*; 
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPAAgentManagement.ServiceDescription; 
import jade.domain.FIPAAgentManagement.DFAgentDescription; 
import jade.domain.DFService; 
import jade.domain.FIPAException;

public class MotoristaAgent extends Agent {
	// The localDeAtuacao of books for sale (maps the title of a book to its price)
	private Hashtable localDeAtuacao;

	// Put agent initializations here
	protected void setup() {
		// Create the localDeAtuacao
		localDeAtuacao = new Hashtable();

		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("realizar-viagem");
		sd.setName("JADE-uber");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
	}

	// Put agent clean-up operations here
	protected void takeDown() {
		// Deregister from the yellow pages
		try {
			DFService.deregister(this);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		// Printout a dismissal message
		System.out.println("Motorista "+getAID().getName()+" pronto.");
	}

	/**
     This is invoked by the GUI when the user adds a new book for sale
	 */
	public void atualizarLocaisDeAtuacao(final String local, final int preco) {
		addBehaviour(new OneShotBehaviour() {
			public void action() {
				localDeAtuacao.put(local, new Integer(preco));
				System.out.println("Viagem para " + local+". Preço = "+preco);
			}
		} );
	}
}
